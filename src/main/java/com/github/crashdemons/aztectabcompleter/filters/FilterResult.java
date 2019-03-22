/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.crashdemons.aztectabcompleter.filters;

/**
 * Defines an enumation of possible Filter outcomes that can be decided
 * @author crash
 */
public enum FilterResult {
    /**
     * The filter decided that it was not applicable to this input and the input should continue to be filtered.
     * If the result must be final for all filtering (ie: it is finalized), a provided default filtering result is used instead.
     */
    SKIP(1,false,true,true),
    /**
     * The filter decided that the input should be allowed, but only if no other filters override it.
     * If the result must be final for all filtering (ie: it is finalized), this becomes ALLOW_FINAL.
     */
    ALLOW_AND_CONTINUE(2,false,true,false),
    /**
     * The filter decided the input should be blocked, but only if no other filters override it.
     * If the result must be final for all filtering (ie: it is finalized), this becomes DENY_FINAL.
     */
    DENY_AND_CONTINUE(3,false,false,false),
    /**
     * The filter decided that the input should be allowed, regardless of other filtering rules.
     */
    ALLOW_FINAL(4,true,true,false),
    /**
     * The filter decided that the input should be blocked, regardless of other filtering rules.
     */
    DENY_FINAL(5,true,false,false);
    
    /**
     * The priority of the filter result where higher priority results override lower ones.
     * @see #overrides(com.github.crashdemons.aztectabcompleter.filters.FilterResult) 
     */
    public final int priority;
    /**
     * Whether this filter result is final (ends filtering / skips all other rules).
     */
    public final boolean isFinal;
    /**
     * Whether this filter result indicates that the input is allowed.
     */
    public final boolean isAllowed;
    /**
     * Whether this filter result indicates that the input is not applicable to it / should default to other rules.
     */
    public final boolean isSkipped;
    
    //Creates a filter result with the defined parameters
    private FilterResult(int priority,boolean _final, boolean allowed, boolean skipped){
        this.priority=priority;
        isFinal = _final;
        isAllowed=allowed;
        isSkipped=skipped;
    }
    
    /**
     * Convert a result into a definitive ALLOW_FINAL or DENY_FINAL result decision for the end of filtering.
     * If the result was SKIP (no intermediate filtering matched), then the provided default result is used instead.
     * @param defaultResult The result to return if the value being converted is SKIP (no intermediate filtering matched / SKIP was the only intermediate result).
     * @return The finalized or default filter result.
     */
    public FilterResult finalize(FilterResult defaultResult){
        if(isSkipped) return defaultResult;
        return isAllowed ? ALLOW_FINAL : DENY_FINAL;
    }
    /**
     * Convert a result into a definitive true or false result decision of whether the input is allowed (for the end of filtering).
     * If the result was SKIP (no intermediate filtering matched), then the provided default boolean value is used instead.
     * @param defaultResult The boolean value to return if the value being converted is SKIP (no intermediate filtering matched / SKIP was the only intermediate result).
     * @return The finalized boolean value indicating whether the filters allowed the input.
     */
    public boolean finalizeBoolean(boolean defaultResult){
        if(isSkipped) return defaultResult;
        return isAllowed;
    }
    
    /**
     * Determines whether this filter result should override another (ie: it has a higher priority than the other).
     * @param otherResult the other filter result to compare against.
     * @return Whether this filter has a higher priority.
     */
    public boolean overrides(FilterResult otherResult){
        return priority > otherResult.priority;
    }
}
