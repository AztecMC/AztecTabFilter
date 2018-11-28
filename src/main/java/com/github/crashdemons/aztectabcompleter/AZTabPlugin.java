package com.github.crashdemons.aztectabcompleter;


import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.brigadier.tree.CommandNode;


import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.crashdemons.aztectabcompleter.filters.FilterArgs;
import com.github.crashdemons.aztectabcompleter.filters.FilterSet;
import com.github.crashdemons.util.Pair;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author crash
 */
public class AZTabPlugin extends JavaPlugin implements Listener {
    //internal variables
    private static final int TPS = 20;
    private ProtocolManager protocolManager;
    private FilterSet filters;
    
    //runtime behavior variables
    public volatile boolean enabled = false;
    
    private ConcurrentHashMap<UUID,Pair<LocalDateTime,PacketContainer>> packetQueue = new ConcurrentHashMap<>();//don't store Temporary Player object
    
    private BukkitTask expireQueueEntriesTask=null;
    
    private long expirationSeconds=1;
    
    
    private boolean sendPacket(Player playerDestination, PacketContainer packet){
        if(playerDestination==null) return false;
        if(!playerDestination.isOnline()) return false;
        String name = playerDestination.getName();
        if(name==null) name = "[null]";
        try{
            log("Sending filtered commands to "+name);
            ProtocolLibrary.getProtocolManager().sendServerPacket(playerDestination, packet, false);//send packet - disable further filtering.
            return true;
        }catch(IllegalArgumentException e){
            log("Problem sending packet to " + name +" "+playerDestination.getUniqueId());
        }catch(InvocationTargetException e){
            e.printStackTrace();
        }
        return false;
    }
    
    public AZTabPlugin() {
        filters = new FilterSet(this);
    }
    
    private void log(String s){
        getLogger().info(s);
    }
    
    private void loadConfig(){
        saveDefaultConfig();//fails silently if config exists
        reloadConfig();
        
        filters.load(getConfig());
    }
    
    
    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        log("Disabling...");
        if(expireQueueEntriesTask!=null) expireQueueEntriesTask.cancel();
        enabled=false;
        log("Disabed.");
    }
    
    @Override
    public void onEnable() {
        log("Enabling... v"+this.getDescription().getVersion());
        loadConfig();
        log("Loaded config.");
        getServer().getPluginManager().registerEvents(this, this);
        protocolManager = ProtocolLibrary.getProtocolManager();
        createInitialCommandsFilter();
        //createTabCompleteOverride();
        
        
        expirationSeconds = getConfig().getLong("queue-expiration-seconds");
        long expirationInterval = getConfig().getLong("queue-expiration-check-seconds");
        
        if(expireQueueEntriesTask!=null) expireQueueEntriesTask.cancel();
        expireQueueEntriesTask = new BukkitRunnable() {
            public void run() {
                LocalDateTime now = LocalDateTime.now();
                packetQueue.entrySet().removeIf((entry)->entry.getValue().getKey().plusSeconds(expirationSeconds).isBefore(LocalDateTime.now())
                );
            }
        }.runTaskTimer(this,expirationInterval*TPS,expirationInterval*TPS);
        
        
        enabled=true;
        log("Enabled.");
    }
    
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!enabled) return true;
        if (cmd.getName().equalsIgnoreCase("aztabreload")) {
            if(!sender.hasPermission("aztectabcompleter.reload")){ sender.sendMessage("You don't have permission to do this."); return true; }
            loadConfig();
            sender.sendMessage("[AZTab] Config reloaded.");
            return true;
        }
        return false;
    }
   
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event){
        //log("playerjoinevent");
        if(!enabled) return;
        Player player = event.getPlayer();
        if(player==null) return;
        UUID uuid = player.getUniqueId();
        if(uuid==null) return;
        //log("Player joined: "+uuid);
        
        Pair<LocalDateTime,PacketContainer> record = packetQueue.remove(uuid);
        if(record==null) return;
        PacketContainer packet = record.getValue();
        if(packet!=null) sendPacket(player,packet);
    }
    
    private PacketContainer filterPacketFor(Player playerDestination, PacketContainer epacket){
        //the new Commands packet syntax contains a RootNode object containing multiple CommandNode objects inside in the form of a list
        //CommandNode is difficult to construct, so instead we just selectively remove them from the collection.
        RootCommandNode rcn = epacket.getSpecificModifier(RootCommandNode.class).read(0);//get the Root object
        //this.plugin.getLogger().info("RCN Name: "+rcn.getName());
        //this.plugin.getLogger().info("RCN Usage: "+rcn.getUsageText());
        @SuppressWarnings("unchecked")
        Collection<CommandNode<Object>> children = rcn.getChildren();
        //this.plugin.getLogger().info("RCN Children: "+children.size());
        Iterator<CommandNode<Object>> iterator = children.iterator();
        while (iterator.hasNext()) {
            CommandNode<Object> cn = iterator.next();
            //this.plugin.getLogger().info("   CN Name: "+cn.getName());
            //this.plugin.getLogger().info("   CN Usage: "+cn.getUsageText());
            if(!filters.filter(new FilterArgs(playerDestination,cn.getName())).isAllowed)
                iterator.remove();
        }
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.COMMANDS);
        packet.getSpecificModifier(RootCommandNode.class).write(0, rcn);//write the modified root object into a new packet
        return packet;
    }

    private void createInitialCommandsFilter(){
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST, new PacketType[] { PacketType.Play.Server.COMMANDS }) {

        @Override
        public void onPacketSending (PacketEvent event) {
            AZTabPlugin pl = (AZTabPlugin) this.plugin;
           
            if(!pl.enabled) return;
            Player playerDestination = event.getPlayer();
            if(playerDestination==null) return;
            if(playerDestination.hasPermission("aztectabcompleter.bypass")) return;
            
            
            pl.log("Intercepted Commands packet, filtering...");
            
            PacketContainer epacket = event.getPacket();//get the outgoing spigot packet containing the command list
            PacketContainer packet = filterPacketFor(playerDestination,epacket);
            
            UUID uuid = playerDestination.getUniqueId();
            packetQueue.put(uuid, new Pair<LocalDateTime,PacketContainer>(LocalDateTime.now(),epacket));
            pl.log("Queued packet for "+uuid);
            //sendPacket(playerDestination, packet);
            
            event.setCancelled(true);//prevent default tabcomplete
            
        }

    });
    }
    
}
