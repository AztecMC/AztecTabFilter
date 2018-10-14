/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

import com.github.crashdemons.util.Pair;
import org.bukkit.entity.Player;

/**
 * Semantic-sugar for Pair syntax
 * @author crash
 */
public class FilterArgs extends Pair<Player,String> {
    public FilterArgs(){
        super(null,null);
    }
    public FilterArgs(Player destination, String command){
        super(destination,command);
    }
    public Player getDestination(){ return getKey(); }
    public String getCommand(){ return getValue(); }
}
