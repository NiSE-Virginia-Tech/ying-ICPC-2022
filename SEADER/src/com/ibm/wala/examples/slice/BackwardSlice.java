package com.ibm.wala.examples.slice;

import com.ibm.wala.classLoader.*;
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
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


public class BackwardSlice {
    private ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph;
    private CallGraph completeCG = null;
    private HeapModel heapModel;
    private SDG<InstanceKey> completeSDG;
    private ClassHierarchy cha;
    private PointerAnalysis<InstanceKey> pa;
    private HashMap<Integer, List<Object>> paramValue = new HashMap<>();
    private Set<String> fieldNames = new HashSet<>();
    private Set<String> instanceFieldNames = new HashSet<>();
    private Map<String, Object> varMap = new HashMap<>(); //for save the field value
    private Map<SSAInstruction, Object> instValMap = new HashMap<>();
    private Map<String, Map<Integer, List<Object>>> classVarMap = new HashMap<>();
    private Set<Statement> allRelatedStmt = new HashSet<>();
    private List<String> classorder = new ArrayList<>();
    private Map<String, String> classInitmap = new HashMap<>();
    private ArrayList<Integer> sourceLineNums = new ArrayList<>();
    private HashMap<Integer, List<Integer>> paramsSourceLineNumsMap = new HashMap<>();
    private Map<String, HashMap<Integer, List<Integer>>> classParamsLinesNumsMap = new HashMap<>();
    private AnalysisCacheImpl cache;
    private CallGraphBuilder<InstanceKey> builder;
    private Slicer.DataDependenceOptions dataDependenceOptions = Slicer.DataDependenceOptions.FULL;
    private Slicer.ControlDependenceOptions controlDependenceOptions = Slicer.ControlDependenceOptions.FULL;
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public Map<String, List<Statement>> classStmtMap = new HashMap<>();
    //private Map<String, Map<Integer, List<Object>>> classVarMap = new HashMap<>();
    //private Statement targetStmt;


    public void run(String path,
                    String callee,
                    String functionType
    ) throws IOException, ClassHierarchyException, CancelException {

        //clear the related parameters
        init();
        completeCGBuilder(path, callee, functionType);
        for (CGNode node : completeCG) {
            findAllCallTo(node, callee, functionType);
        }
        LOGGER.info("FindAllCallTo finished");

        for (Statement stmt : allRelatedStmt) {
            LOGGER.info("Processing " + stmt);
            Statement targetStmt = stmt;
            clearInit();
            String className = targetStmt.getNode().getMethod().getDeclaringClass().getName().toString();

//            if (className.compareTo("Lorg/cryptoapi/bench/predictablecryptographickey/PredictableCryptographicKeyCorrected") != 0)
//                continue;

            Collection<CGNode> roots = new ArrayList<>();
            roots.add(targetStmt.getNode());
            try {
                Collection<Statement> relatedStmts = Slicer.computeBackwardSlice(targetStmt, completeCG, builder.getPointerAnalysis(),
                        dataDependenceOptions, controlDependenceOptions);
                LOGGER.info("Backward Slicing Finished");
                List<Statement> stmtList = filterStatement(relatedStmts);
                setParamValue(targetStmt, stmtList);

                //System.out.println("--------------------------------------------");
                //System.out.println(targetStmt.getNode().getMethod().getReference().getSignature());
                for (int i = 0; i < paramValue.size(); i++) {
                    //System.out.println("target parameter is : " + paramValue.get(i));
                }
                classVarMap.put(className, (Map<Integer, List<Object>>) paramValue.clone());
                classParamsLinesNumsMap.put(className, (HashMap<Integer, List<Integer>>) paramsSourceLineNumsMap.clone());
                classStmtMap.put(className, stmtList);
                LOGGER.info("Finish");
            } catch (NullPointerException e) {
                //System.out.println("#Statement error#: " + targetStmt);
                e.printStackTrace();
            }
        }

    }

    private void init() {
        LOGGER.info("Init");
        paramValue = new HashMap<>();
        classVarMap = new HashMap<>();
        //private List<Statement> stmtList = new ArrayList<>();// save the filter slice result
        fieldNames = new HashSet<>();
        instanceFieldNames = new HashSet<>();
        varMap = new HashMap<>(); //for save the field value
        instValMap = new HashMap<>();
        allRelatedStmt = new HashSet<>();
        classorder = new ArrayList<>();
        classInitmap = new HashMap<>();
        sourceLineNums = new ArrayList<>();
        paramsSourceLineNumsMap = new HashMap<>();
        classParamsLinesNumsMap = new HashMap<>();
        classStmtMap = new HashMap<>();
    }

