package com.changedistiller.test.SSLDetect;

import com.ibm.wala.util.CancelException;
import org.junit.Test;

import java.io.IOException;

public class DetectionTest {

    @Test
    public void run() throws CancelException, IOException {
        String path = "-appJar C:\\Users\\ying\\Documents\\JAVA_CODE\\cryptoapi-bench\\rigorityj-samples.jar ";
        String mainClass ="-mainClass Lorg/cryptoapi/bench/dummyhostnameverifier/HostnameVerifierCase2 ";
        String caller = "-srcCaller main ";
        String callee = "-srcCallee getInstance ";
        String settings = "-dd full -cd full -dir forward";

        Detection multiClass = new Detection(path+mainClass+callee+caller+settings, "KeyPairGenerator");
        //pdfSlice.runInit(path, mainClass,caller,callee,);
        multiClass.run();
    }
}