//package com.changedistiller.test.SSLDetect;
//
//
//import com.Constant;
//import com.ibm.wala.examples.slice.BackwardSlicer2;
//import com.ibm.wala.ipa.cha.ClassHierarchyException;
//import com.ibm.wala.util.CancelException;
//import org.junit.Test;
//import java.io.IOException;
//import java.lang.reflect.Array;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class Pipline {
//
//    private Map<Integer, List<Object>> targetPara = new HashMap<>();
//    private int position = 0 ;
//
//    @Test
//    public void run() throws ClassHierarchyException, CancelException, IOException {
//        String path = Constant.FILEPATH;
//        String caller = "main";
//
//
//        //add a while loop to retrive the value from the database, position information;
//        String mainClass ="Lorg/cryptoapi/bench/predictablecryptographickey/PredictableCryptographicKeyBBCase1";
//        String callee = "<init>";
//        String functionType = "SecretKeySpec";
//        position = 1;
//        BackwardSlicer2 backwardSlicer = new BackwardSlicer2();
//        backwardSlicer.run(path, mainClass, callee, caller, functionType);
//        targetPara = backwardSlicer.getParamValue();
//        //add if compare with the  targetmap
//
//        //check
//
//
//    }
//
//}
