package com.changedistiller.test;

import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {

        String lfilename = "15.java";
        String rfilename = "15.java";

        File left = new File("/xxx/code/cipher_test/src/cipher_test/insecure/parameter/" + lfilename);
        String[] lsourcepath = {"/xxx/code/cipher_test/src/cipher_test/"};
        File right = new File("/xxx/code/cipher_test/src/cipher_test/secure/parameter/" + rfilename);
        String[] rsourcepath = {"/xxx/code/cipher_test/src/cipher_test/"};
        FiletoAST leftast = new FiletoAST(lsourcepath, left, lfilename);
        FiletoAST rightast = new FiletoAST(rsourcepath, right, rfilename);
        CompilationUnit lcu = leftast.getComplicationUnit();
        CompilationUnit rcu = rightast.getComplicationUnit();
        GeneratePattern gp = new GeneratePattern();
        gp.Compare(left, right, lcu, rcu);
    }
}

