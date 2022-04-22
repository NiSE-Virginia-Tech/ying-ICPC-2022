package com.ibm.wala.examples.slice;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/*do backward slice for a given stmt, grape all sensitive stmt*/

public class BackwardResult {

    private SDG<InstanceKey> completeSDG;
    private PointerAnalysis<InstanceKey> pa;
    private Slicer.DataDependenceOptions dataDependenceOptions;
    private Slicer.ControlDependenceOptions controlDependenceOptions;
    private CallGraph completeCG;
    private HeapModel heapModel;
    private Collection<Statement> reachingStmts = new ArrayList<>();
    private List<Statement> filterReaStmts = new ArrayList<>();

    public BackwardResult(CallGraph completeCG, PointerAnalysis<InstanceKey> pa, SDG<InstanceKey> completeSDG, Slicer.DataDependenceOptions dataDependenceOptions, Slicer.ControlDependenceOptions controlDependenceOptions){
        this.completeCG = completeCG;
        this.pa = pa;
        this.heapModel = pa.getHeapModel();
        this.completeSDG = completeSDG;
        this.dataDependenceOptions = dataDependenceOptions;
        this.controlDependenceOptions = controlDependenceOptions;
    }

    public SDG<InstanceKey> getCompleteSDG(){
        return completeSDG;
    }

    /*
    * this function used for group all reaching statements
    *  from the starting point(sensitive init location)
    * Also do a filter to the statement which is "Primordial"*/
    public void setReachingStmts(Statement startStmt ) throws CancelException {
        this.reachingStmts = Slicer.computeBackwardSlice(startStmt, completeCG, pa,
                dataDependenceOptions, controlDependenceOptions);
        this.filterReaStmts = filterStatement(reachingStmts);
    }

    public Collection<Statement> getReachingStmt(){
        return reachingStmts;
    }

    public List<Statement> getFilterReaStmts(){
        filterReaStmts = filterStatement(reachingStmts);
        return filterReaStmts;
    }

    public List<Statement> filterStatement(Collection<Statement> reachingStmts) {
        List<com.ibm.wala.ipa.slicer.Statement> stmtList = new ArrayList<>();
        for (com.ibm.wala.ipa.slicer.Statement stmt : reachingStmts) {
            if (!stmt.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial")) {
                stmtList.add(stmt);
            }
        }
        return stmtList;
    }
}
