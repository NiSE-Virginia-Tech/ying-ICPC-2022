package com.changedistiller.test;

import org.json.simple.JSONObject;

import java.io.Serializable;

public interface CodePattern extends Serializable {
    public JSONObject marshall();
}
