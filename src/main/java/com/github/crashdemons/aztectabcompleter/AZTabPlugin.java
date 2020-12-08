package com.github.crashdemons.aztectabcompleter;

import com.github.crashdemons.aztectabcompleter.filters.FilterArgs;
import com.github.crashdemons.aztectabcompleter.filters.FilterSet;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerLoginEvent;
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

    //internal variables
    private final FilterSet filters;

    //runtime behavior variables
    public volatile boolean loaded = false;
    private volatile boolean ready = false;

    private boolean kickEarlyJoins = true;
    private String kickMessage = "The server is still loading - check back in a moment!";

    private boolean blockCommands = false;
    private String blockMessage = "You are not allowed to use that command!";

    private boolean dumpFiltering = false;

    public AZTabPlugin() {
        filters = new FilterSet(this);
    }

    private void log(String s) {
        getLogger().info(s);
    }

    private void loadConfig() {
        saveDefaultConfig();//fails silently if config exists
        reloadConfig();

        filters.load(getConfig());
        kickEarlyJoins = getConfig().getBoolean("kick-early-joins");
        kickMessage = getConfig().getString("kick-message");

        blockCommands = getConfig().getBoolean("block-commands");
        String str = getConfig().getString("block-message");
        if (str != null) {
            blockMessage = ChatColor.translateAlternateColorCodes('&', str);
        }
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {
        log("Disabling...");

        loaded = false;
        log("Disabed.");
    }

    @Override
    public void onLoad() {
        log("Loading... v" + this.getDescription().getVersion());
        loadConfig();
        loaded = true;
        log("Loaded config.");
    }

    @Override
    public void onEnable() {
        log("Enabling... v" + this.getDescription().getVersion());
        getServer().getPluginManager().registerEvents(this, this);
        loaded = true;
        ready = true;
        log("Enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!loaded) {
            return true;
        }
        String command = cmd.getName();
        if (command.equalsIgnoreCase("aztabreload")) {
            if (!sender.hasPermission("aztectabcompleter.reload")) {
                sender.sendMessage("You don't have permission to do this.");
                return true;
            }
            loadConfig();
            sender.sendMessage("[AZTab] Config reloaded.");
            return true;
        }else if (command.equalsIgnoreCase("aztabdump")) {
            if (!sender.hasPermission("aztectabcompleter.dump")) {
                sender.sendMessage("You don't have permission to do this.");
                return true;
            }
            dumpFiltering = !dumpFiltering;
            String dumpResult = dumpFiltering? "Enabled" : "Disabled";
            sender.sendMessage("[AZTab] Console Filter Logging: "+dumpResult);
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandSuggestion(PlayerCommandSendEvent event) {
        if(event.isAsynchronous()){
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("aztectabcompleter.bypass")) {
            if(dumpFiltering) getLogger().info(player.getName()+" bypassed filtering by permission.");
            return;
        }
        if(!ready){
            if(dumpFiltering) getLogger().info(player.getName()+" denied suggestions - plugin not ready.");
            event.getCommands().clear();
            return;
        }
        if (!player.hasPermission("aztectabcompleter.suggest")) {
            if(dumpFiltering) getLogger().info(player.getName()+" denied suggestions by permission.");
            event.getCommands().clear();
        } else {
            if(dumpFiltering) getLogger().info(player.getName()+" commands,  pre-filter: "+event.getCommands());
            event.getCommands().removeIf(entry -> !filters.filter(new FilterArgs(player, entry)).isAllowed);
            if(dumpFiltering) getLogger().info(player.getName()+" commands, post-filter: "+event.getCommands());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!loaded) {
            return;
        }
        if (!ready) {
            if (kickEarlyJoins) {
                event.setKickMessage(kickMessage);
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent event) {
        if (!blockCommands) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("aztectabcompleter.bypass")) {
            if(dumpFiltering) getLogger().info(player.getName()+" bypassed command filtering by permission.");
            return;
        }

        int space = event.getMessage().indexOf(" ");
        String command;
        if (space == -1) {
            command = event.getMessage().substring(1);
        } else {
            command = event.getMessage().substring(1, space);
        }

        if (!filters.filterBoolean(new FilterArgs(player, command.toLowerCase()))) {
            player.sendMessage(blockMessage);
            event.setCancelled(true);
        }
    }

}
