package ch.uzh.ifi.seal.changedistiller.structuredifferencing;

/*
 * #%L
 * ChangeDistiller
 * %%
 * Copyright (C) 2011 - 2013 Software Architecture and Evolution Lab, Department of Informatics, UZH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.DatabaseControl;


/**
 * Calculates structure differences between two trees of {@link StructureNode}s.
 *
 * @author Beat Fluri
 */
public class StructureDifferencer {

    private StructureDiffNode fDifferences;

    /**
     * Types of differences.
     *
     * @author Beat Fluri
     */
    public enum DiffType {
        ADDITION,
        DELETION,
        CHANGE,
        NO_CHANGE
    }

    // this code is inspired by org.eclipse.compare.structureMergeViewer.Differencer
    /**
     * Finds and returns the structure differences between a left and right {@link StructureNode} tree.
     *
     * @param left
     *            to compare with right
     * @param right
     *            to with left
     */
    public void extractDifferences(StructureNode left, StructureNode right) {
        if ((left == null) && (right == null)) {
            return;
        }
        fDifferences = traverse(left, right);
    }

    private StructureDiffNode traverse(StructureNode left, StructureNode right) {
        StructureNode[] leftChildren = getChildren(left);
        StructureNode[] rightChildren = getChildren(right);
        //added by shengzhe Nov30,2017
        if (left != null && right != null) {
            check_import(left, right);
        }
        //----

        StructureDiffNode root = new StructureDiffNode(left, right);
        if ((leftChildren != null) && (rightChildren != null)) {
            root = traverseChildren(root, leftChildren, rightChildren);
            // added by shengzhe May-3, 2017
            if (left.isClassOrInterface() &&
                    right.isClassOrInterface() &&
                    !contentsEqual(left, right)) {
                root.setLeft(left);
                root.setRight(right);
                root.setDiffType(DiffType.CHANGE);
            }
        } else {
            root = extractLeaveChange(root, left, right);
        }
        if (hasChanges(root)) {
            return root;
        }
        return null;
    }