    private void completeCGBuilder(String path, String callee, String functionType) throws IOException, ClassHierarchyException, CancelException {
        LOGGER.info("Build CG");
        if (completeCG != null) return;
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
        cache = new AnalysisCacheImpl();
        builder = Util.makeZeroOneCFABuilder(Language.JAVA, options,
                cache, cha, scope);
        completeCG = builder.makeCallGraph(options, null);
        Set<CGNode> keep = new HashSet<>();
        for (CGNode n : completeCG) {
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
            List<Statement> stmtList = filterStatement(relatedStmts);
            setParamValue(targetStmt, stmtList);

            System.out.println("--------------------------------------------");
            System.out.println(targetStmt.getNode().getMethod().getReference().getSignature());
            for (int i = 0; i < paramValue.size(); i++) {
                System.out.println("target parameter is : " + paramValue.get(i));

            }
        }

    }


    public void setParamValue(Statement targetStmt, List<Statement> stmtList) {

        SSAInstruction targetInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
        Set<Integer> uses = new HashSet<>();
        CGNode targetNode = targetStmt.getNode();
        IR targetIR = targetNode.getIR();
        SymbolTable st = targetIR.getSymbolTable();
        DefUse du = targetNode.getDU();
        Set<Integer> visited = new HashSet<>();

        if (targetInst instanceof SSAInvokeInstruction) {
            int i = ((SSAInvokeInstruction) targetInst).isStatic() ? 0 : 1;
            int neg = ((SSAInvokeInstruction) targetInst).isStatic() ? 0 : -1;
            int numOfUse = targetInst.getNumberOfUses();
            //get all parameter, by process one by one
            while (i < numOfUse) {
                LOGGER.info("Finding the " + i + "th parameter");
                List<Object> ans = new ArrayList<>(); //have more possible value;
                sourceLineNums.clear();
                int use = targetInst.getUse(i);
                if (st.isConstant(use)) { //can get the para directly, put in Value map
                    ans.add(st.getConstantValue(use));
                    paramValue.put(i + neg, ans);
                    sourceLineNums.add(getLineNumber(targetInst, targetIR));
                } else {
                    uses.add(use);
                    uses = getDU(targetStmt, uses, i + neg, st, visited, ans, du);// can't get the parameter within one block;
                    if (!uses.isEmpty()) {
                        List<Statement> stmtInBlock = new ArrayList<>();
                        useCheckHelper(targetStmt, uses, stmtList, visited, ans, i + neg);
                        //loopStatementInBlock(targetStmt, uses, stmtInBlock, i + neg);
                    }
                    //setParamValue(targetStmt, uses, stmtInBlock, i + neg);
                }
                paramsSourceLineNumsMap.put(i + neg, (List<Integer>) sourceLineNums.clone());
                i++;

            }
            if (paramValue.size() == numOfUse)
                return;
        }
    }


    /* pass the use here as set, if the use can't be retrive within one block, then should do the edge check*/
    public Set<Integer> getDU(Statement targetStmt, Set<Integer> uses, int pos, SymbolTable st, Set<Integer> visited, List<Object> ans, DefUse du) {
        LOGGER.info("get the def-use chain");
        Queue<Integer> q = new LinkedList<>();
        q.addAll(uses);
        while (!q.isEmpty()) {
        Set<Integer> conUse = new HashSet<>();
            for(Integer i: uses){
                if(st.isConstant(i)){
                    ans.add(st.getConstantValue(i));
                    paramValue.put(pos, ans);
                    sourceLineNums.add(getLineNumber(du.getDef(i), targetStmt.getNode().getIR()));
                    //uses.remove(i);
                    q.remove(i);
                    visited.add(i);
                }
            }

            if (q.isEmpty()) {
                uses.clear();
                return uses;
            }
            uses.removeAll(conUse);

            Integer use1 = q.poll();
            // if the use has def, check the def's use could be retrival, else, continue this process;
            if (du.getDef(use1) != null) {
                SSAInstruction inst = du.getDef(use1);

                if (inst instanceof SSAGetInstruction) {
                    // if it is get inst, then no use traced, should check more
                    continue;
                }
                //TODO: here used to handle the map case, haven't finished
                if (inst instanceof SSACheckCastInstruction) {
                    uses.remove(use1);
                    q.add(((SSACheckCastInstruction) inst).getVal());
                    uses.add(((SSACheckCastInstruction) inst).getVal());
                    break;
                }

                if (inst instanceof SSAPhiInstruction) {
                    // if condition
                    uses.remove(use1);
                    for (int i = 0; i < inst.getNumberOfUses(); i++) {
                        uses.add(inst.getUse(i));
                        q.add(inst.getUse(i));
                        uses = getDU(targetStmt, uses, pos, st, visited, ans, du);
                    }
                    continue;
                }

                if (inst instanceof SSANewInstruction) {//TODO: how to get the prenode;
                    Iterator<SSAInstruction> insts = du.getUses(use1);
                    while (insts.hasNext()) {
                        SSAInstruction targetInst = insts.next();
                        if (targetInst instanceof SSAArrayStoreInstruction) {
                            if (use1 == ((SSAArrayStoreInstruction) targetInst).getArrayRef()) {
                                uses.remove(use1);
                                visited.add(use1);
                                q.add(((SSAArrayStoreInstruction) targetInst).getValue());
                                uses.add(((SSAArrayStoreInstruction) targetInst).getValue());
                                continue;
                            }
                        }

                        if (targetInst instanceof SSAInvokeInstruction && ((SSAInvokeInstruction) targetInst).getDeclaredTarget().getName().toString().contains("<init>")) {
                            for (int i = 0; i < targetInst.getNumberOfUses(); i++) {
                                if (use1 == targetInst.getUse(0)) {
                                    uses.remove(use1);
                                    visited.add(use1);
                                    for (int j = i + 1; j < targetInst.getNumberOfUses(); j++) {
                                        q.add(targetInst.getUse(j));
                                        uses.add(targetInst.getUse(j));
                                    }
                                    break;
                                }
                            }
                        }
                        //uses.remove(use1);
                        continue;
                    }
                    continue;

                }


                if (inst instanceof SSANewInstruction && ((SSANewInstruction) inst).getNewSite().getDeclaredType().getName().toString().contains("SecureRandom")) {
                    uses.remove(use1);
                    ans.add("random value");
                    paramValue.put(pos, ans);
                    sourceLineNums.add(getLineNumber(inst, targetStmt.getNode().getIR()));
                    uses.remove(use1);
                    continue;
                }

                if (inst.getNumberOfUses() == 0) return uses;
                uses.remove(use1);
                for (int j = 0; j < inst.getNumberOfUses(); j++) {
                    Integer use = inst.getUse(j);

                    if (j == 0 && !isInstStatic(inst))
                        continue;

                    //not sure it can deal with field value;
                    if (!st.isConstant(use)) {
                        uses.add(use);
                        q.add(use);
                        //if (du.getDef(use) != null) definsts.add(du.getDef(use));
                    } else {
                        if (uses.size() == 0 && !visited.contains(use)) {
                            ans.add(st.getConstantValue(use));
                            this.paramValue.put(pos, ans);
                            sourceLineNums.add(getLineNumber(inst, targetStmt.getNode().getIR()));
                            visited.add(use);
                        }
                    }
                }
            } else continue;
        }

        return uses;
    }

