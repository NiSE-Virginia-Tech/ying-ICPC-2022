package Slice;

import com.ibm.wala.util.CancelException;
import org.junit.Test;

import java.io.IOException;

public class Slice1Test {

    @Test
    public void run() throws CancelException, IOException {
        Slice1 s = new Slice1();
        s.run();
    }
}