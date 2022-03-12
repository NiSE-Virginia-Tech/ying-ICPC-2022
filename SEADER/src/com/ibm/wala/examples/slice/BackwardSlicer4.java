package com.ibm.wala.examples.slice;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.dataflow.IFDS.BackwardsSupergraph;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.examples.ExampleUtil;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;

import java.io.IOException;
import java.util.*;


public class BackwardSlicer4 {

    private HashMap<Integer, List<Object>> paramValue = new HashMap<>();
    //private List<Statement> stmtList = new ArrayList<>();// save the filter slice result
    private Set<String> fieldNames = new HashSet<>();
    private Map<String, Object> varMap = new HashMap<>();
    private Map<SSAInstruction, Object> instValMap = new HashMap<>();
    private Set<Statement> allRelatedStmt = new HashSet<>();
    private List<String> classorder = new ArrayList<>();
    private Map<String, String> classInitmap = new HashMap<>();
    private Map<String, Map<Integer, List<Object>>> classVarMap = new HashMap<>();
    //private Statement targetStmt;
    private ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph;
    private CallGraph completeCG;
    private HeapModel heapModel;
    private SDG<InstanceKey> completeSDG;
    private FieldReference fieldRef;
    private ClassHierarchy cha;
    private PointerAnalysis<InstanceKey> pa;

    public void run(String path,
                    String callee,
                    String functionType
    ) throws IOException, ClassHierarchyException, CancelException {
        Slicer.DataDependenceOptions dataDependenceOptions = Slicer.DataDependenceOptions.FULL;
        Slicer.ControlDependenceOptions controlDependenceOptions = Slicer.ControlDependenceOptions.FULL;
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(path, null);
        ExampleUtil.addDefaultExclusions(scope);
        cha = ClassHierarchyFactory.make(scope);
        Set<Entrypoint> entryPoints = new HashSet<>();
        for (IClass klass : cha) {
            if (!klass.isInterface() && !klass.getClassLoader().getName().toString().contains("Primordial")) {
                for (IMethod method : klass.getDeclaredMethods()) {
                    entryPoints.add(new DefaultEntrypoint(method, cha));
                }
            }
        }
//            Iterable<Entrypoint> entryPoints = new AllApplicationEntrypoints(scope, cha);
        AnalysisOptions options = new AnalysisOptions(scope, entryPoints);
        AnalysisCacheImpl cache = new AnalysisCacheImpl();
        CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneCFABuilder(Language.JAVA, options,
                cache, cha, scope);
        completeCG = builder.makeCallGraph(options, null);
        Set<CGNode> keep = new HashSet<>();
        for (CGNode n: completeCG) {
            if (!isPrimordial(n))
                keep.add(n);
        }
        PrunedCallGraph pcg = new PrunedCallGraph(completeCG, keep);
        completeCG = pcg;
        completeSDG = new SDG<>(completeCG, builder.getPointerAnalysis(), dataDependenceOptions, controlDependenceOptions);
        pa = builder.getPointerAnalysis();
        this.heapModel = pa.getHeapModel();
        SDGSupergraph forwards = new SDGSupergraph(completeSDG, true);
        backwardSuperGraph = BackwardsSupergraph.make(forwards);

        for (CGNode node : completeCG) {
            findAllCallTo(node, callee, functionType);
        }

        for (Statement stmt : allRelatedStmt) {
            Statement targetStmt = stmt;
            clearInit();
            cache.clear();
            String className = targetStmt.getNode().getMethod().getDeclaringClass().getName().toString();
            if (className.compareTo("Lorg/cryptoapi/bench/predictablecryptographickey/PredictableCryptographicKeyABICase2") != 0)
                continue;
            Collection<CGNode> roots = new ArrayList<>();
            roots.add(targetStmt.getNode());

            Collection<Statement> relatedStmts = Slicer.computeBackwardSlice(targetStmt, completeCG, builder.getPointerAnalysis(),
                    dataDependenceOptions, controlDependenceOptions);
//             Filter all non application stmts
//            Collection<Statement> relatedStmts = Slicer.computeBackwardSlice(completeSDG, targetStmt);
            List<Statement> stmtList = filterStatement(relatedStmts);
            //Graph<Statement> g = pruneCG(completeCG, completeSDG, targetStmt.getNode());
            setParamValue(targetStmt, stmtList);
            //Vulnerfinder(stmtList,g,targetStmt, className);
        }

    }


