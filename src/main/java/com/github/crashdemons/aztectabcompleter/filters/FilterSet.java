/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author crash
 */
public class FilterSet {
    JavaPlugin plugin;
    String permission;
    
    FilterResult defaultResult;
    private Filter filterWhitelist;
    private Filter filterBlacklist;
    private HashMap<String,Filter> filters;
    
    private volatile HashSet<String> visibleCommands;
    private volatile HashSet<String> invisibleCommands;
    private HashMap<String,Filter> filtersEnabled;
    private Filter filterAll;
    
    HashMap<String,FilterSet> filterGroups;
    
   
    public FilterSet(JavaPlugin pl){
        defaultResult=FilterResult.DENY_FINAL;//changed during load()
        permission = null;//TODO set permission for groups
        visibleCommands = new HashSet<>();
        invisibleCommands = new HashSet<>();
        filtersEnabled = new HashMap<>();
        filterGroups = new HashMap<>();
        
        plugin = pl;
        filters=new HashMap<>();
        
        filterWhitelist = new Filter(FilterResult.ALLOW_FINAL,FilterResult.SKIP,
                args-> visibleCommands.contains(args.getCommand())
        );
        filterBlacklist = new Filter(FilterResult.DENY_FINAL,FilterResult.SKIP,
                args-> invisibleCommands.contains(args.getCommand())
        );
        
        
        filters.put("whitelist", filterWhitelist);
        filters.put("blacklist", filterBlacklist);
        
        filters.put("group-whitelists", new Filter(FilterResult.ALLOW_FINAL, FilterResult.SKIP,
                args->{
                    for(FilterSet groupFilterSet : filterGroups.values())
                        if(groupFilterSet.hasPermission(args))
                            if(groupFilterSet.filterWhitelist.test(args)==FilterResult.ALLOW_FINAL) return true;
                    return false;
                }
        ));
        filters.put("group-blacklists", new Filter(FilterResult.DENY_FINAL, FilterResult.SKIP,
                args->{
                    for(FilterSet groupFilterSet : filterGroups.values())
                        if(groupFilterSet.hasPermission(args))
                            if(groupFilterSet.filterWhitelist.test(args)==FilterResult.DENY_FINAL) return true;
                    return false;
                }
        ));
        filterAll = new Filter(FilterResult.ALLOW_FINAL,FilterResult.DENY_FINAL,
            args -> {
                FilterResult currentResult=FilterResult.SKIP;
                for(Filter filter : filtersEnabled.values()){
                    FilterResult result = filter.test(args);
                    if(result.isFinal) return result.isAllowed;
                    if(!result.isSkipped && result.overrides(currentResult))
                        currentResult = result;
                }
                if(currentResult.isSkipped) currentResult=defaultResult;
                return currentResult.isAllowed;
            }
        );
    }
    
    public boolean hasPermission(FilterArgs args){
        if(permission==null) return true;
        return args.getDestination().hasPermission(permission);
    }
    
    public FilterResult filter(FilterArgs args){
        return filterAll.test(args);
    }
    public boolean filterBoolean(FilterArgs args){
        return filterAll.test(args).isAllowed;
    }
    
    private void log(String s){ if(plugin!=null) plugin.getLogger().info(s); }
    
    private void loadDefaultResult(ConfigurationSection config){
        String resultName = config.getString("filter-default");
        try{
            defaultResult = FilterResult.valueOf(resultName.toUpperCase());
            log("Loaded default action: "+defaultResult.name());
        }catch(Exception e){
            plugin.getLogger().warning("Unknown filter-default action, defaulting to DENY_FINAL");
            defaultResult = FilterResult.DENY_FINAL;
        }
    }
    
    private void loadLists(ConfigurationSection config, boolean logoutput){
        visibleCommands.clear();
        invisibleCommands.clear();
        try{
            visibleCommands = new HashSet<>( config.getStringList("visible-commands") );
        }catch(Exception e){
            if(logoutput) log("error loading visible-commands, skipping.");
        }
        if(logoutput) log("Loaded "+visibleCommands.size()+" whitelist entries.");
        
        try{
            invisibleCommands = new HashSet<>( config.getStringList("invisible-commands") );
        }catch(Exception e){
            if(logoutput) log("error loading invisible-commands , skipping.");
        }
        if(logoutput) log("Loaded "+invisibleCommands.size()+" blacklist entries.");
    }
    private List<String> loadFilterOrder(ConfigurationSection config,boolean logoutput){
        ConfigurationSection defaults = config.getDefaultSection();
        if(config.contains("filter-order", true)){
            List<String> order = config.getStringList("filter-order");
            if(logoutput) log("Loaded filter-order: "+order.toString());
            return order;
        }else{
            if(logoutput) plugin.getLogger().warning("No filter-order defined, using default.");
            return config.getDefaultSection().getStringList("filter-order");
        }
    }
    private void buildFilterOrder(List<String> filterOrder){
        //build filter order list
        filtersEnabled.clear();
        if(filterOrder==null) return;
        for(String filterName : filterOrder){
            System.out.println("build filter name: "+filterName);
            System.out.println(""+filters);
            System.out.println(""+filters.size());
            Filter filter = filters.get(filterName);
            if(filter == null){
                plugin.getLogger().warning("Unsupported filter: "+filterName);
            }else{
                filtersEnabled.put(filterName,filter);
            }
        }
    }
    private void loadGroups(ConfigurationSection config, boolean logoutput, List<String> filterOrder){
        filterGroups.clear();
        ConfigurationSection groups = config.getConfigurationSection("groups");
        if(groups==null){
            if(logoutput) plugin.getLogger().warning("No default groups were present!");
            return;
        }
        Set<String> groupnames = groups.getKeys(false);
        if(logoutput) log(""+groupnames.size());
        if(logoutput) log(groupnames.toString());
        for(String groupname : groupnames){
            ConfigurationSection groupConfig = groups.getConfigurationSection(groupname);
            FilterSet filterGroup = new FilterSet(this.plugin);
            filterGroup.permission = "aztectabcompleter.group."+groupname;
            //filterGroup.load(groupConfig);
            filterGroup.loadLists(groupConfig, false);
            filterGroups.put(groupname, filterGroup);
        }
        log("Loaded "+filterGroups.size()+" groups.");
    }
    
    public void load(ConfigurationSection config){//detect group load and don't load nested groups etc.
        loadDefaultResult(config);
        loadLists(config,true);
        List<String> order = loadFilterOrder(config,true);
        buildFilterOrder(order);
        loadGroups(config,true,order);
        
        
        
        log("Loaded "+filtersEnabled.size()+" filters.");
    }
}
