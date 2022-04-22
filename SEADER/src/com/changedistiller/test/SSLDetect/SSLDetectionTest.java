package com.changedistiller.test.SSLDetect;

import com.changedistiller.test.FiletoAST;
import com.changedistiller.test.SSLDetection;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SSLDetectionTest {

    @Test
    public void detect() throws IOException {
        String lfilename = "DummyCertValidationCase1.java";
        File left = new File("/mnt/windows_share/JAVA_CODE/cryptoapi-bench/src/main/java/org/cryptoapi/bench/dummycertvalidation/" + lfilename);
        String[] lsourcepath = {"/mnt/windows_share/JAVA_CODE/cryptoapi-bench/src/main/java/org/cryptoapi/bench/dummycertvalidation/"};
        List<Path> javafiles = new ArrayList<>();
        javafiles.add(Paths.get(left.getAbsolutePath()));
//        Files.find(Paths.get("E:\\Code\\experiment\\test_set\\jclouds.git"), Integer.MAX_VALUE, ((path, basicFileAttributes) -> basicFileAttributes.isRegularFile())).forEach(x->javafiles.add(x));
        for (Path p: javafiles) {
            if (p.getFileName().toString().endsWith("java")) {
                System.out.println(p);
//                String[] lsourcepath = {p.getParent().toString()};
                try {
                    FiletoAST leftast = new FiletoAST(lsourcepath, new File(p.toString()), p.toString());
                    CompilationUnit lcu = leftast.getComplicationUnit();
                    SSLDetection detection = new SSLDetection();
                    detection.detect(lcu, p.toString(), "test-ssl-20211016.txt");
                } catch (Exception e) {
                    continue;
                }
            }
        }

    }
}