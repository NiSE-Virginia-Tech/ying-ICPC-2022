package com.ibm.wala.examples.slice;

import com.ibm.wala.dataflow.IFDS.BackwardsSupergraph;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.*;

import java.util.*;

public class ParaRetrive {
    private Statement targetStmt = null;
    private List<Statement> reachingStmts = new ArrayList<>();
    private ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph;
    private HashMap<Integer, List<Object>> paramValue = new HashMap<>();
    private HashMap<Integer, Statement> statementWithIndex;
    private int pos;
    private Set<Integer> instVisited = new HashSet<>();

    public ParaRetrive(Statement stmt, List<Statement> reachingStmts, SDG<InstanceKey> completeSDG, int pos){
        this.targetStmt = stmt;
        this.reachingStmts = reachingStmts;
        this.pos = pos;
        SDGSupergraph backward = new SDGSupergraph(completeSDG,true);
        this.backwardSuperGraph = BackwardsSupergraph.make(backward);
    }


    public void setParamValue(Statement targetStmt){
        SSAInstruction targetInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
        instVisited.add(targetInst.iIndex());
        Set<Integer> uses = new HashSet<>();
        CGNode targetNode = targetStmt.getNode();
        IR targetIR = targetNode.getIR();
        SymbolTable st = targetIR.getSymbolTable();
        Set<Integer> visited = new HashSet<>();

        if (targetInst instanceof SSAInvokeInstruction) {
            int i = ((SSAInvokeInstruction) targetInst).isStatic() == true ? 0 : 1;
            int neg = ((SSAInvokeInstruction) targetInst).isStatic() == true ? 0 : -1; // for control the position
            int numOfUse = targetInst.getNumberOfUses();

            while (i < numOfUse) {
                List<Object> ans = new ArrayList<>();
                int use = targetInst.getUse(i);
                if (st.isConstant(use)) {
                    ans.add(st.getConstantValue(use));
                    paramValue.put(i + neg, ans);
                } else {
                    uses.add(use);
                    uses = getDU(targetStmt, uses, i + neg, visited, ans);//can't get the parameter within one block;
                    if (!uses.isEmpty()) {
                        List<Statement> stmtInBlock = new ArrayList<>();
                        //useCheckHelper(targetStmt, uses, stmtList, visited, ans, i + neg);
                        //loopStatementInBlock(targetStmt, uses, stmtInBlock, i + neg);
                    }
                }
                i++;
            }
            if (paramValue.size() == numOfUse)
                return;
        }


    }

    public void useCheckHelper(){

    }


