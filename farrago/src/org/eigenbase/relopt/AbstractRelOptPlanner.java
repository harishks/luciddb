/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2006-2006 The Eigenbase Project
// Copyright (C) 2006-2006 Disruptive Tech
// Copyright (C) 2006-2006 LucidEra, Inc.
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.eigenbase.relopt;

import org.eigenbase.rel.*;
import org.eigenbase.rel.metadata.*;
import org.eigenbase.oj.rel.*;

import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

/**
 * AbstractRelOptPlanner is an abstract base for implementations
 * of the {@link RelOptPlanner} interface.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public abstract class AbstractRelOptPlanner implements RelOptPlanner
{
    //~ Constants -------------------------------------------------------------
    /**
     * Regular expression for integer.
     */
    private static final Pattern IntegerPattern = Pattern.compile("[0-9]+");

    /**
     * Maps rule description to rule, just to ensure that rules' descriptions
     * are unique.
     */
    private final Map<String, RelOptRule> mapDescToRule;
    
    private MulticastRelOptListener listener;

    protected AbstractRelOptPlanner()
    {
        mapDescToRule = new HashMap<String, RelOptRule>();
    }

    protected void mapRuleDescription(RelOptRule rule)
    {
        // Check that there isn't a rule with the same description,
        // also validating description string.
        
        final String description = rule.toString();
        assert description != null;
        assert description.indexOf("$") < 0 :
            "Rule's description should not contain '$': " + description;
        assert !IntegerPattern.matcher(description).matches() :
            "Rule's description should not be an integer: " +
            rule.getClass().getName() + ", " + description;
        
        RelOptRule existingRule =
            mapDescToRule.put(description, rule);
        if (existingRule != null) {
            if (existingRule == rule) {
                throw new AssertionError(
                    "Rule should not already be registered");
            } else {
                // This rule has the same description as one previously
                // registered, yet it is not equal. You may need to fix the
                // rule's equals and hashCode methods.
                throw new AssertionError("Rule's description should be unique; "
                    + "existing rule=" + existingRule + "; new rule=" + rule);
            }
        }
    }
    
    protected void unmapRuleDescription(RelOptRule rule)
    {
        String description = rule.toString();
        mapDescToRule.remove(description);
    }

    protected RelOptRule getRuleByDescription(String description)
    {
        return mapDescToRule.get(description);
    }
    
    // implement RelOptPlanner
    public RelOptPlanner chooseDelegate()
    {
        return this;
    }
    
    // implement RelOptPlanner
    public void registerSchema(RelOptSchema schema)
    {
    }

    // implement RelOptPlanner
    public long getRelMetadataTimestamp(RelNode rel)
    {
        return 0;
    }
    
    // implement RelOptPlanner
    public RelOptCost makeCost(
        double dRows,
        double dCpu,
        double dIo)
    {
        return new RelOptCostImpl(dRows);
    }

    // implement RelOptPlanner
    public RelOptCost makeHugeCost()
    {
        return new RelOptCostImpl(Double.MAX_VALUE);
    }

    // implement RelOptPlanner
    public RelOptCost makeInfiniteCost()
    {
        return new RelOptCostImpl(Double.POSITIVE_INFINITY);
    }

    // implement RelOptPlanner
    public RelOptCost makeTinyCost()
    {
        return new RelOptCostImpl(1.0);
    }

    // implement RelOptPlanner
    public RelOptCost makeZeroCost()
    {
        return new RelOptCostImpl(0.0);
    }
    
    // implement RelOptPlanner
    public RelOptCost getCost(RelNode rel)
    {
        return RelMetadataQuery.getCumulativeCost(rel);
    }
    
    // implement RelOptPlanner
    public void addListener(RelOptListener newListener)
    {
        if (listener == null) {
            listener = new MulticastRelOptListener();
        }
        listener.addListener(newListener);
    }
    
    // implement RelOptPlanner
    public JavaRelImplementor getJavaRelImplementor(RelNode rel)
    {
        return null;
    }
    
    // implement RelOptPlanner
    public void registerMetadataProviders(ChainedRelMetadataProvider chain)
    {
    }
    
    // implement RelOptPlanner
    public boolean addRelTraitDef(RelTraitDef relTraitDef)
    {
        return false;
    }

    /**
     * Fires a rule, taking care of tracing and listener notification.
     *
     * @param ruleCall description of rule call
     */
    protected void fireRule(
        RelOptRuleCall ruleCall)
    {
        if (tracer.isLoggable(Level.FINE)) {
            tracer.fine("Apply rule [" + ruleCall.getRule() + "] to ["
                + RelOptUtil.toString(ruleCall.getRels()) + "]");
        }
            
        if (listener != null) {
            RelOptListener.RuleAttemptedEvent event =
                new RelOptListener.RuleAttemptedEvent(
                    this,
                    ruleCall.getRels()[0],
                    ruleCall,
                    true);
            listener.ruleAttempted(event);
        }
        
        ruleCall.getRule().onMatch(ruleCall);
            
        if (listener != null) {
            RelOptListener.RuleAttemptedEvent event =
                new RelOptListener.RuleAttemptedEvent(
                    this,
                    ruleCall.getRels()[0],
                    ruleCall,
                    false);
            listener.ruleAttempted(event);
        }
    }

    /**
     * Takes care of tracing and listener notification when a rule's
     * transformation is applied.
     *
     * @param ruleCall description of rule call
     *
     * @param newRel result of transformation
     *
     * @param before true before registration of new rel; false after
     */
    protected void notifyTransformation(
        RelOptRuleCall ruleCall,
        RelNode newRel,
        boolean before)
    {
        if (before && tracer.isLoggable(Level.FINE)) {
            tracer.fine("Rule " + ruleCall.getRule() + " arguments "
                + RelOptUtil.toString(ruleCall.getRels()) + " created "
                + newRel);
        }
        
        if (listener != null) {
            RelOptListener.RuleProductionEvent event =
                new RelOptListener.RuleProductionEvent(
                    this,
                    newRel,
                    ruleCall,
                    before);
            listener.ruleProductionSucceeded(event);
        }
    }

    /**
     * Takes care of tracing and listener notification when a rel
     * is chosen as part of the final plan.
     *
     * @param rel chosen rel
     */
    protected void notifyChosen(RelNode rel)
    {
        if (tracer.isLoggable(Level.FINE)) {
            tracer.fine("For final plan, using " + rel);
        }
        
        if (listener != null) {
            RelOptListener.RelChosenEvent event =
                new RelOptListener.RelChosenEvent(
                    this,
                    rel);
            listener.relChosen(event);
        }
    }

    /**
     * Takes care of tracing and listener notification when a rel
     * equivalence is detected.
     *
     * @param rel chosen rel
     */
    protected void notifyEquivalence(
        RelNode rel,
        Object equivalenceClass,
        boolean physical)
    {
        if (listener != null) {
            RelOptListener.RelEquivalenceEvent event =
                new RelOptListener.RelEquivalenceEvent(
                    this,
                    rel,
                    equivalenceClass,
                    physical);
            listener.relEquivalenceFound(event);
        }
    }
    
    /**
     * Takes care of tracing and listener notification when a rel
     * is discarded
     *
     * @param rel discarded rel
     */
    protected void notifyDiscard(
        RelNode rel)
    {
        if (listener != null) {
            RelOptListener.RelDiscardedEvent event =
                new RelOptListener.RelDiscardedEvent(
                    this,
                    rel);
            listener.relDiscarded(event);
        }
    }

    protected MulticastRelOptListener getListener()
    {
        return listener;
    }
}

// End AbstractRelOptPlanner.java