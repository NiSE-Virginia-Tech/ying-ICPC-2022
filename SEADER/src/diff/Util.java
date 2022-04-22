package diff;

import com.changedistiller.test.SSLDetect.VariableDeclarationVisitor;
import com.github.javaparser.JavaToken;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.resolution.types.ResolvedType;
import utils.StringSimilarity;

import java.util.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Util {

    public LogManager logManager = LogManager.getLogManager();
    public Logger log = logManager.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public String getClassName(CompilationUnit cu) {
        List<ClassOrInterfaceDeclaration> classDeclarations =
                cu.findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration c : classDeclarations) {
            for (Object f : c.getMembers()) {
                if (f instanceof MethodDeclaration) {
                    if (((MethodDeclaration) f).getAnnotations().size() > 0) {
                        if (((MethodDeclaration) f)
                                .getAnnotation(0)
                                .getName()
                                .asString()
                                .contains("Override")) {
                            log.info("find override");
                            return c.getImplementedTypes().get(0).getNameAsString();
                        }
                    }
                }
            }
        }
        return "";
    }

    public boolean isSecurityAPI(String type) {
        Set<String> keywords = new HashSet<>(Arrays.asList("security", "crypto", "javax"));
        for (String keyword : keywords) {
            if (type.contains(keyword)) return true;
        }
        return false;
    }

    public String extractCallee(Expression exp) {
        log.info("Extract callee");
        if (exp instanceof ObjectCreationExpr) {
            return "<init>";
        }
        if (exp instanceof MethodCallExpr) {
            // ResolvedType type = (MethodCallExpr) exp.calculateResolvedType().describe();
            return ((MethodCallExpr) exp).getName().toString();
        }
        return "";
    }

    public int extractArgSize(Expression exp) {
        log.info("Extract Aug #");
        if (exp instanceof ObjectCreationExpr) {
            return exp.asObjectCreationExpr().getArguments().size();
        }
        if (exp instanceof MethodCallExpr) {
            // ResolvedType type = (MethodCallExpr) exp.calculateResolvedType().describe();
            return exp.asMethodCallExpr().getArguments().size();
        }
        return -1;
    }

    class ExprComp implements Comparator<Expression> {
        @Override
        public int compare(Expression e1, Expression e2) {
            return e1.getRange().get().begin.line - e2.getRange().get().begin.line;
        }
    }

    public void putValue(Map<String, TreeSet<Expression>> varMap, String v, Expression e) {
        if (varMap.containsKey(v)) {
            varMap.get(v).add(e);
        } else {
            TreeSet<Expression> l = new TreeSet<>(new ExprComp());
            l.add(e);
            varMap.put(v, l);
        }
    }

    public String resolveType(Expression expr) {
        try {
            ResolvedType type = expr.calculateResolvedType();
            return type.describe();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public int extractPosDirectly(Node lNode) {
        Node parentNode = lNode.getParentNode().get();
        String type = "";
        if (parentNode instanceof Expression) {
            type = resolveType((Expression) parentNode);
        }
        if (!isSecurityAPI(type)) return -1;
        int pos = -1;
        NodeList<Expression> children = ((ObjectCreationExpr) parentNode).getArguments();
        pos = children.indexOf(lNode);
        return pos;
    }

    public void extractLiterals(Expression leftExpr, Expression rightExpr, DiffType diff) {
        ObjectCreationExpr left = leftExpr.findFirst(ObjectCreationExpr.class).get();
        ObjectCreationExpr right = rightExpr.findFirst(ObjectCreationExpr.class).get();
        diff.incorrect = left.getArguments().toString();
        diff.correct = right.getArguments().toString();
        diff.pos = 0;
    }

    public void addStmtToDiff(DiffType diff, Map<Range, Node> stmts, CompilationUnit cu) {
        Map<Range, String> stmtMap = normalizeStmts(cu, stmts, false);
        for (String str : stmtMap.values()) {
            diff.stmts.add(str);
        }
    }

    public void addStmtToDiffTypeStmt(
            DiffType diff, Map<Range, Statement> stmts, CompilationUnit cu) {
        Map<Range, String> stmtMap = normalizeStmtsTypeStmt(cu, stmts, false);
        for (String str : stmtMap.values()) {
            diff.stmts.add(str);
        }
    }

    public List<Node> extractNode(Expression expr) {
        List<Node> nodes = new ArrayList<>();
        //        System.out.println(expr);
        expr.stream()
                .forEach(
                        x -> {
                            nodes.add(x);
                            //            System.out.println("\t" + x + " " + x.getClass() + "\n");
                        });

        return nodes;
    }

    public Expression getRightExp(
            Expression exp, Map<Range, Expression> rightStmts, HashMap<Range, Range> lineMap) {
        Node parent = exp.getParentNode().get();
        while (!(parent instanceof ExpressionStmt)) {
            parent = parent.getParentNode().get();
        }
        log.info(parent.toString());
        Expression rightExp = rightStmts.get(lineMap.get(parent.getRange().get()));
        if (rightExp == null) return exp;
        for (Expression e : rightExp.findAll(exp.getClass())) {
            return e;
        }
        return null;
    }

    //    public CompilationUnit getCU(String file_path) throws FileNotFoundException {
    //        return StaticJavaParser.parse(new File(file_path));
    //    }
    class RangeComparator implements Comparator<Range> {
        @Override
        public int compare(Range o1, Range o2) {
            return o1.begin.line - o2.begin.line;
        }
    }

    public Map<Range, Expression> getStatements(CompilationUnit cu) {
        Map<Range, Expression> stmtMap = new TreeMap<>(new RangeComparator());
        List<ExpressionStmt> expStmts = cu.findAll(ExpressionStmt.class);
        for(ExpressionStmt es: expStmts){
            stmtMap.put(es.getRange().get(), es.getExpression());
        }
//                .forEach(x -> stmtMap.put(x.getRange().get(), x.getExpression()));
        return stmtMap;
    }

    public HashMap<Range, Range> getMatchStmts(
            Map<Range, Expression> left,
            Map<Range, Expression> right,
            CompilationUnit leftCU,
            CompilationUnit rightCU,
            float threshold) {
        HashMap<Range, Range> lineMap = new HashMap<>();
        StringSimilarity ss = new StringSimilarity();
        Map<Range, String> leftStmts = normalizeStmts(leftCU, toMapRangeNode(left), true);
        Map<Range, String> rightStmts = normalizeStmts(rightCU, toMapRangeNode(right), true);
        for (Range i : leftStmts.keySet()) {
            for (Range j : rightStmts.keySet()) {
                String leftStmt = leftStmts.get(i);
                String rightStmt = rightStmts.get(j);
                float score = (float) ss.calculateSimilarity(leftStmt, rightStmt);
                //                System.out.println(leftStmt + "-" + rightStmt + ":" + score +
                // "\n");
                if (score >= threshold) {
                    //                    System.out.println(leftStmt + right);
                    lineMap.put(i, j);
                    break;
                }
            }
        }
        return lineMap;
    }

    public Map<Range, String> normalizeStmts(
            CompilationUnit cu, Map<Range, Node> lineMap, boolean isArgumentNormalized) {
        if (isArgumentNormalized) {
            return normalizeStmtsWithArguments(cu, lineMap);
        }
        Map<String, String> paramMap = new HashMap<>();
        Map<String, String> varList = new HashMap<>();
        Deque<Integer> st = new ArrayDeque<Integer>();
        int count = 0;
        int paramIndex = 0;
        VoidVisitor<Map<String, String>> visitor = new VariableDeclarationVisitor();
        cu.accept(visitor, varList);
        log.info(varList.toString());
        Set<Integer> lineNumSet = new HashSet<>();
        Map<Integer, Range> lineRangeMap = new HashMap<>();
//        for (Range r : lineMap.keySet()) {
//            lineNumSet.add(r.begin.line);
//            lineRangeMap.put(r.begin.line, r);
//        }
        for (Map.Entry<String, String> entry : varList.entrySet()) {
            paramMap.put("\\" + entry.getValue(), entry.getKey());
        }
        int prev = -1;
        Map<Range, String> matchingStmt = new TreeMap<>(new RangeComparator());

        for (Map.Entry<Range, Node> entry: lineMap.entrySet()) {
            String str = "";
            for (JavaToken tr : entry.getValue().getTokenRange().get()) {
//                int nline = tr.getRange().get().begin.line;
//                if (lineNumSet.contains(nline)) {
//                    if (prev == -1) prev = nline;
//                    if (prev != nline) {
//                    matchingStmt.put(lineRangeMap.get(prev), str.trim());
//                        str = "";
//                        prev = nline;
//                    }
                if (varList.containsKey(tr.getText())) {
                    str += varList.get(tr.getText());
                } else {
                    str += tr.getText();
                }
            }
            matchingStmt.put(entry.getKey(), str.trim());
        }


        return matchingStmt;
    }

    public Map<Range, String> normalizeStmtsWithArguments(
            CompilationUnit cu, Map<Range, Node> lineMap) {
        Map<String, String> paramMap = new HashMap<>();
        Map<String, String> varList = new HashMap<>();
        Deque<Integer> st = new ArrayDeque<Integer>();
        int count = 0;
        int paramIndex = 0;
        VoidVisitor<Map<String, String>> visitor = new VariableDeclarationVisitor();
        cu.accept(visitor, varList);
        log.info(varList.toString());
        Set<Integer> lineNumSet = new HashSet<>();
        Map<Integer, Range> lineRangeMap = new HashMap<>();
        for (Range r : lineMap.keySet()) {
            lineNumSet.add(r.begin.line);
            lineRangeMap.put(r.begin.line, r);
        }
        for (Map.Entry<String, String> entry : varList.entrySet()) {
            paramMap.put("\\" + entry.getValue(), entry.getKey());
        }
        int prev = -1;
        Map<Range, String> matchingStmt = new HashMap<>();

        String str = "";
        for (JavaToken tr : cu.getTokenRange().get()) {
            int nline = tr.getRange().get().begin.line;
            if (lineNumSet.contains(nline)) {
                if (prev == -1) prev = nline;
                if (prev != nline) {
                    matchingStmt.put(lineRangeMap.get(prev), str.trim());
                    str = "";
                    prev = nline;
                }
                if (tr.getText().contains(",")) {
                    str += "#" + paramIndex;
                    continue;
                }
                if (tr.getText().contains("(")) {
                    str += tr.getText();
                    count += 1;
                    if (count > 1) {
                        st.add(paramIndex);
                        paramIndex = 0;
                    }
                }
                if (tr.getText().contains(")")) {
                    count -= 1;
                    str += "#" + paramIndex;
                    if (st.size() > 0) paramIndex = st.pop();
                }
                if (count == 0) {
                    if (varList.containsKey(tr.getText())) {
                        str += varList.get(tr.getText());
                    } else if (tr.getText().startsWith("\"")) {
                        str += "$sl";
                    } else str += tr.getText();
                }
            }
        }

        matchingStmt.put(lineRangeMap.get(prev), str.trim());
        return matchingStmt;
    }

    public HashMap<Range, Range> getMatchStmtsTypeStmt(
            Map<Range, Statement> left,
            Map<Range, Statement> right,
            CompilationUnit leftCU,
            CompilationUnit rightCU,
            float threshold) {
        HashMap<Range, Range> lineMap = new HashMap<>();
        StringSimilarity ss = new StringSimilarity();
        Map<Range, String> leftStmts = normalizeStmtsTypeStmt(leftCU, left, true);
        Map<Range, String> rightStmts = normalizeStmtsTypeStmt(rightCU, right, true);
        for (Range i : leftStmts.keySet()) {
            float maxScore = 0;
            Range candidate = null;
            for (Range j : rightStmts.keySet()) {
                String leftStmt = leftStmts.get(i);
                String rightStmt = rightStmts.get(j);
                float score = (float) ss.calculateSimilarity(leftStmt, rightStmt);
                if (score >= threshold && maxScore < score) {
                    maxScore = score;
                    candidate = j;
                }
            }
            if (candidate != null) {
                lineMap.put(i, candidate);
            }
        }
        return lineMap;
    }

    public Map<Range, String> normalizeStmtsTypeStmt(
            CompilationUnit cu, Map<Range, Statement> lineMap, boolean isArgumentNormalized) {
        if (isArgumentNormalized) {
            return normalizeStmtsWithArgumentsTypeStmt(cu, lineMap);
        }
        Map<String, String> paramMap = new HashMap<>();
        Map<String, String> varList = new HashMap<>();
        VoidVisitor<Map<String, String>> visitor = new VariableDeclarationVisitor();
        cu.accept(visitor, varList);
        log.info(varList.toString());
        Set<Range> lineNumSet = new HashSet<>();
        Map<Integer, Range> lineRangeMap = new HashMap<>();
        for (Range r : lineMap.keySet()) {
            lineNumSet.add(r);
            lineRangeMap.put(r.begin.line, r);
        }
        for (Map.Entry<String, String> entry : varList.entrySet()) {
            paramMap.put("\\" + entry.getValue(), entry.getKey());
        }
        Map<Range, String> matchingStmt = new HashMap<>();

        for (Statement s : lineMap.values()) {
            TokenRange token = s.getTokenRange().get();
            Iterator<JavaToken> it = token.iterator();
            String str = "";
            while (it.hasNext()) {
                JavaToken tr = it.next();
                if (varList.containsKey(tr.getText())) {
                    str += varList.get(tr.getText());
                } else str += tr.getText();
            }
            matchingStmt.put(s.getRange().get(), str.trim());
        }
        return matchingStmt;
    }

    public Map<Range, String> normalizeStmtsWithArgumentsTypeStmt(
            CompilationUnit cu, Map<Range, Statement> lineMap) {
        Map<String, String> paramMap = new HashMap<>();
        Map<String, String> varList = new HashMap<>();

        VoidVisitor<Map<String, String>> visitor = new VariableDeclarationVisitor();
        cu.accept(visitor, varList);
        log.info(varList.toString());
        Set<Range> lineNumSet = new HashSet<>();
        Map<Integer, Range> lineRangeMap = new HashMap<>();
        for (Range r : lineMap.keySet()) {
            lineNumSet.add(r);
            lineRangeMap.put(r.begin.line, r);
        }
        for (Map.Entry<String, String> entry : varList.entrySet()) {
            paramMap.put("\\" + entry.getValue(), entry.getKey());
        }
        Range prev = null;
        Map<Range, String> matchingStmt = new HashMap<>();

        for (Statement s : lineMap.values()) {
            TokenRange token = s.getTokenRange().get();
            Iterator<JavaToken> it = token.iterator();
            String str = "";
            Deque<Integer> st = new ArrayDeque<Integer>();
            int count = 0;
            int paramIndex = 0;
            while (it.hasNext()) {
                JavaToken tr = it.next();
                if (tr.getText().contains(",")) {
                    str += "#" + paramIndex;
                    continue;
                }
                if (tr.getText().contains("(")) {
                    str += tr.getText();
                    count += 1;
                    if (count > 1) {
                        st.add(paramIndex);
                        paramIndex = 0;
                    }
                }
                if (tr.getText().contains(")")) {
                    count -= 1;
                    str += "#" + paramIndex;
                    if (st.size() > 0) paramIndex = st.pop();
                }
                if (count == 0) {
                    if (varList.containsKey(tr.getText())) {
                        str += varList.get(tr.getText());
                    } else if (tr.getText().startsWith("\"")) {
                        str += "$sl";
                    } else str += tr.getText();
                }
            }
            matchingStmt.put(s.getRange().get(), str.trim());
        }

        return matchingStmt;
    }

    public Map<Range, Statement> StmtsListtoMap(NodeList<Statement> stmtList) {
        Map<Range, Statement> stmtMap = new HashMap<>();
        for (Statement st : stmtList) {
            stmtMap.put(st.getRange().get(), st);
        }
        return stmtMap;
    }

    // 判断token在哪一个statement里面
    public Range isRangeinSet(Set<Range> rangeSet, Range r) {
        for (Range range : rangeSet) {
            if (range.contains(r)) return range;
        }
        return null;
    }

    // a super simple treediff comparison, only for handling the SecretkeySpec
    public Set<Node> treeDiff(CompilationUnit leftCU, CompilationUnit rightCU) {
        List<Node> lchildNodes = leftCU.getChildNodes();
        List<Node> rchildNodes = rightCU.getChildNodes();
        List<Node> leftClassNodes = lchildNodes.get(lchildNodes.size() - 1).getChildNodes();
        List<Node> rightClassNodes = rchildNodes.get(rchildNodes.size() - 1).getChildNodes();
        Set<Node> missingNodes = new HashSet<>();
        for (Node rnode: rightClassNodes) {
            if (!(rnode instanceof MethodDeclaration) && !(rnode instanceof FieldDeclaration)) continue;
            boolean match = false;
            for (Node lnode: leftClassNodes) {
                if (!(lnode instanceof MethodDeclaration) && !(lnode instanceof FieldDeclaration)) continue;
                if (lnode instanceof MethodDeclaration && rnode instanceof MethodDeclaration) {
                    MethodDeclaration lMethod = (MethodDeclaration) lnode, rMethod = (MethodDeclaration) rnode;
                    if (lMethod.getNameAsString().equals(rMethod.getNameAsString())) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                missingNodes.add(rnode);
            }
        }
        return missingNodes;
    }

    public Map<Range, Node> toMapRangeNode(Map<Range, Expression> stmts) {
        Map<Range, Node> stmts_ = new HashMap<>();
        for (Map.Entry<Range, Expression> entry: stmts.entrySet()) {
            stmts_.put(entry.getKey(), (Node) entry.getValue());
        }
        return stmts_;
    }

}
