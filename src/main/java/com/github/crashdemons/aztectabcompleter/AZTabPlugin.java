package com.github.crashdemons.aztectabcompleter;

import com.github.crashdemons.aztectabcompleter.filters.FilterArgs;
import com.github.crashdemons.aztectabcompleter.filters.FilterSet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
    private FilterSet filters;

    //runtime behavior variables
    public volatile boolean loaded = false;
    private volatile boolean ready = false;

    private boolean kickEarlyJoins = true;
    private String kickMessage = "The server is still loading - check back in a moment!";

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
        if (cmd.getName().equalsIgnoreCase("aztabreload")) {
            if (!sender.hasPermission("aztectabcompleter.reload")) {
                sender.sendMessage("You don't have permission to do this.");
                return true;
            }
            loadConfig();
            sender.sendMessage("[AZTab] Config reloaded.");
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandSuggestion(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("aztectabcompleter.bypass")) {
            return;
        }
        if (!ready || !player.hasPermission("aztectabcompleter.suggest")) {
            event.getCommands().clear();
        } else {
            event.getCommands().removeIf(entry -> !filters.filter(new FilterArgs(player, entry)).isAllowed);
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

}
