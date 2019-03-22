/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

/**
 * Filter class that defines a condition on which a filter matches or fails against input arguments, and decides the resulting action of the match.
 * @author crash
 */
public class Filter {
    private final FilterCondition condition;
    private final FilterResult matchResult;
    private final FilterResult failResult;
    
    /**
     * Creates a new filter
     * @param matchResult The result to be decided/returned when the filter condition is satisfied (matched) by the input
     * @param failResult The result to be decided/returned when the filter condition is not satisfied (no match)
     * @param condition The condition (boolean function) against which to test inputs
     */
    Filter(FilterResult matchResult, FilterResult failResult, FilterCondition condition){
        this.condition=condition;
        this.matchResult=matchResult;
        this.failResult=failResult;
    }
    /**
     * Check input arguments against the defined filter condition (boolean function).
     * @param args The input argument Pair (Player, Command) to check
     * @return the value of whether the check succeeded (matched) against the condition function.
     */
    boolean match(FilterArgs args){ return condition.test(args); }
    
    /**
     * Decides the resulting action from the filter depending on whether the defined filter condition (boolean function) matches.
     * The corresponding result for each outcome of the condition function is defined when the filter is created.
     * @param args The input argument Pair (Player, Command) to check
     * @return the filter result corresponding to the outcome of the condition function.
     */
    FilterResult decide(FilterArgs args){
        return condition.test(args) ? matchResult : failResult;
    }
}
