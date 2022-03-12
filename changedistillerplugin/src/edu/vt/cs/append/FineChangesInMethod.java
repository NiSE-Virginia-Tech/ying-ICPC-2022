package edu.vt.cs.append;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;


// added by nameng
public class FineChangesInMethod extends SourceCodeChange {
	private List<SourceCodeChange> fineChanges = null;
	// from old range to change
	private Map<SourceRange, SourceCodeChange> oldRangeToChange = null;
	private Map<SourceRange, SourceCodeChange> newRangeToChange = null;
	
	public FineChangesInMethod(SourceCodeEntity oEntity, SourceCodeEntity nEntity, List<SourceCodeChange> changes) {
		super();
		super.setChangedEntity(new CompositeEntity(oEntity, nEntity));
		fineChanges = new ArrayList<SourceCodeChange>(changes);
		oldRangeToChange = new HashMap<SourceRange, SourceCodeChange>();
		newRangeToChange = new HashMap<SourceRange, SourceCodeChange>();
		for (SourceCodeChange c : fineChanges) {
			SourceCodeEntity e = c.getChangedEntity();
			if (c instanceof Insert) {
				newRangeToChange.put(new SourceRange(e.getStartPosition(), e.getEndPosition()), c);
			} else if (c instanceof Delete) {
				oldRangeToChange.put(new SourceRange(e.getStartPosition(), e.getEndPosition()), c);
			} else if (c instanceof Update) {
				oldRangeToChange.put(new SourceRange(e.getStartPosition(), e.getEndPosition()), c);
				e = ((Update) c).getNewEntity();
				newRangeToChange.put(new SourceRange(e.getStartPosition(), e.getEndPosition()), c);
			}			
		}
	}

	public Set<SourceRange> getAllNewRangesWithChange() {
		return newRangeToChange.keySet();
	}

	//add by Ying, why it is not right??
//	public Set<SourceCodeChange> getAllOldRangesWithChange(){
//		return oldRangeToChange.keySet();
//	}
	public SourceCodeChange getChangeWithNewRange(SourceRange r) {
		return newRangeToChange.get(r);
	}
	
	public SourceCodeChange getChangeWithOldRange(SourceRange r) {
		return oldRangeToChange.get(r);
	}
	
	public int getIndexOfChangeForOldRange(SourceRange r) {
		return fineChanges.indexOf(oldRangeToChange.get(r));
	}
	
	public List<SourceCodeChange> getChanges() {
		return fineChanges;
	}



	//add by shengzhe
//	public List<Update> getUpdate() {
//		List<Update> fineUpdate = new ArrayList<Update>();
//		for (SourceCodeChange c : fineChanges) {
//			if (c instanceof Update) {
//				Update e = ((Update) c).getNewEntity();
//				fineUpdate.add(e);
//			}
//		}
//		return fineUpdate;
//	}
	
	public SourceRange getNewRangeWithIndex(int index) {
		SourceCodeChange c = fineChanges.get(index);
		for (Entry<SourceRange, SourceCodeChange> entry : newRangeToChange.entrySet()) {
			if (entry.getValue().equals(c)) {
				return entry.getKey();
			}
		}
		return null;
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
