/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.DelegatingContext;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;

/**
 * Checks ContextSelectors A and B, then returns the union of their findings.
 *
 * <p>The returned Context contains the ContextKeys from A and B. If for a given key a value is
 * generated by A as well as B the value generated by A has the precedence.
 *
 * <p>As the UnionContextSelector optionally returns a DelegatingContext it cannot be used to
 * anihilate ContextValues of B to null.
 *
 * @see com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector
 * @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public class UnionContextSelector implements ContextSelector {
  private final ContextSelector A;
  private final ContextSelector B;

  public UnionContextSelector(final ContextSelector A, final ContextSelector B) {
    if (A == null) {
      throw new IllegalArgumentException("The ContextSelector given as A may not be null");
    }
    if (B == null) {
      throw new IllegalArgumentException("The ContextSelector given as B may not be null");
    }
    this.A = A;
    this.B = B;
  }

  /** If only one Context exists return it, else return a DelegatingContext. */
  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    final Context ctxA = A.getCalleeTarget(caller, site, callee, receiver);
    final Context ctxB = B.getCalleeTarget(caller, site, callee, receiver);

    if (ctxA == null) {
      return ctxB;
    } else if (ctxB == null) {
      return ctxA;
    } else {
      final Context ctxU = new DelegatingContext(ctxA, ctxB);
      return ctxU;
    }
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return A.getRelevantParameters(caller, site).union(B.getRelevantParameters(caller, site));
  }

  @Override
  public String toString() {
    return "<UnionContextSelector A=" + A + " B=" + B + " />";
  }
}
