package diff;

import edu.vt.cs.append.diff_match_patch;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.*;

public class DiffCheckerTest {

    @Test
    public void run() throws IOException {
        DiffChecker dc = new DiffChecker();
        dc.run();
    }
}