    //Set<Integer> loopUses = new HashSet<>();
    public void useCheckHelper(Statement targetStmt, Set<Integer> uses, List<Statement> stmtList, Set<Integer> visited, List<Object> ans, int pos) {
        LOGGER.info("useCheckHelper");
        HashSet<Integer> tempSet = new HashSet<>();
        tempSet = (HashSet<Integer>) ((HashSet) uses).clone();
        for (Integer use : tempSet) {
            List<Statement> stmtInBlock = getStmtInBlock(targetStmt.getNode().getMethod().getSignature(), stmtList);
            usechek(use, targetStmt, uses, stmtList, visited, ans, pos, stmtInBlock);
        }
    }

    public boolean checkSpecialCase(Set<Integer> uses, int use, List<Statement> stmtList, int pos, Set<Integer> visited, List<Object> ans, DefUse du) {
        LOGGER.info("Checking special case");
        Set<Integer> newUses = new HashSet<>();
        for (Statement st : stmtList) { //check map
            if (st instanceof StatementWithInstructionIndex) {
                if (((StatementWithInstructionIndex) st).getInstruction().hasDef()) {
                    int index = ((StatementWithInstructionIndex) st).getInstruction().getDef();
                    if (index == use) {
                        SSAInstruction inst = ((StatementWithInstructionIndex) st).getInstruction();
                        if (inst instanceof SSAAbstractInvokeInstruction && ((SSAAbstractInvokeInstruction) inst).getDeclaredTarget().getDeclaringClass().getName().toString().compareTo("Ljava/util/Map") == 0) {
                            for (int i = 0; i < inst.getNumberOfUses(); i++) {
                                newUses.add(inst.getUse(i));
                            }
                            break;
                        }
                    }
                    // Iterator<Statement> succNode =  backwardSuperGraph.getSuccNodes(st)
                }
            } else continue;
        }
        if (newUses.isEmpty()) return false;

        Set<Integer> pairUse = new HashSet<>();

        for (Statement put : stmtList) {
            if (put instanceof StatementWithInstructionIndex) {
                SSAInstruction putInst = ((StatementWithInstructionIndex) put).getInstruction();
                for (int i = 0; i < putInst.getNumberOfUses(); i++) {
                    pairUse.add(putInst.getUse(i));
                }
                if (pairUse.containsAll(newUses)) {
                    pairUse.removeAll(newUses);
                    visited.add(putInst.getDef());
                    for (Integer i : pairUse) {
                        SymbolTable st = put.getNode().getIR().getSymbolTable();
                        if (st.isConstant(i)) {
                            ans.add(st.getConstantValue(i));
                            paramValue.put(pos, ans);
                            sourceLineNums.add(getLineNumber(put));
                            return true;
                        } else {
                            uses = getDU(put, pairUse, pos, put.getNode().getIR().getSymbolTable(), visited, ans, put.getNode().getDU());
                            if (uses.isEmpty()) return true;
                            else {
                                //System.out.println("more process for hashmap");
                                return true;
                            }
                        }
                    }

                } else {
                    pairUse.clear();
                    continue;
                }
            }
        }
        return false;
    }

