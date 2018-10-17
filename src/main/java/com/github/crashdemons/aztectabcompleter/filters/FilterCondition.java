/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

import java.util.function.Predicate;

/**
 * Semantic-sugar for Predicate syntax.
 * 
 * A filter is defined as a conditional check of filter arguments with the following implications:
   FilterCondition returns True: the command has passed filtering and should remain/be allowed to the next filter.
   FilterCondition returns False: the command has failed filtering and should be removed/blocked.
 * @author crash
 */
public interface FilterCondition extends Predicate<FilterArgs> {
    @Override
    public boolean test(FilterArgs pair);
}
