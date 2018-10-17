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
public enum FilterResult {
    SKIP(1,false,true,true),
    ALLOW_AND_CONTINUE(2,false,true,false),
    DENY_AND_CONTINUE(3,false,false,false),
    ALLOW_FINAL(4,true,true,false),
    DENY_FINAL(5,true,false,false);
    
    
    public final int priority;
    public final boolean isFinal;
    public final boolean isAllowed;
    public final boolean isSkipped;
    
    
    FilterResult(int priority,boolean _final, boolean allowed, boolean skipped){
        this.priority=priority;
        isFinal = _final;
        isAllowed=allowed;
        isSkipped=skipped;
    }
    
    public FilterResult finalize(FilterResult defaultResult){
        if(isSkipped) return defaultResult;
        return isAllowed ? ALLOW_FINAL : DENY_FINAL;
    }
    public boolean finalizeBoolean(boolean defaultResult){
        if(isSkipped) return defaultResult;
        return isAllowed;
    }
    
    public boolean overrides(FilterResult otherResult){
        return priority > otherResult.priority;
    }
}
