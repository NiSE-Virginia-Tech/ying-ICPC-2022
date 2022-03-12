package Slice;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.CommandLine;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

public class SlicingTest {

    @Test
    public void run() throws ClassHierarchyException, IOException, CancelException {
        String[] args = "-appJar D:\\work\\Java\\cipherJar\\out\\artifacts\\cipherJar_jar\\cipherJar.jar -mainClass LSampleCipher1 -srcCaller main -srcCallee getInstance -dd full -cd full -dir backward".split(" ");
        Properties p = CommandLine.parse(args);
        Slicing slice = new Slicing();
        slice.run(p.getProperty("appJar"), p.getProperty("mainClass"), p.getProperty("srcCaller"), p.getProperty("srcCallee"),
                goBackward(p), getDataDependenceOptions(p), getControlDependenceOptions(p));
    }

    private static boolean goBackward(Properties p) {
        return !p.getProperty("dir", "backward").equals("forward");
    }

    public static Slicer.DataDependenceOptions getDataDependenceOptions(Properties p) {
        String d = p.getProperty("dd", "full");
        for (Slicer.DataDependenceOptions result : Slicer.DataDependenceOptions.values()) {
            if (d.equals(result.getName())) {
                return result;
            }
        }
        Assertions.UNREACHABLE("unknown data datapendence option: " + d);
        return null;
    }

    public static Slicer.ControlDependenceOptions getControlDependenceOptions(Properties p) {
        String d = p.getProperty("cd", "full");
        for (Slicer.ControlDependenceOptions result : Slicer.ControlDependenceOptions.values()) {
            if (d.equals(result.getName())) {
                return result;
            }
        }
        Assertions.UNREACHABLE("unknown control datapendence option: " + d);
        return null;
    }
}

//