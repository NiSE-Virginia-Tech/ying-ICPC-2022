package edu.vt.cs.append;

import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;

//added by nameng
public class FineChangesInField extends SourceCodeChange{
	private List<SourceCodeChange> fineChanges = null;
	
	public FineChangesInField(SourceCodeEntity oEntity, SourceCodeEntity nEntity, List<SourceCodeChange> changes) {
		super();
		super.setChangedEntity(new CompositeEntity(oEntity, nEntity));
		fineChanges = new ArrayList<SourceCodeChange>(changes);
	}
	
	public List<SourceCodeChange> getChanges() {
		return fineChanges;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer(super.toString());
		buf.append("\n");
		for (SourceCodeChange c : fineChanges) {
			buf.append("\t").append(c);
		}
		buf.append("\n");
		return buf.toString();
	}
}
