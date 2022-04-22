package diff;

import com.changedistiller.test.SSLDetect.VariableDeclarationVisitor;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class DiffChecker {
    private TypeSolver typeSolver =
            new CombinedTypeSolver(
                    new ReflectionTypeSolver(),
                    new JavaParserTypeSolver("/xxx/code/cipher_test/src/"));
    private JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
    ParserConfiguration parserConfiguration =
            new ParserConfiguration().setSymbolResolver(symbolSolver);
    private LogManager logManager = LogManager.getLogManager();
    private Logger log = logManager.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public Map<String, TreeSet<Expression>> lVarMap = new TreeMap<String, TreeSet<Expression>>();
    public Map<String, TreeSet<Expression>> rVarMap = new TreeMap<String, TreeSet<Expression>>();
    public Map<Expression, Set<String>> eToVL = new HashMap<>();
    public Map<Expression, Set<String>> eToVR = new HashMap<>();
    public Map<Expression, Boolean> expLable = new HashMap<>();
    public Util util = new Util();

    public DiffChecker() throws IOException {}

    public void run() throws IOException {
        String left_file_path = "/xxx/code/cipher_test/src/cipher_test/insecure/parameter/15.java";
        String right_file_path = "/xxx/code/cipher_test/src/cipher_test/secure/parameter/15.java";

        CompilationUnit leftCU = getCU(left_file_path);
        CompilationUnit rightCU = getCU(right_file_path);
        Map<Range, Expression> leftStmts = util.getStatements(leftCU);
        Map<Range, Expression> rightStmts = util.getStatements(rightCU);
        HashMap<Range, Range> lineMap = null;
        extractVars(leftStmts, lVarMap, eToVL);
        extractVars(rightStmts, rVarMap, eToVR);
        util.treeDiff(leftCU, rightCU);
        DiffType diff = null;

        // 这里需不需要判断相等存疑，可以直接进行匹配
        if (leftStmts.size() == rightStmts.size()) {
            lineMap = util.getMatchStmts(leftStmts, rightStmts, leftCU, rightCU, 0.69f);
            diff = extractDiff(lineMap, leftStmts, rightStmts);
        } else {
            // 判断多行情况
            lineMap = util.getMatchStmts(leftStmts, rightStmts, leftCU, rightCU, 0.7f);
            diff = extractMultiDiff(lineMap, leftStmts, rightStmts, leftCU, rightCU);
        }

        if (leftStmts.size() != 0) extractTargetStmt(diff, leftCU, rightStmts, lineMap);
        else {
            extractTargetStmt(diff, rightCU, rightStmts, lineMap);
        }
        // 如果方法类型是keypairgenerator，特殊处理
        if (diff.methodType.compareTo("KeyPairGenerator") == 0) {
            extractKPG(diff, leftStmts, leftCU);
        }
        for (String str : util.normalizeStmts(leftCU, util.toMapRangeNode(leftStmts), false).values()) {
            diff.oldStmts.add(str);
        }
        if (diff.className.length() == 0) {
            diff.className = util.getClassName(leftCU);
        }
        System.out.println(diff);
    }

    private DiffType extractDiff(
            HashMap<Range, Range> lineMap,
            Map<Range, Expression> leftStmts,
            Map<Range, Expression> rightStmts) {
        Expression leftliterals = null;
        Expression rightliterals = null;
        DiffType diff = null;
        for (Range l : lineMap.keySet()) {
            Range r = lineMap.get(l);
            Expression leftExpr = leftStmts.get(l);
            Expression rightExpr = rightStmts.get(r);
            if (leftExpr.toString().toLowerCase().contains("stringliterals") || leftExpr.toString().toLowerCase().contains("ByteLiterals") ) {
                leftliterals = leftExpr;
                rightliterals = rightExpr;
                continue;
            }
            List<Node> leftNodes = util.extractNode(leftExpr);
            List<Node> rightNodes = util.extractNode(rightExpr);
            log.info("left: " + leftNodes.size() + " " + " right: " + rightNodes.size());
            assert leftNodes.size() == rightNodes.size()
                    : "leftNodes and rightNodes is not the same length";
            diff = extractDiff(leftNodes, rightNodes);
            if (diff.pattern != null) {
                if (diff.pattern == Pattern.PARAMETER || diff.pattern == Pattern.NUMBER) {
                    extractPos(leftExpr, diff);
                }
                if (leftliterals != null) {
                    util.extractLiterals(leftliterals, rightliterals, diff);
                }
                return diff;
            }
        }

        return new DiffType();
    }

    // get the similat stmt in multi-line case

    class RangeComparator implements Comparator<Range> {
        @Override
        public int compare(Range o1, Range o2) {
            return o1.begin.line - o2.begin.line;
        }
    }

    private DiffType extractMultiDiff(
            HashMap<Range, Range> lineMap,
            Map<Range, Expression> leftStmts,
            Map<Range, Expression> rightStmts,
            CompilationUnit leftCU,
            CompilationUnit rightCU) {
        DiffType diff = new DiffType();
        diff.stmts = new ArrayList<>();
        diff.pattern = Pattern.COMPOSITE;
        Map<String, NodeList<Statement>> leftMap = extractStructure(leftCU);
        Map<String, NodeList<Statement>> rightMap = extractStructure(rightCU);
        // 如果存在匿名类，需要另外进行stmt匹配
        if (leftMap.size() != 0 || rightMap.size() != 0) {
            return extractMultiDiffByStructure(leftMap, rightMap, leftCU, rightCU);
        }
        Map<Range, Expression> leftStmts_ = new TreeMap<>(new RangeComparator());
        leftStmts_.putAll(leftStmts);
        Map<Range, Node> rightStmts_ = new TreeMap<>(new RangeComparator());
        Set<Node> missingStmts = util.treeDiff(leftCU, rightCU);
        if (missingStmts.size() > 0) {
            for (Node node: missingStmts) {
                rightStmts_.put(node.getRange().get(), node);
            }
        } else {
            // in here means the structure of leftCU and rightCU are the same
            rightStmts_.putAll(rightStmts);
        }

        for (Range l : lineMap.keySet()) {
            Range r = lineMap.get(l);
           // if(leftStmts_.get(l))
            leftStmts_.remove(l);
            rightStmts_.remove(r);
        }
        if (leftStmts_.size() > 0) {
            diff.action = Action.DELETE;
        } else if (rightStmts_.size() > 0) {
            diff.action = Action.ADD;
        }
        util.addStmtToDiff(diff, rightStmts_, rightCU);
        return diff;
    }

    private Map<String, NodeList<Statement>> extractStructure(CompilationUnit cu) {
        ObjectCreationExpr expr = cu.findFirst(ObjectCreationExpr.class).get();
        Map<String, NodeList<Statement>> methodStmtsMap = new HashMap<>();
        if (expr.getAnonymousClassBody().isPresent()) {
            NodeList<?> classList = expr.getAnonymousClassBody().get();
            // 存在子节点
            for (Object clazz : classList) {
                if (clazz instanceof MethodDeclaration) {
                    MethodDeclaration md = (MethodDeclaration) clazz;
                    BlockStmt blockStmt = md.getBody().get();
                    methodStmtsMap.put(md.getNameAsString(), blockStmt.getStatements());
                }
            }
        }
        return methodStmtsMap;
    }

    private DiffType extractMultiDiffByStructure(
            Map<String, NodeList<Statement>> leftMap,
            Map<String, NodeList<Statement>> rightMap,
            CompilationUnit leftCU,
            CompilationUnit rightCU) {
        DiffType diff = new DiffType();
        for (String key : leftMap.keySet()) {
            Map<Range, Statement> leftStmts = util.StmtsListtoMap(leftMap.get(key));
            Map<Range, Statement> rightStmts = util.StmtsListtoMap(rightMap.get(key));
            HashMap<Range, Range> lineMap = util.getMatchStmtsTypeStmt(leftStmts, rightStmts, leftCU, rightCU, 0.7f);
            Map<Range, Statement> leftStmts_ = new HashMap<>(leftStmts);
            Map<Range, Statement> rightStmts_ = new HashMap<>(rightStmts);
            for (Range l : lineMap.keySet()) {
                Range r = lineMap.get(l);
                leftStmts_.remove(l);
                rightStmts_.remove(r);
            }
            log.info("leftStmts_: " + leftStmts_.size() + " rightStmts_: " + rightStmts_.size());
            if (leftStmts_.size() > 0) {
                diff.action = Action.DELETE;
                diff.className = key;
            } else if (rightStmts_.size() > 0) {
                diff.action = Action.ADD;
                diff.className = key;
            }
            util.addStmtToDiffTypeStmt(diff, rightStmts, rightCU);
        }
        return diff;
    }

    private void extractVars(
            Map<Range, Expression> stmts,
            Map<String, TreeSet<Expression>> varMap,
            Map<Expression, Set<String>> etoV) {
        for (Expression e : stmts.values()) {
            Map<String, String> varList = new HashMap<>();
            VoidVisitor<Map<String, String>> visitor = new VariableDeclarationVisitor();
            e.accept(visitor, varList);
            Set<String> vars = ((VariableDeclarationVisitor) visitor).getVariables();
            expLable.put(e, ((VariableDeclarationVisitor) visitor).keyStmt);
            etoV.put(e, vars);
            for (String v : vars) {
                util.putValue(varMap, v, e);
            }
        }
    }

    private void extractPos(Expression expr, DiffType diff) {
        log.info("extract pos");
        if (diff.pos != -1) return;
        Set<String> vars = eToVL.get(expr);
        for (String v : vars) {
            TreeSet<Expression> expressions = lVarMap.get(v);
            if (!util.isSecurityAPI(util.resolveType(expr))) expressions.remove(expr);
            for (Expression exp : expressions) {
                if (exp instanceof VariableDeclarationExpr) {
                    VariableDeclarationExpr VDExp = (VariableDeclarationExpr) exp;
                    Map<String, String> varList = new HashMap<>();
                    VoidVisitor<Map<String, String>> visitor = new VariableDeclarationVisitor();
                    VDExp.accept(visitor, varList);
                    Integer pos = ((VariableDeclarationVisitor) visitor).getPos(v);
                    diff.pos = pos;
                }
            }
        }
    }

    // extract the detail diff
    private DiffType extractDiff(List<Node> leftNodes, List<Node> rightNodes) {
        for (int i = leftNodes.size() - 1; i >= 0; i--) {
            Node lNode = leftNodes.get(i);
            Node rNode = rightNodes.get(i);

            // 名字比了没意义
            if (lNode instanceof SimpleName) {
                continue;
            }
            // 如果是字符串或者是整数，就直接进行比较，如果不一样，就是参数问题
            if (lNode instanceof IntegerLiteralExpr) {
                log.info("Literal Comparison");
                if (!((IntegerLiteralExpr) lNode)
                        .getValue()
                        .equals(((IntegerLiteralExpr) rNode).getValue())) {
                    log.info("Number Parameter Err");
                    return new DiffType(
                            Pattern.NUMBER,
                            String.valueOf(((IntegerLiteralExpr) lNode).getValue()),
                            String.valueOf(((IntegerLiteralExpr) rNode).getValue()),
                            null,
                            null,
                            util.extractPosDirectly(lNode),
                            null,
                            null);
                }
            }

            if (lNode instanceof StringLiteralExpr) {
                log.info("Literal Comparison");
                if (!((LiteralStringValueExpr) lNode)
                        .getValue()
                        .equals(((LiteralStringValueExpr) rNode).getValue())) {
                    log.info("String Parameter Err");
                    return new DiffType(
                            Pattern.PARAMETER,
                            String.valueOf(((LiteralStringValueExpr) lNode).getValue()),
                            String.valueOf(((LiteralStringValueExpr) rNode).getValue()),
                            null,
                            null,
                            -1,
                            null,
                            null);
                }
            }

            // 比较方法名不一样
            if (lNode instanceof ClassOrInterfaceType && rNode instanceof ClassOrInterfaceType) {
                log.info("Method Comparison");
                log.info(
                        ((ClassOrInterfaceType) lNode).getName()
                                + " "
                                + ((ClassOrInterfaceType) rNode).getName());
                SimpleName lName = ((ClassOrInterfaceType) lNode).getName();
                SimpleName rName = ((ClassOrInterfaceType) rNode).getName();
                if (!lName.equals(rName)) {
                    log.info("MethodName Err");
                    return new DiffType(
                            Pattern.NAME,
                            String.valueOf(lName.toString()),
                            String.valueOf(rName.toString()),
                            null,
                            null,
                            -1,
                            null,
                            null);
                }
            }
        }
        return new DiffType();
    }

    private void extractKPG(DiffType diff, Map<Range, Expression> stmts, CompilationUnit cu) {
        log.info("Meet KeyPairGenerator");
        // change callee to initialize for backward slicing
        diff.callee = "initialize";
        // need to check the value of kpg.getInstance
        if (diff.pattern == Pattern.NUMBER) diff.pos = 0;
        //        Expression getInstance = null;
        //        for (Expression e: stmts.values()) {
        //            if (e.toString().contains("getInstance")) {
        //                getInstance = e;
        //                break;
        //            }
        //        }
        //        String algo =
        // getInstance.findFirst(MethodCallExpr.class).get().getArgument(0).toString();
        //        System.out.println(algo);
        Map<Range, String> normalizeStmts = util.normalizeStmts(cu, util.toMapRangeNode(stmts), false);
        List<String> matchStmts = new ArrayList<>();
        for (String stmt : normalizeStmts.values()) {
            if (stmt.contains("getInstance")) {
                matchStmts.add(stmt);
            }
        }
        diff.stmts = matchStmts;
    }

    private void extractTargetStmt(
            DiffType diff,
            CompilationUnit cu,
            Map<Range, Expression> rightStmts,
            HashMap<Range, Range> lineMap) {

        log.info("extracting the target stmt");
        List<Expression> exps = new ArrayList<>();
        exps.addAll(cu.findAll(ObjectCreationExpr.class));
        exps.addAll(cu.findAll(MethodCallExpr.class));
        for (Expression exp : exps) {
            String type = util.resolveType(exp);
            if (util.isSecurityAPI(type)) {
                // 拿到第几个参数
                diff.methodType = type.substring(type.lastIndexOf(".") + 1);
                diff.callee = util.extractCallee(exp);
                diff.args = util.extractArgSize(exp);
                if (diff.pattern == Pattern.COMPOSITE) {
                    diff.pos =
                            extractCompositePos(
                                    exp, util.getRightExp(exp, rightStmts, lineMap), diff);
                }
            }
            if (type.contains("Random")) {
                diff.pos =
                        extractCompositePos(exp, util.getRightExp(exp, rightStmts, lineMap), diff);
                diff.methodType = type.substring(type.lastIndexOf(".") + 1);
                diff.callee = "<init>";
                diff.args = 0;
            }
            if (exp.toString().contains("setSeed")) {
                diff.pos = 0;
                diff.callee = "setSeed";
                diff.incorrect = "constant";
                diff.args = 1;
            }
        }
    }

    private int extractCompositePos(Expression left, Expression right, DiffType diff) {
        log.info("Extracting composite pos");
        if (left instanceof ObjectCreationExpr) {
            NodeList<Expression> leftArgs = ((ObjectCreationExpr) left).getArguments();
            NodeList<Expression> rightArgs = ((ObjectCreationExpr) right).getArguments();
            for (int i = 0; i < leftArgs.size(); i++) {
                Expression leftExp = leftArgs.get(i);
                Expression rightExp = rightArgs.get(i);
                if (leftExp instanceof FieldAccessExpr
                        && (leftExp.toString().contains("StringLiterals.CONSTANT")
                                || leftExp.toString().contains("IntegerLiterals.CONSTANT"))) {
                    diff.incorrect = "constant";
                    return i;
                }

                if(leftExp instanceof NameExpr) {
                    int k  = i;
                    i++;
                    while(i < leftArgs.size()) {
                        Expression lExp = leftArgs.get(i);
                        Expression rExp = rightArgs.get(i);
                        if(lExp.toString().equals(rExp.toString())) i++;
                        else {
                            return i;
                        }
                    }
                    return k;
                }

                if (leftExp instanceof MethodCallExpr
                                && ((leftExp
                                        .toString()
                                        .contains("ByteLiterals.CONSTANT_ARRAY"))
                        || ((MethodCallExpr) leftExp)
                                .getScope()
                                .toString()
                                .contains("IntegerLiterals.CONSTANT"))) {
                    diff.incorrect = "constant";
                    return i;
                }
                if (leftExp.getClass() != rightExp.getClass()) {
                    diff.incorrect = "constant";
                    return i;
                }
            }
        }

        if (left instanceof MethodCallExpr) {

            NodeList<Expression> leftArgs = ((MethodCallExpr) left).getArguments();
            NodeList<Expression> rightArgs = ((MethodCallExpr) right).getArguments();
            for (int j = 0; j < leftArgs.size(); j++) {
                Expression leftExp = leftArgs.get(j);
                Expression rightExp = rightArgs.get(j);
                if (leftExp.getClass() == rightExp.getClass()) {
                    // diff.incorrect ="static";
                    return j;
                }
            }
        }
        return -1;
    }

    public CompilationUnit getCU(String file_path) throws IOException {
        Path path = Paths.get(file_path);
        SourceRoot sourceRoot = new SourceRoot(path.getParent());
        sourceRoot.setParserConfiguration(parserConfiguration);
        CompilationUnit cu = sourceRoot.parse("", path.getFileName().toString());

        return cu;
    }
}
