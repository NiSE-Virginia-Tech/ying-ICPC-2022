package edu.vt.cs.append.terms;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class TypeNameTerm extends Term{
	private static final long serialVersionUID = 1L;
	private String qualifiedName = null;
	private ITypeBinding binding = null;
	
	private void init() {
		this.termType = TermType.TypeNameTerm;
	}

	public TypeNameTerm(int nodeType, String name, String qName) {
		super(nodeType, name);
		this.qualifiedName = qName;
		init();
	}

	public TypeNameTerm(int nodeType, String name, String qName,
			String abstractName) {
		this(nodeType, name, qName);
		this.abstractName = abstractName;
	}

	public TypeNameTerm(String name, String qName) {// the node type is not
													// useful here
		this(0, name, qName);
	}

	public Object clone() {
		return super.clone();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof TypeNameTerm))
			return false;
		TypeNameTerm other = (TypeNameTerm) obj;
		if (this.name == null && other.name != null)
			return false;
		if (this.name == null && other.name == null) {
			// do nothing
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		// this.name == null == other.name || this.name.equals(other.name)
		if (this.abstractName == null && other.abstractName == null)
			return true;
        return this.abstractName != null
                && this.abstractName.equals(other.abstractName);
    }

	public String getAbstractTypeName() {
		return super.getAbstractName();
	}

	public String getQualifiedName() {
		return qualifiedName;
	}

	public String getTypeName() {
		return super.getName();
	}
	
	public ITypeBinding getTypeBinding() {
		return binding;
	}

	public int hashCode() {
		if (this.abstractName != null)
			return this.abstractName.hashCode() * 100 + this.name.hashCode();
		else
			return this.name.hashCode();
	}

	public void setAbstractTypeName(String abstractTypeName) {
		super.setAbstractName(abstractTypeName);
	}

	public void setTypeName(String typeName) {
		super.setName(typeName);
	}
	
	public void setTypeBinding(ITypeBinding binding) {
		this.binding = binding;
	}

	public String toString() {
		return this.name + "--" + this.abstractName;
	}
}
