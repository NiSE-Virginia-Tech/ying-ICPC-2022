package com.ibm.wala.examples.slice;

import com.Constant;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import org.junit.Test;

import java.io.IOException;

public class BackwardSlicerTest {

    @Test
    public void run() throws ClassHierarchyException, CancelException, IOException {
        String path = Constant.FILEPATH;
        String caller = "main";

//        String mainClass ="Lcom/example/lesson16/Lesson16Application";
        String callee = "<init>";
        String functionType = "PBEParameterSpec";

//        String mainClass = "Lorg/cryptoapi/bench/brokencrypto/BrokenCryptoABICase8";
//        String callee = "getInstance";
//        String methodType = "Cipher";
//        String type = "parameter";

//        InterRetrive backwardSlicer = new InterRetrive();
//        backwardSlicer.start(path, callee, methodType, type);
//        BackwardSlicer backwardSlicer = new BackwardSlicer();
//        backwardSlicer.run(path, mainClass, callee, caller, functionType);

        BackwardSlice backwardSlicer = new BackwardSlice();
        backwardSlicer.run(path, callee, functionType);

//        InterRetrive interRetrive = new InterRetrive();
//        interRetrive.start(path, callee, functionType);


    }
}