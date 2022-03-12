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

import java.util.List;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.StructureEntityVersion;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.TreeDifferencer;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.TreeEditOperation;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.operation.MoveOperation;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.operation.UpdateOperation;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Distills and classifies {@link SourceCodeChange} between two {@link Node} trees. Changes are attached to the
 * {@link StructureEntityVersion} in which the changes happened.
 * 
 * @author Beat Fluri
 */
public class Distiller {

    private StructureEntityVersion fStructureEntity;
    private TreeDifferencer fTreeDifferencer;
    private SourceCodeChangeConverter fChangeConverter;
    private SourceCodeChangeClassifier fClassifier;

    @Inject
    Distiller(
            @Assisted StructureEntityVersion structureEntity,
            TreeDifferencer treeDifferencer,
            SourceCodeChangeClassifier classifier) {
        fStructureEntity = structureEntity;
        fChangeConverter = new SourceCodeChangeConverter(fStructureEntity);
        fTreeDifferencer = treeDifferencer;
        fClassifier = classifier;
    }

    /**
     * Extracts and classifies {@link SourceCodeChange}s between the left and the right {@link Node} tree. The
     * {@link SourceCodeChange}s are attached to the {@link StructureEntityVersion}.
     * 
     * @param leftRoot
     *            of the node tree
     * @param rightRoot
     *            of the node tree
     */
    public void extractClassifiedSourceCodeChanges(Node leftRoot, Node rightRoot) {
        if ((leftRoot != null) && (rightRoot != null)) {
            fTreeDifferencer.calculateEditScript(leftRoot, rightRoot);
            List<TreeEditOperation> ops = fTreeDifferencer.getEditScript(); //Fahad: returns edit script for code difference

            if (leftRoot.getValue().contains("Initializer1")) {
            	int ldelta = leftRoot.getEntity().getSourceRange().getStart();
                int rdelta = rightRoot.getEntity().getSourceRange().getStart();
              
            	System.out.println("Position Adjusted!");
            	for (TreeEditOperation x : ops) {
                	if (x instanceof UpdateOperation) {
                		UpdateOperation inter_x = (UpdateOperation) x;
                		SourceRange range = inter_x.getNodeToUpdate().getEntity().getSourceRange();
                		int toup_start = range.getStart();
                		int toup_len = range.getEnd() - toup_start;
                		System.out.println(toup_len);
                		int changed_toup_start = toup_start - 14 + ldelta;
                		int changed_toup_end = changed_toup_start + toup_len;
                		inter_x.getNodeToUpdate().getEntity().setSourceRange(new SourceRange(changed_toup_start, changed_toup_end));
                		
                		range = inter_x.getNewNode().getEntity().getSourceRange();
                		int new_start = range.getStart();
                		int new_len = range.getEnd() - new_start;
                		System.out.println(new_len);
                		int changed_new_start = new_start - 14 + rdelta;
                		int changed_new_end = changed_new_start + new_len + 1;
                		inter_x.getNewNode().getEntity().setSourceRange(new SourceRange(changed_new_start, changed_new_end));
                		System.out.println();
                	}
                }
            }
            
            fChangeConverter.addTreeEditOperationsAsSourceCodeChanges(ops);
            List<SourceCodeChange> classifiedChanges =
                    fClassifier.classifySourceCodeChanges(fChangeConverter.getSourceCodeChanges());
            fStructureEntity.addAllSourceCodeChanges(classifiedChanges);
            System.out.println();
        }
    }

}
