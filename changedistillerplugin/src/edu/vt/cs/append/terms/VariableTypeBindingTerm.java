package edu.vt.cs.append.terms;

import org.eclipse.jdt.core.dom.IBinding;

import edu.vt.cs.append.ASTUtil;

public class VariableTypeBindingTerm extends Term{
	// the "name" field defined in super class is used to store "variable_name"
		// here
		private static final long serialVersionUID = 1L;
		public IBinding binding;

		// public List<Object> tags = new ArrayList<Object>(); //this is used to
		// hold some information

		private void init() {
			this.termType = TermType.VariableTypeBindingTerm;
		}

		private TypeNameTerm typeNameTerm;

		public VariableTypeBindingTerm(int nodeType, String name) {
			super(nodeType, name);
			init();
		}

		public VariableTypeBindingTerm(int nodeType, String name,
				String abstractName) {
			this(nodeType, name);
			this.abstractName = abstractName;
		}

		public VariableTypeBindingTerm(int nodeType, String name,
				String abstractName, TypeNameTerm t) {
			this(nodeType, name, abstractName);
			this.typeNameTerm = t;
		}

		@Override
		public Object clone() {
			Object obj;
			obj = super.clone();
			((VariableTypeBindingTerm) obj)
					.setTypeNameTerm((TypeNameTerm) (this.typeNameTerm.clone()));
			return obj;
		}

		public String getAbstractVariableName() {
			return super.getAbstractName();
		}

		public TypeNameTerm getTypeNameTerm() {
			return this.typeNameTerm;
		}

		public String getVariableName() {
			return super.getName();
		}

		public boolean equals(Object obj) {
			if (!super.equals(obj))
				return false;
			if (!(obj instanceof VariableTypeBindingTerm))
				return false;
			VariableTypeBindingTerm other = (VariableTypeBindingTerm) obj;
			return this.getTypeNameTerm().equals(other.getTypeNameTerm());
		}

		public int hashCode() {
			return super.hashCode() * 10000 + this.getTypeNameTerm().hashCode();
		}

		public void setAbstractVariableName(String abstractVariableName) {
			this.setAbstractName(abstractVariableName);
		}

		public void setTypeNameTerm(TypeNameTerm t) {
			this.typeNameTerm = t;
		}

		public void setVariableName(String variableName) {
			this.setName(variableName);
		}
		


		public String toString() {
			return ASTUtil.convertToStringTypeName(this.getNodeType()) + ": "
					+ this.getTypeNameTerm().getAbstractTypeName() + "--"
					+ this.getAbstractVariableName() + ": "
					+ this.getTypeNameTerm().getTypeName() + "--"
					+ this.getVariableName();
		}
}