    public void usechek(Integer use, Statement targetStmt, Set<Integer> uses, List<Statement> stmtList, Set<Integer> visited, List<Object> ans, int pos, List<Statement> stmtInBlock) {
        LOGGER.info("useChek");
        Statement tmp = checkPassin(targetStmt, use, uses, stmtInBlock);
        if (tmp != null) {
            Iterator<Statement> callerStmt = backwardSuperGraph.getSuccNodes(tmp);
            while (callerStmt.hasNext()) {
                Statement curStmt = callerStmt.next();
                if (curStmt.getKind() == Statement.Kind.PARAM_CALLER) {
                    ParamCaller paraCaller = (ParamCaller) curStmt;
                    int curUse = paraCaller.getValueNumber();
                    SymbolTable st = curStmt.getNode().getIR().getSymbolTable();
                    if (st.isConstant(curUse)) {
                        ans.add(st.getConstantValue(curUse));
                        paramValue.put(pos, ans);
                        sourceLineNums.add(getLineNumber(curStmt));
                        return;// return or break, may have more possibility
                    } else {
                        uses.add(paraCaller.getValueNumber());
                        processParaCaller(targetStmt, paraCaller.getValueNumber(), stmtList, paraCaller, uses, visited, ans, pos, stmtInBlock);
                    }
                    //return;
                }
                if (curStmt.getKind() == Statement.Kind.HEAP_PARAM_CALLEE) {
//TODO: what is the logic here? ??
//                    processParaCallee(targetStmt, );
                    return;
                } else {
                    continue;
                }
            }
            //System.out.println("no caller for callee, method not call");
            return;
        } else {
            //System.out.println("not passin, checkStaticField");
        }
        if (uses.isEmpty()) return;
        StatementWithInstructionIndex getFieldStmt = checkStaticField(targetStmt, use, uses, stmtList, pos, visited, ans);

        if (getFieldStmt != null) {
            SSAGetInstruction getinst = (SSAGetInstruction) getFieldStmt.getInstruction();
            FieldReference fieldRef = getinst.getDeclaredField();
            SSAInstruction targetInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
            String fieldName = fieldRef.getName().toString();
            if (getinst.isStatic()) {
                this.fieldNames.add(fieldName);
                Iterator<Statement> succStatement = backwardSuperGraph.getSuccNodes(getFieldStmt);
                processSuccStmt(targetStmt, fieldName, stmtList, succStatement, uses, visited, ans, pos, stmtInBlock);
            } else {
                this.instanceFieldNames.add(fieldName);
                Iterator<Statement> succStatement = backwardSuperGraph.getSuccNodes(getFieldStmt);
                processSuccStmt(targetStmt, fieldName, stmtList, succStatement, uses, visited, ans, pos, stmtInBlock);
            }
            return;

        } else {
            //System.out.println("not static field, check special case field");
        }
        DefUse du = targetStmt.getNode().getDU();
        if (checkSpecialCase(uses, use, stmtList, pos, visited, ans, du)) {
            uses.remove(use);
            return;
        } else {
            ans.add("no value");
            paramValue.put(pos, ans);
            sourceLineNums.add(-1);
            //System.out.println("no value assigned for this para");
            uses.remove(use);
            return;
        }


    }

