package com.changedistiller.test.SSLDetect;

import com.Constant;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import org.junit.Test;

import java.io.IOException;

public class  MultiLineDetectionTest {

    @Test
    public void start() throws ClassHierarchyException, CancelException, IOException {
        String classPath = Constant.FILEPATH;
        String callee = "<init>";
        String functionType = "SecretKeySpec";
        String projectSource = "E:\\Code\\Java\\cryptoapi-bench\\src\\main\\java\\";
        MultiLineDetection detection = new MultiLineDetection(classPath, projectSource, null);
        detection.start(callee, functionType,0, 2);
    }
}