package com.changedistiller.test.DAO;

import com.changedistiller.test.*;
import javafx.util.Pair;
import redis.clients.jedis.Jedis;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DBHandler {

    private final Jedis jedis = null;


    public DBHandler() {
//        jedis = new Jedis ("192.168.32.129");
//        jedis.auth("");
    }

    private int typeConverter(CodePattern pattern) {
        if (pattern instanceof ParameterPattern) return 0;
        else if (pattern instanceof NamePattern) return 1;
        else if (pattern instanceof CompositePattern) return 2;
        else if (pattern instanceof NumberPattern) return 3;
        return -1;
    }

    public void WritetoDB(String name, CodePattern pattern, String matchExpression) {
        switch (typeConverter(pattern)) {
            case 0:
                this.WriteToDB((ParameterPattern) pattern, name, matchExpression);
                break;
            case 1:
                this.WriteToDB((NamePattern) pattern, name, matchExpression);
                break;
            case 2:
                this.WriteToDB((CompositePattern) pattern, name, matchExpression);
                break;
            case 3:
                this.WritetoDB((NumberPattern) pattern, name, matchExpression);
                break;
            default:
                break;
        }
     }

    private void WriteToDB(ParameterPattern pattern, String name, String matchExpression) {
        Map<String, String> patternField = new HashMap<>();
        patternField.put("StringMatchingExpression", matchExpression);
        patternField.put("Type", Integer.toString(typeConverter(pattern)));
        for (String str: pattern.getIncorrectParameterSet()) jedis.sadd(name + "IArg", str);
        for (String str: pattern.getCorrectParametersSet()) jedis.sadd(name + "CArg", str);
        jedis.hmset(name, patternField);
        jedis.sadd(patternField.get("Type"), name);
    }

    private void WriteToDB(NamePattern pattern, String name, String matchExpression) {
        Map<String, String> patternField = new HashMap<>();
        patternField.put("StringMatchingExpression", matchExpression);
        patternField.put("Type", Integer.toString(typeConverter(pattern)));
        jedis.hmset(name, patternField);
        for (String str: pattern.getIncorrectNameSet()) jedis.sadd(name + "IName", str);
        for (String str: pattern.getIncorrectClassSet()) jedis.sadd(name + "IClass", str);
        for (String str: pattern.getCorrectNameSet()) jedis.sadd(name + "CName", str);
        for (String str: pattern.getCorrectClassSet()) jedis.sadd(name + "CClass", str);
        jedis.sadd(patternField.get("Type"), name);
    }

    private void WriteToDB(CompositePattern pattern, String name, String matchExpression) {
        Map<String, String> patternField = new HashMap<>();
        List<String> lcuStmt = null;
        List<String> rcuStmt = null;
        for (Pair<String, String> a: pattern.getRcuTemplateStatements()){
            rcuStmt.add(a.getValue());
        }
        for (Pair<String, String> a: pattern.getLcuTemplateStatements()){
            lcuStmt.add(a.getValue());
        }
        patternField.put("InsecureTemplate", GenerateString(lcuStmt));
        patternField.put("SecureTemplate", GenerateString(rcuStmt));
        patternField.put("StringMatchingExpression", matchExpression);
        patternField.put("Type", Integer.toString(typeConverter(pattern)));
        jedis.hmset(name, patternField);
        jedis.sadd(patternField.get("Type"), name);
    }

    private void WritetoDB(NumberPattern pattern, String name, String matchExpression) {
        Map<String, String> patternField = new HashMap<>();
        for (String str: pattern.getInsecureParameterSet()) jedis.sadd(name + "ISet", str);
        for (String str: pattern.getSecureParametersSet()) jedis.sadd(name + "CSet", str);
        patternField.put("whichArg", Integer.toString(pattern.getPos()));
        patternField.put("StringMatchingExpression", matchExpression);
        patternField.put("Type", Integer.toString(typeConverter(pattern)));
        jedis.hmset(name, patternField);
        jedis.sadd(patternField.get("Type"), name);
    }

    private void WritetoDB_parameter(Set<String> iSet, Set<String> cSet, String matchExpression, String name, int type) {
        Map<String, String> patternField = new HashMap<>();
        patternField.put("StringMatchingExpression", matchExpression);
        patternField.put("Type", Integer.toString(type));
        for (String str: iSet) jedis.sadd(name + "IArg", str);
        for (String str: cSet) jedis.sadd(name + "CArg", str);
        jedis.hmset(name, patternField);
        jedis.sadd(patternField.get("Type"), name);
    }

    public void writetoJson(CodePattern codePattern) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://192.168.32.129/InsecureFix", "root", "123456")) {
            if (conn != null) {
                String query = "insert into InsecureFix (Name, JsonString) " + "values (?,?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, Integer.toString(codePattern.hashCode()));
                stmt.setString(2, codePattern.marshall().toJSONString());
                stmt.execute();
            }
            else {
                System.out.println("fail");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (FileWriter file = new FileWriter("pattern.json")) {
            file.write(codePattern.marshall().toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Transform the set to String -> arg1, arg2, arg3
     * @param set
     * @return String
     */
    public String GenerateString(Set<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String str: set) {
            sb.append(str);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        System.out.println(sb);
        return sb.toString();
    }

    public String GenerateString(List<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String str: set) {
            sb.append(str);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        System.out.println(sb);
        return sb.toString();
    }
}
