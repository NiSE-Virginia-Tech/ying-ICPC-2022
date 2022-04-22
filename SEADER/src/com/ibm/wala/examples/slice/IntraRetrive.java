package com.ibm.wala.examples.slice;

import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.*;

import java.util.*;

/*for intra case, consider normal case, static case, Array_case, if branch case and Newsite case*/

public class IntraRetrive {
    private Statement targetStmt = null;
    private List<Statement> reachingStmts = new ArrayList<>();
    private HashMap<Integer, List<Object>> paramValue = new HashMap<>();
    private HashMap<Integer, Statement> statementWithIndex = new HashMap<>();
    private int startUse;
    private Set<Integer> instVisited = new HashSet<>();
    private DefUse du;
    private SymbolTable st;
    private IR ir;
    public ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph;
    public HashMap<Integer, Statement> resultMap = new HashMap<>();


    public IntraRetrive(Statement targetStmt, List<Statement> reachingStmts, ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph, HashMap<Integer, List<Object>> paramValue, int use){
        this.targetStmt = targetStmt;
        this.reachingStmts = reachingStmts;
        this.paramValue = paramValue;
        this.du = targetStmt.getNode().getDU();
        this.ir = targetStmt.getNode().getIR();
        this.st = this.ir.getSymbolTable();
        this.startUse = use;
        this.backwardSuperGraph = backwardSuperGraph;
    }


    public IntraRetrive(DefUse du, SymbolTable st, Integer use, ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph,  HashMap<Integer, List<Object>> paramValue){
        this.du = du;
        this.st = st;
        this.backwardSuperGraph = backwardSuperGraph;
        this.paramValue = paramValue;
        this.startUse = use;
    }

    public IntraResult setParamValue(Statement targetStmt, int pos, List<Object> ans){
        String signature = targetStmt.getNode().getMethod().getSignature();
        List<Statement> stmtInBlock = getStmtInBlock(signature,reachingStmts);
        statementWithIndex(stmtInBlock);
        if(stmtInBlock.isEmpty()){
            System.out.println("method call by init here!");

        }
        SSAInstruction targetInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
        instVisited.add(targetInst.iIndex()); /*only add the iindex right? dulpicate index in two different block*/
        Set<Integer> uses = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        Boolean result = true;

        if (st.isConstant(startUse)) {
            ans.add(st.getConstantValue(startUse));
            paramValue.put(pos, ans);
            result = true;

        } else {
            uses.add(startUse);
            uses = getDU(uses, pos, visited, ans);//can't get the parameter within one block;
            if (!uses.isEmpty()) {
               result = false;
            }
        }
        IntraResult intraResult = new IntraResult(targetStmt,paramValue, uses, result, ir, resultMap);
        return intraResult;
    }

