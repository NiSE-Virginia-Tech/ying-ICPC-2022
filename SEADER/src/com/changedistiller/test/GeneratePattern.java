package com.changedistiller.test;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.entities.*;
import com.changedistiller.test.DAO.DBHandler;
import edu.vt.cs.append.FineChangesInMethod;
import javafx.util.Pair;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;

import java.io.*;
import java.util.*;

public class GeneratePattern {

    private Set<String> methodtype = new HashSet<>();
    private Map<String, CodePattern> patternMap = new HashMap<>();
    private File left;
    private File right;
    private CompilationUnit lcu;
    private CompilationUnit rcu;

    GeneratePattern(){}

    GeneratePattern(File left, File right, CompilationUnit lcu, CompilationUnit rcu){
        this.left = left;
        this.right = right;
        this.lcu = lcu;
        this.rcu= rcu;
    }

    private SourceRange reconstruct(List<SourceCodeChange> changeList) {
        System.out.println("---------reconstruct----------");
        Set<SourceCodeEntity> changes = new HashSet<>();
        for (SourceCodeChange scc: changeList) {
            if (changes.contains(scc.getChangedEntity())) {
                System.out.println("YES");
            }
            else{
                changes.add(scc.getChangedEntity());
            }
        }

        int left = -1, right = -1;
        for (SourceCodeEntity sce: changes) {
            if (left == -1 && right == -1) {
                left = sce.getSourceRange().getStart();
                right = sce.getSourceRange().getEnd();
            }
            else {
                left = Math.min(left, sce.getStartPosition());
                right = Math.max(right, sce.getEndPosition());
            }
        }
        SourceRange sr = new SourceRange(left, right);
        System.out.println("Start: " + left + " END: " + right);
        System.out.println("-----------------------------");
        return sr;
    }


