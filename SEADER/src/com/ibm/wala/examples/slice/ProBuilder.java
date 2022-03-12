package com.ibm.wala.examples.slice;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.dataflow.IFDS.BackwardsSupergraph;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.examples.ExampleUtil;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/*create by Ying, for build the cg, sdg, supergraph for give project*/
public class ProBuilder {
    private AnalysisScope scope;
    private ClassHierarchy cha;
    private PointerAnalysis<InstanceKey> pa;
    private AnalysisOptions options;
    private AnalysisCacheImpl cache;
    private CallGraph completeCG;
    private CallGraphBuilder<InstanceKey> builder;
    private Set<Entrypoint> entryPoints = new HashSet<>();
    private SDG<InstanceKey> completeSDG;
    private ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph;

    public ProBuilder(String path, Slicer.DataDependenceOptions dataDependenceOptions, Slicer.ControlDependenceOptions controlDependenceOptions) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
        this.scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(path, null);
        ExampleUtil.addDefaultExclusions(scope);
        this.cha = ClassHierarchyFactory.make(scope);
        this.entryPoints = getEntryPoints(cha);
        this.options = new AnalysisOptions(scope, entryPoints);

        this.cache = new AnalysisCacheImpl();
        this.builder = Util.makeZeroOneCFABuilder(Language.JAVA, options,
                cache, cha, scope);
       // this.pa = builder.getPointerAnalysis();
        CallGraph CG = builder.makeCallGraph(options, null);
        Set<CGNode> keep = new HashSet<>();
        for (CGNode n : CG) {
            if (!isPrimordial(n)) {
                keep.add(n);
            }

        }
        PrunedCallGraph pcg = new PrunedCallGraph(CG, keep);
        this.completeCG = pcg;
        this.completeSDG = new SDG<>(completeCG, builder.getPointerAnalysis(), dataDependenceOptions, controlDependenceOptions);
        SDGSupergraph forwards = new SDGSupergraph(completeSDG, true);
        this.backwardSuperGraph = BackwardsSupergraph.make(forwards);
    }


    public ISupergraph<Statement, PDG<? extends InstanceKey>> getBackwardSuperGraph() {
        return backwardSuperGraph;
    }

    public PointerAnalysis<InstanceKey> getPa() {
        return pa;
    }

    public SDG<InstanceKey> getCompleteSDG() {
        return completeSDG;
    }

    public CallGraph getTargetCG(){
        return completeCG;
    }

    public CallGraphBuilder<InstanceKey> getBuilder(){
        return builder;
    }

    /*get entrypoints while filter out the primordial class*/
    public Set<Entrypoint> getEntryPoints(ClassHierarchy cha){
        Set<Entrypoint> entryPoints = new HashSet<>();
        for (IClass klass : cha) {
            if (!klass.isInterface()
                    && !klass.getClassLoader().getName().toString().contains("Primordial")
            ) {
                if (klass.toString().contains("springsecurity")) {
                    System.out.println(klass);
                };
                for (IMethod method : klass.getDeclaredMethods()) {
                    entryPoints.add(new DefaultEntrypoint(method, cha));
                }
            }
        }
        return entryPoints;

    }



    public boolean isPrimordial(CGNode n) {
        return n.getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial");
    }

}
