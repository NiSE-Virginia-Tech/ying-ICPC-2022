package com.changedistiller.test;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;

public class FiletoAST {

    private String[] classpath = {"/usr/lib/jvm/jdk1.8.0_301/jre/lib/rt.jar"};
    private String[] encodings = {"utf-8"};
    private final CompilationUnit cu;

    public FiletoAST(String[] sourcepath, File file, String filename) throws Exception{
        String sourcefile = FileUtils.readFileToString(file);
        ASTParser parser = ASTParser.newParser(AST.JLS11);
        parser.setSource(sourcefile.toCharArray());
        parser.setEnvironment(classpath,sourcepath,encodings,true);
        parser.setBindingsRecovery(true);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setCompilerOptions(JavaCore.getOptions());
        parser.setUnitName(filename);
        cu = (CompilationUnit) parser.createAST(null);

    }

    public CompilationUnit getComplicationUnit()
    {
        return cu;
    }


}
