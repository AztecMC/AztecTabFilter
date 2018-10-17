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
    FilterCondition condition;
    FilterResult passResult;
    FilterResult failResult;
    Filter(FilterResult passResult, FilterResult failResult, FilterCondition condition){
        this.condition=condition;
        this.passResult=passResult;
        this.failResult=failResult;
    }
    FilterResult test(FilterArgs args){
        return condition.test(args) ? passResult : failResult;
    }
}
