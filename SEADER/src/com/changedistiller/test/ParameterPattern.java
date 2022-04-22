package com.changedistiller.test;


import org.json.simple.JSONObject;

import java.util.*;

public class ParameterPattern implements CodePattern {
    private int pos = 0;
    private String bindingType = "";
    private Set<String> correctParametersSet = new HashSet<>();
    private Set<String> incorrectParameterSet = new HashSet<>();

    ParameterPattern(String str, int n){
        bindingType = str;
        this.pos = n;
    }

    public Set<String> getIncorrectParameterSet() {
        return incorrectParameterSet;
    }

    public Set<String> getCorrectParametersSet() {
        return correctParametersSet;
    }

    public void AppendtoCSet(List<String> lstr) {
        correctParametersSet.addAll(lstr);
    }

    public void AppendtoISet(List<String> lstr){
        incorrectParameterSet.addAll(lstr);
    }

    public void AppendtoCSet(String str) {
        correctParametersSet.add(str);
    }

    public void AppendtoISet(String str){
        incorrectParameterSet.add(str);
    }

    public String getBingingType() {
        return bindingType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ParameterPattern) {
            ParameterPattern rhs = (ParameterPattern) obj;
            return rhs.getBingingType().equals(this.bindingType);
        }
        return false;
    }

    public int getPos() {
        return this.pos;
    }

    public String toString() {
        return String.format("%s_%d", this.bindingType, this.pos);
    }

    @Override
    public JSONObject marshall() {
        Map<String, String> jsonFields = new HashMap<>();
        jsonFields.put("Type", "parameter");
        jsonFields.put("Check", Integer.toString(this.pos));
        jsonFields.put("Incorrect", this.incorrectParameterSet.toString());
        jsonFields.put("Correct", this.correctParametersSet.toString());
        String[] strList = this.bindingType.split(" ");
        if (strList.length == 1) {
            jsonFields.put("MethodType", this.bindingType);
            jsonFields.put("callee", "<init>");
        }
        else {
            for (int i = 0; i<strList.length; i++) {
                String str = strList[i];
                if (str.startsWith("java")) {
                    String[] sepStrList = str.split("[.]");
                    jsonFields.put("MethodType", sepStrList[sepStrList.length-1]);
                    sepStrList = strList[i+1].split("[(]");
                    jsonFields.put("callee", sepStrList[0]);
                    break;
                }
            }
        }
        return new JSONObject(jsonFields);
    }
}
