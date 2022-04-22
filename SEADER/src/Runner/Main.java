package Runner;

import com.Constant;
import com.ibm.wala.examples.slice.InterRetrive;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static void main(String []args) throws ClassHierarchyException, CancelException, IOException, ParseException {
        boolean isRunApache = true;
        PrintStream stream = new PrintStream(new FileOutputStream("apache-dataset-20211013.txt" , true));
        System.setOut(stream);
        System.setErr(stream);
//        Files.write(Paths.get("result.txt"), "".getBytes());
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader("src/caller.json");
        System.out.println("filename: " + args[0]);
        Object obj = jsonParser.parse(reader);
        JSONArray checkingCase = (JSONArray) obj;
        if (isRunApache) {
//            File folder = new File("C:\\Users\\Ying\\Desktop\\experiment\\java-security-test-jar");
//            File[] files = folder.listFiles();
//            ExecutorService executorService = Executors.newFixedThreadPool(100);
//            for (File f: files) {
//                System.out.println(f.getCanonicalPath());
//                long caseStartTime = System.nanoTime();
//                FutureTask<Void> future = new FutureTask<>(new Callable<Void>() {
//                    @Override
//                    public Void call() throws Exception {
//                        new Main().runCaseChecking(f.getCanonicalPath(), checkingCase);
//                        return null;
//                    }
//                });
//                executorService.execute(future);
//                try {
//                    future.get(120, TimeUnit.SECONDS);
//                } catch (Exception e) {
//                    System.out.println("Cancel the task: " + future.cancel(true));
//                    e.printStackTrace();
//                    System.out.println("TIMEOUT: " + f.getCanonicalPath() + "\n");
//                }
//                break;
            long caseStartTime = System.nanoTime();
            new Main().runCaseChecking(args[0], checkingCase);
            System.out.println("\nDURATION: " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - caseStartTime) + " SECONDS\n");
        } else {
            new Main().runCaseChecking(Constant.FILEPATH, checkingCase);
        }
    }

    public void runCaseChecking(String filePath, JSONArray checkingCase) throws IOException {
        int count = 0;
        Map<Integer, String> caseName = new HashMap<>();
        Map<Integer, Integer> caseCount = new HashMap<>();
        for (Object o: checkingCase) {
            CodeCase codeCase = new CodeCase((JSONObject)o);
            System.out.println("\n=====" + codeCase.type + " " + codeCase.methodType + " " + codeCase.callee + "=====");
            //running apache case
            try {
                InterRetrive backwardSlicer = new InterRetrive();
                backwardSlicer.start(filePath, codeCase.callee, codeCase.methodType, codeCase.type, codeCase.argNums);
                Map<String, Map<Integer, List<Object>>> varMap = backwardSlicer.getClassVarMap();
//                Map<String, HashMap<Integer, List<Integer>>> classLineNums = backwardSlicer.getClassParamsLinesNumsMap();
//            System.err.println(classLineNums);
                caseCount.put(count, codeCase.checking(varMap));
                caseName.put(count, codeCase.methodType + " " + codeCase.callee);
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
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

    public void writetoFile(String str) throws IOException {
        Files.write(Paths.get("result_20210611.txt"), str.getBytes(), StandardOpenOption.APPEND);
    }
}
