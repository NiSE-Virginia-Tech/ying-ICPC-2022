package Slice;

import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.NodeDecorator;

import java.io.IOException;
import java.util.Collection;

import static com.ibm.wala.classLoader.Language.JAVA;

public class Slicing {
    //run backward slicing

    public void run(String appJar, String mainClass, String srcCaller, String srcCallee, boolean goBackward,
                       Slicer.DataDependenceOptions dOptions, Slicer.ControlDependenceOptions cOptions)
            throws IllegalArgumentException, CancelException, IOException, ClassHierarchyException {

        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, (new FileProvider())
                .getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
        // build a class hierarchy, call graph, and system dependence graph
        ClassHierarchy cha = ClassHierarchyFactory.make(scope);
        Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
        AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
        CallGraphBuilder<InstanceKey> builder = Util.makeVanillaZeroOneCFABuilder(JAVA, options, new AnalysisCacheImpl(), cha, scope);
        // CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, new
        // AnalysisCache(), cha, scope);
        CallGraph cg = builder.makeCallGraph(options, null);
        SDG<InstanceKey> sdg = new SDG<>(cg, builder.getPointerAnalysis(), dOptions, cOptions);

        // find the call statement of interest
        CGNode callerNode = SlicerTest.findMethod(cg, srcCaller);
        Statement s = SlicerTest.findCallTo(callerNode, srcCallee);
        System.err.println("Statement: " + s);

        // compute the slice as a collection of statements
        Collection<Statement> slice = null;
        if (goBackward) {
            final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
            slice = Slicer.computeBackwardSlice(s, cg, pointerAnalysis, dOptions, cOptions);
        } else {
            // for forward slices ... we actually slice from the return value of
            // calls.
            s = getReturnStatementForCall(s);
            final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
            slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis, dOptions, cOptions);
        }

        System.out.println(slice.size());
    }

    /**
     * check that g is a well-formed graph, and that it contains exactly the number of nodes in the slice
     */
    private static void sanityCheck(Collection<Statement> slice, Graph<Statement> g) {
        try {
            GraphIntegrity.check(g);
        } catch (GraphIntegrity.UnsoundGraphException e1) {
            e1.printStackTrace();
            Assertions.UNREACHABLE();
        }
        Assertions.productionAssertion(g.getNumberOfNodes() == slice.size(), "panic " + g.getNumberOfNodes() + " " + slice.size());
    }

    /**
     * If s is a call statement, return the statement representing the normal return from s
     */
    public static Statement getReturnStatementForCall(Statement s) {
        if (s.getKind() == Statement.Kind.NORMAL) {
            NormalStatement n = (NormalStatement) s;
            SSAInstruction st = n.getInstruction();
            if (st instanceof SSAInvokeInstruction) {
                SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) st;
                if (call.getCallSite().getDeclaredTarget().getReturnType().equals(TypeReference.Void)) {
                    throw new IllegalArgumentException("this driver computes forward slices from the return value of calls.\n" + ""
                            + "Method " + call.getCallSite().getDeclaredTarget().getSignature() + " returns void.");
                }
                return new NormalReturnCaller(s.getNode(), n.getInstructionIndex());
            } else {
                return s;
            }
        } else {
            return s;
        }
    }

    /**
     * return a view of the sdg restricted to the statements in the slice
     */
    public static Graph<Statement> pruneSDG(SDG<InstanceKey> sdg, final Collection<Statement> slice) {
        return GraphSlicer.prune(sdg, slice::contains);
    }

    /**
     * @return a NodeDecorator that decorates statements in a slice for a dot-ted representation
     */
    public static NodeDecorator<Statement> makeNodeDecorator() {
        return s -> {
            switch (s.getKind()) {
                case HEAP_PARAM_CALLEE:
                case HEAP_PARAM_CALLER:
                case HEAP_RET_CALLEE:
                case HEAP_RET_CALLER:
                    HeapStatement h = (HeapStatement) s;
                    return s.getKind() + "\\n" + h.getNode() + "\\n" + h.getLocation();
                case NORMAL:
                    NormalStatement n = (NormalStatement) s;
                    return n.getInstruction() + "\\n" + n.getNode().getMethod().getSignature()
                            + "\\nLine Number: " + n.getNode().getMethod().getLineNumber(n.getInstructionIndex());
                case PARAM_CALLEE:
                    ParamCallee paramCallee = (ParamCallee) s;
                    return s.getKind() + " " + paramCallee.getValueNumber() + "\\n" + s.getNode().getMethod().getName();
                case PARAM_CALLER:
                    ParamCaller paramCaller = (ParamCaller) s;
                    return s.getKind() + " " + paramCaller.getValueNumber() + "\\n" + s.getNode().getMethod().getName() + "\\n"
                            + paramCaller.getInstruction().getCallSite().getDeclaredTarget().getName();
                case EXC_RET_CALLEE:
                case EXC_RET_CALLER:
                case NORMAL_RET_CALLEE:
                case NORMAL_RET_CALLER:
                case PHI:
                default:
                    return s.toString();
            }
        };
    }
}