    public void processSuccStmt(Statement targetStmt, String fieldName, List<Statement> stmtList, Iterator<Statement> succStatement, Set<Integer> uses, Set<Integer> visited, List<Object> ans, int pos, List<Statement> stmtInBlock) {
        LOGGER.info("Process succ stmt");
        while (succStatement.hasNext()) {
            Statement currStmt = succStatement.next();
            if (currStmt.getKind() == Statement.Kind.HEAP_PARAM_CALLEE) {
                HeapStatement.HeapParamCallee heapcallee = (HeapStatement.HeapParamCallee) currStmt;
                PointerKey loc = heapcallee.getLocation();
                if (loc instanceof InstanceFieldKey) {
                    //System.out.println("instance field check");
                    Collection<IField> fields = currStmt.getNode().getMethod().getDeclaringClass().getDeclaredInstanceFields();
                    InstanceFieldKey insLoc = (InstanceFieldKey) loc;
                    if (fields.contains(insLoc.getField())) {
                        searchInit(currStmt, insLoc.getField(), pos, uses, visited);
                        return;

                    }
                    //if current instance field can't retrive the value, need more process; site & node
                    InstanceKey insKey = insLoc.getInstanceKey();
                    Iterator<Pair<CGNode, NewSiteReference>> siteIn = insKey.getCreationSites(completeCG);
                    while (siteIn.hasNext()) {
                        Pair<CGNode, NewSiteReference> next = siteIn.next();
                        SymbolTable st = next.fst.getIR().getSymbolTable();
                        //if(next.fst.getIR().getMethod().getDeclaringClass().getName().toString() != )
                        if (isPrimordial(next.fst)) continue;
                        //get the newinst and find the previous stmt-> get parameter
                        int value = next.fst.getIR().peiMapping.get(new ProgramCounter(next.snd.getProgramCounter()));
                        SSAInstruction creation = next.fst.getIR().getInstructions()[value];//get the creation SSAinst
                        int creDef = creation.getDef();
                        Set<Integer> newUse = new HashSet<>();
                        for (SSAInstruction inst : next.fst.getIR().getInstructions()) {
                            if (inst instanceof SSAInvokeInstruction && ((SSAInvokeInstruction) inst).getDeclaredTarget().getName().toString().contains("<init>")) {
                                for (int i = 0; i < inst.getNumberOfUses(); i++) {
                                    if (inst.getUse(i) == creDef) {
                                        SSAInvokeInstruction targetNewInst = (SSAInvokeInstruction) inst;
                                        for (int j = i + 1; j < inst.getNumberOfUses(); j++) {
                                            newUse.add(inst.getUse(j));
                                        }
                                        break;
                                    }
                                }
                            }
                            continue;
                        }

                        for (int use : newUse) {
                            if (st.isConstant(use)) {
                                ans.add(st.getConstantValue(use));
                                paramValue.put(pos, ans);
                                sourceLineNums.add(getLineNumber(currStmt));
                            } else {
                                newUse = getDU(currStmt, newUse, pos, st, visited, ans, next.fst.getDU());
                                if (newUse.isEmpty()) return;
                                //TODO: need more deal here!!
                                //processParaCallee(targetStmt, fieldName, stmtList, heapcallee, newUse, visited, ans, pos, stmtInBlock);
                            }
                        }

                    }
                    continue;
                }
                //    Iterator<? extends Statement> HeapCaller = backwardSuperGraph.getCalledNodes(heapcallee);
                processParaCallee(targetStmt, fieldName, stmtList, heapcallee, uses, visited, ans, pos, stmtInBlock);

                return;
            }
            if (currStmt.getKind() == Statement.Kind.PARAM_CALLEE) {
                //System.out.println(" I don't want deal with this case now");
                continue;
            }
            if (currStmt instanceof StatementWithInstructionIndex) {
                SSAInstruction curtInst = ((StatementWithInstructionIndex) currStmt).getInstruction();
                if (curtInst instanceof SSAPutInstruction) {
                    SSAPutInstruction curPut = (SSAPutInstruction) curtInst;
                    if (curPut.getDeclaredField().getName().toString().compareTo(fieldName) == 0) {
                        uses.add(curPut.getVal());
                        targetStmt = currStmt;
                        uses = getDU(currStmt, uses, pos, currStmt.getNode().getIR().getSymbolTable(), visited, ans, currStmt.getNode().getDU());
                        useCheckHelper(targetStmt, uses, stmtList, visited, ans, pos);
                        return;
                    }

                }
            }
        }

    }


    public Set<Integer> searchInit(Statement stmt, IField field, int pos, Set<Integer> uses, Set<Integer> visited) {
        LOGGER.info("Search the constructor");
        CGNode initNode = null;
        List<Object> ans = new ArrayList<>();
        Collection<? extends IMethod> methodList = stmt.getNode().getMethod().getDeclaringClass().getDeclaredMethods();
        for (IMethod m : methodList) {
            ShrikeCTMethod method = (ShrikeCTMethod) m;
            MethodReference mr = method.getReference();
            if (mr.getName().toString().contains("init")) {
                Set<CGNode> nodes = completeCG.getNodes(mr);
                //TODO: Handle multiple initalizer
                for (CGNode node : nodes) {
                    initNode = node;
                    break;
                }
            }
        }
        SymbolTable st = initNode.getIR().getSymbolTable();
        DefUse du = initNode.getDU();
        for (SSAInstruction inst : initNode.getIR().getInstructions()) {
            if (inst instanceof SSAPutInstruction) {
                SSAPutInstruction putInst = (SSAPutInstruction) inst;
                if (putInst.getDeclaredField() == field.getReference()) {
                    int val = putInst.getVal();
                    if (st.isConstant(val)) {
                        ans.add(st.getConstantValue(val));
                        this.paramValue.put(pos, ans);
                        sourceLineNums.add(getLineNumber(putInst, initNode.getIR()));
                        instanceFieldNames.remove(field.getName().toString());
                        uses.clear();
                        return uses;
                    } else {
                        uses.clear();
                        uses.add(val);
                        uses = getDU(stmt, uses, pos, st, visited, ans, du);
                        if (!uses.isEmpty()) {
                            for (Integer i : uses) {
                                if (st.isConstant(i)) {
                                    ans.add(st.getConstantValue(i));
                                    this.paramValue.put(pos, ans);
                                    sourceLineNums.add(getLineNumber(putInst, initNode.getIR()));
                                    uses.remove(i);
                                }
                            }
                        }
                        //System.out.println("Cannot find the value directly");
                        return uses;
                    }
                }
            }
        }
        return uses;
    }