    private void check_import(StructureNode left, StructureNode right) {
        ASTNode leftast = (left).getASTNode();
        ASTNode rightast = (right).getASTNode();
//        for (ImportReference x : ((CompilationUnitDeclaration)leftast)).
        /*Fahad: Check for library import changes and adding pattern*/
        if (leftast instanceof CompilationUnitDeclaration //Fahad: what does CompilationUnitDeclaration means?
                && rightast instanceof CompilationUnitDeclaration){
            CompilationUnitDeclaration leftcud = (CompilationUnitDeclaration)leftast;
            CompilationUnitDeclaration rightcud = (CompilationUnitDeclaration)rightast;
            CommonValue.resetimports();
            ImportReference[] leftimt = leftcud.imports;
            ImportReference[] rightimt = rightcud.imports;
            if (leftimt!=null && rightimt!=null) {
                for (int lfti=0;lfti<leftimt.length;lfti++) {
                    String x = leftimt[lfti].toString();
                    // add by Ying Zhang, the String.startsWith() function cannot accept null
                    if (CommonValue.possible_lib_name1 != null && x.startsWith(CommonValue.possible_lib_name1)) {
                        CommonValue.leftimport.add(x);
                    }
                }
                for (int rti=0;rti<rightimt.length;rti++) {
                    String x = rightimt[rti].toString();
                    // add by Ying Zhang, the String.startsWith() function cannot accept null
                    if (CommonValue.possible_lib_name1 != null && x.startsWith(CommonValue.possible_lib_name1)) {
                        CommonValue.rightimport.add(x);
                    }
                }
                for (int lfti=0;lfti<leftimt.length;lfti++)
                    for (int rti=0;rti<rightimt.length;rti++) {
                        ImportReference x = leftimt[lfti];
                        ImportReference y = rightimt[rti];
                        char[][] one_x = x.getImportName();
                        char[][] one_y = y.getImportName();
                        if (Arrays.equals(one_x[one_x.length-1], one_y[one_y.length-1])) {
                            boolean lab = true;
                            // add by Ying Zhang, the String.startsWith() function cannot accept null
                            if (CommonValue.possible_lib_name1 == null
                                    ||!x.toString().startsWith(CommonValue.possible_lib_name1)
                                    || !y.toString().startsWith(CommonValue.possible_lib_name1)
                            ) {
                                continue;
                            }
                            else if (one_x.length != one_y.length) {
                                lab = false;
                            }
                            else {
                                for (int i=0;i<one_x.length-1;i++)
                                    if (!Arrays.equals(one_x[i], one_y[i])) {
                                        lab = false;
                                    }
                            }
                            if (x.toString().endsWith("*") || y.toString().endsWith("*")) {
                                lab = true;
                            }
                            if (!lab) {

//            				System.out.println("-Old Import:\n" + x.toString());
//                    		System.out.println("-New Import:\n" + y.toString());                			
                                DatabaseControl data1 = new DatabaseControl();
                                int label = data1.insertpattern(x.toString(), y.toString(), CommonValue.common_old_version, CommonValue.common_new_version, "Lib address change", "1");
                                data1.insertsnippet(x.toString(), y.toString(), CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label), CommonValue.common_old_version+"-"+CommonValue.common_new_version);
                            }
                        }
                    }
            }
        }
        if (leftast instanceof TypeDeclaration
                && rightast instanceof TypeDeclaration){
            int labell = 0;
            TypeDeclaration x = (TypeDeclaration)leftast;
            if (x.superclass != null) {
                String cx = x.superclass.toString();
                TypeDeclaration y = (TypeDeclaration)rightast;
                if (y.superInterfaces!=null) {
                    String cy = y.superInterfaces[0].toString();
                    String pattern_o = "";
                    String pattern_n = "";
                    if (CommonValue.checkleft(cx) && CommonValue.checkright(cy)) {
                        if (cx.endsWith(cy) || cy.endsWith(cx)) {
                            pattern_o += "extends " + cx;
                            pattern_n += "implements " + cy;
                        }
                    }
                    for (AbstractMethodDeclaration x_methods : x.methods) {
                        if (x_methods instanceof ConstructorDeclaration) continue;
                        if (labell == 1) break;
                        for (AbstractMethodDeclaration y_methods : y.methods) {
                            if (y_methods instanceof ConstructorDeclaration) continue;
                            System.out.println();
                            if (String.valueOf(x_methods.selector).equals(String.valueOf(y_methods.selector))) {
                                System.out.println();
                                if (x_methods.annotations == null || y_methods.annotations == null)
                                    continue;
                                if (String.valueOf(x_methods.annotations).equals("@Override")
                                        && !String.valueOf(y_methods.annotations).equals("@Override")) {
                                    String dealwith_y = String.valueOf(y_methods.annotations);
                                    dealwith_y = dealwith_y.split(" ")[0];
                                    dealwith_y = dealwith_y.substring(1);
                                    if (CommonValue.checkright(dealwith_y)) {
                                        if (!pattern_o.equals("")) {
                                            pattern_o += "\n...\n";
                                        }
                                        if (!pattern_n.equals("")) {
                                            pattern_n += "\n...\n";
                                        }
                                        pattern_o += "@Override";
                                        pattern_n += String.valueOf(y_methods.annotations);
                                        labell = 1;
                                    }
                                }
                            }
                        }
                    }
                    if (!pattern_o.isEmpty() && !pattern_n.isEmpty()) {
                        CommonValue.pushpattern(pattern_o, pattern_n, CommonValue.common_old_version, CommonValue.common_new_version, "Interface Change", "1");
                        CommonValue.pushsnippet(pattern_o, pattern_n, CommonValue.common_project_name, CommonValue.common_commit_number);
                    }
                }
            }
        }
    }

    private StructureDiffNode extractLeaveChange(StructureDiffNode root, StructureNode left, StructureNode right) {
        if (left == null) {
            if (right != null) {
                root.setLeft(null);
                root.setRight(right);
                root.setDiffType(DiffType.ADDITION);
            } else {
                assert (false);
            }
        } else if (right == null) {
            root.setLeft(left);
            root.setRight(null);
            root.setDiffType(DiffType.DELETION);
        } else {
            if (!contentsEqual(left, right)) {
                root.setLeft(left);
                root.setRight(right);
                root.setDiffType(DiffType.CHANGE);
            } else {
                return null;
            }
        }
        return root;
    }

    private StructureDiffNode traverseChildren(
            StructureDiffNode root,
            StructureNode[] leftChildren,
            StructureNode[] rightChildren) {
        Set<StructureNode> allSet = new HashSet<StructureNode>(20);
        Map<StructureNode, StructureNode> leftSet = new HashMap<StructureNode, StructureNode>(10);
        Map<StructureNode, StructureNode> rightSet = new HashMap<StructureNode, StructureNode>(10);
        for (StructureNode node : leftChildren) {
            allSet.add(node);
            leftSet.put(node, node);
        }
        for (StructureNode node : rightChildren) {
            allSet.add(node);
            rightSet.put(node, node);
        }
        if (leftChildren.length == 5) {
//        	System.out.println("");
        }
        for (StructureNode node : allSet) {
            StructureNode leftChild = leftSet.get(node);
            StructureNode rightChild = rightSet.get(node);
            if (leftChild == null && rightChild != null) {
//            	System.out.println("ha? pure add");
                if (rightChild.isMethodOrConstructor()) {
                    CommonValue.pureadd(rightChild);
                }
            }
            if (leftChild != null && rightChild == null) {
//            	System.out.println("ha? pure delete");
                if (leftChild.isMethodOrConstructor()) {
                    CommonValue.puredelete(leftChild);
                }
            }
            if (leftChild == null || rightChild == null) {
                continue;
            }
            StructureDiffNode diff = traverse(leftChild, rightChild);
            if (diff != null) {
                root.addChild(diff);
            }
        }
        return root;
    }

    private boolean hasChanges(StructureDiffNode root) {
        return (root != null) && (!root.getChildren().isEmpty() || (root.getDiffType() != DiffType.NO_CHANGE));
    }

    private boolean contentsEqual(StructureNode left, StructureNode right) {
        if ((left.getContent() == null) && (right.getContent() == null)) {
            return true;
        }
        StringReader leftContent = getStream(left);
        StringReader rightContent = getStream(right);
        try {
            if ((leftContent == null) || (rightContent == null)) {
                return false;
            }
            while (true) {
                int l = leftContent.read();
                int r = rightContent.read();
                if ((l == -1) && (r == -1)) {
                    return true;
                }
                if (l != r) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(leftContent != null) { // shouldn't happen - checked to calm sonar
                leftContent.close();
            }

            if(rightContent != null) { // shouldn't happen - checked to calm sonar
                rightContent.close();
            }
        }
        return false;
    }

    private StringReader getStream(StructureNode left) {
        String content = left.getContent();
        if (content != null) {
            return new StringReader(content);
        }
        return null;
    }

    private StructureNode[] getChildren(StructureNode node) {
        if ((node != null) && !node.getChildren().isEmpty()) {
            List<? extends StructureNode> nodes = node.getChildren();
            return nodes.toArray(new StructureNode[nodes.size()]);
        }
        return null;
    }

    public StructureDiffNode getDifferences() {
        return fDifferences;
    }
}
