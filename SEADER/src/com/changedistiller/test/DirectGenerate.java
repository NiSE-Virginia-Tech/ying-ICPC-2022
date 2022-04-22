package com.changedistiller.test;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DirectGenerate {

    private CompilationUnit cu;

    public DirectGenerate(String file_path) throws Exception {
        Set<String> keywords = new HashSet<>(Arrays.asList("StringLiterals", "IntegerLiterals"));
        TypeSolver typeSolver = new ReflectionTypeSolver();
        TypeSolver javasolver = new JavaParserTypeSolver("src/template");
        typeSolver.setParent(typeSolver);
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(typeSolver);
        combinedTypeSolver.add(javasolver);
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        file_path = "src/test.java";
        cu = StaticJavaParser.parse(new File(file_path));
        List<ObjectCreationExpr> statementList = new ArrayList<>();
        AtomicReference<String> binding = new AtomicReference<>("");
        cu.findAll(ObjectCreationExpr.class).forEach(ae -> {
            ResolvedType resolvedType = ae.calculateResolvedType();
            //binding.set(ae.resolveInvokedMethod().getQualifiedSignature());
            ae.getArguments().stream().forEach(x -> System.out.println(x));
            if (keywords.contains(ae.getType().getName().toString())){
                statementList.add(ae);
            }
        });
        int pos = 0;
        String name = binding.get() + "_" + pos;
        String matchExpression = name;
        int type = 0;
        Set<String> iSet = new HashSet<>();
        Set<String> cSet = new HashSet<>();
        statementList.get(0).getArguments().stream().forEach(x -> iSet.add(x.toString()));
        statementList.get(1).getArguments().stream().forEach(x -> cSet.add(x.toString()));

        System.out.println(iSet);
        System.out.println(cSet);
    }

}
