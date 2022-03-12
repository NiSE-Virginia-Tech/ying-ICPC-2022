package Runner;

import com.changedistiller.test.FiletoAST;
import com.changedistiller.test.SSLDetection;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SSLMain {

    public static void main(String[] args) throws IOException {
        String outputFile = "SSLOutput-20211017.txt";
        PrintStream stream = new PrintStream(new FileOutputStream(outputFile, true));
        System.setOut(stream);
        System.setErr(stream);
        String jarfile = args[0];
        List<Path> javafiles = new ArrayList<>();
        Files.find(
                Paths.get(args[0]),
                Integer.MAX_VALUE,
                ((path, basicFileAttributes) -> basicFileAttributes.isRegularFile()))
                .forEach(x -> javafiles.add(x));
        for (Path p : javafiles) {
            if (p.getFileName().toString().endsWith("java")) {
                String[] lsourcepath = {p.getParent().toString()};
                try {
//                    com.github.javaparser.ast.CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
                    String pathStr = p.toString();
                    FiletoAST leftast =
                            new FiletoAST(lsourcepath, new File(pathStr), pathStr);
                    CompilationUnit lcu = leftast.getComplicationUnit();
                    SSLDetection detection = new SSLDetection();
                    detection.detect(lcu, pathStr, outputFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }
}