    //TODO: hre should be modify to deal with heap callee and para_callee
    public void processParaCallee(Statement targetStmt, String fieldname, List<Statement> StmtList, HeapStatement.HeapParamCallee heapParaCallee, Set<Integer> uses, Set<Integer> visited, List<Object> ans, int pos, List<Statement> stmtInBlock) {
        LOGGER.info("Processing Callee");
        Iterator<Statement> succCallee = backwardSuperGraph.getSuccNodes(heapParaCallee);
        while (succCallee.hasNext()) {
            Statement curStmt = succCallee.next();
            if (curStmt.getKind() == Statement.Kind.HEAP_PARAM_CALLER) {
                HeapStatement.HeapParamCaller heapParaCaller = (HeapStatement.HeapParamCaller) curStmt;
                processParaCaller(targetStmt, fieldname, StmtList, heapParaCaller, uses, visited, ans, pos, stmtInBlock);
            }
            // any other possible??

        }
        return;

    }

    public void processParaCaller(Statement targetStmt, String fieldname, List<Statement> StmtList, HeapStatement.HeapParamCaller heapParamCaller, Set<Integer> uses, Set<Integer> visited, List<Object> ans, int pos, List<Statement> stmtInBlock) {
        LOGGER.info("Processing Caller");
        Iterator<Statement> succCaller = backwardSuperGraph.getSuccNodes(heapParamCaller);
        while (succCaller.hasNext()) {
            Statement currStmt = succCaller.next();
            if (currStmt.getKind() == Statement.Kind.HEAP_RET_CALLER) {
                HeapStatement.HeapReturnCaller heapReturnCallerStmt = (HeapStatement.HeapReturnCaller) currStmt;
                SSAAbstractInvokeInstruction call = heapReturnCallerStmt.getCall();
                String signature = call.getDeclaredTarget().getSignature();
                stmtInBlock.removeAll(stmtInBlock);
                stmtInBlock = getStmtInBlock(signature, StmtList);
                for (int i = 0; i < stmtInBlock.size(); i++) {
                    Statement returnCallee = stmtInBlock.get(i);
                    if (returnCallee.getKind() == Statement.Kind.HEAP_RET_CALLEE) {
                        HeapStatement.HeapReturnCallee returnCalleeStmt = (HeapStatement.HeapReturnCallee) returnCallee;
                        if (returnCalleeStmt.getLocation().equals(heapReturnCallerStmt.getLocation())) {
                            Iterator<Statement> succStmt = backwardSuperGraph.getSuccNodes(returnCalleeStmt);
                            processSuccStmt(returnCallee, fieldname, StmtList, succStmt, uses, visited, ans, pos, stmtInBlock);
                            return;
                        }

                    }
                }

                // backwardSuperGraph.getCalledNodes(heapReturnStmt);
            }
            //TODO: no method call it , not return caller?
            if (currStmt.getKind() == Statement.Kind.HEAP_PARAM_CALLEE) {
                HeapStatement.HeapParamCallee hepcallee = (HeapStatement.HeapParamCallee) currStmt;
                PointerKey loc = hepcallee.getLocation();
                if (loc instanceof StaticFieldKey) {
                    StaticFieldKey staticLoc = (StaticFieldKey) loc;
                    IField ifeild = staticLoc.getField();
                    Set<Integer> newUse = searchInit(currStmt, ifeild, pos, uses, visited);

                }

            }
        }
    }