    /*this method for deal with the use trace within one block, retrive the def-use chain*/
    public Set<Integer> getDU(Set<Integer>uses, int pos, Set<Integer> visited, List<Object> ans){
        Queue<Integer> q = new LinkedList<>();
        q.addAll(uses);
        if(targetStmt == null){
            //.clear();
            return uses;
        }

        SSAInstruction curInst = ((StatementWithInstructionIndex)targetStmt).getInstruction();
        while (!q.isEmpty()) {
            Integer use1 = q.poll();
            if(st.isConstant(use1)){
                ans.add(st.getConstantValue(use1));
                paramValue.put(pos, ans);
                uses.remove(use1);
                visited.add(use1);

                //new added by Ying, if get the value, directly return, which may lower the precision;
                if(uses.isEmpty())
                    break;
                else  continue;
//                uses.remove(use1);
//                visited.add(use1);
//                continue;
            }

            if (du.getDef(use1) != null && !(visited.contains(use1))) {
                SSAInstruction inst = du.getDef(use1);
                instVisited.add(inst.iIndex());
                curInst = inst;
                // if it is get inst, trace the put pair;
                if (inst instanceof SSAGetInstruction) {
                    SSAGetInstruction getInst = (SSAGetInstruction)inst;
                    Integer use = checkFiled(getInst);
                    if(use!= null) {
                        uses.remove(use1);
                        uses.add(use);
                        q.add(use);
                        visited.add(use1);
                        curInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
                    }
                    this.targetStmt = locateStatement(inst);
                    resultMap.put(use1, targetStmt);
                    continue;
                }

                if (inst instanceof SSAPhiInstruction) {
                    uses.remove(use1);
                    visited.add(use1);
                    for (int i = 0; i < inst.getNumberOfUses(); i++) {
                        uses.add(inst.getUse(i));
                        q.add(inst.getUse(i));
                    }
                    continue;
                }

                if (inst instanceof SSACheckCastInstruction) {
                    uses.remove(use1);
                    visited.add(use1);
                    q.add(((SSACheckCastInstruction) inst).getVal());
                    uses.add(((SSACheckCastInstruction) inst).getVal());
                    continue;
                }

                if(inst instanceof SSAInvokeInstruction){
                    SSAInvokeInstruction call =(SSAInvokeInstruction) inst;
                    String methodT = call.getDeclaredTarget().getDeclaringClass().toString();
                    String methodN = call.getCallSite().getDeclaredTarget().getName().toString();
                    if(methodT.contains("java/util/Map")&& methodN.equals("get")){
                        Integer tmpUse = checkMap(call);
                        uses.add(tmpUse);
                        q.add(tmpUse);
                        uses.remove(use1);
                        visited.add(use1);
                        curInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
                        continue;
                    }

                }

                if(inst instanceof SSAArrayLoadInstruction) {
                    Integer tmpUse =((SSAArrayLoadInstruction) inst).getArrayRef();
                    uses.add(tmpUse);
                    q.add(tmpUse);
                    uses.remove(use1);
                    visited.add(use1);
                    continue;
                }


//                if(inst instanceof SSANewInstruction) {
//                    if (((SSANewInstruction) inst).getNewSite().getDeclaredType().getName().toString().contains("SecureRandom") && inst.getNumberOfUses() == 0) {
//                        uses.remove(use1);
//                        visited.add(use1);
//                        ans.add("can be random value");
//                        paramValue.put(pos, ans);
//                        continue;
//
//                    }
//                    if (((SSANewInstruction) inst).getNewSite().getDeclaredType().toString().endsWith("<Primordial,[B>")){
//                        uses.remove(use1);
//                        visited.add(use1);
//                        if(inst.getNumberOfUses()>0){
//                            int newUse = inst.getUse(0);
//                            q.add(newUse);
//                        }
//                        ans.add("no value assigned");
//                        paramValue.put(pos, ans);
//                        continue;
//                    }
//
//                }
                //TODO:arraylength inst should be further deal
                if(inst instanceof SSANewInstruction){
                    Iterator<SSAInstruction> insts =  du.getUses(use1);
                    boolean flag = false;
                    while(insts.hasNext()){//arrystore
                        SSAInstruction targetInst  = insts.next();
                        if(instVisited.contains(targetInst.iIndex())) continue;
                        if(targetInst instanceof SSAArrayStoreInstruction){
                            if(use1 == ((SSAArrayStoreInstruction) targetInst).getArrayRef()){
                                int k = ((SSAArrayStoreInstruction) targetInst).getValue();
                                //refine code for code
                                if(st.isConstant(k)) {
                                    ans.add(st.getConstantValue(k));
                                    paramValue.put(pos, ans);
                                    instVisited.add(targetInst.iIndex());
                                    uses.remove(use1);
                                    visited.add(use1);
                                    continue;
                                }

                                else {
                                    uses.remove(use1);
                                    visited.add(use1);
                                    instVisited.add(targetInst.iIndex());
                                    curInst = targetInst;
                                    inst = targetInst;
                                    q.add(((SSAArrayStoreInstruction) targetInst).getValue());
                                    uses.add(((SSAArrayStoreInstruction) targetInst).getValue());
                                    flag = true;
                                    break;
                                }

                            }
                        }
                        if(targetInst instanceof SSAInvokeInstruction){
                            String methodT = ((SSAInvokeInstruction) targetInst).getDeclaredTarget().getDeclaringClass().toString();
                            if(methodT.equals(((SSAInvokeInstruction) targetInst).getDeclaredTarget().getDeclaringClass().toString())) {
                                for(int i =0; i< targetInst.getNumberOfUses(); i++){
                                    if(use1 == targetInst.getUse(i)){
                                        instVisited.add(targetInst.iIndex());
                                        curInst = targetInst;
                                        uses.remove(use1);
                                        visited.add(use1);
                                        for(int j = 0; j< targetInst.getNumberOfUses(); j++){
                                            if(targetInst.getUse(j) == use1) continue;
                                            q.add(targetInst.getUse(j));
                                            uses.add(targetInst.getUse(j));
                                            flag = true;
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            if(methodT.contains("<Application,Ljava/security/SecureRandom>")) {
                                if((((SSAInvokeInstruction) targetInst).getDeclaredTarget().getName().toString().contains("<init>") && inst.getNumberOfUses() ==0)) {
                                    uses.remove(use1);
                                    visited.add(use1);
                                    ans.add("parameter can be random value");
                                    paramValue.put(pos, ans);
                                    break;
                                } else {
                                    for(int i =0; i< targetInst.getNumberOfUses(); i++){
                                        if(use1 == targetInst.getUse(i)){
                                            instVisited.add(targetInst.iIndex());
                                            curInst = targetInst;
                                            uses.remove(use1);
                                            visited.add(use1);
                                            for(int j = 0; j< targetInst.getNumberOfUses(); j++){
                                                if(targetInst.getUse(j) == use1) continue;
                                                q.add(targetInst.getUse(j));
                                                uses.add(targetInst.getUse(j));
                                                flag = true;
                                            }
                                            break;
                                        }
                                    }
                                }
                            }

                        }
                        instVisited.add(targetInst.iIndex());
                        continue;
                    }
                    if(flag) continue;

                    if (((SSANewInstruction) inst).getNewSite().getDeclaredType().getName().toString().contains("SecureRandom") && inst.getNumberOfUses() == 0) {
                        uses.remove(use1);
                        visited.add(use1);
                        ans.add("can be random value");
                        paramValue.put(pos, ans);
                        continue;

                    }
                    if (((SSANewInstruction) inst).getNewSite().getDeclaredType().toString().endsWith("<Primordial,[B>")){
                        uses.remove(use1);
                        visited.add(use1);
                        if(inst.getNumberOfUses()>0){
                            int newUse = inst.getUse(0);
                            q.add(newUse);
                        }
                        ans.add("no value assigned");
                        paramValue.put(pos, ans);
                        continue;
                    }
                    continue;
                }




                //no use here, maybe should remove
                if(inst.getNumberOfUses() ==0) return uses;
                uses.remove(use1);
                for (int j = 0; j < inst.getNumberOfUses(); j++) {
                    Integer use = inst.getUse(j);
                    if (j == 0 && !isInstStatic(inst)) //not count 1st use for static invoke
                        continue;
                    if (!st.isConstant(use)) {
                        uses.add(use);
                        q.add(use);
                    }else {
                        if(curInst.getNumberOfUses()==1){
                            ans.add(st.getConstantValue(use));
                            this.paramValue.put(pos, ans);
                            visited.add(use);
                        }
                    }
                }
                if(!uses.isEmpty()) continue;

                else{
                    for(int j =0; j< inst.getNumberOfUses(); j++){
                        Integer use = inst.getUse(j);
                        if (st.isConstant(use) && !visited.contains(use)) {
                            ans.add(st.getConstantValue(use));
                            this.paramValue.put(pos, ans);
                            visited.add(use);
                        }
                    }
                }
            } else{
                Statement curStmt = locateStatement(curInst);
                if(curStmt == null && curInst instanceof SSAPhiInstruction){
                   Iterator<SSAInstruction> insts = du.getUses(use1);
                   while (insts.hasNext()){
                       SSAInstruction tmpInst = insts.next();
                       if(instVisited.contains(tmpInst.iIndex())) continue;
                       curInst = tmpInst;
                       curStmt = locateStatement(curInst);
                       break;
                       //if(instVisited(insts.next())
                   }
                }
                resultMap.put(use1, curStmt);
                continue;
            }
        }

        return uses;
    }

    public Integer checkFiled(SSAGetInstruction getInst){
        Statement getStatement = locateStatement(getInst);
        String fieldName = getInst.getDeclaredField().getName().toString();
        Iterator<Statement> getsucc = backwardSuperGraph.getSuccNodes(getStatement);
        if(getsucc.hasNext()){
            Statement currStmt = getsucc.next();
            if (currStmt instanceof StatementWithInstructionIndex) {
                SSAInstruction curtInst = ((StatementWithInstructionIndex) currStmt).getInstruction();
                if (curtInst instanceof SSAPutInstruction) {
                    SSAPutInstruction curPut = (SSAPutInstruction) curtInst;
                    if (curPut.getDeclaredField().getName().toString().compareTo(fieldName) == 0) {
                        int use = curPut.getVal();
                        this.targetStmt = currStmt;
                        return use;
                    }
                }
            }
        }
        return null;
    }

    public Integer checkMap(SSAInvokeInstruction call){

        Set<Integer> uses = new HashSet<>();
        SSAInvokeInstruction putInst = null;
        int use =0;
        for(int i =0; i< call.getNumberOfUses();i++){
            uses.add(call.getUse(i));
            if(i>0){
                Iterator<SSAInstruction> useInst =  du.getUses(call.getUse(i));
                while (useInst.hasNext()){
                    SSAInstruction inst = useInst.next();
                    int index = inst.iIndex();
                    if(inst instanceof SSAInvokeInstruction && !instVisited.contains(index)){
                        putInst = (SSAInvokeInstruction)inst;
                        String methodN = putInst.getCallSite().getDeclaredTarget().getName().toString();
                        if(putInst.getDeclaredTarget().getDeclaringClass().toString().contains("Ljava/util/Map") && methodN.equals("put")){
                            instVisited.add(index);
                            this.targetStmt = statementWithIndex.get(index);
                            for(int j =0; j< putInst.getNumberOfUses();j++){
                                if(uses.contains(putInst.getUse(j)))continue;
                                use =putInst.getUse(j);
                            }
                        }
                    }
                }
            }

        }
        return use;


    }

    public Statement locateStatement(SSAInstruction inst){
        int index = inst.iIndex();
        if(statementWithIndex.containsKey(index)){
            return statementWithIndex.get(index);
        }

        return null;
    }


    /*get the stmt within one method*/
    public List<Statement> getStmtInBlock(String signature, List<Statement> StmtList) {
        List<Statement> stmtInBloack = new ArrayList<>();
        for (int i = 0; i < StmtList.size(); i++) {
            Statement stmt = StmtList.get(i);
            if (stmt.getNode().getMethod().getSignature().compareTo(signature) == 0) {
                stmtInBloack.add(stmt);
            }
        }
        return stmtInBloack;
    }

    public void statementWithIndex(List<Statement> stmtInBlock){
        for(Statement stmt: stmtInBlock){
            if (stmt instanceof  StatementWithInstructionIndex && ((StatementWithInstructionIndex) stmt).getInstruction()!= null &&
                    stmt.getKind()!= Statement.Kind.NORMAL_RET_CALLEE && stmt.getKind()!= Statement.Kind.NORMAL_RET_CALLER){
                SSAInstruction inst =((StatementWithInstructionIndex) stmt).getInstruction();
                this.statementWithIndex.put(inst.iIndex(),stmt);
            }
            else continue;
        }
    }

    public boolean isInstStatic(SSAInstruction inst) {
        if (inst instanceof SSAAbstractInvokeInstruction) return true;
        if (inst instanceof SSAInvokeInstruction) return ((SSAInvokeInstruction) inst).isStatic();
        if (inst instanceof SSAGetInstruction) return ((SSAGetInstruction) inst).isStatic();
        if (inst instanceof SSAPutInstruction) return ((SSAPutInstruction) inst).isStatic();
        //abstractinvoke from 0;sd
        return false;
    }




}
