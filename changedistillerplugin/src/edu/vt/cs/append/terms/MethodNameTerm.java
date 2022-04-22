package edu.vt.cs.append.terms;

import org.eclipse.jdt.core.dom.IMethodBinding;

public class MethodNameTerm extends Term{
	//the "name" field defined in super class is used to store "method_name" here
		private static final long serialVersionUID = 1L;
		private IMethodBinding binding = null;
		
		private void init(){
			this.termType = TermType.MethodNameTerm;
		}
		
		private TypeNameTerm typeTerm;
		
		public MethodNameTerm(int nodeType, String name, String abstractName) {
			super(nodeType, name, abstractName);
			init();
		}
		
		public Object clone(){
			return super.clone();
		}

		public boolean equals(Object obj){
			if(!super.equals(obj)) return false;
            return obj instanceof MethodNameTerm;
        }

		public String getAbstractMethodName(){
			return super.getAbstractName();
		}
		
		public String getMethodName(){
			return super.getName();
		}
		
		public int hashCode(){
			return super.hashCode();
		}
		
		public void setAbstractMethodName(String abstractMethodName){
			super.setAbstractName(abstractMethodName);
		}
		
		public void setMethodName(String methodName){
			super.setName(methodName);
		}
		
		public IMethodBinding getMethodBinding() {
			return this.binding;
		}
		
		public void setMethodBinding(IMethodBinding binding) {
			this.binding = binding;
		}
		
		public String toString(){
			return this.name + "--" + this.abstractName;
		}
		
		public TypeNameTerm getTypeNameTerm(){
			return typeTerm;
		}
		
		public void setTypeNameTerm(TypeNameTerm tTerm){
			this.typeTerm = tTerm;
		}
}
