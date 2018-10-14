/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

import com.github.crashdemons.util.Pair;
import java.util.function.Predicate;
import org.bukkit.entity.Player;

/**
 * Semantic-sugar for Predicate syntax
 * @author crash
 */
public interface Filter extends Predicate<FilterArgs> {
    @Override
    public boolean test(FilterArgs pair);
}
