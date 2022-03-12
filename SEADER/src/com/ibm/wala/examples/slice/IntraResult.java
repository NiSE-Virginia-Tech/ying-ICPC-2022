package com.ibm.wala.examples.slice;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;

import java.util.*;

public class IntraResult {

    public HashMap<Integer, List<Object>> paramValue = new HashMap<>();
    public Set<Integer> uses = new HashSet<>();
    public IR ir;
    public boolean result = false;
    public Statement targetStmt = null;
    public Set<Statement> visitedStmt = new HashSet<>();

    public List<Statement> stmtsInBlock = new ArrayList<>();
    public int pos;
    public HashMap<Integer,Statement> resultMap = new HashMap<>();

    public IntraResult(Statement targetStmt,HashMap<Integer, List<Object>> paramValue, Set<Integer> uses, boolean result, IR ir, HashMap<Integer,Statement> resultMap ){
        this.paramValue = paramValue;
        this.uses = uses;
        this.targetStmt = targetStmt;
        this.result = result;
         this.ir = ir;
        this.resultMap = resultMap;

    }

    public void setVisitedStmt(Set<Statement> visited){
        this.visitedStmt.addAll(visited);
    }
    public HashMap<Integer, List<Object>> getParamValue() {
        return paramValue;
    }

    public Set<Integer> getUses() {
        return uses;
    }

    public HashMap<Integer, Statement> getResultMap() {
        return resultMap;
    }

    public boolean getResult(){
        return result;
    }

    public Statement getTargetStmt() {
        return targetStmt;
    }




}
