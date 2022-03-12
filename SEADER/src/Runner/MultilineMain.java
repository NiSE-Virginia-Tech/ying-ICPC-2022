package Runner;

import com.Constant;
import com.ibm.wala.examples.slice.BackwardSlice;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MultilineMain {

    public static void main(String []args) throws ClassHierarchyException, CancelException, IOException, ParseException {
        boolean isRunApache = false;
        PrintStream stream = new PrintStream(new FileOutputStream("multiline_output.txt", true));
        System.setOut(stream);
        System.setErr(stream);
//        System.out.println(args[0]);
//        Files.write(Paths.get("result.txt"), "".getBytes());
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader("src/multiline_caller.json");
        Object obj = jsonParser.parse(reader);
        JSONArray checkingCase = (JSONArray) obj;
        if (isRunApache) {
//            File folder = new File("C:\\Users\\LinG\\Desktop\\experiment\\java-security-test-jar");
//            File[] files = folder.listFiles();
//
//            for (File f: files) {
//                System.out.println(f.getCanonicalPath());
//                long caseStartTime = System.nanoTime();
//                new Main().runCaseChecking(f.getCanonicalPath(), checkingCase);
//                System.out.println("\nDURATION: " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - caseStartTime) + " SECONDS\n");
//            }
            long caseStartTime = System.nanoTime();
            new MultilineMain().keypairCaseChecking(args[0]);
            System.out.println("\nDURATION: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - caseStartTime) + " SECONDS\n");
        } else {
            new MultilineMain().keypairCaseChecking(Constant.FILEPATH);
        }
    }

    public void runCaseChecking(String filePath, JSONArray checkingCase) throws IOException {
        int count = 0;
        Map<Integer, String> caseName = new HashMap<>();
        Map<Integer, Integer> caseCount = new HashMap<>();
        for (Object o: checkingCase) {
            MultilineCodeCase codeCase = new MultilineCodeCase((JSONObject)o);
            System.out.println("\n=====" + codeCase.type + " " + codeCase.methodType + " " + codeCase.callee + "=====");
            //running apache case
            try {
                BackwardSlice backwardSlicer = new BackwardSlice();
                backwardSlicer.run(filePath, codeCase.callee, codeCase.methodType);
                Map<String, Map<Integer, List<Object>>> varMap = backwardSlicer.getClassVarMap();
                Map<String, HashMap<Integer, List<Integer>>> classLineNums = backwardSlicer.getClassParamsLinesNumsMap();
//            System.err.println(classLineNums);
                caseCount.put(count, codeCase.checking(varMap));
                caseName.put(count, codeCase.methodType + " " + codeCase.callee);
            } catch (Throwable e) {
                e.printStackTrace();
                continue;
            }
            count ++;
        }
        // output the summary
        writetoFile("############################################################\n");
        writetoFile(filePath + "\n");
        for (Map.Entry<Integer, Integer> e: caseCount.entrySet()) {
//            System.out.printf("Rule %s: %d\n", caseName.get(e.getKey()), e.getValue());
            writetoFile(String.format("Rule %s: %d\n", caseName.get(e.getKey()), e.getValue()));
        }
    }

    public void keypairCaseChecking(String filePath) throws IOException {
        int count = 0;
        Map<Integer, String> caseName = new HashMap<>();
        Map<Integer, Integer> caseCount = new HashMap<>();
        String callee = "initialize";
        String methodType = "KeyPairGenerator";
        System.out.println("\n=====" + "KeypairGenerator" + " " + "initialize" + "=====");
        //running apache case
        try {
            BackwardSlice backwardSlicer = new BackwardSlice();
            backwardSlicer.run(filePath, callee, methodType);
            Map<String, Map<Integer, List<Object>>> varMap = backwardSlicer.getClassVarMap();
            Map<String, HashMap<Integer, List<Integer>>> classLineNums = backwardSlicer.getClassParamsLinesNumsMap();
            for (Map.Entry<String, List<Statement>> e : backwardSlicer.classStmtMap.entrySet()) {
                List<Statement> stmts = e.getValue();
                Statement stmt = null;
                for (Statement s : stmts) {
                    String s_str = s.toString();
                    if (s_str.contains("KeyPairGenerator") && s_str.contains("getInstance")) {
                        stmt = s;
                    }
                }
//                backwardSlicer.run(stmt);
                int var_init = (int) getValue(varMap.get(e.getKey()).get(0));
                String var_getInstance = (String) getValue(backwardSlicer.getClassVarMap().get(e.getKey()).get(0));
                if (var_getInstance == "RSA" && var_init < 2048) {
                    writetoFile(String.format("Insecure: %s %d \n", var_getInstance, var_init));
                }
                if (var_getInstance == "ECC" && var_init < 224) {
                    writetoFile(String.format("Insecure: %s %d \n", var_getInstance, var_init));
                }
            }

//            System.err.println(classLineNums);
            count++;
            // output the summary
            writetoFile("############################################################\n");
            writetoFile(filePath + "\n");
//            for (Map.Entry<Integer, Integer> e : caseCount.entrySet()) {
//                //            System.out.printf("Rule %s: %d\n", caseName.get(e.getKey()), e.getValue());
//                writetoFile(String.format("Rule %s: %d\n", caseName.get(e.getKey()), e.getValue()));
//            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }


    public void writetoFile(String str) throws IOException {
        Files.write(Paths.get("multiline_result.txt"), str.getBytes(), StandardOpenOption.APPEND);
    }

    public Object getValue(List<Object> var) {
        return var.get(0);
    }

}
