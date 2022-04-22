package com.ibm.wala.examples.slice;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.dataflow.IFDS.BackwardsSupergraph;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.examples.ExampleUtil;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
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
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.intset.IntSet;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class BackwardSlicer3 {
    private HashMap<Integer, List<Object>> paramValue = new HashMap<>();
    private List<Statement> stmtList = new ArrayList<>();// save the filter slice result
    private Set<String> fieldName = new HashSet<>();
    private Map<String, Object> varMap = new HashMap<>();
    private Map<SSAInstruction, Object> instValMap = new HashMap<>();
    private Set<Statement> allRelatedStmt = new HashSet<>();
    private List<String> classorder = new ArrayList<>();
    private Map<String, String> classInitmap = new HashMap<>();
    private Map<String, Map<Integer, List<Object>>> classVarMap = new HashMap<>();
    private Statement targetStmt;
    private ISupergraph<Statement, PDG<? extends InstanceKey>> backwardSuperGraph;
    private CallGraph completeCG;
    private HeapModel heapModel;
    private FieldReference fieldRef;

    public Map<Integer, List<Object>> getParamValue() {
        return paramValue;
    }

    public Map<String, Map<Integer, List<Object>>> getClassVarMap() {
        return classVarMap;
    }

    public void run(String path,
                    String callee,
                    String functionType
    ) throws IOException, ClassHierarchyException, CancelException {
        Slicer.DataDependenceOptions dataDependenceOptions = Slicer.DataDependenceOptions.NO_HEAP;
        Slicer.ControlDependenceOptions controlDependenceOptions = Slicer.ControlDependenceOptions.FULL;

        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(path, null);
        ExampleUtil.addDefaultExclusions(scope);
        ClassHierarchy cha = ClassHierarchyFactory.make(scope);
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
        SDG<InstanceKey> completeSDG = new SDG<>(completeCG, builder.getPointerAnalysis(), dataDependenceOptions, controlDependenceOptions);
        PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();
        this.heapModel = pa.getHeapModel();
        SDGSupergraph forwards = new SDGSupergraph(completeSDG, true);
        backwardSuperGraph = BackwardsSupergraph.make(forwards);

        for (CGNode node : completeCG) {
            findAllCallTo(node, callee, functionType);
        }

        for (Statement stmt : allRelatedStmt) {
            targetStmt = stmt;
            clearInit();
            cache.clear();
            String className = targetStmt.getNode().getMethod().getDeclaringClass().getName().toString();
            if (className.compareTo("Lorg/cryptoapi/bench/predictablecryptographickey/PredictableCryptographicKeyABICase2") != 0)
                continue;
            Collection<CGNode> roots = new ArrayList<>();
            roots.add(targetStmt.getNode());

            Collection<Statement> relatedStmts = Slicer.computeBackwardSlice(targetStmt, completeCG, builder.getPointerAnalysis(),
                    dataDependenceOptions, controlDependenceOptions);
            // Filter all non application stmts
            filterStatement(relatedStmts);
            Graph<Statement> g = pruneCG(completeCG, completeSDG, targetStmt.getNode());
            setParamValue(targetStmt);
            //Vulnerfinder(stmtList,g,targetStmt, className);
        }

    }

    public void getParameter(int i) {
        System.out.println("This is the " + i + "th parameter for the target function: " + paramValue.get(i));
    }


    //dealwith the targetstmt1 and split the block stmt
    public void setParamValue(Statement targetStmt) {
        SSAInstruction targetInst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
        Set<Integer> uses = new HashSet<>();
        IR targetIR = targetStmt.getNode().getIR();
        SymbolTable st = targetIR.getSymbolTable();
        if (targetInst instanceof SSAInvokeInstruction) {
            int i = ((SSAInvokeInstruction) targetInst).isStatic() == true ? 0 : 1;
            int neg = ((SSAInvokeInstruction) targetInst).isStatic() == true ? 0 : -1;
            int numOfUse = targetInst.getNumberOfUses();
            while (i < numOfUse) {
                List<Object> ans = new ArrayList<>(); //have more possible value;
                int use = targetInst.getUse(i);
                if (st.isConstant(use)) {
                    ans.add(st.getConstantValue(use));
                    paramValue.put(i + neg, ans);
                } else {
                    uses.add(use); // can't get the parameter within one block;
                    /*
                     * If I cannot get the value on that statement directly, which means that the value is not
                     * in the symboltable, then run the following analysis:
                     *   1. separate all the statements by their functions/blocks
                     *   2. reversely loop all functions/blocks. reverse means from the block of targetstmt -> main
                     *   3. when handling the specific function, pass the target use value number list to each function
                     */
                    String selector = null;
                    List<Statement> stmtInBlock = new ArrayList<>();
                    for (Statement stmt : stmtList) {
                        String func = stmt.getNode().getMethod().getDeclaringClass().getName().toString() + " " +
                                stmt.getNode().getMethod().getSelector().getName().toString();

                        if (selector == null) {
                            selector = func;
                            //System.out.println(selector);
                            stmtInBlock.add(stmt);
                        } else if (selector.compareToIgnoreCase(func) == 0) {
                            //System.out.println(stmt);
                            stmtInBlock.add(stmt);
                        } else {
                            loopStatementInBlock(targetStmt, uses, stmtInBlock, i + neg);
                            //                       setParamValue(targetStmt, uses, stmtInBlock, i + neg);
                            targetStmt = this.targetStmt;
                            stmtInBlock.clear();
                            stmtInBlock.add(stmt);
                            selector = func;
                        }
                    }
                    loopStatementInBlock(targetStmt, uses, stmtInBlock, i + neg);
                    //setParamValue(targetStmt, uses, stmtInBlock, i + neg);
                }
                i++;
            }
            if (paramValue.size() == numOfUse)
                return;
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

    public Statement isPassin(int use, List<Statement> stmtInBlock, Set<Integer> uses) {
        // if caller, which means it's a passin para
        boolean flag = false;
        for (int i = 0; i < stmtInBlock.size(); i++) {
            Statement stm = stmtInBlock.get(i);
            if (stm.getKind() == Statement.Kind.PARAM_CALLER) {
                ParamCaller paramCaller = (ParamCaller) stm;
                int valNum = paramCaller.getValueNumber();
                SymbolTable st = paramCaller.getNode().getIR().getSymbolTable();
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

    public int getStmtpos(Statement stmt, List<Statement> stmtInBlock) {
        if (stmtInBlock.contains(stmt))
            return stmtInBlock.indexOf(stmt);
        else
            return -1;
    }

    public StatementWithInstructionIndex isStaticField(int use, List<Statement> stmtInBlock, Set<Integer> uses) {
        // return the get_stmt; if caller, which means it's a passin para
        boolean flag = false;
        for (int i = 0; i < stmtInBlock.size(); i++) {
            Statement stm = stmtInBlock.get(i);

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

    public Statement getMethodEntry(List<Statement> stmtInBlock) {
        for (int i = 0; i < stmtInBlock.size(); i++) {
            Statement stm = stmtInBlock.get(i);
            if (stm.getKind() == Statement.Kind.METHOD_ENTRY) {
                return stm;
            } else continue;
        }
        return null;
    }

    public StatementWithInstructionIndex getPutStmt(List<Statement> stmtInBlock, int indexnumber, FieldReference fieldref, Statement stmt) {
        /*find the paired putinst, the putinst should index< getindex and have the same fieldname as get*/
        //TODO: this method should be futher extented to handel instance field
        IR ir = stmt.getNode().getIR();
        SymbolTable st = ir.getSymbolTable();
        int bound = stmtInBlock.indexOf(stmt);
        for (int i = bound; i > 0; i--) {//get the latest put field;
            Statement stm = stmtInBlock.get(i);
            if (stm instanceof StatementWithInstructionIndex) {
                StatementWithInstructionIndex indexput = (StatementWithInstructionIndex) stm;
                SSAInstruction inst = indexput.getInstruction();
                int index = indexput.getInstructionIndex();
                //if find the  paired put, return
                if (inst instanceof SSAPutInstruction && ((SSAPutInstruction) inst).isStatic() && index < indexnumber) {
                    SSAPutInstruction putinst = (SSAPutInstruction) inst;
                    if (putinst.getDeclaredField().getName() == fieldref.getName()) {
                        int use = putinst.getVal();
                        if (st.isConstant(use)) {
                            varMap.put(putinst.getDeclaredField().getName().toString(), st.getConstantValue(use));
                            return indexput;
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

    public void loopPreNode(Statement stm, int pos) {
        Iterator<Statement> statements = this.backwardSuperGraph.getPredNodes(stm);
        Iterator<Statement> statementWithinPreNode;
        List<Statement> stmtInBlock = null;
        while (statements.hasNext()) {
            Statement current = statements.next();
            if (current.getKind() == Statement.Kind.METHOD_ENTRY) {
                System.out.println("here is the entry point");
                Statement methodEntry = current;
                statementWithinPreNode = backwardSuperGraph.getPredNodes(methodEntry);
                SSAInstruction[] SSAintructions = methodEntry.getNode().getIR().getInstructions();
                System.out.println(SSAintructions);
                while (statementWithinPreNode.hasNext()) {
                    stmtInBlock.add(statementWithinPreNode.next());
                }
                loopStatementInBlockHelper(current, stmtInBlock, stmtInBlock.size(), pos);
            }
        }
    }

    /*checkusevalue(){
          checkconstant(); terminate;
          checkpassin(); terminate, upperblck;
          checkstaticfield(); getfield, reocred transfer "fieldname"

    // }

    checstaticfield(){
     putstatic not in block terminate-> upper block
     putstatic{
     du();
     checkusevalue();
     }

     if(instancefield) go constctor ==

    }
    */


    public void loopStatementInBlockHelper(Statement targetStmt, List<Statement> stmtInBlock, int index, int pos) {
        //check if there is put stmt;
        int bound = stmtInBlock.indexOf(targetStmt);
        Set<Integer> uses = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        List<Object> ans = new ArrayList<>();
        IR ir = targetStmt.getNode().getIR();
        SymbolTable st = ir.getSymbolTable();
        DefUse du = targetStmt.getNode().getDU();

        for (int i = bound; i > 0; i--) {
            Statement stm = stmtInBlock.get(i);
            if (stm instanceof StatementWithInstructionIndex) {
                int stmIndex = ((StatementWithInstructionIndex) stm).getInstructionIndex();
                if (stmIndex < index) {
                    SSAInstruction inst = ((StatementWithInstructionIndex) stm).getInstruction();
                    if (inst instanceof SSAInvokeInstruction) {
                        loopPreNode(stm, pos);
                        return;
                    }

                    if (inst instanceof SSAPutInstruction) {
                        /* TODO:check if use the same field value;
                        *   if same: then get the use number and get the value from st, can;t find in st, use the usenumber futher trace back;
                        * possible 1: change the fieldname(use-def is getstatic) getstatic; 2 passin parameter
                       done here*/
                        StatementWithInstructionIndex putfield = getPutStmt(stmtInBlock, inst.iIndex(), fieldRef, stm);
                        int use = ((SSAPutInstruction)inst).getUse(pos);
                        if (putfield == null) {
                            /*no put, lose the use trace, should use the fieldname as trace; go back the upper layer and check stmt one by one*/
                            uses.remove(use);
                            Statement methodentry = getMethodEntry(stmtInBlock);
                            this.targetStmt = getCalleePosition(methodentry);
                            this.fieldName.add(fieldRef.getName().toString());
                            this.fieldRef = fieldRef;
                            return;
                        } else {
                            SSAPutInstruction putInst = (SSAPutInstruction) putfield.getInstruction();
                            uses.remove(use);
                            int putuse = putInst.getVal();
                            if (!st.isConstant(putuse)) {
                                //TODO: here how about uses is getstatic again? how to refect the process
                                uses = getDU(uses, pos, ir, st, visited, ans, du);
                                if (isStaticField(use, stmtInBlock, uses) != null) {
                                    System.out.println("should swith the fieldname");
                                }
                                if (isPassin(use, stmtInBlock, uses) != null) continue;
                            }
                            //check the usenumber and
                        }

                    }
                }
            }
        }
    }

    public void loopStatementInBlock(Statement targetStmt, Set<Integer> uses, List<Statement> stmtInBlock, int pos) {

        Set<Integer> visited = new HashSet<>();
        List<Object> ans = new ArrayList<>();
        IR ir = targetStmt.getNode().getIR();
        SymbolTable st = ir.getSymbolTable();
        DefUse du = targetStmt.getNode().getDU();

        //check static fieldname, use fieldname as trace
        if (!fieldName.isEmpty()) {
            System.out.println("static_fieldname: " + fieldName);
            if (targetStmt instanceof StatementWithInstructionIndex) {
                int index = ((StatementWithInstructionIndex) targetStmt).getInstructionIndex();
                Statement methodentry = getMethodEntry(stmtInBlock);//make sure have entry?
                loopStatementInBlockHelper(targetStmt, stmtInBlock, index, pos);
            }
        }
        //TODO: check instancefield name


        //check du, if static field, should another round process, if passin, find the upper layer;
        uses = getDU(uses, pos, ir, st, visited, ans, du);
        Queue<Integer> q = new LinkedList<>();
        q.addAll(uses);
        while (!q.isEmpty()) {
            int use = q.poll();

            Statement tmp = isPassin(use, stmtInBlock, uses); //return the method entry
            if (tmp != null) {
                this.targetStmt = getCalleePosition(tmp);
                if (this.targetStmt != null) {
                    SSAInstruction inst = ((StatementWithInstructionIndex) this.targetStmt).getInstruction();
                    int newUse = inst.getUse(use - 1);
                    uses.remove(use);
                    uses.add(newUse);
                    q.add(newUse);
                    return;
                } else {
                    //no caller found in the cg, let's check constructor;
                    SSAInstruction inst = ((StatementWithInstructionIndex) targetStmt).getInstruction();
                    uses.add(inst.getUse(0)); //get itself and see the result
                    q.add(inst.getUse(0));
                }
            } else {
                System.out.println("Case is out of scope!");
            }

            StatementWithInstructionIndex getFieldStmt = isStaticField(use, stmtInBlock, uses);
            if (getFieldStmt != null) {
                SSAGetInstruction getinst = (SSAGetInstruction) getFieldStmt.getInstruction();
                FieldReference fieldRef = getinst.getDeclaredField();
                StatementWithInstructionIndex putfield = getPutStmt(stmtInBlock, getFieldStmt.getInstructionIndex(), fieldRef, getFieldStmt);
                if (putfield == null) {
                    /*no put, lose the use trace, should use the fieldname as trace; go back the upper layer and check stmt one by one*/
                    uses.remove(use);
                    Statement methodentry = getMethodEntry(stmtInBlock);
                    this.targetStmt = getCalleePosition(methodentry);
                    if (targetStmt == null) {
                        System.out.println("can't find the value! out of scope");
                    }
                    this.fieldName.add(fieldRef.getName().toString());
                    this.fieldRef = fieldRef;
                    return;
                } else {
                    SSAPutInstruction putInst = (SSAPutInstruction) putfield.getInstruction();
                    uses.remove(use);
                    int putuse = putInst.getVal();
                    if (!st.isConstant(putuse)) {
                        //TODO: here how about uses is getstatic again? how to refect the process
                        uses = getDU(uses, pos, ir, st, visited, ans, du);
                        if (isStaticField(use, stmtInBlock, uses) != null) {
                            System.out.println("should swith the fieldname");
                        }
                        if (isPassin(use, stmtInBlock, uses) != null) continue;
                    } else continue;

                }
            }

        }
    }

    public void setParamValue(Statement targetStmt, Set<Integer> uses,
                              List<Statement> stmtInBlock, int pos) {
        int calleeCount = 0, callerCount = 0;
        Set<SSAInstruction> definsts = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        List<Object> ans = new ArrayList<>();
        for (int i = 0; i < stmtInBlock.size(); i++) {
            Statement stm = stmtInBlock.get(i);
            if (stm.toString().equals(targetStmt.toString())) continue;
            if (stm.getKind() == Statement.Kind.PARAM_CALLER) {
                if (uses.contains(-callerCount)) {
                    uses.remove(-callerCount);
                    ParamCaller paramCaller = (ParamCaller) stm;
                    int use = paramCaller.getValueNumber();
                    SymbolTable st = paramCaller.getNode().getIR().getSymbolTable();
                    if (uses.size() == 0 && !visited.contains(use)) {
                        if (st.isConstant(use)) {
                            ans.add(st.getConstantValue(use));
                            this.paramValue.put(pos, ans);
                            visited.add(use);
                        } else {
                            uses.add(use);
                        }
                    }
                }
                /*
                    There is a situation causing dangling paramcaller which we cannot find the callee to map with it.
                    The reason causing this issue is that the paramcallee has been ignored since its classloader
                    is primordial. Currently, we defined that if we find a paramcaller and the current paramcaller count
                    is equal to paramcallee, then this paramcaller statement is a dangling paramcaller. And the actual
                    use number is useful maybe. And it needs to be taken care of.
                 */
                if (callerCount < calleeCount) callerCount++;
                else {
                    ParamCaller paramCaller = (ParamCaller) stm;
                    int use = paramCaller.getValueNumber();
                    SymbolTable st = paramCaller.getNode().getIR().getSymbolTable();
                    if (uses.size() == 0 && !visited.contains(use)) {
                        if (st.isConstant(use)) {
                            ans.add(st.getConstantValue(use));
                            this.paramValue.put(pos, ans);
                            visited.add(use);
                        } else {
                            uses.add(use);
                        }
                    }
                }
                continue;
            }
            if (stm.getKind() == Statement.Kind.PARAM_CALLEE) {
                ParamCallee paramCallee = (ParamCallee) stm;
                int use = paramCallee.getValueNumber();
                if (uses.contains(use)) {
                    uses.remove(use);
                    uses.add(-calleeCount);
                    calleeCount++;
                }
                continue;
            }

//                if (stm.getKind() == Statement.Kind.PHI) {
//                    PhiStatement pstmt = (PhiStatement) stm;
//                    SSAPhiInstruction inst = pstmt.getPhi();
//                    SymbolTable st = pstmt.getNode().getIR().getSymbolTable();
//                    int def = inst.getDef();
//                    if (uses.contains(def)) {
//                        uses.remove(def);
//                        for (int ii = 0; ii<inst.getNumberOfUses(); ii++) {
//                            int use = inst.getUse(ii);
//                            if (st.isConstant(use)) {
//                                putVarMap(pos, st.getConstantValue(use));
//                            } else {
//                                uses.add(use);
//                            }
//                        }
//                    }
//                    continue;
//                }

            if (!(stm instanceof StatementWithInstructionIndex)) continue;
            SSAInstruction inst = ((StatementWithInstructionIndex) stm).getInstruction();
            IR ir = stm.getNode().getIR();
            DefUse du = stm.getNode().getDU();
            SymbolTable st = ir.getSymbolTable();

            if (inst instanceof SSAGetInstruction || inst instanceof SSAPutInstruction) {
                if (inst instanceof SSAGetInstruction) {
                    if (uses.contains(inst.getDef())) {
                        String name = ((SSAFieldAccessInstruction) inst).getDeclaredField().getName().toString();
                        if (varMap.containsKey(name)) {
                            ans.add(varMap.get(name));
                            this.paramValue.put(pos, ans);
                            break;
                        }
                    }
                }
                //this.ParamValue.add(instValMap.get(inst));
            }
            boolean isDef = false;
            for (int j = 0; j < inst.getNumberOfDefs(); j++) {
                if (uses.contains(inst.getDef(j))) {
                    uses.remove(inst.getDef(j));
                    isDef = true;
                }

            }

            if (isDef) {
                for (int j = 0; j < inst.getNumberOfUses(); j++) {
                    int use = inst.getUse(j);
                    if (j == 0 && (inst instanceof SSAInvokeInstruction)
                            && ((SSAInvokeInstruction) inst).getDeclaredTarget().getSelector().getName().toString().contains("getBytes")) {
                        if (!st.isConstant(use)) {
                            uses.add(use);
                            if (du.getDef(use) != null) definsts.add(du.getDef(use));
                        } else {
                            //System.out.println("\t" + use + " " + st.getConstantValue(use));
                            if (uses.size() == 0 && !visited.contains(use)) {
                                ans.add(st.getConstantValue(use));
                                this.paramValue.put(pos, ans);
                                visited.add(use);
                            }
                        }
                        break;
                    }
                    if (j == 0 && ((inst instanceof SSAInvokeInstruction
                            && !((SSAInvokeInstruction) inst).isStatic()) || !(inst instanceof SSAAbstractInvokeInstruction))
                            && !st.isConstant(use)
                            && !(inst instanceof SSAPutInstruction))
                        continue;
                    if (!st.isConstant(use)) {
                        uses.add(use);
                        if (du.getDef(use) != null) definsts.add(du.getDef(use));
                    } else {
                        //System.out.println("\t" + use + " " + st.getConstantValue(use));
                        if (uses.size() == 0 && !visited.contains(use)) {
                            ans.add(st.getConstantValue(use));
                            this.paramValue.put(pos, ans);
                            visited.add(use);
                        }
                    }
                }
            }
        }

    }


    // here is the interface for filter out the unrelated statement
    public void filterStatement(Collection<Statement> relatedStmts) {
        for (Statement stmt : relatedStmts) {
            if (!stmt.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial")) {
                stmtList.add(stmt);
            }
        }
    }

    /* find the target method, should be a method invocation*/
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

    /**
     * Get all related function statements
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

    public Graph<Statement> pruneSDG(SDG<InstanceKey> sdg, String mainClass) {
        Predicate<Statement> ifStmtInBlock = (i) -> (i.getNode().getMethod().getReference().getDeclaringClass().
                getName().toString().contains(mainClass));
        return GraphSlicer.prune(sdg, ifStmtInBlock);
    }

    public Graph<Statement> pruneSDG(SDG<InstanceKey> sdg, Statement targetStmt) {
        Queue<Statement> stmtQueue = new LinkedList<>();
        stmtQueue.add(targetStmt);
        Set<Statement> relatedStmt = new HashSet<>();
        Set<String> relatedClass = new HashSet<>();
        while (!stmtQueue.isEmpty()) {
            Statement head = stmtQueue.poll();
            if (head.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial"))
                continue;
            relatedStmt.add(head);
            relatedClass.add(head.getNode().getMethod().getReference().getDeclaringClass().getName().toString());
            Iterator<Statement> itst = sdg.getPredNodes(head);
            while (itst.hasNext()) {
                Statement stmt = itst.next();
                if (relatedStmt.contains(stmt)) continue;
                stmtQueue.add(stmt);
            }
        }
        Predicate<Statement> ifStmtinBlock = (i) -> (relatedClass.contains(i.getNode().getMethod().getReference().
                getDeclaringClass().getName().toString()));
        return GraphSlicer.prune(sdg, ifStmtinBlock);
    }

    public Graph<Statement> pruneCG(CallGraph cg, SDG<InstanceKey> sdg, CGNode cgnode) {

        Set<CGNode> visited = new HashSet<>();
        Queue<CGNode> nodeQueue = new LinkedList<>();
        Set<String> relatedClass = new HashSet<>();
        nodeQueue.add(cgnode);
        visited.add(cgnode);
        //classorder.add(cgnode.getMethod().getName().toString());
        List<String> tmpClassOrder = new ArrayList<>();
        while (!nodeQueue.isEmpty()) {
            CGNode head = nodeQueue.poll();
//                System.out.println(head);
            Iterator<CGNode> itnode = cg.getPredNodes(head);
            relatedClass.add(head.getMethod().getReference().getDeclaringClass().getName().toString());
            while (itnode.hasNext()) {
                CGNode n = itnode.next();
                IR callerIR = n.getIR();
//                    System.out.println("\t" + n);
                tmpClassOrder.clear();
                if (n.getMethod().getDeclaringClass().getName().toString().contains("FakeRootClass")) continue;
                tmpClassOrder.add(n.getMethod().getReference().toString());

                Iterator<CallSiteReference> callIter = n.iterateCallSites();

                while (callIter.hasNext()) {
                    CallSiteReference csRef = callIter.next();
                    SSAAbstractInvokeInstruction callInstrs[] = callerIR.getCalls(csRef);
                    System.out.println(callInstrs[0]);
                    if (csRef.getDeclaredTarget().getName().toString().contains("fakeWorldClinit")) continue;
                    MethodReference mRef = csRef.getDeclaredTarget();
                    if (mRef.getDeclaringClass().getClassLoader().getName().toString().contains("Primordial")) continue;
                    tmpClassOrder.add(mRef.toString());
                }
                this.classorder.clear();
                this.classorder.addAll(tmpClassOrder);
                if (visited.contains(n)) continue;
                nodeQueue.add(n);
                if (!n.getMethod().getDeclaringClass().getName().toString().contains("FakeRootClass")) visited.add(n);
            }
        }
        Set<CGNode> visitedSucc = new HashSet<>();
        for (CGNode node : visited) {
            nodeQueue.add(node);
            visitedSucc.add(node);
            while (!nodeQueue.isEmpty()) {
                CGNode head = nodeQueue.poll();
                relatedClass.add(head.getMethod().getReference().getDeclaringClass().getName().toString());
//                System.out.println("Current Node: " + head);
                Iterator<CGNode> itnode = cg.getSuccNodes(head);
//                System.out.println("\tChild Nodes: ");
                while (itnode.hasNext()) {
                    CGNode n = itnode.next();
//                    System.out.println("\t\t" + n);
                    if (visitedSucc.contains(n)) continue;
                    nodeQueue.add(n);
                    if (!n.getMethod().getDeclaringClass().getName().toString().contains("FakeRootClass")
                            && !n.getMethod().getDeclaringClass().getClassLoader().toString().contains("Primordial")) {
                        visitedSucc.add(n);
                    }
                }
            }
        }

        Predicate<Statement> ifStmtinBlock = (i) ->
                ((visited.contains(i.getNode()) || visitedSucc.contains(i.getNode()) ||
                        relatedClass.contains(i.getNode().getMethod().getReference().getDeclaringClass().getName().toString()))
                        && !i.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().equals("Primordial"));
        return GraphSlicer.prune(sdg, ifStmtinBlock);
    }

    public void clearInit() {
        varMap.clear();
        instValMap.clear();
        paramValue.clear();
        stmtList.clear();
        this.classorder.clear();
    }

    public boolean isStmtinOrder(List<Statement> stmtBlock) {
        int def = 0;
        for (Statement stmt : stmtBlock) {
            if (stmt instanceof StatementWithInstructionIndex) {
                SSAInstruction inst = ((StatementWithInstructionIndex) stmt).getInstruction();
                try {
                    if (def == 0) def = inst.getDef();
                    else if (def <= inst.getDef()) return true;
                    else return false;
                } catch (AssertionError e) {
                    continue;
                }
            }
        }
        return false;
    }

    public boolean isInstStatic(SSAInstruction inst) {
        if (inst instanceof SSAAbstractInvokeInstruction) return true;
        if (inst instanceof SSAInvokeInstruction) return ((SSAInvokeInstruction) inst).isStatic();
        if (inst instanceof SSAGetInstruction) return ((SSAGetInstruction) inst).isStatic();
        if (inst instanceof SSAPutInstruction) return ((SSAPutInstruction) inst).isStatic();
        //abstractinvoke from 0;
        return false;
    }

    public void putVarMap(int pos, Object o) {
        this.paramValue.putIfAbsent(pos, new ArrayList<>());
        List<Object> ans = paramValue.get(pos);
        ans.add(o);
        this.paramValue.put(pos, ans);
    }
}