    public void Compare(File left, File right, CompilationUnit lcu, CompilationUnit rcu){
        FileDistiller distiller = ChangeDistiller.createFileDistiller();
        FileDistiller.setIgnoreComments();
        try {

            distiller.extractClassifiedSourceCodeChanges(left, right);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("warning " + e.getMessage());
        }
        //get the statement of changedistiller and oldfrang, newfrang
        List<SourceCodeChange> changes = distiller.getSourceCodeChanges();//the size of changes=1

        List<SourceRange> srlist = new ArrayList<>(); //old entity
        List<SourceRange> newsrlist  = new ArrayList<>(); //new entity
        List<SourceCodeChange> changelist = new ArrayList<>();
        List<SourceCodeChange> extractChanges = new ArrayList<>();

        if (changes != null) {
            for (SourceCodeChange change : changes) {
                System.out.println("change operation "+ change);
                FineChangesInMethod fc = (FineChangesInMethod) change;
                //changelist = fc.getChanges();
                for (SourceCodeChange scc : fc.getChanges()) {
                    if(scc instanceof Update) {
                        srlist.add(scc.getChangedEntity().getSourceRange()); //old entity
                        System.out.println(scc.getChangedEntity().getSourceRange()); //old range
                        System.out.println(((Update) scc).getNewEntity().getSourceRange()); //get frange in secure
                        newsrlist.add(((Update) scc).getNewEntity().getSourceRange());
                        extractChanges.add(scc);
                    }

                    if(scc instanceof Insert){
                        System.out.println(scc.getChangedEntity().getSourceRange());
                        //System.out.println(((Insert) scc).getParentEntity().getSourceRange());
                        newsrlist.add(scc.getChangedEntity().getSourceRange());
                        extractChanges.add(scc);
                    }

                    if(scc instanceof Delete) {
                        srlist.add(scc.getChangedEntity().getSourceRange());
                        System.out.println(scc.getChangedEntity().getSourceRange());
//                        System.out.println(((Delete) scc).getNewEntity().getSourceRange());
                    }

                    if(scc instanceof Move) {
                        srlist.add(scc.getChangedEntity().getSourceRange());
                        System.out.println(scc.getChangedEntity().getSourceRange());
                        extractChanges.add(scc);
                    }
                }
            }
        }

       // newsrlist.clear();
        //newsrlist.add(reconstruct(extractChanges));

        //TODO: need save the parent node when generate pattern
        //get the AST node from list and filter by API
        List<ASTNode> lNode = new ArrayList<>();
        List<String> lNodeType = new ArrayList<>();
        List<ASTNode> lNodeArgument = new ArrayList<>();


        List<ASTNode> rNode = new ArrayList<>();
        List<String> rNodeType = new ArrayList();
        List<ASTNode> rNodeArgument = new ArrayList<>();
        String matchingExpression = "";
        CustomVisitor customVisitor = new CustomVisitor();
        for (SourceRange l: srlist) {
            ASTNode ltmpNode = NodeFinder.perform(lcu.getRoot(),l.getStart(),l.getEnd()-l.getStart());
            customVisitor.VisitTarget(ltmpNode, lNode, lNodeArgument);
            lNodeType.add(customVisitor.bindingName);
            if(customVisitor.bindingName == null){
                System.out.println("find the target expression");
            }
            matchingExpression = customVisitor.matchingExpression;
        }
        for (SourceRange r: newsrlist){ // can't get the node
            ASTNode rtmpNode = NodeFinder.perform(rcu.getRoot(), r.getStart(),r.getEnd()-r.getStart());
            customVisitor.VisitTarget(rtmpNode, rNode, rNodeArgument); // not only vist the API but the foreach and try catch statement
            rNodeType.add(customVisitor.bindingName);
        }

        if(!matchingExpression.contains("$nl") && !matchingExpression.contains("$sl")) {
            CodePattern name = new NamePattern(matchingExpression);
            //compare type, the name are different
            for(int i =0; i<lNodeType.size();i++) {
                if(!(lNodeType.get(i).equals(rNodeType.get(i)))){
                    ((NamePattern) name).AppendtoINameSet(lNodeType.toString());
                    ((NamePattern) name).AppendtoIClassSet(lNode.toString());

                    ((NamePattern) name).AppendtoCNameSet(rNodeType.toString());
                    ((NamePattern) name).AppendtoCClassSet(rNode.toString());
                }
            }
        }
        //composite
//        if (lNode.size() > 1 || rNode.size() > 1) {
        if (extractChanges.size() > 1 || !customVisitor.typeParameter.isEmpty()){
            ASTNode lTarget = null, rTarget = null;
            int lstart = Integer.MAX_VALUE, lend = Integer.MIN_VALUE, rstart = Integer.MAX_VALUE, rend = Integer.MIN_VALUE;
            for (ASTNode node: lNode) {
                lstart = Math.min(lstart, node.getStartPosition());
                lend = Math.max(lend, node.getStartPosition() + node.getLength());
            }
            for (ASTNode node: rNode) {
                rstart = Math.min(rstart, node.getStartPosition());
                rend = Math.max(rend, node.getStartPosition() + node.getLength());
            }
            lTarget = NodeFinder.perform(lcu.getRoot(), lstart, lend - lstart);
            rTarget = NodeFinder.perform(rcu.getRoot(), rstart, rend - rstart);
            CompositePattern compositePattern = new CompositePattern(lTarget, rTarget);
            compositePattern.setMatchingExpression(matchingExpression);
            patternMap.put(compositePattern.getName(), compositePattern);

            List<Pair<String,String>> lcuStmt = compositePattern.getLcuTemplateStatements();
            List<Pair<String,String>> rcuStmt = compositePattern.getRcuTemplateStatements();

            System.out.println("left:");
            compositePattern.getLcuTemplateStatements().stream().forEach(x -> System.out.println(x));
            System.out.println("right");
            compositePattern.getRcuTemplateStatements().stream().forEach(x -> System.out.println(x));
            //return

            System.out.println("end the analysis for composite pattern generation ");
            System.out.println(".-------------------------------------------------.");



            //handle composite pattern and size pattern
            if(!customVisitor.typeParameter.isEmpty() && lcuStmt.size() == rcuStmt.size()) {
                patternMap.clear();
                CodePattern sizePattern = null;
                String size = customVisitor.typeParameter.get("size").toString();
                for(Pair<String, String> a :compositePattern.getLcuTemplateStatements()){
                    if(compositePattern.getRcuTemplateStatements().contains(a)) {
                        matchingExpression = a.getValue();
                        sizePattern = new NumberPattern(a.getKey(),0); //pos need more process
                        //((NumberPattern) sizePattern).setMinNum();
                        ((NumberPattern) sizePattern).AppendtoCSet(size);
                        ((NumberPattern) sizePattern).AppendtoISet(size);

                        patternMap.put(sizePattern.toString(), sizePattern);
                        System.out.println(sizePattern.marshall());
                    }
                }
            }
        }
        //after custom the pattern, case: namePattern


        // Compare arguments directly
        for(int i = 0; i < lNodeArgument.size(); i++) {
            CodePattern codePattern;
            String lArg = lNodeArgument.get(i).toString();
            String rArg = rNodeArgument.get(i).toString();
            if (!lArg.equals(rArg)) {
                if (lArg.contains("/")) {
                    codePattern = new ParameterPattern(lNodeType.get(0), i);
                    ((ParameterPattern) codePattern).AppendtoISet(this.divideArgument(lArg, null));
                    ((ParameterPattern) codePattern).AppendtoCSet(this.divideArgument(rArg, null));
                    this.patternMap.put(codePattern.toString(), codePattern);
                } else {
                    if(lArg.matches("\\d+")){//if the argument is number
                        codePattern = new NumberPattern(lNodeType.get(0), i);
                        ((NumberPattern) codePattern).AppendtoISet(lArg);
                        ((NumberPattern) codePattern).AppendtoCSet(rArg);
                        this.patternMap.put(codePattern.toString(), codePattern);
                    }
                    //If the argument does not contain any slash, go with the normal case
                    else {
                        codePattern = new ParameterPattern(lNodeType.get(0), i);
                        ((ParameterPattern) codePattern).AppendtoISet(lArg);
                        ((ParameterPattern) codePattern).AppendtoCSet(rArg);
                        this.patternMap.put(codePattern.toString(), codePattern);
                        System.out.println(codePattern.marshall());
                    }
                }
            }
        }


        DBHandler db = new DBHandler();
        for(Map.Entry<String, CodePattern> entry: this.patternMap.entrySet()) {
//            db.WritetoDB(entry.getKey(), entry.getValue(), matchingExpression);
            db.writetoJson(entry.getValue());
        }


    }

