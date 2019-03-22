/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

/**
 *
 * @author crash
 */
public class Filter {
    private final FilterCondition condition;
    private final FilterResult matchResult;
    private final FilterResult failResult;
    Filter(FilterResult matchResult, FilterResult failResult, FilterCondition condition){
        this.condition=condition;
        this.matchResult=matchResult;
        this.failResult=failResult;
    }
    FilterResult test(FilterArgs args){
        return condition.test(args) ? matchResult : failResult;
    }
}
