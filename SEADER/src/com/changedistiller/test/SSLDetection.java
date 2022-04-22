package com.changedistiller.test;

import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SSLDetection {

    Map<String, String> paramMap = new HashMap<String, String>();
    String fixPatch = null;
    public void detect(CompilationUnit cu, String filename, String outputFile) throws FileNotFoundException {
        PrintStream stream = new PrintStream(new FileOutputStream(outputFile, true));
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (node.getName().toString().contains("checkClientTrusted") ||
                node.getName().toString().contains("checkServerTrusted")){
                    if (check_if_x509_method(node)) {
                        if (node.getBody().statements().size() == 0 || node.thrownExceptionTypes().size() == 0) {
                            //get parameter and save to a map
                            stream.println(filename + ": " + node.getName().toString() + " insecure");
                            for(int i = 0; i<node.parameters().size(); i++) {
                                paramMap.put("$v_" + i + "$" , node.parameters().get(i).toString());
                            }
                            String name =  node.getName().toString();
                            fixPatch = readTemp(name);
                        }
                    }
                }
                if (node.getName().toString().contains("verify")) {
                    try {
                        if (node.getBody().statements().size() == 1 || node.thrownExceptionTypes().size() == 0) {
                            stream.println(filename + ": " + node.getName().toString() + " insecure");
                            for(int i = 0; i< node.parameters().size(); i++) {
                                paramMap.put("$v_" + i + "$" , node.parameters().get(i).toString());
                            }
                            String name =  node.getName().toString();
                            fixPatch = readTemp(name);
                        }
                    } catch (Exception e) {
                        // the node did not have body, it should be within an interface, hence continue and do nothing

                    }

                }

                return super.visit(node);
            }
        });

    }

    private String readTemp(String name) {
        String fix = null;
        try {
            Scanner scanner;
            if(name.contains("checkClientTrusted")) {
                scanner = new Scanner(new File("src/SSLtemplate/checkclienttrust.template"));
            } else if (name.contains("checkServerTrusted")) {
                scanner = new Scanner(new File("src/SSLtemplate/checkservertrust.template"));
            } else {
                scanner = new Scanner(new File("src/SSLtemplate/hostnameverify.template"));
            }

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                line = replaceVar(line);
                fix += line;
                System.out.println(line);
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return fix;
    }

    private String replaceVar(String stmtwithvar) {
        String s = stmtwithvar;
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String searchStr = entry.getKey(); //key?
//            searchStr = searchStr.substring(0, searchStr.length() - 1) + "\\$";
            s = s.replace(searchStr, entry.getValue().split(" ")[1]);
        }
        return s;
    }

    private boolean check_if_x509_method(MethodDeclaration node) {
        boolean override = false, classCheck = false;
        // check override
        for (Object n: node.modifiers() ) {
            if (n.toString().contains("@Override")) {
                override = true;
            }
        }
        List<ASTNode> superinterfaceTypes = ((TypeDeclaration) node.getParent()).superInterfaceTypes();
        classCheck = superinterfaceTypes.size() == 1 && superinterfaceTypes.get(0).toString().contains("X509TrustManager");
        return override && classCheck;
    }

}
