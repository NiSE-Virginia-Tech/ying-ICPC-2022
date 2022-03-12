package com.ibm.wala.examples.slice;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Pair;

import java.util.*;

public class InitRetrive {

    private Statement targetStmt;
    private IField iField = null;
    private PointerKey loc = null;
    private HashMap<Integer, List<Object>> paramValue = new HashMap<>();
    public ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph;

    public InitRetrive(Statement targetStmt, IField field, PointerKey loc,HashMap<Integer, List<Object>> paramValue,ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph){
        this.targetStmt = targetStmt;
        this.iField = field;
        this.loc = loc;
        this.paramValue = paramValue;
        this.backwardSuperGraph = backwardSuperGraph;
    }

    public boolean setValues(CallGraph completeCG, int pos, List<Object> ans, Set<Integer> uses, Set<Integer> visited){
        Set<CGNode> initNodes = searchInit(completeCG);
        Boolean result = false;


        /*here need remove the duplicate init node check*/

        for(CGNode initNode: initNodes){
            if(setInitValue(initNode, pos, ans, uses,visited)){
                result = true;
                continue;
            }
        }
        if(result) return result;

        if(loc instanceof InstanceFieldKey){
            InstanceFieldKey insLoc = (InstanceFieldKey)loc;
            if(setInstValue(insLoc, pos, ans,completeCG)){
                result =true;
                //return result;
            }

        }


        return result;
    }

    public boolean setInitValue(CGNode initNode, int pos, List<Object>ans, Set<Integer> uses, Set<Integer> visited){
        SymbolTable st = initNode.getIR().getSymbolTable();
        DefUse du = initNode.getDU();
        SSAInstruction instList[] = initNode.getIR().getInstructions();
        for (SSAInstruction inst : instList) {
            if(inst== null) continue;
            if (inst instanceof SSAPutInstruction) {
                SSAPutInstruction putInst = (SSAPutInstruction) inst;
                if (putInst.getDeclaredField() == this.iField.getReference()) {
                    int val = putInst.getVal();
                    if (st.isConstant(val)) {
                        ans.add(st.getConstantValue(val));
                        this.paramValue.put(pos, ans);
                        uses.clear();
                        return true;
                    } else {
                        uses.clear();
                        uses.add(val);
                        //check array store
                        boolean flag = false;
                        Iterator<SSAInstruction> useInst = initNode.getDU().getUses(val);
                        while(useInst.hasNext()){
                            SSAInstruction UInst = useInst.next();
                            if(UInst instanceof SSAArrayStoreInstruction){
                                SSAArrayStoreInstruction arrayInst = (SSAArrayStoreInstruction) UInst;
                                int use = arrayInst.getValue();
                                if(st.isConstant(use)){
                                    ans.add(st.getConstantValue(use));
                                    this.paramValue.put(pos, ans);
                                    flag = true;
                                }
                            }
                        }

                        if(flag == true) {
                            uses.clear();
                            return true;
                        }


                        IntraRetrive intra = new IntraRetrive(du, st, val, backwardSuperGraph,paramValue);
                        uses =  intra.getDU(uses,pos,visited,ans);
                        if(!uses.isEmpty()){
                            System.out.println("cannot find the value in the <init> function!");
                            return false;
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public  boolean setInstValue(InstanceFieldKey insLoc, int pos, List<Object>ans, CallGraph completeCG){
        InstanceKey insKey = insLoc.getInstanceKey();
        Iterator<Pair<CGNode, NewSiteReference>> siteIn = insKey.getCreationSites(completeCG);
        while (siteIn.hasNext()) {
            Pair<CGNode, NewSiteReference> next = siteIn.next();
            if (isPrimordial(next.fst)) continue;
            SymbolTable st = next.fst.getIR().getSymbolTable();
            DefUse du = next.fst.getDU();
//            if(!(next.fst.getMethod().getDeclaringClass().getAllFields().contains(iField))) {
//                break;
//            }
            int value = next.fst.getIR().peiMapping.get(new ProgramCounter(next.snd.getProgramCounter()));
            SSAInstruction creation = next.fst.getIR().getInstructions()[value];
            int creDef= creation.getDef();
            Iterator<SSAInstruction> creInsts= du.getUses(creDef);
            Set<Integer> newUse = new HashSet<>();

            while(creInsts.hasNext()){
                SSAInstruction inst = creInsts.next();
                if(inst instanceof SSAInvokeInstruction && ((SSAInvokeInstruction) inst).getDeclaredTarget().getName().toString().contains("<init>")){
                    for(int i =0; i< inst.getNumberOfUses();i++){
                        SSAInvokeInstruction targetNewInst= (SSAInvokeInstruction) inst;
                        for (int j =i+1; j< inst.getNumberOfUses(); j++){
                            newUse.add(inst.getUse(j));
                        }
                        break;
                    }
                    break;
                }

            }

//            SSAInstruction[] instList = next.fst.getIR().getInstructions();
//            for(SSAInstruction inst: instList){ //find the allocation site
//                if(inst instanceof SSAInvokeInstruction && ((SSAInvokeInstruction) inst).getDeclaredTarget().getName().toString().contains("<init>")){
//                    for(int i =0; i< inst.getNumberOfUses();i++){
//                        if(inst.getUse(i) == creDef){
//                            SSAInvokeInstruction targetNewInst= (SSAInvokeInstruction) inst;
//                            for (int j =i+1; j< inst.getNumberOfUses(); j++){
//                                newUse.add(inst.getUse(j));
//                            }
//                            break;
//                        }
//                    }
//                    break;
//                }
//                continue;
//            }
            for(int use: newUse){
                if (st.isConstant(use)) {
                    ans.add(st.getConstantValue(use));
                    paramValue.put(pos, ans);
                    return true;
                } else {
                    Set<Integer> visited = new HashSet<>();
                    IntraRetrive intra = new IntraRetrive(du, st, use, backwardSuperGraph,paramValue);
                    newUse = intra.getDU(newUse,pos,visited,ans);
                    if(newUse.isEmpty()) return true;
                }
            }

        }
        return false;
    }

    public Set<CGNode> searchInit(CallGraph completeCG) {
        CGNode initNode = null;
        IClass curclass = targetStmt.getNode().getMethod().getDeclaringClass();
        Collection<? extends IMethod> methodList =curclass.getDeclaredMethods();

//        if(curclass.getSuperclass().isAbstract()){
//            Collection<? extends IMethod> methodList1 = curclass.getSuperclass().getDeclaredMethods();
//            //methodList.addAll()
//            for (IMethod m : methodList1) {
//                ShrikeCTMethod method = (ShrikeCTMethod) m;
//                MethodReference mr = method.getReference();
//
//                if (mr.getName().toString().contains("init")) {
//                    Set<CGNode> nodes = completeCG.getNodes(mr);
//
//                    return nodes;
//                }
//            }
//        }
        Set<CGNode> nodes = new HashSet<>();
        for (IMethod m : methodList) {
            ShrikeCTMethod method = (ShrikeCTMethod) m;
            MethodReference mr = method.getReference();
            if (mr.getName().toString().contains("init")) {
                 nodes= completeCG.getNodes(mr);
            }
        }
        return nodes;
    }


    public boolean isPrimordial(CGNode n) {
        return n.getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial");
    }


}
