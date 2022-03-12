package Slice;

import com.ibm.wala.examples.slice.PDFSlice;
import com.ibm.wala.util.CancelException;

import java.io.IOException;

public class Slice1 {
    public void run() throws CancelException, IOException {
        String str = "-appJar C:\\Users\\ling\\Documents\\JAVA_CODE\\cipherJar\\out\\artifacts\\cipherJar_jar\\cipherJar.jar -mainClass LSampleCipher1 -srcCaller main -srcCallee getInstance -dd none -cd none -dir backward";
        new PDFSlice(str, "Algo").run();
    }

}
