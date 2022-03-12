package com.changedistiller.test;

import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NumberPattern implements CodePattern{
    private String name;
    private int pos = 0;
    private int minNum = Integer.MAX_VALUE;
    private String bindingType;
    private Set<String> secureParametersSet = new HashSet<>();
    private Set<String> insecureParameterSet = new HashSet<>();

    NumberPattern(String str, int n){
        bindingType = str;
        pos = n;
    }

    public int getMinNum() {
        return minNum;
    }

    public void setMinNum(int minNum) {
        this.minNum = minNum;
    }

    public void AppendtoCSet(String lstr) {
        secureParametersSet.add(lstr);
        int num = Integer.valueOf(lstr);
        minNum = Math.min(num, minNum);
    }

    public void AppendtoISet(String lstr){
        insecureParameterSet.add(lstr);
    }

    public String getBingingType() {
        return bindingType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NumberPattern) {
            NumberPattern rhs = (NumberPattern) obj;
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

    public Set<String> getSecureParametersSet() {
        return secureParametersSet;
    }

    public Set<String> getInsecureParameterSet() {
        return insecureParameterSet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public JSONObject marshall() {
        Map<String, String> jsonFields = new HashMap<>();
        jsonFields.put("Type", "number");
        jsonFields.put("Check", Integer.toString(this.pos));
        jsonFields.put("MinNum", Integer.toString(this.minNum));

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
