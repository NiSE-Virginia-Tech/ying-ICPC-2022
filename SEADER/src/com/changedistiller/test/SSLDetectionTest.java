package com.changedistiller.test;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

import java.io.File;

public class SSLDetectionTest {

    @Test
    public void detect() throws Exception {
        String lfilename = "checkclient.java";
        File left = new File("C:\\Users\\Ying\\Documents\\JAVA_CODE\\cipher_test\\src\\cipher_test\\insecure\\SSL\\" + lfilename);
        String[] lsourcepath = {"C:\\Users\\Ying\\Documents\\JAVA_CODE\\cipher_test\\src\\cipher_test"};
        FiletoAST leftast = new FiletoAST(lsourcepath, left, lfilename);
        CompilationUnit lcu = leftast.getComplicationUnit();
        com.changedistiller.test.SSLDetection detection = new com.changedistiller.test.SSLDetection();
//        detection.detect(lcu);
    }
}