package com.changedistiller.test.DAO;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

public class DBHandlerTest extends TestCase {

    public void testgenerateString() {
        DBHandler db = new DBHandler();
        Set<String> set = new HashSet<>();
        set.add("123");
        set.add("234");
        System.out.println(db.GenerateString(set));
    }
}