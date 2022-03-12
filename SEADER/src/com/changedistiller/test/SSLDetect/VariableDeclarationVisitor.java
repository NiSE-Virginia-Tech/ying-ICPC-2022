package com.changedistiller.test.SSLDetect;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VariableDeclarationVisitor extends VoidVisitorAdapter<Map<String, String>> {
    int trace = 0;

    public Set<String> variables = new HashSet<>();
    public Map<String, Integer> varPos = new HashMap<>();
    public Boolean keyStmt = false;

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<String, String> arg) {
        super.visit(n, arg);
        int pos = 0;
        for (FieldDeclaration fd: n.getFields()) {
            for (VariableDeclarator e: fd.getVariables()) {
                addVariables(e, arg);
                varPos.put(e.getNameAsString(), pos);
                pos++;
            }
        }
    }

    @Override
    public void visit(MethodCallExpr n, Map<String, String> arg) {
        super.visit(n, arg);
        int pos = 0;
        for (Expression x : n.getArguments()) {
            if (x instanceof NameExpr) {
                variables.add(((NameExpr) x).getNameAsString());
                varPos.put(x.toString(), pos);
                pos++;
            }
        }
    }

    @Override
    public void visit(VariableDeclarationExpr n, Map<String, String> arg) {
        super.visit(n, arg);
        int pos = 0;
        for (VariableDeclarator e: n.getVariables()) {
            addVariables(e, arg);
            varPos.put(e.getNameAsString(), pos);
            pos++;
        }
    }

    @Override
    public void visit(ObjectCreationExpr n, Map<String, String> arg) {
        super.visit(n, arg);
        int pos = 0;
        if (n.getType().getName().toString().equals("PBEParameterSpec")) {
            keyStmt = true;
        }
        for (Expression x : n.getArguments()) {
            if (x instanceof NameExpr) {
                variables.add(((NameExpr) x).getNameAsString());
                varPos.put(x.toString(), pos);
                pos++;
            }
        }
    }

    @Override
    public void visit(CatchClause n, Map<String, String> arg) {
        super.visit(n, arg);
        int pos = 0;
        SimpleName p = n.getParameter().getName();
        arg.put(p.toString(), "$v" + "_" + trace++ + "$");
        variables.add(p.toString());
        varPos.put(p.toString(), pos);
    }


    private void addVariables(VariableDeclarator v, Map<String, String> arg) {
        arg.put(v.getNameAsString(), "$v" + "_" + trace++ + "$");
        variables.add(v.getNameAsString());
    }

    public int getPos(String v) {
        int pos = varPos.get(v);
        return pos;
    }

    public Set<String> getVariables() {
        return variables;
    }

}
