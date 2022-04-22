package com.changedistiller.test;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomVisitor extends ASTVisitor {

    public String bindingName;
    public String matchingExpression;
    public List<MethodDeclaration> methodDeclarList = new ArrayList<>();
    public HashMap<String, Object> typeParameter = new HashMap<>();
    public String parameterName = null;

    public CustomVisitor() {}

    public void VisitTarget(ASTNode cu, List<ASTNode> targetnode, List<ASTNode> nodeArguments) {
        targetnode.add(cu.getParent());

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodInvocation node) {
                bindingName = node.resolveMethodBinding() != null? node.resolveMethodBinding().toString(): "";
                nodeArguments.addAll(node.arguments());
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%s.%s", node.getExpression(), node.getName()));
                for (int i = 0; i < nodeArguments.size(); i++) {
                    if (i == 0) sb.append("(");
                    if (i != 0) sb.append(",");
                    sb.append(GetVariableWildcardName(nodeArguments.get(i)));
                    if (i == nodeArguments.size() - 1) sb.append(")");
                }
                matchingExpression = sb.toString();
                System.out.println("matching Expression: " + matchingExpression);
                targetnode.add(node);
                return super.visit(node);
            }

            @Override
            public boolean visit(TryStatement node){
                System.out.println("This is SSL try statement");
                System.out.println("nodeBody: "+ node.getBody());
                targetnode.add(node);
                node.getBody().accept(this);
                //nodeArguments.add(null);
                return  super.visit(node);
            }

            @Override
            public boolean visit(IfStatement node){
                System.out.println("SSL if statement" + node.getExpression());
                targetnode.add(node);
                node.getThenStatement().accept(this);
                if (node.getElseStatement()!= null) node.getElseStatement().accept(this);
                return false;
            }

            @Override
            public boolean visit(CatchClause node){
                System.out.println("SSL catch statement");
                System.out.println("node body"+ node.getBody());
                return super.visit(node);
            }

            @Override
            public boolean visit(ThrowStatement node){
                System.out.println("throw statement in SSL");
                System.out.println("Throw" + node.getExpression());
                targetnode.add(node);
                //nodeArguments.add(null);
                return super.visit(node);
            }

            @Override
            public boolean visit(ClassInstanceCreation node){
                bindingName = node.getType().toString();
                targetnode.add(node);
                nodeArguments.addAll(node.arguments());
                System.out.println("Arguments: " + node.arguments());
                return super.visit(node);
            }

            //if it is a constant, we should discard it and save a $V_C in the template,
            @Override
            public boolean visit(VariableDeclarationFragment node){
//                System.out.println("test Va: "+ node.getInitializer().resolveConstantExpressionValue());
                StringBuilder sb = new StringBuilder(node.getName().toString());
                parameterName = node.getName().toString();
                sb.append("=");

                if (node.getInitializer().getNodeType() == ASTNode.ARRAY_CREATION){
                    ArrayCreation acNode = (ArrayCreation) node.getInitializer();
                    if(!acNode.dimensions().isEmpty()) {
                        Object a = acNode.dimensions().get(0);
                        if (a instanceof NumberLiteral) {
                            typeParameter.put("size", a);
                        }
                    }
                    sb.append(GetVariableWildcardName(node.getInitializer()));
                }

                bindingName = null;
                matchingExpression = sb.toString();
                System.out.println(matchingExpression);
                return super.visit(node);
            }

            @Override
            public boolean visit(EnhancedForStatement node) {
                System.out.println("EnhanceFor: " + node);
                targetnode.add(node);
                StringBuilder sb = new StringBuilder(node.getExpression().resolveTypeBinding().toString());
                return super.visit(node);
            }

            @Override
            public boolean visit(Block node) {
                System.out.println("aaa");
                return super.visit(node);
            }
        });
    }

    public String GetVariableWildcardName(ASTNode node) {
        String wildCard;
        switch (node.getNodeType()) {
            case ASTNode.STRING_LITERAL:
                wildCard = "$sl";
                break;
            case ASTNode.NUMBER_LITERAL:
                wildCard = "$nl";
                break;
            case ASTNode.ARRAY_CREATION:
                wildCard = node.toString().replaceAll("\\[\\d+\\]", "\\[\\$nl\\]");
                break;
            default:
                wildCard = "";
                break;
        }
        return wildCard;
    }

}
