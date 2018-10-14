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
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author crash
 */
public class FilterSet {
    JavaPlugin plugin;
    private HashMap<String,Filter> filters;
    
    private List<String> filterOrder;
    private volatile HashSet<String> visibleCommands;
    private volatile HashSet<String> invisibleCommands;
    private List<Filter> filtersEnabled;
    private Filter filterAll;
    
   
    public FilterSet(JavaPlugin pl){
        filterOrder = new ArrayList<>();
        visibleCommands = new HashSet<>();
        invisibleCommands = new HashSet<>();
        filtersEnabled = new ArrayList<>();
        
        plugin = pl;
        filters=new HashMap<>();
        filters.put("whitelist", 
                args-> visibleCommands.contains(args.getCommand()) 
        );
        filters.put("blacklist", 
                args-> !invisibleCommands.contains(args.getCommand()) 
        );
        filters.put("permission", 
                args-> {
                    Player destination = args.getDestination();
                    String command = args.getCommand();
                    if(destination!=null){
                        PluginCommand pc = Bukkit.getPluginCommand(command);
                        if(pc!=null){
                            String perm = pc.getPermission();
                            if(perm!=null){
                                return destination.hasPermission(perm);
                            }
                        }
                    }
                    return true;
                }
        );
        filterAll = args -> {
            for(Filter filter : filtersEnabled){
                if(!filter.test(args)) return false;
            }
            return true;
        };
    }
    public boolean filter(FilterArgs args){
        return filterAll.test(args);
    }
    
    private void log(String s){ if(plugin!=null) plugin.getLogger().info(s); }
    
    
    public void load(FileConfiguration config){
        try{
            visibleCommands = new HashSet<>( config.getStringList("visible-commands") );
        }catch(Exception e){
             log("error loading visible-commands, skipping.");
        }
        log("Loaded "+visibleCommands.size()+" whitelist entries.");
        
        
        try{
            invisibleCommands = new HashSet<>( config.getStringList("invisible-commands") );
        }catch(Exception e){
            log("error loading invisible-commands , skipping.");
        }
        log("Loaded "+invisibleCommands.size()+" blacklist entries.");
        
        try{
            filterOrder = config.getStringList("filter-order");
        }catch(Exception e){
            log("error loading filter-order, setting default.");
            filterOrder.clear();
            filterOrder.add("blacklist");
            filterOrder.add("permission");
            filterOrder.add("whitelist");
        }
        
        filtersEnabled.clear();
        for(String filterName : filterOrder){
            Filter filter = filters.get(filterName);
            if(filter == null){
                plugin.getLogger().warning("Unsupported filter: "+filterName);
            }else{
                filtersEnabled.add(filter);
            }
        }
        log("Loaded "+filtersEnabled.size()+" filters.");
    }
}
