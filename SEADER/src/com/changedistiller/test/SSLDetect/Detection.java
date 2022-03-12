package com.changedistiller.test.SSLDetect;

import com.Constant;
import com.github.javaparser.JavaToken;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.ExampleUtil;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import utils.StringSimilarity;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Detection {

    public String[] args;
    public String methodT; //method type
    public Map<String, String> paramMap = new HashMap<>();
    public List<Object> paramList = new ArrayList<>();

    public Detection(String str, String type) {
        args = str.split(" ");
        methodT = type;
    }

    /**
     * Usage: PDFSlice -appJar [jar file name] -mainClass [main class] -srcCaller [method name] -srcCallee [method name] -dd [data
     * dependence options] -cd [control dependence options] -dir [forward|backward]
     *
     * <ul>2
     * <li>"jar file name" should be something like "c:/temp/testdata/java_cup.jar"
     * <li>"main class" should be something like "c:/temp/testdata/java_cup.jar"
     * <li>"method name" should be the name of a method. This takes a slice from the statement that calls "srcCallee" from "srcCaller"
     * <li>"data dependence options" can be one of "-full", "-no_base_ptrs", "-no_base_no_heap", "-no_heap",
     * "-no_base_no_heap_no_cast", or "-none".
     * </ul>
     *
     * @throws CancelException
     * @throws IllegalArgumentException
     * @throws IOException
     * @see com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions <li>"control dependence options" can be "-full" or "-none" <li>the
     * -dir argument tells whether to compute a forwards or backwards slice. </ul>
     */

    public Process run() throws IllegalArgumentException, CancelException, IOException {
        // parse the command-line into a Properties object
        Properties p = CommandLine.parse(args);
        // validate that the command-line has the expected format
        validateCommandLine(p);
        // run the applications
        return run(p.getProperty("appJar"), p.getProperty("mainClass"), p.getProperty("srcCaller"), p.getProperty("srcCallee"),
                goBackward(p), getDataDependenceOptions(p), getControlDependenceOptions(p));
    }

    /**
     * Should the slice be a backwards slice?
     */
    private boolean goBackward(Properties p) {
        return !p.getProperty("dir", "backward").equals("forward");
    }

    /**
     * Compute a slice from a call statements, dot it, and fire off the PDF viewer to visualize the result
     *
     * @param appJar     should be something like "c:/temp/testdata/java_cup.jar"
     * @param mainClass  should be something like "c:/temp/testdata/java_cup.jar"
     * @param srcCaller  name of the method containing the statement of interest
     * @param srcCallee  name of the method called by the statement of interest
     * @param goBackward do a backward slice?
     * @param dOptions   options controlling data dependence
     * @param cOptions   options controlling control dependence
     * @return a Process running the PDF viewer to visualize the dot'ted representation of the slice
     * @throws CancelException
     * @throws IllegalArgumentException
     */
    public Process run(String appJar, String mainClass, String srcCaller, String srcCallee, boolean goBackward,
                       Slicer.DataDependenceOptions dOptions, Slicer.ControlDependenceOptions cOptions) throws IllegalArgumentException, CancelException,
            IOException {
        try {
            // create an analysis scope representing the appJar as a J2SE application
            AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, (new FileProvider()).getFile(Constant.EXCLUDES));
            //slice 不要进包内slice
            ExampleUtil.addDefaultExclusions(scope);

            // build a class hierarchy, call graph, and system dependence graph
            ClassHierarchy cha = ClassHierarchyFactory.make(scope);
            Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
            AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
            options.setReflectionOptions(AnalysisOptions.ReflectionOptions.FULL);
            CallGraphBuilder<InstanceKey> builder = Util.makeVanillaZeroOneCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha, scope);
            CallGraph cg = builder.makeCallGraph(options, null);
            PointerAnalysis pa = builder.getPointerAnalysis();
            SDG<InstanceKey> sdg = new SDG<>(cg, builder.getPointerAnalysis(), dOptions, cOptions);
            Statement s = null;
            CGNode keyNode = null;
            IClassLoader loader = null;
            for (CGNode node : cg) {
                Statement statement = findCallTo(node, srcCallee, methodT);
                if (statement != null) {
                    s = statement;
                }
            }
            System.err.println("Statement: " + s);
            Collection<Statement> slice = null;
            final PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
            slice = Slicer.computeForwardSlice(s, cg, pointerAnalysis, dOptions, cOptions);
            //dumpSlice(slice);

            //get the line number in the source code
            List<Integer> lineNum = new ArrayList<>();
            for (Statement stmt: slice) {
                if (stmt.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().compareToIgnoreCase("primordial") == 0
                        || stmt.getKind() != Statement.Kind.NORMAL)
                    continue;
                lineNum.add(getLineNumber(stmt));
                System.out.println(getLineNumber(stmt));
            }
            // create a view of the SDG restricted to nodes in the slice
            Graph<Statement> g = pruneSDG(sdg, slice);
            sanityCheck(slice, g);
            String filePath = "C:\\Users\\ying\\Documents\\JAVA_CODE\\cryptoapi-bench\\src\\main\\java\\org\\cryptoapi\\bench\\dummyhostnameverifier\\HostnameVerifierCase2.java";
            //String filePath = "C:\\Users\\ying\\Documents\\JAVA_CODE\\cryptoapi-bench\\src\\main\\java\\org\\cryptoapi\\bench\\predictablecryptographickey\\keypair.java";

            // The following part is to get the matching statements from the source code:
            // Use javaparser
            // 1. visit the file to get all the variables and save to the varList
            // 2. normalize the statements by replacing the variables but leave the constants.
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            Map<String, String> varList = new HashMap<>();
            VoidVisitor<Map<String, String>> visitor = new VariableDeclarationVisitor();
            cu.accept(visitor, varList);
            for (Map.Entry<String, String> entry: varList.entrySet()) {
                paramMap.put("\\" + entry.getValue(), entry.getKey());
            }
            int prev = -1;
            List<String> matchingStmt = new ArrayList<>();
            String str = "";
            for (JavaToken tr: cu.getTokenRange().get()) {
                int nline = tr.getRange().get().begin.line;
                if (lineNum.contains(nline)) {
                    if (prev == -1) prev = nline;
                    if (prev != nline) {
                        matchingStmt.add(str.trim());
                        str = "";
                        prev = nline;
                    }
                    if (varList.containsKey(tr.getText())) {
                        str += varList.get(tr.getText());
                    }
                    else {
                        str += tr.getText();
                    }
                }
            }
            matchingStmt.add(str.trim());
            StringSimilarity ss = new StringSimilarity();

            //correct template
            List<String> correctstmts = new ArrayList<>();
            correctstmts.add(" KeyPairGenerator $v_9 = KeyPairGenerator.getInstance(\"JKS\");"); // can't detect the algorithm
            correctstmts.add(" $v_9.initialize(2048);");
//            correctstmts.add("SSLSocket $v_1 = (SSLSocket) $v_0.createSocket(\"mail.google.com\", 443);");
//            correctstmts.add("HostnameVerifier $v_2  = HttpsURLConnection.getDefaultHostnameVerifier();");
//            correctstmts.add("SSLSession $v_3 = socket.getSession();");
//            correctstmts.add("if (!$v_2.verify(\"mail.google.com\", $v_3)) {");
//            correctstmts.add("throw new SSLHandshakeException(\"Expected mail.google.com, not found \" +\n" +
//                    "\t\t\t\t\ts.getPeerPrincipal());}");

            // get the matching ratio
            Set<Integer> visited = new HashSet<>();
            HashMap<String, String> varMap = new HashMap<>();
            for(int i = 0; i < matchingStmt.size(); i++) {
                for (int j = 0; j < correctstmts.size(); j++) {
                    if (visited.contains(j)) continue;
                    double result = ss.calculateSimilarity(matchingStmt.get(i), correctstmts.get(j));
                    if (result >= 0.9) {
                        visited.add(j);
                        String oriVar = getVar(matchingStmt.get(i));
                        String descVar = getVar(correctstmts.get(j));
                        varMap.put(oriVar, '\\' + descVar);
                    }
                }
            }
            for (int j = 0; j<correctstmts.size(); j++) {
                if (visited.contains(j)) continue;
                String stmtwithvar = correctstmts.get(j);
                for (Map.Entry<String, String> entry: paramMap.entrySet()) {
                    if (varMap.containsKey(entry.getKey().substring(1))) {
                        stmtwithvar = stmtwithvar.replaceAll(varMap.get(entry.getKey().substring(1)), entry.getValue());
                    }
                }
                System.out.println("MISSING: " + stmtwithvar);
            }

            return null;
        } catch (WalaException e) {
            // something bad happened.
            e.printStackTrace();
            return null;
        }
    }

    private String getVar(String s) {
        String pattern = "(.*)(\\$v_\\d+)(.*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(s);
        if (m.find()) {
            return m.group(2);
        }
        else
            return "";
    }

    /**
     * check that g is a well-formed graph, and that it contains exactly the number of nodes in the slice
     */
    private void sanityCheck(Collection<Statement> slice, Graph<Statement> g) {
        try {
            GraphIntegrity.check(g);
        } catch (GraphIntegrity.UnsoundGraphException e1) {
            e1.printStackTrace();
            Assertions.UNREACHABLE();
        }
        Assertions.productionAssertion(g.getNumberOfNodes() == slice.size(), "panic " + g.getNumberOfNodes() + " " + slice.size());
    }

    /**
     * return a view of the sdg restricted to the statements in the slice
     */
    public Graph<Statement> pruneSDG(SDG<InstanceKey> sdg, final Collection<Statement> slice) {
        return GraphSlicer.prune(sdg, slice::contains);
    }

    /**
     * Validate that the command-line arguments obey the expected usage.
     * <p>
     * Usage:
     * <ul>
     * <li>args[0] : "-appJar"
     * <li>args[1] : something like "c:/temp/testdata/java_cup.jar"
     * <li>args[2] : "-mainClass"
     * <li>args[3] : something like "Lslice/TestRecursion" *
     * <li>args[4] : "-srcCallee"
     * <li>args[5] : something like "print" *
     * <li>args[4] : "-srcCaller"
     * <li>args[5] : something like "main"
     * </ul>
     *
     * @throws UnsupportedOperationException if command-line is malformed.
     */
    void validateCommandLine(Properties p) {
        if (p.get("appJar") == null) {
            throw new UnsupportedOperationException("expected command-line to include -appJar");
        }
        if (p.get("mainClass") == null) {
            throw new UnsupportedOperationException("expected command-line to include -mainClass");
        }
        if (p.get("srcCallee") == null) {
            throw new UnsupportedOperationException("expected command-line to include -srcCallee");
        }
        if (p.get("srcCaller") == null) {
            throw new UnsupportedOperationException("expected command-line to include -srcCaller");
        }
    }

    public Statement findCallTo(CGNode n, String methodName, String methodType) {
        IR ir = n.getIR();
        if (ir == null) return null;

        for (SSAInstruction s : Iterator2Iterable.make(ir.iterateAllInstructions())) {
            if (s instanceof SSAInvokeInstruction) {
                if (((SSAInvokeInstruction) s).getCallSite().getDeclaredTarget().getDeclaringClass().
                        getClassLoader().getName().toString().compareToIgnoreCase("primordial") == 0)
                    continue;
//                System.out.println(s);
                SSAInvokeInstruction call = (SSAInvokeInstruction) s;
                // Get the information binding
                String methodT = call.getCallSite().getDeclaredTarget().getSignature();
                if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)
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

    public Slicer.DataDependenceOptions getDataDependenceOptions(Properties p) {
        String d = p.getProperty("dd", "full");
        for (Slicer.DataDependenceOptions result : Slicer.DataDependenceOptions.values()) {
            if (d.equalsIgnoreCase(result.getName())) {
                return result;
            }
        }
        Assertions.UNREACHABLE("unknown data datapendence option: " + d);
        return null;
    }

    public Slicer.ControlDependenceOptions getControlDependenceOptions(Properties p) {
        String d = p.getProperty("cd", "full");
        for (Slicer.ControlDependenceOptions result : Slicer.ControlDependenceOptions.values()) {
            if (d.equalsIgnoreCase(result.getName())) {
                return result;
            }
        }
        Assertions.UNREACHABLE("unknown control datapendence option: " + d);
        return null;
    }

    public void dumpSlice(Collection<Statement> slice) {
        dumpSlice(slice, new PrintWriter(System.err));
    }

    public void dumpSlice(Collection<Statement> slice, PrintWriter w) {
        w.println("SLICE:\n");
        int i = 1;

        for (Statement s : slice) {
            if (s.getNode().getMethod().getDeclaringClass().getClassLoader().getName().toString().compareToIgnoreCase("primordial") == 0)
                continue;
            if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
                int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
                try {
                    bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                    try {
                        int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
                        System.err.println ( "Source line number = " + src_line_number );
                    } catch (Exception e) {
                        System.err.println("Bytecode index no good");
                        System.err.println(e.getMessage());
                    }
                } catch (Exception e ) {
                    System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
                    System.err.println(e.getMessage());
                }

                String line = (i++) + "   " + s;
                w.println(line);
                w.flush();
            }
        }
    }

    public int getLineNumber(Statement stmt) {
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
        } catch (Exception e ) {
            System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
            System.err.println(e.getMessage());
        }
        return -1;
    }

}