    public Set<Integer> getDU(Set<Integer> uses, int pos, IR ir, SymbolTable st, Set<Integer> visited, List<Object> ans, DefUse du) {
        Queue<Integer> q = new LinkedList<>();
        q.addAll(uses);
        while (!q.isEmpty()) {
            int use1 = q.poll();
            // if the use has def, check the def's use could be retrival, else, continue this process;
            if (du.getDef(use1) != null) {
                SSAInstruction inst = du.getDef(use1);
                if (inst.getNumberOfUses() == 0 && inst instanceof SSAGetInstruction)
                    continue;
                uses.remove(use1);
                for (int j = 0; j < inst.getNumberOfUses(); j++) {
                    int use = inst.getUse(j);
                    // should dealwith getbyte(), get the first use.
                    if (j == 0 && (inst instanceof SSAInvokeInstruction)
                            && ((SSAInvokeInstruction) inst).getDeclaredTarget().getSelector().getName().toString().contains("getBytes")) {
                        if (!st.isConstant(use)) {
                            uses.add(use);
                            q.add(use);
                        } else {
                            if (uses.size() == 0 && !visited.contains(use)) {
                                ans.add(st.getConstantValue(use));
                                this.paramValue.put(pos, ans);
                                visited.add(use);
                            }
                        }
                        break;
                    }

                    //TODO: handel the instance field, if the usenumber ==1, should record the def, else should record use, check constructor;
                    //


                    if (j == 0 && !isInstStatic(inst))
                        continue;

                    //not sure it can deal with field value;

                    if (inst instanceof SSAGetInstruction) {
                        uses.add(inst.getDef());
                        visited.add(use);
                        return uses;
                    }

                    if (!st.isConstant(use)) {
                        uses.add(use);
                        q.add(use);
                        //if (du.getDef(use) != null) definsts.add(du.getDef(use));
                    } else {
                        if (uses.size() == 0 && !visited.contains(use)) {
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


    public void setParamValue(Statement targetStmt, List<Statement> stmtList) {
        /*first round loop must have use, all checked are SSAinvokeinst */
        SSAInstruction targetInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
        Set<Integer> uses = new HashSet<>();
        CGNode targetNode = targetStmt.getNode();
        IR targetIR = targetNode.getIR();
        SymbolTable st = targetIR.getSymbolTable();
        DefUse du = targetNode.getDU();
        Set<Integer> visited = null;

        if (targetInst instanceof SSAInvokeInstruction) {
            int i = ((SSAInvokeInstruction) targetInst).isStatic() == true ? 0 : 1;
            int neg = ((SSAInvokeInstruction) targetInst).isStatic() == true ? 0 : -1;
            int numOfUse = targetInst.getNumberOfUses();
            //get all parameter, by process one by one
            while (i < numOfUse) {
                List<Object> ans = new ArrayList<>(); //have more possible value;
                int use = targetInst.getUse(i);
                if (st.isConstant(use)) {
                    ans.add(st.getConstantValue(use));
                    paramValue.put(i + neg, ans);
                } else {
                    uses.add(use);
                    uses = getDU(uses, i + neg, targetIR, st, visited, ans, du);// can't get the parameter within one block;
                    if (!uses.isEmpty()) {
                        useCheckHelper(targetStmt, uses, stmtList, visited, ans, i + neg);
                        //loopStatementInBlock(targetStmt, uses, stmtInBlock, i + neg);
                    } else return;
                    //setParamValue(targetStmt, uses, stmtInBlock, i + neg);
                }
                i++;
            }
            if (paramValue.size() == numOfUse)
                return;
        }
    }

    public void useCheckHelper(Statement targetStmt, Set<Integer> uses, List<Statement> stmtList, Set<Integer> visited, List<Object> ans, int pos) {
        for (int use : uses) {
            usechek(use, targetStmt, uses, stmtList, visited, ans, pos);
        }
    }

    public void usechek(int use, Statement targetStmt, Set<Integer> uses, List<Statement> stmtList, Set<Integer> visited, List<Object> ans, int pos) {
        Statement tmp = checkPassin(targetStmt, use, uses, stmtList);
        if (tmp != null) {
            targetStmt = getCalleePosition(tmp);
            if (targetStmt != null) {
                /*check stmtlist have the stmt, has, not has*/
                if (stmtList.contains(targetStmt)) {
                    SSAInstruction inst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
                    int newUse = inst.getUse(use - 1);
                    uses.remove(use);
                    uses.add(newUse);
                    setParamValue(targetStmt, stmtList);
                } else {
                    System.out.println("should loop the pre node with in block");
                }
                //q.add(newUse);
                return;
            } else {
                //no caller found in the cg, let's check constructor;
                SSAInstruction inst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
                uses.add(inst.getUse(0)); //get itself and see the result
                //q.add(inst.getUse(0));
            }
        } else {
            System.out.println("not passin, checkStaticField");
        }

        StatementWithInstructionIndex getFieldStmt = checkStaticField(targetStmt, use, uses, stmtList);
        if (getFieldStmt != null) {
            SSAGetInstruction getinst = (SSAGetInstruction) getFieldStmt.getInstruction();
            int indexnumber = getinst.iIndex();
            FieldReference fieldRef = getinst.getDeclaredField();
            SSAInstruction targetInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
            if (!fieldNames.isEmpty())
                fieldNames.remove(((SSAGetInstruction) targetInst).getDeclaredField().getName().toString());
            String fieldName = fieldRef.getName().toString();
            this.fieldNames.add(fieldName);
            uses.remove(use);
            StatementWithInstructionIndex putstmt = retrivePutStaticStmt(getFieldStmt, fieldName, fieldNames, stmtList, indexnumber);
            checkPutStatic(getFieldStmt, targetStmt, putstmt, uses, use, stmtList, visited, ans, pos);
        } else {
            System.out.println("not static field, check instance field");
        }

    }

    public Statement checkPassin(Statement targetStmt, int use, Set<Integer> uses, List<Statement> stmtList) {
        boolean flag = false;
        String func = targetStmt.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                targetStmt.getNode().getMethod().getSelector().getName().toString();

        for (int i = 0; i < stmtList.size(); i++) {
            Statement stm = stmtList.get(i);
            String signature = stm.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                    stm.getNode().getMethod().getSelector().getName().toString();
            if (signature.compareToIgnoreCase(func) != 0) continue;

            if (stm.getKind() == Statement.Kind.PARAM_CALLER) {
                ParamCaller paramCaller = (ParamCaller) stm;
                int valNum = paramCaller.getValueNumber();
                //SymbolTable st = paramCaller.getNode().getIR().getSymbolTable();
                if (use == valNum) {
                    continue;
                }
            }
            // paramter be called by others
            if (stm.getKind() == Statement.Kind.PARAM_CALLEE) {
                ParamCallee paramCallee = (ParamCallee) stm;
                int valnum = paramCallee.getValueNumber();
                if (valnum == use) {
                    uses.remove(use);
                    flag = true;
                }
            }

            if (flag) {
                if (stm.getKind() == Statement.Kind.METHOD_ENTRY) {
                    return stm;
                }
            } else
                continue;

        }
        return null;
    }

    public Statement getCalleePosition(Statement stm) {
        //FilterIterator<?> it = (FilterIterator<?>) backwardSuperGraph.getCalledNodes(stm);
        Iterator<Statement> succ = backwardSuperGraph.getSuccNodes(stm);
        while (succ.hasNext()) {
            Statement s = succ.next();
            if (s.getNode().getMethod().getDeclaringClass().getName().toString().contains("FakeRootClass")) continue;
            return s;
        }
        return null;
    }

    public StatementWithInstructionIndex checkStaticField(Statement targetStmt, int use, Set<Integer> uses, List<Statement> stmtList) {
        String func = targetStmt.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                targetStmt.getNode().getMethod().getSelector().getName().toString();
        for (int i = 0; i < stmtList.size(); i++) {
            Statement stm = stmtList.get(i);
            String signature = stm.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                    stm.getNode().getMethod().getSelector().getName().toString();
            if (signature.compareToIgnoreCase(func) != 0) continue;

            if (!(stm instanceof StatementWithInstructionIndex)) continue;

            SSAInstruction inst = ((StatementWithInstructionIndex) stm).getInstruction();
            if (inst instanceof SSAGetInstruction) {
                SSAGetInstruction getInst = (SSAGetInstruction) inst;
                if (getInst.getDef() == use && getInst.isStatic()) {
                    return (StatementWithInstructionIndex) stm;
                }
            }
            continue;

        }
        return null;
    }

    public StatementWithInstructionIndex retrivePutStaticStmt(Statement getStmt, String fieldname, Set<String> fieldNames, List<Statement> stmtList, int indexnumber) {

        // getstmt should be set as targetstmt(invoked place) when in different block
        String func = getStmt.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                getStmt.getNode().getMethod().getSelector().getName().toString();
        IR ir = getStmt.getNode().getIR();
        SymbolTable st = ir.getSymbolTable();
        int bound = stmtList.indexOf(getStmt);
        for (int i = bound; i > 0; i--) {//get the latest put field;
            Statement stm = stmtList.get(i);
            String signature = stm.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                    stm.getNode().getMethod().getSelector().getName().toString();
            if (signature.compareToIgnoreCase(func) != 0) continue;

            if (stm instanceof StatementWithInstructionIndex) {
                StatementWithInstructionIndex indexput = (StatementWithInstructionIndex) stm;
                SSAInstruction inst = indexput.getInstruction();
                int index = indexput.getInstructionIndex();
                //if find the  paired put, return
                if (inst instanceof SSAPutInstruction && ((SSAPutInstruction) inst).isStatic() && index < indexnumber) {
                    SSAPutInstruction putinst = (SSAPutInstruction) inst;
                    if (putinst.getDeclaredField().getName().toString().compareToIgnoreCase(fieldname) == 0) {
                        this.fieldNames.add(fieldname);
                        int use = putinst.getVal();
                        if (st.isConstant(use)) {
                            varMap.put(putinst.getDeclaredField().getName().toString(), st.getConstantValue(use));
                            this.fieldNames.remove(fieldname);
                            //return indexput;
                            //TODO:here should be figure out, return null?
                        } else {
                            return indexput;
                        }


                    }
                }

            } else continue;
        }
        return null;
    }

    int i = 0;


    public void checkPutStatic(StatementWithInstructionIndex getStmt, Statement targetStmt, StatementWithInstructionIndex putstmt, Set<Integer> uses, int use, List<Statement> stmtList, Set<Integer> visited, List<Object> ans, int pos) {
        if (putstmt == null) {
//            if(fieldNames.isEmpty()) return;
//            /*TODO::complecated case 2) loop stmtinblock no put, lose the use trace, should use the fieldname as trace; go back the upper layer and check stmt one by one*/
//            uses.remove(use);
//            Statement methodentry = getMethodEntry(targetStmt, stmtList); //here should get the callermethod node
//            System.out.println("-----------No putinst found, loop Stmt " + i + "in block, check callsit before getstmt..-------");
//            i++;
//            //checkCallSiteInst(methodentry,getStmt);
//            checkCallsite(getStmt, methodentry, targetStmt, uses, use, stmtList, visited, ans, pos);
//
//            if (!fieldNames.isEmpty()) {
//                System.out.println("-----------not found with in block, check the caller block-------");
//                targetStmt = getCalleePosition(methodentry);
//                methodentry = getMethodEntry(targetStmt, null);
//                if (targetStmt != null) {
//                    loopStatementWithStaticField(getStmt, methodentry, targetStmt, uses, use, stmtList, visited, ans, pos);
//                    //setParamValue(targetStmt,stmtList,fieldNames);
//                } else {
//                    System.out.println("Can't find the value, out of scope");
//                }
//            }
//            return;

            System.out.println("not in the block, check heap_param_callee");
            Iterator<Statement> heapCallees = completeSDG.getPredNodes(getStmt);
            while (heapCallees.hasNext()){
                Statement callee = heapCallees.next();
                if(callee instanceof HeapStatement.HeapParamCallee){
                    HeapStatement.HeapParamCallee heapcallee = (HeapStatement.HeapParamCallee) callee;
                    PointerKey  loc= heapcallee.getLocation();
                    if(loc instanceof StaticFieldKey){
                        StaticFieldKey staticLoc = (StaticFieldKey) loc;
                        if(fieldNames.contains(staticLoc.getField().getName().toString())){

                        }
                    }
                }
            }

        } else {
            CGNode putnode = putstmt.getNode();
            SymbolTable st = putnode.getIR().getSymbolTable();
            SSAPutInstruction putInst = (SSAPutInstruction) putstmt.getInstruction();
            uses.remove(use);
            int putuse = putInst.getVal();
            Set<Integer> newUses = new HashSet<>();
            newUses.add(putuse);

            newUses = getDU(newUses, pos, putnode.getIR(), st, visited, ans, putnode.getDU());
            useCheckHelper(putstmt, newUses, stmtList, visited, ans, pos);
        }
    }



//    public void checkCallSiteInst(Statement targetStmt, Statement endUseStmt){
//
//        CGNode targetNode= targetStmt.getNode();
//
//        int index = ((StatementWithInstructionIndex)endUseStmt).getInstructionIndex();
//        List<SSAAbstractInvokeInstruction> reverseOrderSite = new ArrayList<>();
//        List<SSAAbstractInvokeInstruction> callSiteInst = getCallSiteInst(targetNode);
//
//        for(SSAAbstractInvokeInstruction inst: callSiteInst ){
//            if(inst.iindex < index && inst instanceof  SSAInvokeInstruction)
//                reverseOrderSite.add(inst);
//        }
//        Collections.reverse(reverseOrderSite);
//        if(reverseOrderSite.isEmpty()) return;
//        for(SSAAbstractInvokeInstruction inst: reverseOrderSite){
//            checkEachCGnode(inst);
//        }
//    }

    public List<SSAAbstractInvokeInstruction> getCallSiteInst(CGNode targetNode){
        IR callerIR = targetNode.getIR();
        Iterator<CallSiteReference> callSiteRefs = targetNode.iterateCallSites();
        List<SSAAbstractInvokeInstruction> callSiteInst = new ArrayList<>();


        while(callSiteRefs.hasNext()){
            CallSiteReference ref = callSiteRefs.next();
            if (ref.getDeclaredTarget().getName().toString().contains("fakeWorldClinit")) continue;
            MethodReference mRef = ref.getDeclaredTarget();
            if (mRef.getDeclaringClass().getClassLoader().getName().toString().contains("Primordial")) continue;
            SSAAbstractInvokeInstruction callInstrs[] = callerIR.getCalls(ref);//here the inst not 1?
            if(callInstrs!= null){
                callSiteInst.add(callInstrs[0]); // look the code in Js, see the first is the invocationinst
            }

        }

        return callSiteInst;
    }
    public  void  checkEachCGnode(SSAAbstractInvokeInstruction inst){
        Set<CGNode> itNodes =  completeCG.getNodes(inst.getDeclaredTarget()); // here may have a lot of node
        for(CGNode node: itNodes){

            SSAInstruction[] insts =  node.getIR().getInstructions();
            List<SSAAbstractInvokeInstruction> invokeInsts =  getCallSiteInst(node);
        }

    }

    public void checkCallsite(StatementWithInstructionIndex targetgetStmt, Statement methodEntry, Statement targetStmt, Set<Integer> uses, int use, List<Statement> stmtList, Set<Integer> visited, List<Object> ans, int pos) {
        /*get method invocaktion stmt in current block, callsite, should I method entry ?  */
        //get methodcall
        //NormalStatement n = (NormalStatement) targetgetStmt;
        PDG<?> pdg = completeSDG.getPDG(targetgetStmt.getNode());
        Iterator<Statement> statements = this.backwardSuperGraph.getSuccNodes(methodEntry);//.e should it be methodentry
        backwardSuperGraph.getCallSites(methodEntry,pdg);

        Iterator<Statement> statementWithinPreNode;
        List<Statement> newStmtList = new ArrayList<>();
        Statement newTargetStmt = null;

        while (statements.hasNext()) {
            Statement current = statements.next();
            Iterator<Statement> findEntryNode = this.backwardSuperGraph.getPredNodes(current);
            while (findEntryNode.hasNext()) {
                Statement possiEntry = findEntryNode.next();
                if (possiEntry.getNode().getMethod().getSelector().toString().
                        compareToIgnoreCase(targetStmt.getNode().getMethod().getSelector().toString()) == 0)
                    continue;
                if (possiEntry.getKind() == Statement.Kind.METHOD_ENTRY) {
                    System.out.println("here is the entry point");
                    statementWithinPreNode = backwardSuperGraph.getPredNodes(possiEntry);
                    SSAInstruction[] SSAintructions = possiEntry.getNode().getIR().getInstructions();
                    System.out.println(SSAintructions);
                    while (statementWithinPreNode.hasNext()) {
                        newTargetStmt = statementWithinPreNode.next();
                        newStmtList.add(newTargetStmt);
                    }

                    System.out.println("start check the block" + possiEntry.getNode().getMethod().toString());
                    loopStatementWithStaticField(targetgetStmt, possiEntry, newTargetStmt, uses, use, newStmtList, visited, ans, pos);
                }
            }

        }
        return;

    }

    public void loopStatementWithStaticField(StatementWithInstructionIndex getStmt, Statement methodEntry, Statement targetStmt, Set<Integer> uses, int use, List<Statement> stmtList, Set<Integer> visited, List<Object> ans, int pos) {
        //check if there is put stmt;

        int index = ((StatementWithInstructionIndex) targetStmt).getInstructionIndex();
        String fieldname = fieldNames.iterator().next();
        StatementWithInstructionIndex putStatic = retrivePutStaticStmt(targetStmt, fieldname, fieldNames, stmtList, index);
        getStmt = (StatementWithInstructionIndex) targetStmt;
        checkPutStatic(getStmt, targetStmt, putStatic, uses, use, stmtList, visited, ans, pos);
        return;
//        for (int i = bound; i > 0; i--) {
//            Statement stm = stmtInBlock.get(i);
//            if (stm instanceof StatementWithInstructionIndex) {
//                int stmIndex = ((StatementWithInstructionIndex) stm).getInstructionIndex();
//                if (stmIndex < index) {
//                    SSAInstruction inst = ((StatementWithInstructionIndex) stm).getInstruction();
//                    if (inst instanceof SSAInvokeInstruction) {
//                        loopPreNode(stm, pos);
//                        return;
//                    }
//
//                    if (inst instanceof SSAPutInstruction) {
//                        /* TODO:check if use the same field value;
//                        *   if same: then get the use number and get the value from st, can;t find in st, use the usenumber futher trace back;
//                        * possible 1: change the fieldname(use-def is getstatic) getstatic; 2 passin parameter
//                       done here*/
//                        SSAPutInstruction putinst =  (SSAPutInstruction) inst;
//                        if((putinst.isStatic() && fieldNames.contains(putinst.getDeclaredField().getName().toString()))){
//                            int newuse = putinst.getVal();
//                            Set<Integer> newuses = new HashSet<>();
//                            newuses.add(newuse);
//                            checkPutStatic(getStmt, (StatementWithInstructionIndex) stm, newuses, newuse, stmtList, visited,  ans, pos);
//                        }
//
//                    }
//                }
//            }
//        }
    }

    //public void setParamValue(Statement targetStmt,)

    public List<Statement> filterStatement(Collection<Statement> relatedStmts) {
        List<Statement> stmtList = new ArrayList<>();
        for (Statement stmt : relatedStmts) {
            if (!stmt.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial")) {
                stmtList.add(stmt);
            }
        }
        return stmtList;
    }

    //find a signle one
    public Statement findCallTo(CGNode n, String methodName, String methodType, String mainclass) {
        IR ir = n.getIR();
        if (ir == null) return null;
        if (ir.getMethod().getDeclaringClass().getName().toString().compareTo(mainclass) != 0) return null;
        for (SSAInstruction s : Iterator2Iterable.make(ir.iterateAllInstructions())) {
            if (s instanceof SSAInvokeInstruction) {
                SSAInvokeInstruction call = (SSAInvokeInstruction) s;
                // Get the information binding
                String methodT = call.getCallSite().getDeclaredTarget().getSignature();
                if (call.getCallSite().getDeclaredTarget().getName().toString().compareTo(methodName) == 0
                        && methodT.contains(methodType)) {
                    // 一个例子
                    //if (call.getCallSite().getDeclaredTarget().getSignature().contains("Cipher")) continue;
                    IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
                    Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
                    return new NormalStatement(n, indices.intIterator().next());
                }
            }
        }
        //Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
        return null;
    }

    public void clearInit() {
        varMap.clear();
        instValMap.clear();
        paramValue.clear();
        //stmtList.clear();
        this.classorder.clear();
    }

    /**
     * Get all related target function statements
     *
     * @param n
     * @param methodName
     * @param methodType
     */
    public void findAllCallTo(CGNode n, String methodName, String methodType) {
        IR ir = n.getIR();
        if (ir == null) return;
        for (SSAInstruction s : Iterator2Iterable.make(ir.iterateAllInstructions())) {
            if (s instanceof SSAInvokeInstruction) {
                SSAInvokeInstruction call = (SSAInvokeInstruction) s;
                // Get the information binding
                String methodT = call.getCallSite().getDeclaredTarget().getSignature();
                if (call.getCallSite().getDeclaredTarget().getName().toString().compareTo(methodName) == 0
                        && methodT.contains(methodType)) {
                    // 一个例子
                    //if (call.getCallSite().getDeclaredTarget().getSignature().contains("Cipher")) continue;
                    IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
                    Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
                    allRelatedStmt.add(new NormalStatement(n, indices.intIterator().next()));
                }
            }
        }
        //Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
        return;
    }

    public Statement getMethodEntry(Statement targetStmt, List<Statement> stmtInBlock) {
        String signature = targetStmt.getNode().getMethod().getSignature();
        Queue<Statement> q = new LinkedList<>();
        q.add(targetStmt);
        Statement ans = null;
        Set<Statement> visited = new HashSet<>();
        visited.add(targetStmt);
        while(!q.isEmpty()) {
            Statement head = q.poll();
            if (head.getKind() == Statement.Kind.METHOD_ENTRY && head.getNode().getMethod().getSignature().compareToIgnoreCase(signature) == 0) {
                ans = head;
                break;
            }
            else {
                Iterator<Statement> it = completeSDG.getPredNodes(head);
                while(it.hasNext()) {
                    Statement s = it.next();
                    if (!visited.contains(s)) {
                        q.add(s);
                        visited.add(s);
                    }
                }
            }
        }
//        System.out.println(ans);
//        for (int i = 0; i < stmtInBlock.size(); i++) {
//            Statement stm = stmtInBlock.get(i);
//            if (stm.getKind() == Statement.Kind.METHOD_ENTRY && stm.getNode().getMethod().getSignature().compareToIgnoreCase(signature) == 0) {
//                return stm;
//            } else continue;
//        }
        assert(ans != null);
        return ans;
    }

    public boolean isInstStatic(SSAInstruction inst) {
        if (inst instanceof SSAAbstractInvokeInstruction) return true;
        if (inst instanceof SSAInvokeInstruction) return ((SSAInvokeInstruction) inst).isStatic();
        if (inst instanceof SSAGetInstruction) return ((SSAGetInstruction) inst).isStatic();
        if (inst instanceof SSAPutInstruction) return ((SSAPutInstruction) inst).isStatic();
        //abstractinvoke from 0;
        return false;
    }

    public boolean isPrimordial(CGNode n) {
        return n.getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial");
    }

    public boolean isPrimordial(Statement s) {
        return s.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial");
    }
}
