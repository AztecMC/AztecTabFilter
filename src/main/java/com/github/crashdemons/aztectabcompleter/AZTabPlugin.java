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
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
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
    public volatile boolean loaded = false;
    private volatile boolean ready = false;
    
    private ConcurrentHashMap<InetSocketAddress,Pair<LocalDateTime,PacketContainer>> packetQueue = new ConcurrentHashMap<>();
    //don't store Player from packet event since it will be a "temporary" player object that doesn't support every method.
    
    private BukkitTask expireQueueEntriesTask=null;
    private BukkitTask sendQueueEntriesTask=null;
    
    private long expirationSeconds=60;
    private long expirationInterval=60;
    private long tryUnsentPacketsInterval=10;
    
    private boolean kickEarlyJoins=true;
    private String kickMessage="The server is still loading - check back in a moment!";
    
    private boolean sendPacket(Player playerDestination, PacketContainer packet){
        if(playerDestination==null) return false;
        if(!playerDestination.isOnline()) return false;
        String name = playerDestination.getName();
        if(name==null) name = "[null]";
        try{
            log("Sending commands to "+name);
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
        
        kickEarlyJoins = getConfig().getBoolean("kick-early-joins");
        kickMessage = getConfig().getString("kick-message");
        expirationSeconds = getConfig().getLong("queue-expiration-seconds");
        expirationInterval = getConfig().getLong("queue-expiration-check-seconds");
        tryUnsentPacketsInterval=getConfig().getLong("queue-try-unsent-seconds");
        log("commands queue unsent retry time: "+tryUnsentPacketsInterval+"s");
        log("commands queue expiration time: "+expirationSeconds+"s");
        log("commands queue check interval: "+expirationInterval+"s");
    }
    
    
    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        log("Disabling...");
        if(expireQueueEntriesTask!=null) expireQueueEntriesTask.cancel();
        if(sendQueueEntriesTask!=null) sendQueueEntriesTask.cancel();
        loaded=false;
        log("Disabed.");
    }
    
    @Override
    public void onLoad() {
        log("Loading... v"+this.getDescription().getVersion());
        loadConfig();
        log("Loaded config.");
        protocolManager = ProtocolLibrary.getProtocolManager();
        loaded=true;
        
        createInitialCommandsFilter();
        
        

        
    }
    @Override
    public void onEnable() {
        log("Enabling... v"+this.getDescription().getVersion());
        getServer().getPluginManager().registerEvents(this, this);
        if(expireQueueEntriesTask!=null) expireQueueEntriesTask.cancel();
        if(sendQueueEntriesTask!=null) sendQueueEntriesTask.cancel();
        expireQueueEntriesTask = new BukkitRunnable() {
            public void run() {
                LocalDateTime now = LocalDateTime.now();
                packetQueue.entrySet().removeIf((entry)->entry.getValue().getKey().plusSeconds(expirationSeconds).isBefore(LocalDateTime.now())
                );
            }
        }.runTaskTimer(this,expirationInterval*TPS,expirationInterval*TPS);
        sendQueueEntriesTask = new BukkitRunnable() {
            public void run() {
                Bukkit.getOnlinePlayers().stream().forEach(
                        (player) -> {
                            if(player==null) return;
                            processQueueFor(player);
                        }
                );
            }
        }.runTaskTimer(this,tryUnsentPacketsInterval*TPS,tryUnsentPacketsInterval*TPS);
        loaded=true;
        ready = true;
        log("Enabled.");
    }
    
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!loaded) return true;
        if (cmd.getName().equalsIgnoreCase("aztabreload")) {
            if(!sender.hasPermission("aztectabcompleter.reload")){ sender.sendMessage("You don't have permission to do this."); return true; }
            loadConfig();
            sender.sendMessage("[AZTab] Config reloaded.");
            return true;
        }
        return false;
    }
    
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event){
        if(!loaded) return;
        if(!ready){
            if(kickEarlyJoins){
                event.setKickMessage(kickMessage);
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            }
        }
    }
   
    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event){
        //log("playerjoinevent");
        if(!loaded) return;
        if(!ready) return;
        Player player = event.getPlayer();
        //log("trying packets for joined player");
        processQueueFor(player);
    }
    
    private void processQueueFor(Player playerDestination){
        if(playerDestination==null) return;
        InetSocketAddress addr = playerDestination.getAddress();
        String name = playerDestination.getName();
        if(addr==null) return;
        //log("Player joined: "+uuid);
        boolean bypassFiltering=playerDestination.hasPermission("aztectabcompleter.bypass");
        Pair<LocalDateTime,PacketContainer> record = packetQueue.remove(addr);
        if(record==null) return;
        PacketContainer packet = record.getValue();
        if(packet==null) return;
        PacketContainer packet_filtered;
        if(bypassFiltering){
            packet_filtered=packet;
            //log("player "+name+" is exempt from command filtering");
        }else{
            packet_filtered=filterPacketFor(playerDestination,packet);
            //log("filtered packet for player "+name);
        }
        sendPacket(playerDestination,packet_filtered);
    }
    private boolean queuePacketFor(Player playerDestination, PacketContainer epacket){
        InetSocketAddress addr = playerDestination.getAddress();
        if(addr==null){
            getLogger().warning("Could not queue packet for player with null address: "+playerDestination.toString());
            return false;
        }
        packetQueue.put(addr, new Pair<LocalDateTime,PacketContainer>(LocalDateTime.now(),epacket));
        //log("Queued commands for "+uuid);
        return true;
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
           
            if(!pl.loaded) return;
            event.setCancelled(true);//prevent default tabcomplete
            Player playerDestination = event.getPlayer();
            if(playerDestination==null) return;
            
            //pl.log("Intercepted Commands packet, filtering...");
            
            PacketContainer epacket = event.getPacket();//get the outgoing spigot packet containing the command list
            queuePacketFor(playerDestination,epacket);
            
            
        }

    });
    }
    
}
