package Runner;

import com.changedistiller.test.SSLDetect.MultiLineDetection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class RandomMain {

    public static void main(String []args) throws IOException, ParseException {
        PrintStream stream = new PrintStream(new FileOutputStream("test-20211013-1.txt", true));
        System.setOut(stream);
        System.setErr(stream);
        String jarfile = args[0];
        String filePath = args[1];
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader("src/multiline_caller.json");
        Object obj = jsonParser.parse(reader);
        JSONArray checkingCase = (JSONArray) obj;
        System.out.println(jarfile + " " + filePath);
        for (Object o: checkingCase) {
            try{
                MultilineCodeCase codeCase = new MultilineCodeCase((JSONObject)o);
                MultiLineDetection detection = new MultiLineDetection(jarfile, filePath, codeCase.correctSet);
                detection.start(codeCase.callee, codeCase.methodType, codeCase.checkParameter, codeCase.args);
            }
            catch (Throwable e) {
                System.out.println(jarfile);
                System.out.println(filePath);
                e.printStackTrace();
                continue;
            }
        }
    }
}