    /*this method for deal with the use trace within one block*/
    public Set<Integer> getDU(Statement stmt, Set<Integer>uses, int pos, Set<Integer> visited, List<Object> ans){
        SymbolTable st = stmt.getNode().getIR().getSymbolTable();
        DefUse du = stmt.getNode().getDU();
        Queue<Integer> q = new LinkedList<>();
        q.addAll(uses);

        while (!q.isEmpty()) {
            Integer use1 = q.poll();
            if (du.getDef(use1) != null && !(visited.contains(use1))) {
                SSAInstruction inst = du.getDef(use1);
                instVisited.add(inst.iIndex());
                if (inst instanceof SSAGetInstruction) { // if it is get inst, then no use traced, should check more
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
                    String methodT = call.getCallSite().getDeclaredTarget().getSignature();
                    String methodN = call.getCallSite().getDeclaredTarget().getName().toString();
                    if(methodT.contains("Ljava/util/Map")&& methodN.equals("get")){
                        uses.add(checkMap(call, du));
                        q.add(checkMap(call,du));
                        uses.remove(use1);
                        visited.add(use1);
                        continue;
                    }

                }


                if(inst instanceof SSANewInstruction){//TODO: how to get the prenode; current method may cause the infinte loop
                    Iterator<SSAInstruction> insts =  du.getUses(use1);
                    while(insts.hasNext()){ //arrystore
                        SSAInstruction targetInst  = insts.next();
                        if(targetInst instanceof SSAArrayStoreInstruction){
                            if(use1 == ((SSAArrayStoreInstruction) targetInst).getArrayRef()){
                                uses.remove(use1);
                                visited.add(use1);
                                q.add(((SSAArrayStoreInstruction) targetInst).getValue());
                                uses.add(((SSAArrayStoreInstruction) targetInst).getValue());
                                continue;
                            }
                        }

                        if(targetInst instanceof SSAInvokeInstruction && ((SSAInvokeInstruction) targetInst).getDeclaredTarget().getName().toString().contains("<init>") && !instVisited.contains(targetInst.iIndex())){
                            String methodT = ((SSAInvokeInstruction) targetInst).getDeclaredTarget().getDeclaringClass().toString();
                            if(((SSANewInstruction) inst).getNewSite().getDeclaredType().toString().equals(methodT)){
                                for(int i =0; i< targetInst.getNumberOfUses(); i++){
                                    if(use1 == targetInst.getUse(0)){
                                        instVisited.add(targetInst.iIndex());
                                        uses.remove(use1);
                                        visited.add(use1);
                                        for(int j = i+1; j< targetInst.getNumberOfUses(); j++){
                                            q.add(targetInst.getUse(j));
                                            uses.add(targetInst.getUse(j));
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        continue;
                    }
                    continue;

                }

                if(inst instanceof SSANewInstruction && ((SSANewInstruction) inst).getNewSite().getDeclaredType().getName().toString().contains("SecureRandom") && inst.getNumberOfUses() ==0) {
                    uses.remove(use1);
                    visited.add(use1);
                    ans.add("can be random value");
                    paramValue.put(pos, ans);
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
            } else continue;
        }

        return uses;
    }


    public Integer checkMap(SSAInvokeInstruction call, DefUse du){

        Set<Integer> uses = new HashSet<>();
        SSAInvokeInstruction putInst = null;
        int use =0;
        for(int i =0; i< call.getNumberOfUses();i++){
            uses.add(i);
            if(i>0){
                Iterator<SSAInstruction> useInst =  du.getUses(i);
                while (useInst.hasNext()){
                    SSAInstruction inst = useInst.next();
                    int index = inst.iIndex();
                    if(inst instanceof SSAInvokeInstruction && !instVisited.contains(index)){
                        putInst = (SSAInvokeInstruction)inst;
                        String methodN = call.getCallSite().getDeclaredTarget().getName().toString();
                        if(putInst.getCallSite().getDeclaredTarget().getSignature().contains("Ljava/util/Map") && methodN.equals("put")){
                            instVisited.add(putInst.iIndex());
                            for(int j =0; j< putInst.getNumberOfUses();j++){
                                if(uses.contains(j))continue;
                                use =j;
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


    public void statementWithIndex(List<Statement> reachingStmts){
        for(Statement stmt: reachingStmts){
            if (stmt instanceof  StatementWithInstructionIndex){
                SSAInstruction inst =((StatementWithInstructionIndex) stmt).getInstruction();
                statementWithIndex.put(inst.iIndex(),stmt);
            }
            else continue;
        }
    }

    public Set<Integer> excludeCons(Statement stmt,Set<Integer> uses, Set<Integer> visited, int pos, List<Object> ans){
        SymbolTable st = stmt.getNode().getIR().getSymbolTable();
        Set<Integer> conUse = new HashSet<>();
        for(Integer i: uses){
            if(st.isConstant(i)){
                ans.add(st.getConstantValue(i));
                paramValue.put(pos,ans);
                conUse.add(i);
                visited.add(i);
            }
        }
        uses.removeAll(conUse);
        return uses;
    }

    public boolean isInstStatic(SSAInstruction inst) {
        if (inst instanceof SSAAbstractInvokeInstruction) return true;
        if (inst instanceof SSAInvokeInstruction) return ((SSAInvokeInstruction) inst).isStatic();
        if (inst instanceof SSAGetInstruction) return ((SSAGetInstruction) inst).isStatic();
        if (inst instanceof SSAPutInstruction) return ((SSAPutInstruction) inst).isStatic();
        //abstractinvoke from 0;sd
        return false;
    }


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
}