    /**
     * Deserialize the file
     * @param
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void SavetoFile() throws IOException {
        FileOutputStream fos = new FileOutputStream("pattern.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(this.patternMap);
        oos.close();
        fos.close();
        System.out.println("Save!");
    }

    /**
     * Deserialize the file
     * @param filename
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void LoadFromFile(String filename) throws IOException, ClassNotFoundException{
        FileInputStream fis = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fis);
        this.patternMap = (HashMap) ois.readObject();
        for(Map.Entry<?,?> entry: this.patternMap.entrySet()) {
            System.out.println(entry.getKey());
        }
        ois.close();
        fis.close();
    }

    /**
     * For argument like "AES/CBC/NoPadding", it will return {"AES/$/$", "$/CBC/$", "$/$/NoPadding"}
     * @param arg,
     * @return
     */
    public List<String> divideArgument(String arg, Set<Integer> groups) {
        List<String> genericArgs = new ArrayList<>();
        String[] splitArg = arg.split("\\/");
        StringBuilder group_str = new StringBuilder();
        if (splitArg.length == 1)
            genericArgs.add(genericArgs.get(0));
        else {
            for (int i = 0; i < splitArg.length; i++) {
                if (groups.contains(i)) {
                    group_str.append(splitArg[i]);
                    group_str.append("/");
                }
                else {
                    genericArgs.add(splitArg[i]);
                }
            }
        }
        if (group_str.length() > 0) group_str.deleteCharAt(group_str.length()-1);
        genericArgs.add(group_str.toString());
        return genericArgs;
    }


    public void extractChangesAsLeftRight() {

    }

}



