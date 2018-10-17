/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author crash
 */
public class FilterSet {
    JavaPlugin plugin;
    String permission;
    private HashMap<String,Filter> filters;
    
    private volatile HashSet<String> visibleCommands;
    private volatile HashSet<String> invisibleCommands;
    private HashMap<String,Filter> filtersEnabled;
    private Filter filterAll;
    
    HashMap<String,FilterSet> filterGroups;
    
   
    public FilterSet(JavaPlugin pl){
        permission = null;//TODO set permission for groups
        visibleCommands = new HashSet<>();
        invisibleCommands = new HashSet<>();
        filtersEnabled = new HashMap<>();
        filterGroups = new HashMap<>();
        
        plugin = pl;
        filters=new HashMap<>();
        filters.put("whitelist", 
                args-> visibleCommands.contains(args.getCommand()) 
        );
        filters.put("blacklist", 
                args-> !invisibleCommands.contains(args.getCommand()) 
        );
        filterAll = args -> {
            for(Filter filter : filtersEnabled.values()){
                if(!filter.test(args)) return false;
            }
            return true;
        };
    }
    public boolean filter(FilterArgs args){
        return filterAll.test(args);
    }
    
    private void log(String s){ if(plugin!=null) plugin.getLogger().info(s); }
    
    
    
    private void loadLists(ConfigurationSection config, boolean logoutput){
        try{
            visibleCommands = new HashSet<>( config.getStringList("visible-commands") );
        }catch(Exception e){
             log("error loading visible-commands, skipping.");
        }
        if(logoutput) log("Loaded "+visibleCommands.size()+" whitelist entries.");
        
        try{
            invisibleCommands = new HashSet<>( config.getStringList("invisible-commands") );
        }catch(Exception e){
            log("error loading invisible-commands , skipping.");
        }
        if(logoutput) log("Loaded "+invisibleCommands.size()+" blacklist entries.");
    }
    
    public void load(ConfigurationSection config){//detect group load and don't load nested groups etc.
        loadLists(config,true);
        
        //get filter order string.
        List<String> filterOrder=new ArrayList<>();
        try{
            filterOrder = config.getStringList("filter-order");
        }catch(Exception e){
            log("error loading filter-order, setting default.");
            filterOrder.clear();
            filterOrder.add("blacklist");
            filterOrder.add("whitelist");
        }
        
        //build filter order list
        filtersEnabled.clear();
        for(String filterName : filterOrder){
            Filter filter = filters.get(filterName);
            if(filter == null){
                plugin.getLogger().warning("Unsupported filter: "+filterName);
            }else{
                filtersEnabled.put(filterName,filter);
            }
        }
        
        filterGroups.clear();
        ConfigurationSection groups = config.getConfigurationSection("groups");
        if(groups==null){
            plugin.getLogger().warning("No default groups were present!");
            return;
        }
        Set<String> groupnames = groups.getKeys(false);
        log(""+groupnames.size());
        log(groupnames.toString());
        for(String groupname : groupnames){
            ConfigurationSection groupConfig = groups.getConfigurationSection(groupname);
            FilterSet filterGroup = new FilterSet(this.plugin);
            filterGroup.load(groupConfig);
            filterGroups.put(groupname, filterGroup);
        }
        
        
        
        log("Loaded "+filtersEnabled.size()+" filters.");
    }
}
