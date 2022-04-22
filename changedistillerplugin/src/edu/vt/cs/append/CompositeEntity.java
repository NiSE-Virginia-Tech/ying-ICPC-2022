package edu.vt.cs.append;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import ch.uzh.ifi.seal.changedistiller.ast.java.JavaASTNodeTypeConverter;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;

public class CompositeEntity extends SourceCodeEntity{

	private SourceCodeEntity oEntity;
	private SourceCodeEntity nEntity;
	
	public CompositeEntity(SourceCodeEntity oEntity, SourceCodeEntity nEntity) {
		this.oEntity = oEntity;
		this.nEntity = nEntity;
		EntityType et = oEntity.getType();
		if (et.equals(JavaEntityType.METHOD)) {
			this.setType(JavaASTNodeTypeConverter.convert(MethodDeclaration.class));
		} else if (et.equals(JavaEntityType.FIELD)) {
			this.setType(JavaASTNodeTypeConverter.convert(FieldDeclaration.class));
		}		
		this.setUniqueName(oEntity.getUniqueName());
		this.setType(et);
	}
	
	public SourceCodeEntity getOldEntity() {
		return oEntity;
	}

	public SourceCodeEntity getNewEntity() {
		return nEntity;
	}
	
	public String toString() {
		return "METHOD: " + oEntity.getUniqueName();
	}

	@Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CompositeEntity other = (CompositeEntity) obj;
        return new EqualsBuilder().append(oEntity, other.oEntity).append(nEntity, other.nEntity).isEquals();
    }
	
	@Override
	public int hashCode() {
		HashCodeBuilder b = new HashCodeBuilder(17, 37);
		b.append(oEntity).append(nEntity);
		return b.toHashCode();
	}
}