    public void processParaCaller(Statement targetStmt, int use, List<Statement> StmtList, ParamCaller paramCaller, Set<Integer> uses, Set<Integer> visited, List<Object> ans, int pos, List<Statement> stmtInBlock) {
        LOGGER.info("Processing Caller");
        Iterator<Statement> succCaller = backwardSuperGraph.getSuccNodes(paramCaller);
        while (succCaller.hasNext()) {
            Statement currStmt = succCaller.next();
            if (currStmt.getKind() == Statement.Kind.NORMAL_RET_CALLER) {
                //if is returned by some method, should find the called place of the method.
                CGNode callerNode = paramCaller.getNode();
                SymbolTable st = callerNode.getIR().getSymbolTable();
                DefUse du = callerNode.getDU();
                Set<Integer> newUses = new HashSet<>();
                newUses.add(use);
                uses.remove(use);
                newUses = getDU(currStmt, newUses, pos, st, visited, ans, du);
                useCheckHelper(paramCaller, newUses, StmtList, visited, ans, pos);
                return;
            }
            if (currStmt.getKind() == Statement.Kind.PARAM_CALLEE) {
                ///dd
                ParamCallee paramCallee = (ParamCallee) currStmt;
                int valnum = paramCallee.getValueNumber();
                if (valnum == use) {
                    uses.remove(use);
                }

                Iterator<Statement> callerStmt = backwardSuperGraph.getSuccNodes(currStmt);
                while (callerStmt.hasNext()) {
                    Statement curStmt = callerStmt.next();
                    if (curStmt.getKind() == Statement.Kind.PARAM_CALLER) {
                        ParamCaller paraCaller = (ParamCaller) curStmt;
                        int newUse = paraCaller.getValueNumber();
                        SymbolTable st = curStmt.getNode().getIR().getSymbolTable();
                        if (st.isConstant(newUse)) {
                            ans.add(st.getConstantValue(newUse));
                            paramValue.put(pos, ans);
                            sourceLineNums.add(getLineNumber(curStmt));
                            return;
                        } else {
                            uses.add(paraCaller.getValueNumber());
                            processParaCaller(targetStmt, paraCaller.getValueNumber(), StmtList, paraCaller, uses, visited, ans, pos, stmtInBlock);
                        }
                        return;
                    }
                    if (curStmt.getKind() == Statement.Kind.HEAP_PARAM_CALLEE) {
//TODO: what is the logic here? ??
//                    processParaCallee(targetStmt, );
                        return;
                    } else {
                        continue;
                    }
                }


            }

            if (currStmt instanceof StatementWithInstructionIndex) {
                SSAInstruction inst = ((StatementWithInstructionIndex) currStmt).getInstruction();
                if (inst instanceof SSAGetInstruction) {
                    SSAGetInstruction getinst = (SSAGetInstruction) inst;
                    String fieldname = getinst.getDeclaredField().getName().toString();
                    if (getinst.isStatic()) {
                        if (!fieldNames.isEmpty()) fieldNames.clear();
                        fieldNames.add(fieldname);
                        Iterator<Statement> succStatement = backwardSuperGraph.getSuccNodes(currStmt);
                        processSuccStmt(currStmt, fieldname, StmtList, succStatement, uses, visited, ans, pos, stmtInBlock);
                        return;
                    } else {
                        instanceFieldNames.removeAll(instanceFieldNames);
                        instanceFieldNames.add(fieldname);
                        Iterator<Statement> succStatement = backwardSuperGraph.getSuccNodes(currStmt);
                        processSuccStmt(currStmt, fieldname, StmtList, succStatement, uses, visited, ans, pos, stmtInBlock);
                        //usechek(getinst.getUse(0), currStmt,uses,StmtList,visited,ans,pos,stmtInBlock);
                        return;
                    }
                }

                if (inst instanceof SSANewInstruction) {
                    SymbolTable st = currStmt.getNode().getIR().getSymbolTable();
                    stmtInBlock = getStmtInBlock(currStmt.getNode().getMethod().getSignature(), StmtList);
                    for (Statement stmt : stmtInBlock) {
                        if (stmt instanceof StatementWithInstructionIndex) {
                            SSAInstruction instArray = ((StatementWithInstructionIndex) stmt).getInstruction();
                            if (instArray instanceof SSAArrayStoreInstruction && ((SSAArrayStoreInstruction) instArray).getArrayRef() == use) {
                                int arrayUse = ((SSAArrayStoreInstruction) instArray).getValue();
                                if (st.isConstant(arrayUse)) {
                                    ans.add(st.getConstantValue(arrayUse));
                                    paramValue.put(pos, ans);
                                    sourceLineNums.add(getLineNumber(stmt));
                                } else uses.add(arrayUse);
                            }
                        }
                    }
                    return;
                }

            } else {
                //System.out.println("more process here fro:" + currStmt.getKind());
                return;
            }
            //TODO: no method call it , not return caller?
        }
    }


