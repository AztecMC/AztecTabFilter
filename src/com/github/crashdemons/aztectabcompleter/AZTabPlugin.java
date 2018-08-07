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
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

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
    private ProtocolManager protocolManager;
    public volatile boolean enabled = false;
    private volatile HashSet<String> visibleCommands;

    public AZTabPlugin() {
        this.visibleCommands = new HashSet<>();
    }
    
    private void log(String s){
        getLogger().info(s);
    }
    
    private void loadConfig(){
        saveDefaultConfig();//fails silently if config exists
        reloadConfig();
        visibleCommands = new HashSet<>( getConfig().getStringList("visible-commands") );
    }
    
    
    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        log("Disabling...");
        enabled=false;
        log("Disabed.");
    }
    
    @Override
    public void onEnable() {
        log("Enabling... v"+this.getDescription().getVersion());
        loadConfig();
        log("Loaded "+visibleCommands.size()+" visible commands.");
        getServer().getPluginManager().registerEvents(this, this);
        protocolManager = ProtocolLibrary.getProtocolManager();
        createInitialCommandsFilter();
        //createTabCompleteOverride();
        enabled=true;
        log("Enabled.");
    }
    
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("aztabreload")) {
            if(!sender.hasPermission("aztectabcompleter.reload")){ sender.sendMessage("You don't have permission to do this."); return true; }
            loadConfig();
            sender.sendMessage("[AZTab] Config reloaded with "+visibleCommands.size()+" commands.");
            return true;
        }
        return false;
    }

    private void createInitialCommandsFilter(){
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST, new PacketType[] { PacketType.Play.Server.COMMANDS }) {

        @Override
        public void onPacketSending (PacketEvent event) {
            AZTabPlugin pl = (AZTabPlugin) this.plugin;
            
            if(!pl.enabled) return;
            pl.log("Intercepted Commands packet, filtering...");
            
            //the new Commands packet syntax contains a RootNode object containing multiple CommandNode objects inside in the form of a list
            //CommandNode is difficult to construct, so instead we just selectively remove them from the collection.
            PacketContainer epacket = event.getPacket();//get the outgoing spigot packet containing the command list
            RootCommandNode rcn = epacket.getSpecificModifier(RootCommandNode.class).read(0);//get the Root object
            //this.plugin.getLogger().info("RCN Name: "+rcn.getName());
            //this.plugin.getLogger().info("RCN Usage: "+rcn.getUsageText());
            
            Collection<CommandNode<Object>> children = rcn.getChildren();
            //this.plugin.getLogger().info("RCN Children: "+children.size());
            Iterator<CommandNode<Object>> iterator = children.iterator();
            while (iterator.hasNext()) {
                CommandNode<Object> cn = iterator.next();
                //this.plugin.getLogger().info("   CN Name: "+cn.getName());
                //this.plugin.getLogger().info("   CN Usage: "+cn.getUsageText());
                if( ! visibleCommands.contains(cn.getName()) )
                    iterator.remove();
            }
           
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.COMMANDS);
            packet.getSpecificModifier(RootCommandNode.class).write(0, rcn);//write the modified root object into a new packet
            try{
                ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), packet, false);//send packet - disable further filtering.
            }catch(InvocationTargetException e){
                e.printStackTrace();
            }
            event.setCancelled(true);//prevent default tabcomplete
            
        }

    });
    }
    
}

/*
Unused code that could be implemented to override per-command autocomplete
=========================================================================================
    private void createTabCompleteOverride(){
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST, new PacketType[] { PacketType.Play.Server.TAB_COMPLETE }) {

            @Override
            public void onPacketSending (PacketEvent event) {
                if(!enabled) return;
                this.plugin.getLogger().info("TAB ARGUMENT SUGGESTIONS \n"+event.getPacket().toString());
                //event.setCancelled(true);
            }

        });
    }
    */

            /*
Personal notes and reverse-engineering the 1.13 CommandNodes/Suggestions

========================================================================================================

            com.mojang.brigadier.tree.RootCommandNode rcn = epacket.getSpecificModifier(com.mojang.brigadier.tree.RootCommandNode.class).read(0);
            try{Thread.sleep(1000);}catch(Exception e){}
            this.plugin.getLogger().info("RCN Name: "+rcn.getName());
            try{Thread.sleep(1000);}catch(Exception e){}
            this.plugin.getLogger().info("RCN Usage: "+rcn.getUsageText());
            try{Thread.sleep(1000);}catch(Exception e){}
            
            
            
            Collection<CommandNode<Object>> children = rcn.getChildren();
            this.plugin.getLogger().info("RCN Children: "+children.size());
            try{Thread.sleep(1000);}catch(Exception e){}
            for (CommandNode<Object> cn : children) {
                this.plugin.getLogger().info("   CN Name: "+cn.getName());
                
                try{Thread.sleep(500);}catch(Exception e){}
                this.plugin.getLogger().info("   CN Usage: "+cn.getUsageText());
                try{Thread.sleep(500);}catch(Exception e){}
            }
            */
            




/*
            event.setCancelled(true);

            //PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TAB_COMPLETE);

            PacketContainer packet = new PacketContainer(PacketType.Play.Server.TAB_COMPLETE);
            this.plugin.getLogger().info("[AZTab] "+packet.toString());
            
            
            StringRange sr = new StringRange(0,4);
            Suggestion s = new Suggestion(sr,"/xxx");
            
            
            //packet.getIntegers().write(0, 1);
            Suggestions ss = packet.getSpecificModifier(Suggestions.class).read(0);
            ss.getList().add(s);
            packet.getSpecificModifier(Suggestions.class).write(0,ss);
            //packet.getStringArrays().write(0,  completions.toArray(new String[0])  );
*/   
           