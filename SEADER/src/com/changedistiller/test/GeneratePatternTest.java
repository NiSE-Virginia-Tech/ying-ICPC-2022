package com.changedistiller.test;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//import static org.junit.jupiter.api.Assertions.*;
//
public class GeneratePatternTest extends TestCase {

    public void testdivideArgument() {
        GeneratePattern gp = new GeneratePattern();
        Set<Integer> integerSet = new HashSet<>();
        List<String> arrStr = gp.divideArgument("AES/CBC/NoPadding", integerSet);
        for (String str: arrStr) {
            System.out.println(str);
        }
    }
}