    public Statement checkPassin(Statement targetStmt, int use, Set<Integer> uses, List<Statement> stmtList) {
        LOGGER.info("Check passin parameter");
        String func = targetStmt.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                targetStmt.getNode().getMethod().getSelector().getName().toString();

        for (int i = 0; i < stmtList.size(); i++) {
            Statement stm = stmtList.get(i);
            String signature = stm.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                    stm.getNode().getMethod().getSelector().getName().toString();
            if (signature.compareToIgnoreCase(func) != 0) continue;
            // paramter be called by others, remove the use
            if (stm.getKind() == Statement.Kind.PARAM_CALLEE) {
                ParamCallee paramCallee = (ParamCallee) stm;
                int valnum = paramCallee.getValueNumber();
                if (valnum == use) {
                    uses.remove(use);
                    return stm;
                }

            }


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

    public StatementWithInstructionIndex checkStaticField(Statement targetStmt, int use, Set<Integer> uses, List<Statement> stmtList, int pos, Set<Integer> visited, List<Object> ans) {
        LOGGER.info("Check Static Field");
        String func = targetStmt.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                targetStmt.getNode().getMethod().getSelector().getName().toString();
        CGNode node = targetStmt.getNode();
        SymbolTable st = node.getIR().getSymbolTable();
        DefUse du = node.getDU();
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
                    uses.remove(use);
                    if (!fieldNames.isEmpty()) {
                        fieldNames.remove(getInst.getDeclaredField().getName().toString());
                    }
                    return (StatementWithInstructionIndex) stm;
                }

                if (getInst.getDef() == use && !(getInst.isStatic())) {
                    //System.out.println("instance field");
                    String instanceField = getInst.getDeclaredField().getName().toString();
                    uses.remove(use);
//                    Set<Integer> newUse = new HashSet<>();
//                    for (int j = 0; j < getInst.getNumberOfUses(); j++) {
//                        newUse.add(getInst.getUse(j));
//                    }
//                    newUse = getDU(newUse, pos, st, visited, ans, du);
//                    useCheckHelper(stm, newUse, stmtList, visited, ans, pos, stmtList);

                    if (!instanceFieldNames.isEmpty()) {
                        instanceFieldNames.remove(getInst.getDeclaredField().getName().toString());
                    }
                    return (StatementWithInstructionIndex) stm;

                }
            }
            continue;

        }
        return null;
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


    public List<Statement> filterStatement(Collection<Statement> relatedStmts) {
        LOGGER.info("filter statement");
        List<Statement> stmtList = new ArrayList<>();
        for (Statement stmt : relatedStmts) {
            if (!stmt.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial")) {
                stmtList.add(stmt);
            }
        }
        return stmtList;
    }


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
        sourceLineNums.clear();
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
                String methodN = call.getCallSite().getDeclaredTarget().getName().toString();
                if (methodN.equals(methodName) && methodT.contains(methodType)) {
                    // 一个例子
                    //if (call.getCallSite().getDeclaredTarget().getSignature().contains("Cipher")) continue;
                    IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
                    Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
                    // did not append the target stmt
                    int num = indices.intIterator().next();
                    allRelatedStmt.add(new NormalStatement(n, num));
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
        while (!q.isEmpty()) {
            Statement head = q.poll();
            if (head.getKind() == Statement.Kind.METHOD_ENTRY && head.getNode().getMethod().getSignature().compareToIgnoreCase(signature) == 0) {
                ans = head;
                break;
            } else {
                Iterator<Statement> it = completeSDG.getPredNodes(head);
                while (it.hasNext()) {
                    Statement s = it.next();
                    if (!visited.contains(s)) {
                        q.add(s);
                        visited.add(s);
                    }
                }
            }
        }
//        //System.out.println(ans);
//        for (int i = 0; i < stmtInBlock.size(); i++) {
//            Statement stm = stmtInBlock.get(i);
//            if (stm.getKind() == Statement.Kind.METHOD_ENTRY && stm.getNode().getMethod().getSignature().compareToIgnoreCase(signature) == 0) {
//                return stm;
//            } else continue;
//        }
        assert (ans != null);
        return ans;
    }

    public boolean isInstStatic(SSAInstruction inst) {
        if (inst instanceof SSAAbstractInvokeInstruction) return true;
        if (inst instanceof SSAInvokeInstruction) return ((SSAInvokeInstruction) inst).isStatic();
        if (inst instanceof SSAGetInstruction) return ((SSAGetInstruction) inst).isStatic();
        if (inst instanceof SSAPutInstruction) return ((SSAPutInstruction) inst).isStatic();
        //abstractinvoke from 0;sd
        return false;
    }

    public boolean isPrimordial(CGNode n) {
        return n.getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial");
    }

    public HashMap<Integer, List<Object>> getParamValue() {
        return paramValue;
    }

    public Map<String, Map<Integer, List<Object>>> getClassVarMap() {
        return classVarMap;
    }

    private int getLineNumber(SSAInstruction inst, IR ir) {
        int sourceLineNum = -1;
        try {
            IBytecodeMethod method = (IBytecodeMethod) ir.getMethod();
            int bytecodeIndex = method.getBytecodeIndex(inst.iIndex());
            sourceLineNum = method.getLineNumber(bytecodeIndex);
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return sourceLineNum;
    }

    private int getLineNumber(Statement stmt) {
        if (stmt.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
            int bcIndex, instructionIndex = ((NormalStatement) stmt).getInstructionIndex();
            try {
                bcIndex = ((ShrikeBTMethod) stmt.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                try {
                    int src_line_number = stmt.getNode().getMethod().getLineNumber(bcIndex);
                    return src_line_number;
                } catch (Exception e) {
                    System.err.println("Bytecode index no good");
                    System.err.println(e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
                System.err.println(e.getMessage());
            }
        }
        return -1;
    }

    public Map<String, HashMap<Integer, List<Integer>>> getClassParamsLinesNumsMap() {
        return classParamsLinesNumsMap;
    }
}
