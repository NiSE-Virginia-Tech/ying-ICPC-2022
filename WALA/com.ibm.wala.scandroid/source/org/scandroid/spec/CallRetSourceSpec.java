/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>)
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.spec;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.scandroid.domain.CodeElement;
import org.scandroid.flow.InflowAnalysis;
import org.scandroid.flow.types.FlowType;
import org.scandroid.flow.types.ReturnFlow;
import org.scandroid.util.CGAnalysisContext;

/**
 * CallRetSourceSpecs represent sources from invocations of other methods (eg: API methods).
 *
 * <p>reading file contents, and returning bytes eg: via {@code int read(...)} is an example of a
 * call return source.
 */
public class CallRetSourceSpec extends SourceSpec {
  final String sig = "CallRetSource";

  public CallRetSourceSpec(MethodNamePattern name, int[] args) {
    namePattern = name;
    argNums = args;
  }

  @Override
  public <E extends ISSABasicBlock> void addDomainElements(
      CGAnalysisContext<E> ctx,
      Map<BasicBlockInContext<E>, Map<FlowType<E>, Set<CodeElement>>> taintMap,
      IMethod im,
      BasicBlockInContext<E> block,
      SSAInvokeInstruction invInst,
      int[] newArgNums,
      ISupergraph<BasicBlockInContext<E>, CGNode> graph,
      PointerAnalysis<InstanceKey> pa,
      CallGraph cg) {

    for (FlowType<E> ft : getFlowType(block)) {
      InflowAnalysis.addDomainElements(
          taintMap, block, ft, CodeElement.valueElements(invInst.getDef(0)));
    }
  }

  private static <E extends ISSABasicBlock> Collection<FlowType<E>> getFlowType(
      BasicBlockInContext<E> block) {

    return Collections.singleton(new ReturnFlow<>(block, true));
  }

  @Override
  public String toString() {
    return String.format("CallRetSourceSpec(%s)", namePattern);
  }
}
