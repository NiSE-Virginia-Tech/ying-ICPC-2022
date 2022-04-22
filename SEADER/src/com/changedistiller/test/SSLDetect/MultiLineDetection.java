package com.changedistiller.test.SSLDetect;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.examples.slice.ProBuilder;
import com.ibm.wala.examples.slice.StartPoints;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;
import javafx.util.Pair;
import utils.StringSimilarity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MultiLineDetection {

    public String[] args;
    public String methodT; // method type
    public Map<String, String> paramMap = new HashMap<>();
    private static final Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private ProBuilder proBuilder;
    private String projectSource;
    public List<String> correctSet;
    public List<Pair<Integer,String>> lineToCaller = null;
    private Slicer.DataDependenceOptions dOptions = Slicer.DataDependenceOptions.FULL;
    private Slicer.ControlDependenceOptions cOptions = Slicer.ControlDependenceOptions.FULL;
    private CallGraph completeCG;
    private SDG<InstanceKey> completeSDG;

    public MultiLineDetection(String classPath, String projectSource, List<String> correctSet)
            throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {
        proBuilder = new ProBuilder(classPath, dOptions, cOptions);
        completeCG = proBuilder.getTargetCG();
        completeSDG = proBuilder.getCompleteSDG();
        this.projectSource = projectSource;
        this.correctSet = correctSet;
        log.setLevel(Level.OFF);
    }

    public void start(String callee, String functionType, long pos, long args)
            throws ClassHierarchyException, CancelException, IOException {
        StartPoints startPoints = new StartPoints(completeCG, callee, functionType, args);
        Set<Statement> allStartStmt = startPoints.getStartStmts();
        for (Statement stmt : allStartStmt) {
            String filePath = this.projectSource;
            String className = stmt.getNode().getMethod().getDeclaringClass().getName().toString();
            filePath = filePath.concat(className.substring(1));
//            filePath = filePath.replace('/', '\\');
            if (filePath.contains("$")) {
                filePath = filePath.substring(0, filePath.indexOf("$"));
            }
            filePath = filePath.concat(".java");
            System.out.println(filePath);
//            if (!className.contains("Lorg/cryptoapi/bench/staticinitializationvector/StaticInitializationVectorABHCase1"))
//                continue;
            System.out.println(
                    "-----------------------------------"
                            + className
                            + "----------------------------------------------");
            List<Statement> stmtList = new ArrayList<>();
            stmtList.add(stmt);
            int pp = (int) pos;
            methodT = functionType;

            List<Integer> lineNum = getStmtLineNum(stmtList);

            Map<Integer, Node> contextStmt = new HashMap<>();
            Map<Integer, String> originStmt = new HashMap<>();
            List<String> matchingStmt = getmatchStmts(lineNum, filePath, pp, callee, contextStmt, originStmt);
            List<String> correctStmt = getCorrectstmts();
            getMissingStmts(correctStmt, matchingStmt, lineNum, contextStmt, originStmt);
            paramMap.clear();
        }
    }

    private List<Integer> getStmtLineNum(Collection<Statement> slice) {
        List<Integer> lineNum = new ArrayList<>();
        for (Statement stmt : slice) {
            if (stmt.getNode()
                    .getMethod()
                    .getDeclaringClass()
                    .getClassLoader()
                    .getName()
                    .toString()
                    .compareToIgnoreCase("primordial") == 0
                    || stmt.getKind() != Statement.Kind.NORMAL) continue;
            Integer a = getLineNumber(stmt);
            lineNum.add(a);
//            String caller = getCaller(stmt);
//            lineToCaller.add(new Pair<>(a, caller));
//            lineToCaller.add((a, stmt.getNode().getMethod().getSignature().toString());
            log.log(Level.INFO, String.valueOf(getLineNumber(stmt)));
        }
        return lineNum;
    }

    private List<String> getCorrectstmts() {
        List<String> correctStmts = new ArrayList<>(this.correctSet);
        return correctStmts;
    }

    private List<String> getmatchStmts(List<Integer> lineNum, String filePath,
                                       int pos, String callee, Map<Integer, Node> contextStmt, Map<Integer, String> originStmt)
            throws IOException {
        // The following part is to get the matching statements from the source code:
        // Use javaparser
        // 1. visit the file to get all the variables and save to the varList
        // 2. normalize the statements by replacing the variables but leave the constants.
        // 3. normalize the parameter value with $v_x$
        TypeSolver typeSolver = new ReflectionTypeSolver();
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        CompilationUnit cu = StaticJavaParser.parse(new File(filePath));

        Map<String, String> varList = new HashMap<>();
        Map<String, String> reverseParams = new HashMap<>();
        VoidVisitor<Map<String, String>> visitor = new VariableDeclarationVisitor();
        //the target stmt is either methodcall or variable de
        List<MethodCallExpr> ls = cu.findAll(MethodCallExpr.class).stream().filter(mce -> {
            try {
                boolean res = mce.resolve().getClassName().toLowerCase().contains(methodT.toLowerCase());
                return res;
            } catch (Exception e) {
                return false;
            }
        }).collect(Collectors.toList());

        List<ObjectCreationExpr> objCExprs = cu.findAll(ObjectCreationExpr.class);

        String targetStmt = "";
        String outerTarget = "";
        MethodCallExpr childMethodCall = null;
        int cons = 0;
        /**keystore.load(); secureRandom.setSeed() * no contextstmt store here*/
        if (!callee.contains("<init>")) {
            for (MethodCallExpr mCall : ls) {
                //&& method call should be same, init kind handle a.foo(b, c)
                if (lineNum.contains(mCall.getRange().get().begin.line) && callee.contains(mCall.getNameAsString())) {

                    // callee's name should always be saved first
                    // keystore.load(x, y) -> $v_0$.load($v_1$, $v_2$);
                    Node caller = getCaller((Node)mCall);
                    contextStmt.put(mCall.getRange().get().begin.line, caller);
                    originStmt.put(mCall.getRange().get().begin.line, mCall.toString());
                    for (int i = -1; i < mCall.getArguments().size(); i++) {
                        String argumentString = "";
                        String normalizeName = "";
                        if (i == -1) {
                            argumentString = mCall.getScope().get().toString();
                            normalizeName = "$v_" + cons + 0 + "$";
                            paramMap.put(normalizeName, argumentString);
                            mCall.setScope(new NameExpr(normalizeName));
                        } else {
                            argumentString = mCall.getArguments().get(i).toString();
                            normalizeName = "$v_" + cons + (i + 1) + "$";
                            if (i == (int) pos) {
                                paramMap.put(normalizeName, "var_" + i); //target pos would not be replaced
                                mCall.setArgument(i, new NameExpr("var_" + i));
                            }
                            else {
                                paramMap.put(normalizeName, argumentString);
                                mCall.setArgument(i, new NameExpr(normalizeName));
                            }
                        }
                        reverseParams.put(argumentString, normalizeName);
                        targetStmt = mCall.toString();
                    }
//                    targetStmt = replaceStmtWithNormalizeName(reverseParams, targetStmt, cons, mCall.getArguments().size());
                    cons++;
                    //check if it is a statement a = xx invocation()? yes -> continue,
                    // no -> normalize the statement with v_x ?
                    break;
                }
                continue;
            }
        }

        // new A(p1, p2, p3)
        for (ObjectCreationExpr objCreation : objCExprs) {
            //here should find the stmt for variable declaration stmt
            if (lineNum.contains(objCreation.getRange().get().begin.line) && methodT.contains(objCreation.getType().toString())) {
                if(objCreation.getParentNode().isPresent()) {
                    Node caller = getCaller((Node) objCreation);
                    contextStmt.put(objCreation.getRange().get().begin.line, caller);
                    originStmt.put(objCreation.getRange().get().begin.line, objCreation.toString());
                    Node parNode = setParameters(objCreation.getParentNode().get(), objCreation, cons, pos, reverseParams);
                    if (parNode instanceof MethodCallExpr) {
                        // methodT $v_x$ = new methodT();
                        Node newTar  = new VariableDeclarator(objCreation.getType(), new SimpleName("$v_x$"), objCreation);
                        outerTarget = parNode.toString();
                        targetStmt = newTar.toString();
                    } else {
                        targetStmt = parNode.toString();
                    }
                }
                cons++;
            }
        }


        int prev = -1;
        List<String> matchingStmt = new ArrayList<>();
        String str = targetStmt;
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            if (str.contains(entry.getValue()) && entry.getValue().length() >= 2) {
                str = str.replace(entry.getValue(), entry.getKey());
            }
        }

        matchingStmt.add(str.trim());
        if (!outerTarget.isEmpty()) matchingStmt.add(outerTarget);
        return matchingStmt;
    }


    public Node setParameters(Node parentNode, ObjectCreationExpr node, int cons, int pos, Map<String, String> reverseMap) {
        //String targetStmt = "";
        String argumentString = "";
        String normalizeName = "";
        if (parentNode instanceof AssignExpr) {
            argumentString = ((AssignExpr) parentNode).getTarget().toString();
            normalizeName = "$v_" + cons + 0 + "$";
            paramMap.put(normalizeName, argumentString);
            ((AssignExpr) parentNode).setTarget(new NameExpr(normalizeName));
        }

        if(parentNode instanceof VariableDeclarator) {
            argumentString = ((VariableDeclarator)parentNode).getName().toString();
            normalizeName = "$v_" + cons + 0 + "$";
            paramMap.put(normalizeName, argumentString);
            ((VariableDeclarator) parentNode).setName(new SimpleName(normalizeName));
        }

        if (parentNode instanceof MethodCallExpr) {
            int k =  ((MethodCallExpr) parentNode).getArguments().size();
            for (int i = 0; i < k; i++) {
                if ( ((MethodCallExpr) parentNode).getArgument(i).isObjectCreationExpr() &&
                        ((MethodCallExpr) parentNode).getArgument(i).toString().equals(node.toString())) {
                    ((MethodCallExpr) parentNode).setArgument(i, new NameExpr("$v_x$"));
                }
            }

        }

        for (int i = 0; i < node.getArguments().size(); i++) {
            argumentString = node.getArguments().get(i).toString();
            normalizeName = "$v_" + cons + (i + 1) + "$";
            if (i == (int) pos) {
                paramMap.put(normalizeName, "var_" + i); //target pos would not be replaced
                node.setArgument(i, new NameExpr("var_" + i));
            }
            else {
                paramMap.put(normalizeName, argumentString);
                node.setArgument(i, new NameExpr(normalizeName));
            }
            reverseMap.put(argumentString, normalizeName);
        }
       // targetStmt = parentNode.toString();
        return parentNode;
    }
    /* here should return all v value */
    private List<String> getVar(String s) {
        String pattern = "(.*?)(\\$v_\\d+\\$)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(s);
        List<String> var = new ArrayList<>();
        while (m.find()) {
            var.add(m.group(2));
//            return m.group(2);
        }
        return var;
    }

    public int getLineNumber(Statement stmt) {
        int bcIndex, instructionIndex = ((NormalStatement) stmt).getInstructionIndex();
        try {
            bcIndex =
                    ((ShrikeBTMethod) stmt.getNode().getMethod())
                            .getBytecodeIndex(instructionIndex);
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
        return -1;
    }

    public Node getCaller(Node expr) {
        Node parent = expr;
        while(!(parent instanceof MethodDeclaration)) {
            if ( parent instanceof FieldDeclaration) return null;
            parent = parent.getParentNode().get();
        }
        return parent;
    }
    // we use similarity distance to catch the target stmt in fix patch
    // and the matching stmt contains only one statement
    private void getMissingStmts(
            List<String> correctstmts, List<String> matchingStmt, List<Integer> lineNum,
            Map<Integer, Node> contextStmt, Map<Integer, String> originStmt) {
        StringSimilarity ss = new StringSimilarity();
        // get the matching ratio
        Set<Integer> visited = new HashSet<>();
        // get the matching line
        HashMap<Integer, Integer> addLines = new HashMap<>();
        HashMap<String, String> varMap = new HashMap<>();


//        for (int i = 0; i < matchingStmt.size(); i++) {
//            for (int j = 0; j < correctstmts.size(); j++) {
//                if (visited.contains(j)) continue;
//                double result = ss.calculateSimilarity(matchingStmt.get(i), correctstmts.get(j));
//                if (result >= 0.6) {
//                    visited.add(j);
//                    String oriVar = getVar(matchingStmt.get(i));
//                    String descVar = getVar(correctstmts.get(j));
//                    varMap.put(oriVar, '\\' + descVar);
//                    addLines.put(j, lineNum.get(i));
//                }
//            }
//        }
        // auto match the last statement
        String orgStmt = "";
        if (contextStmt.size() == 1) {

        }

        int ii = 0;
        int jj = correctstmts.size() - 1;
        visited.add(jj);
        List<String> oriVar = getVar(matchingStmt.get(ii));
        List<String> descVar = getVar(correctstmts.get(jj));
        //if parentNode is empty, insecure and secure has similar pattern
        if (matchingStmt.size() == 1) {
            int k = Math.min(oriVar.size(), descVar.size());
            for (int i = 0; i < k; i++) {
                varMap.put(oriVar.get(i), descVar.get(i));
            }
        } else {
            varMap.put("v_x", descVar.get(0));
            for (int i = 1; i < descVar.size(); i++) {
                varMap.put(oriVar.get(i - 1), descVar.get(i));
            }
        }

        addLines.put(jj, lineNum.get(ii));
        Node functionNode = contextStmt.get(lineNum.get(ii));
        String targetStmt = originStmt.get(lineNum.get(ii));
//        System.out.println("Function Signature: " + ((MethodDeclaration)functionNode).resolve().getQualifiedSignature());
//        System.out.println("Origin Statement: " + targetStmt);
        // get insert after linenum
        int pivot = -1;
        System.out.println(String.format("Update the original stmt: %s to stmt: %s", targetStmt, replaceVar(correctstmts.get(jj), varMap)));

        for (String a :correctstmts) {
            if ((a.contains("public") || a.contains("return")) && functionNode != null) {
                try {
                    System.out.println(String.format("Add Stmts before function: %s", ((MethodDeclaration)functionNode).resolve().getQualifiedSignature().toString()));
                    //String caller = getCaller(lineNum);
                    System.out.println();
                    break;
                } catch (Exception e) {
                    // javaparser parse failure, fallback
                    MethodDeclaration md = (MethodDeclaration) functionNode;
                    StringBuilder parameters = new StringBuilder();
                    parameters.append('(');
                    for (Parameter p: md.getParameters()) {
                        parameters.append(p);
                        parameters.append(", ");
                    }
                    parameters.delete(parameters.length()-2, parameters.length());
                    parameters.append(')');
                    System.out.println(String.format("Add Stmts before function: %s %s %s", md.getType(), md.getName(), parameters.toString()));
                    System.out.println();
                    break;
                }

            }
        }


        for (int j = 0; j < correctstmts.size(); j++) {
            if (correctstmts.get(j).toLowerCase().contains("secretkeyspec")) break;
            if (visited.contains(j)) {
                for (int i = 0; i < j && pivot == -1; i++) {
                    String stmtwithvar = replaceVar(correctstmts.get(i), varMap);
                    System.out.println(
                            String.format(
                                    "Add Statement before Line %d: %s",
                                    addLines.get(j), stmtwithvar));
                }
                pivot = addLines.get(j);
                continue;
            }
            if (pivot == -1) continue;
            String stmtwithvar = replaceVar(correctstmts.get(j), varMap);
            System.out.println(
                    String.format("Add Statement after Line %d: %s", pivot, stmtwithvar));
        }
        if (pivot == -1) {
            System.out.println("Suggest: ");
            for (int j = 0; j < correctstmts.size(); j++) {
                String stmtwithvar = replaceVar(correctstmts.get(j), varMap);
                System.out.println("\t" + stmtwithvar);
            }
        }
    }

    private String replaceVar(String stmtwithvar, HashMap<String, String> varMap) {
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            if (varMap.containsKey(entry.getKey())) {
                stmtwithvar = stmtwithvar.replace(varMap.get(entry.getKey()), entry.getValue());
            }
        }
        return stmtwithvar;
    }

    private String replaceStmtWithNormalizeName(Map<String, String> reverseParams, String targetStmt, int cons, int size) {
        List<String> paramsShouldBeReplaced = new ArrayList<>();
        for (Map.Entry<String, String> entry: reverseParams.entrySet()) {
            paramsShouldBeReplaced.add(entry.getKey());
        }
        Collections.sort(paramsShouldBeReplaced, Collections.reverseOrder());
        for (String str: paramsShouldBeReplaced) {
            targetStmt = targetStmt.replace(str, reverseParams.get(str));
        }
        return targetStmt;
    }


}
