package ch.uzh.ifi.seal.changedistiller.distilling;

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

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.ast.ASTHelper;
import ch.uzh.ifi.seal.changedistiller.ast.ASTHelperFactory;
import ch.uzh.ifi.seal.changedistiller.distilling.refactoring.RefactoringCandidateProcessor;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.entities.ClassHistory;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.StructureEntityVersion;
import ch.uzh.ifi.seal.changedistiller.structuredifferencing.StructureDiffNode;
import ch.uzh.ifi.seal.changedistiller.structuredifferencing.StructureDifferencer;
import ch.uzh.ifi.seal.changedistiller.structuredifferencing.StructureNode;

import com.google.inject.Inject;
import edu.vt.cs.append.FineChangesInMethod;


/**
 * Distills {@link SourceCodeChange}s between two {@link File}.
 * 
 * @author Beat Fluri
 * @author Giacomo Ghezzi
 */
public class FileDistiller {
	//added by nameng
	private static boolean ignoreComments = false;
	
    private DistillerFactory fDistillerFactory;
    private ASTHelperFactory fASTHelperFactory;
    private RefactoringCandidateProcessor fRefactoringProcessor;

    private List<SourceCodeChange> fChanges;
    private ASTHelper<StructureNode> fLeftASTHelper;
    private ASTHelper<StructureNode> fRightASTHelper;
    private ClassHistory fClassHistory;
    private String fVersion;

    @Inject
    FileDistiller(
            DistillerFactory distillerFactory,
            ASTHelperFactory factory,
            RefactoringCandidateProcessor refactoringProcessor) {
        fDistillerFactory = distillerFactory;
        fASTHelperFactory = factory;
        fRefactoringProcessor = refactoringProcessor;
    }

    /**
     * Extracts classified {@link SourceCodeChange}s between two {@link File}s.
     * 
     * @param left
     *            file to extract changes
     * @param right
     *            file to extract changes
     */
    public void extractClassifiedSourceCodeChanges(File left, File right) {
    	extractClassifiedSourceCodeChanges(left, "default", right, "default");
    }

    /**
     * Extracts classified {@link SourceCodeChange}s between two {@link File}s.
     * 
     * @param left
     *            file to extract changes
     * @param leftVersion
     * 			  version of the language in the left file
     * @param right
     *            file to extract changes
     * @param leftVersion
     * 			  version of the language in the right file
     */
    @SuppressWarnings("unchecked")
    public void extractClassifiedSourceCodeChanges(File left, String leftVersion, File right, String rightVersion) {
    	//modified by nameng to allow left == null
    	if (left != null)
    		fLeftASTHelper = fASTHelperFactory.create(left, leftVersion);
    	else
    		fLeftASTHelper = null;
    	if (right != null)
    		fRightASTHelper = fASTHelperFactory.create(right, rightVersion);
    	else 
    		fRightASTHelper = null;
        extractDifferences();
    }

	private void extractDifferences() {
		StructureDifferencer structureDifferencer = new StructureDifferencer();
		//modified by nameng to allow any helper to be null
		StructureNode lNode = fLeftASTHelper == null? null : fLeftASTHelper.createStructureTree();
		StructureNode rNode = fRightASTHelper == null? null : fRightASTHelper.createStructureTree();
        structureDifferencer.extractDifferences(lNode, rNode);
        StructureDiffNode structureDiff = structureDifferencer.getDifferences();
        if (structureDiff != null) {
        	fChanges = new LinkedList<SourceCodeChange>();
            // first node is (usually) the compilation unit
            processRootChildren(structureDiff);
        } else {
        	fChanges = Collections.emptyList();
        }
	}

    public void extractClassifiedSourceCodeChanges(File left, File right, String version) {
    	fVersion = version;
    	this.extractClassifiedSourceCodeChanges(left, right);
    }
    
    private void processRootChildren(StructureDiffNode diffNode) {    	
    	// added by nameng to handle file insertion/deletion [
    	if (!diffNode.hasChildren()) {
    		if(diffNode.isAddition()) {
    			fChanges.add(new Insert(null, null, null));
    		} else if (diffNode.isDeletion()) {
    			StructureEntityVersion scv = fLeftASTHelper.createStructureEntityVersion(diffNode.getLeft());
    			fChanges.add(new Delete(scv, null, null));
    		}
    		return;
    	}// ] added by nameng
        for (StructureDiffNode child : diffNode.getChildren()) {
            if (child.isClassOrInterfaceDiffNode() && mayHaveChanges(child.getLeft(), child.getRight())) {
                if (fClassHistory == null) {
                	if (fVersion != null) {
                		fClassHistory = new ClassHistory(fRightASTHelper.createStructureEntityVersion(child.getRight(), fVersion));
                	} else {
                		fClassHistory = new ClassHistory(fRightASTHelper.createStructureEntityVersion(child.getRight()));
                	}
                }
                processClassDiffNode(child);
            }
        }
    }

    private void processClassDiffNode(StructureDiffNode child) {
    	ClassDistiller classDistiller;
    	if (fVersion != null) {
        classDistiller =
                new ClassDistiller(
                        child,
                        fClassHistory,
                        fLeftASTHelper,
                        fRightASTHelper,
                        fRefactoringProcessor,
                        fDistillerFactory,
                        fVersion);
    	} else {
    		classDistiller =
                new ClassDistiller(
                        child,
                        fClassHistory,
                        fLeftASTHelper,
                        fRightASTHelper,
                        fRefactoringProcessor,
                        fDistillerFactory);
    	}
        classDistiller.extractChanges();
        fChanges.addAll(classDistiller.getSourceCodeChanges());
    }

    private boolean mayHaveChanges(StructureNode left, StructureNode right) {
        return (left != null) && (right != null);
    }

    public List<SourceCodeChange> getSourceCodeChanges() {
        return fChanges;
    }

    public ClassHistory getClassHistory() {
        return fClassHistory;
    }
    
    // added by nameng: to ignore comments
    public static void setIgnoreComments() {
    	ignoreComments = true;
    }
    
    public static boolean checkIgnoreComments() {
    	return ignoreComments;
    }

    public static void main(String[] args) throws Exception{
        String lfilename = "checkclient.java";
        String rfilename = "checkclient.java";
        File left = new File("C:\\Users\\ying\\Documents\\JAVA_CODE\\cipher_test\\src\\cipher_test\\insecure\\SSL\\" + lfilename);
        File right = new File("C:\\Users\\ying\\Documents\\JAVA_CODE\\cipher_test\\src\\cipher_test\\secure\\SSL\\" + rfilename);
        FileDistiller distiller = ChangeDistiller.createFileDistiller();
        FileDistiller.setIgnoreComments();
        try{
            distiller.extractClassifiedSourceCodeChanges(left, right);
        } catch(Exception e) {
            System.err.println("Warning: error while change disitilling. ");
            e.printStackTrace();
        }
        List<SourceCodeChange> changes = distiller.getSourceCodeChanges();

        if(changes != null) {
            for (SourceCodeChange change : changes) {
                System.out.println(change);
                System.out.println("start position :"+changes.size());
            }
        }


    }

}
