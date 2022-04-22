package com.ibm.wala.examples.slice;

import com.Constant;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.CancelException;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class InterRetrive {
    private static final Logger LOGGER = Logger.getLogger(InterRetrive.class.getName());
    private CallGraph completeCG;
    private Set<Statement> allStartStmt = new HashSet<>();
    private SDG<InstanceKey> completeSDG;
    private HashMap<Integer, List<Object>> paramValue = new HashMap<>();
    private Map<String, Map<Integer, List<Object>>> classVarMap = new HashMap<>();
    public ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph;
    public Integer classIndex = 0;

    public void start(String classpath, String callee, String functionType, String checkType, Long args) throws ClassHierarchyException, CancelException, IOException {
        LOGGER.setLevel(Constant.loglevel);
        Slicer.DataDependenceOptions dOptions = Slicer.DataDependenceOptions.FULL;
        Slicer.ControlDependenceOptions cOptions = Slicer.ControlDependenceOptions.FULL;
        ProBuilder proBuilder = new ProBuilder(classpath,dOptions, cOptions);
        completeCG = proBuilder.getTargetCG();
        completeSDG = proBuilder.getCompleteSDG();
        backwardSuperGraph = proBuilder.getBackwardSuperGraph();
        StartPoints startPoints = new StartPoints(completeCG, callee, functionType, args);
        allStartStmt = startPoints.getStartStmts();

        if(checkType.equals("type")) {
            for(Statement stmt: allStartStmt){
                String className = stmt.getNode().getMethod().getDeclaringClass().getName().toString();

                // if condition is for debug purpose
//                if (className.compareTo("Lorg/cryptoapi/bench/untrustedprng/UntrustedPRNGCase1") != 0) continue;

                LOGGER.info("------start analysis checkType.equals(\"type\") " +className+"-------");
                System.out.println("------start analysis " +className+"-------");
                for (int i = 0; i < paramValue.size(); i++) {
                    LOGGER.info("target parameter is : " + paramValue.get(i));
                    System.out.println("target parameter is : " + paramValue.get(i));
                }
                //  System.out.println(stmt.getNode().getMethod().getLineNumber(((StatementWithInstructionIndex)stmt).getInstructionIndex()));

                if(classVarMap.containsKey(className)) {
                   classIndex++;
                   className = className + " + " + classIndex;
                }
                classVarMap.put(className, paramValue);
                LOGGER.info("------done analysis for checkType.equals(\"type\")" + className);
                System.out.println("------done analysis for " + className);

            }

            return;
        }

        for(Statement stmt: allStartStmt){
            String className = stmt.getNode().getMethod().getDeclaringClass().getName().toString();
//            if (className.compareTo("Lorg/cryptoapi/bench/staticsalts/CryptoStaticSalt1") != 0) continue;

            System.out.println("------start analysis " +className+"-------");
            LOGGER.info("------start analysis " +className+"-------");
            BackwardResult backwardResult = new BackwardResult(completeCG,proBuilder.getBuilder().getPointerAnalysis(),completeSDG,dOptions,cOptions);
            backwardResult.setReachingStmts(stmt);
            List<Statement> stmtList = backwardResult.getFilterReaStmts();
            setParamValue(stmt, stmtList);
            for (int i = 0; i < paramValue.size(); i++) {
                System.out.println("target parameter is : " + paramValue.get(i));
            }
          //  System.out.println(stmt.getNode().getMethod().getLineNumber(((StatementWithInstructionIndex)stmt).getInstructionIndex()));
            if(classVarMap.containsKey(className)) {
                classIndex++;
                className = className + " + " +classIndex;
            }
            classVarMap.put(className, paramValue);
            LOGGER.info("------done analysis for " +className+"-------");
            System.out.println("------done analysis for " + className+"--------" );

        }

    }


    public void setParamValue(Statement targetStmt, List<Statement> stmtList){
        SSAInstruction targetInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
        Set<Integer> uses = new HashSet<>();
        Set<Statement> visitedStmt = new HashSet<>();
        Statement newRStmt = targetStmt;
        if (targetInst instanceof SSAInvokeInstruction) {
            int i = ((SSAInvokeInstruction) targetInst).isStatic() == true ? 0 : 1;
            int neg = ((SSAInvokeInstruction) targetInst).isStatic() == true ? 0 : -1;
            int numOfUse = targetInst.getNumberOfUses();
            //get all parameter, by process one by one
            while (i < numOfUse) {
                targetStmt = newRStmt;
                visitedStmt.clear();
                visitedStmt.add(targetStmt);
                List<Object> ans = new ArrayList<>(); //have more possible value;
                int use = targetInst.getUse(i);
                boolean result = true;
                while (result){
                    IntraRetrive intraRetrive= new IntraRetrive(targetStmt, stmtList,backwardSuperGraph, paramValue, use);
                    IntraResult intraResult = intraRetrive.setParamValue(targetStmt, i+ neg, ans);
                    if(!intraResult.result){
                        uses =  intraResult.getUses(); /*may have more than one use to trace*/
                        for(Integer u: uses){
                            EdgeProce edgeProce = new EdgeProce(backwardSuperGraph, intraResult.resultMap, u,stmtList,paramValue,completeCG);
                            edgeProce.visited.addAll(visitedStmt);
                            IntraResult edgeResult = edgeProce.edgeSolver(u,i+neg,ans);
                            LOGGER.info(Constant.getLineNumber() + " edgeResult: " + edgeResult);
                            /*add here to prevent the  infinite loop*/
                            edgeResult.setVisitedStmt(edgeProce.visited);
                            visitedStmt.addAll(edgeResult.visitedStmt);

                            if(edgeResult.resultMap.size()!=0 ) {
                                targetStmt = edgeProce.targetStmt;
                                use = edgeProce.use;
                                result = true;
                                setParamValue(targetStmt, use, result, i+neg, visitedStmt, stmtList,ans);
                                continue;
                            }
                            else continue;
                        }
                        result = false;
                    }
                    else result = false;
                }
                i++;
            }

            if (paramValue.size() == numOfUse){
                System.out.println("finish");
                return;
            }
        }
    }

    public void setParamValue(Statement targetStmt, int use, boolean result, int pos, Set<Statement> visitedStmt, List<Statement> stmtList,List<Object> ans){

        if(result){
            IntraRetrive intraRetrive= new IntraRetrive(targetStmt, stmtList,backwardSuperGraph, paramValue, use);
            IntraResult intraResult = intraRetrive.setParamValue(targetStmt, pos, ans);
            if(!intraResult.result){
                Set<Integer> uses =  intraResult.getUses(); /*mayhave more than one use to trace*/
                for(Integer u: uses){
                    EdgeProce edgeProce = new EdgeProce(backwardSuperGraph, intraResult.resultMap, u,stmtList,paramValue,completeCG);
                    edgeProce.visited.addAll(visitedStmt);
                    IntraResult edgeResult = edgeProce.edgeSolver(u,pos,ans);
                    LOGGER.info(Constant.getLineNumber() +" edgeResult: " + edgeResult);
                    if (edgeResult == null) {
                        paramValue.put(pos, ans);
                        return;
                    }
                    /*add here to prevent the  infinite loop*/
                    edgeResult.setVisitedStmt(edgeProce.visited);
                    visitedStmt.addAll(edgeResult.visitedStmt);
                    if(edgeResult.resultMap.size()!= 0) {
                        targetStmt = edgeProce.targetStmt;
                        use = edgeProce.use;
                        result = true;
                        setParamValue(targetStmt, use,result, pos, visitedStmt,stmtList,ans);
                        break;
                    }
                    else continue;
                }
            }
            else result = false;
        }
        return;
    }

    public HashMap<Integer, List<Object>> getParamValue() {
        return paramValue;
    }

    public Map<String, Map<Integer, List<Object>>> getClassVarMap() {
        return classVarMap;
    }
}
