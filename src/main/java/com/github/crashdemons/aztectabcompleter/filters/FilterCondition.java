/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

import java.util.function.Predicate;

/**
 * Defines a boolean operation tested by Filters against input to decide a resulting action.
 * This is generally defined by a boolean lambda-function taking the FilterArgs pair.
 * (Semantic-sugar for Predicate syntax)
 * 
 * A filter is defined as a conditional check of filter arguments with the following implications:
   FilterCondition returns True: the command has passed filtering and should remain/be allowed to the next filter.
   FilterCondition returns False: the command has failed filtering and should be removed/blocked.
 * @author crash
 */
public interface FilterCondition extends Predicate<FilterArgs> {
    /**
     * Test the function against the input data
     * @param pair the input pair of Player and Command Suggestion to be filtered
     * @return Whether the function returned true or false.
     */
    @Override
    public boolean test(FilterArgs pair);
}
