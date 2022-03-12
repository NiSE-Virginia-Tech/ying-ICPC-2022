package edu.vt.cs.append.terms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.dom.ASTNode;

import edu.vt.cs.append.JavaExpressionConverter;

public class Term {
	public static boolean abstractVariable = true;
	public static boolean abstractMethod = true;
	public static boolean abstractType = true;
	
	public enum TermType {
		//Fahad: Adding literal's type
		Term, MethodNameTerm, TypeNameTerm, VariableTypeBindingTerm, LiteralTerm
	}

	//Fahad: Adding literal's pattern
	public static Pattern Abs_And_Exact_Pattern = Pattern
			.compile(".*(v|m|t|c)\\$_[0-9]+_.*|.*u\\$_[0-9]+_([a-zA-Z]+_).*");
	// AbsPattern is used to check whether the abstract representation is
	// contained in some string
	public static Pattern AbsPattern = Pattern
			.compile(".+(v|m|t|c)\\$_[0-9]+_.*|.*(v|m|t|c)\\$_[0-9]+_.+|.+u\\$_[0-9]+_([a-zA-Z]+_).*|.*u\\$_[0-9]+_([a-zA-Z]+_)+.+");
	public static Pattern U_Pattern = Pattern
			.compile("u\\$_[0-9]+_([a-zA-Z]+_)+");
	// pattern to identify uInfo.name
	public static Pattern U_Pattern2 = Pattern.compile("u[0-9]+");
	public static Pattern U_Simple_Pattern = Pattern.compile("u\\$_[0-9]+_");
	public static Pattern U_List_Literal_Pattern = Pattern
			.compile("u\\$_[0-9]+_ListLiteral_$");
	public static Pattern S_Pattern = Pattern
			.compile("s\\$_[0-9]+_([a-zA-Z]+_)+");
	public static Pattern V_Pattern = Pattern.compile("v\\$_[0-9]+_");
	// pattern to identify vInfo.name
	//Fahad: Adding literal's pattern
	public static Pattern C_Pattern = Pattern.compile("c\\$_[0-9]+_");
	public static Pattern V_Pattern2 = Pattern.compile("v[0-9]+");
	public static Pattern M_Pattern = Pattern.compile("m\\$_[0-9]+_");
	public static Pattern T_Pattern = Pattern.compile("t\\$_[0-9]+_");
	public static Pattern Postfix_Pattern = Pattern.compile("([a-zA-Z]+_)+");
	public static Pattern ExactAbsPattern = Pattern
			.compile("(v|m|t)\\$_[0-9]+_|u\\$_[0-9]+_([a-zA-Z]+_)+");
	public static Pattern IndexPattern = Pattern.compile("[0-9]+");
	public static Pattern SufPattern = Pattern.compile("[a-zA-Z]+_");

	public static Map<Integer, String> IndexToExpr = new HashMap<Integer, String>();
	public static Map<Integer, String> IndexToStmt = new HashMap<Integer, String>();
	public static Map<String, Integer> ExprToIndex = new HashMap<String, Integer>();
	public static Map<String, Integer> StmtToIndex = new HashMap<String, Integer>();

	static {
		IndexToExpr.put(ASTNode.SIMPLE_NAME, "SimpleName");
		IndexToExpr.put(ASTNode.QUALIFIED_NAME, "QualifiedName");
		IndexToExpr.put(ASTNode.NUMBER_LITERAL, "NumberLiteral");
		IndexToExpr.put(ASTNode.CHARACTER_LITERAL, "CharacterLiteral");
		IndexToExpr.put(ASTNode.NULL_LITERAL, "NullLiteral");
		IndexToExpr.put(ASTNode.BOOLEAN_LITERAL, "BooleanLiteral");
		IndexToExpr.put(ASTNode.STRING_LITERAL, "StringLiteral");
		IndexToExpr.put(ASTNode.TYPE_LITERAL, "TypeLiteral");
		IndexToExpr.put(ASTNode.THIS_EXPRESSION, "ThisExpression");
		IndexToExpr.put(ASTNode.SUPER_FIELD_ACCESS, "SuperFieldAccess");
		IndexToExpr.put(ASTNode.FIELD_ACCESS, "FieldAccess");
		IndexToExpr.put(ASTNode.ASSIGNMENT, "Assignment");
		IndexToExpr.put(ASTNode.PARENTHESIZED_EXPRESSION,
				"ParenthesizedExpression");
		IndexToExpr.put(ASTNode.CLASS_INSTANCE_CREATION,
				"ClassInstanceCreation");
		IndexToExpr.put(ASTNode.ARRAY_CREATION, "ArrayCreation");
		IndexToExpr.put(ASTNode.ARRAY_INITIALIZER, "ArrayInitializer");
		IndexToExpr.put(ASTNode.METHOD_INVOCATION, "MethodInvocation");
		IndexToExpr.put(ASTNode.SUPER_METHOD_INVOCATION,
				"SuperMethodInvocation");
		IndexToExpr.put(ASTNode.ARRAY_ACCESS, "ArrayAccess");
		IndexToExpr.put(ASTNode.ARRAY_INITIALIZER, "ArrayInitializer");
		IndexToExpr.put(ASTNode.INFIX_EXPRESSION, "InfixExpression");
		IndexToExpr.put(ASTNode.INSTANCEOF_EXPRESSION, "InstanceofExpression");
		IndexToExpr
				.put(ASTNode.CONDITIONAL_EXPRESSION, "ConditionalExpression");
		IndexToExpr.put(ASTNode.POSTFIX_EXPRESSION, "PostfixExpression");
		IndexToExpr.put(ASTNode.PREFIX_EXPRESSION, "PrefixExpression");
		IndexToExpr.put(ASTNode.CAST_EXPRESSION, "CastExpression");
		IndexToExpr.put(ASTNode.VARIABLE_DECLARATION_EXPRESSION,
				"VariableDeclarationExpression");
		IndexToExpr.put(ASTNode.VARIABLE_DECLARATION_FRAGMENT,
				"VariableDeclarationFragment");
		IndexToExpr.put(ASTNode.VARIABLE_DECLARATION_STATEMENT,
				"VariableDeclarationStatement");
		IndexToExpr.put(JavaExpressionConverter.LIST_LITERAL, "ListLiteral");

		IndexToStmt.put(ASTNode.ASSERT_STATEMENT, "AssertStatement");
		IndexToStmt.put(ASTNode.BLOCK, "Block");
		IndexToStmt.put(ASTNode.BREAK_STATEMENT, "BreakStatement");
		IndexToStmt
				.put(ASTNode.CONSTRUCTOR_INVOCATION, "ConstructorInvocation");
		IndexToStmt.put(ASTNode.CONTINUE_STATEMENT, "ContinueStatement");
		IndexToStmt.put(ASTNode.DO_STATEMENT, "DoStatement");
		IndexToStmt.put(ASTNode.EMPTY_STATEMENT, "EmptyStatement");
		IndexToStmt.put(ASTNode.ENHANCED_FOR_STATEMENT, "EnhancedForStatement");
		IndexToStmt.put(ASTNode.EXPRESSION_STATEMENT, "ExpressionStatement");
		IndexToStmt.put(ASTNode.FOR_STATEMENT, "ForStatement");
		IndexToStmt.put(ASTNode.IF_STATEMENT, "IfStatement");
		IndexToStmt.put(ASTNode.LABELED_STATEMENT, "LabeledStatment");
		IndexToStmt.put(ASTNode.RETURN_STATEMENT, "ReturnStatement");
		IndexToStmt.put(ASTNode.SUPER_CONSTRUCTOR_INVOCATION,
				"SuperConstructorInvocation");
		IndexToStmt.put(ASTNode.SWITCH_CASE, "SwitchCase");
		IndexToStmt.put(ASTNode.SWITCH_STATEMENT, "SwitchStatement");
		IndexToStmt.put(ASTNode.SYNCHRONIZED_STATEMENT, "SynchronizedStatment");
		IndexToStmt.put(ASTNode.THROW_STATEMENT, "ThrowStatement");
		IndexToStmt.put(ASTNode.TRY_STATEMENT, "TryStatement");
		IndexToStmt.put(ASTNode.TYPE_DECLARATION_STATEMENT,
				"TypeDeclarationStatement");
		IndexToStmt.put(ASTNode.WHILE_STATEMENT, "WhileStatement");
		IndexToStmt.put(ASTNode.METHOD_DECLARATION, "MethodDeclaration");

		// NameToIndex does not include the map for "InOp, PreOp, PostOp"
		for (Entry<Integer, String> entry : IndexToExpr.entrySet()) {
			ExprToIndex.put(entry.getValue(), entry.getKey());
		}
		for (Entry<Integer, String> entry : IndexToStmt.entrySet()) {
			StmtToIndex.put(entry.getValue(), entry.getKey());
		}
	}

	public static Term getDefaultTerm() {
		return new Term(-1, "");
	}

	public static List<String> InfixOperators = Arrays.asList("*", "/", "%", "+", "-", "<<", ">>", ">>>", "<", ">", "<=", ">=",
			"==", "!=", "^", "&", "|", "&&", "||", "+=", "-=", "*=", "/=",
			"%=", "=");
	public static List<String> PrefixOperators = Arrays.asList("++", "--", "+", "-", "~", "!");
	public static List<String> PostfixOperators = Arrays.asList("++", "--");

	private static final long serialVersionUID = 1L;
	private int nodeType;
	protected String name;
	protected String abstractName;
	protected TermType termType;

	/**
	 * This function is used to test AbsPattern
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String methodHead = "String verifyText(String string,int start,int end,Event keyEvent){";
		Pattern tmp = Pattern
				.compile("[public |private |protected ]*[a-zA-Z]+ [a-zA-Z]+\\u0028.*\\u0029\\u007B");
		if (tmp.matcher(methodHead).matches()) {
			System.out.println("match!");
		} else {
			System.out.println("not match");
		}
		// Matcher matcher = AbsPattern.matcher("u$_3_FieldAccess_SimpleName_");
		// if (matcher.matches())
		// System.out.print(matcher.group());
		// System.out.println(AbsPattern.matcher("u$_341_BooleanLiteral_").find());
	}

	/**
	 * if nodeType == -1, this is an operator if nodeType == -100, this is an
	 * undefined statement
	 * 
	 * @param nodeType
	 * @param name
	 */
	public Term(int nodeType, String name) {
		this.termType = TermType.Term;
		this.nodeType = nodeType;
		this.name = name;
	}

	public Term(int nodeType, String name, String abstractName) {
		this(nodeType, name);
		this.abstractName = abstractName;
	}

	public static String constructAllSuffixes(Set<String> sufs) {
		StringBuffer buffer = new StringBuffer();
		for (String suf : sufs) {
			buffer.append(suf);
		}
		return buffer.toString();
	}

	public static String createAbstractName(String prefix, int index) {
		// the operators are processed in a special way. Although they belong to
		// unknown categories,
		// their appendix can only be
		// InfixExpressionOperator/PrefixExpressionOperator/SuffixExpressionOperator
		return prefix + index + "_";
	}

	public static String createExprSuffix(int lType, int rType, String lName,
			String rName) {
		String lSuf = getExprName(lType);
		String rSuf = getExprName(rType);
		if (lSuf == null)
			lSuf = "";
		else
			lSuf = lSuf + "_";
		if (rSuf == null)
			rSuf = "";
		else
			rSuf = rSuf + "_";
		return integrateSuffixes(lName, rName, lSuf, rSuf);
	}

	public static String createExprSuffix(List<Integer> nodeTypeList)
			throws Exception {
		Set<String> exprNames = new HashSet<String>();
		String exprName = null;
		for (Integer nodeType : nodeTypeList) {
			if (nodeType == JavaExpressionConverter.OPERATOR) {
				throw new Exception(
						"Different operators are used in different examples which prevent commonality extraction");
			}
			exprName = getExprName(nodeType);
			exprNames.add(exprName + "_");
		}
		return constructAllSuffixes(exprNames);
	}

	public static String createExprSuffix(int lType, List<Integer> rNodeTypes,
			String lName, List<String> rNames) {
		String lSuf = getExprName(lType);
		String rSuf = null;
		List<String> rSufs = new ArrayList<String>();
		for (int i = 0; i < rNodeTypes.size(); i++) {
			rSufs.add(getExprName(rNodeTypes.get(i)));
		}
		if (lSuf == null)
			lSuf = "";
		else
			lSuf = lSuf + "_";
		for (int i = 0; i < rSufs.size(); i++) {
			rSuf = rSufs.get(i);
			if (rSuf == null)
				rSufs.set(i, "");
			else
				rSufs.set(i, rSuf + "_");
		}
		return integrateSuffixes(lName, rNames, lSuf, rSufs);
	}

	public static String createOpSuffix(String lName, String rName) {
		String lOp = getOpName(lName);
		String rOp = getOpName(rName);
		if (!lOp.isEmpty() && lOp.equals(rOp)) {// none of them are abstract
												// identifiers
			return lOp;
		}
		// it is possible that one of the operators has already been generalized
		return "";
	}

	public static String createOpSuffix(String lName, List<String> rNames) {
		String lOp = getOpName(lName);
		if (!lOp.isEmpty()) {
			boolean isEqual = true;
			for (int i = 0; i < rNames.size(); i++) {
				if (!lOp.equals(getOpName(rNames.get(i)))) {
					isEqual = false;
					break;
				}
			}
			if (isEqual) {
				return lOp;
			}
		}
		return "";
	}

	public static String createStmtSuffix(int nodeType1,
			List<Integer> nodeType2s, List<String> abstractIdentifiers) {
		StringBuffer buffer = new StringBuffer();
		String name1 = getStmtName(nodeType1);
		List<String> name2s = new ArrayList<String>();
		for (int i = 0; i < nodeType2s.size(); i++) {
			name2s.add(getStmtName(nodeType2s.get(i)));
		}
		Set<String> suf = new HashSet<String>();
		if (name1 == null)
			name1 = "";
		String name2 = null;
		for (int i = 0; i < name2s.size(); i++) {
			name2 = name2s.get(i);
			if (name2 == null)
				name2 = "";
		}
		Set<String> distinguishedNames = new HashSet<String>(name2s);
		distinguishedNames.add(name1);
		distinguishedNames.remove("");// to remove empty string
		Set<String> newNames = new HashSet<String>();
		for (String name : distinguishedNames) {
			newNames.add(name + "_");
		}

		if (abstractIdentifiers == null) {
			for (String name : newNames) {
				buffer.append(name);
			}
		} else {
			for (String abstractIdentifier : abstractIdentifiers) {
				suf.addAll(getSuffixes(abstractIdentifier));
			}
			suf.addAll(newNames);
			buffer.append(constructAllSuffixes(suf));
		}
		return buffer.toString();
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public int getNodeType() {
		return this.nodeType;
	}

	/**
	 * To query the IndexToExpr map
	 * 
	 * @param nodeType
	 * @return may be null
	 */
	public static String getExprName(int nodeType) {
		String suf = IndexToExpr.get(nodeType);
		return suf;
	}

	public static String getOpName(String name) {
		if (InfixOperators.contains(name))
			return "InOp_";
		if (PrefixOperators.contains(name))
			return "PreOp_";
		if (PostfixOperators.contains(name))
			return "PostOp_";
		// name is an abstract identifier
		return getAllSuffixes(name);
	}

	public static String getStmtName(int nodeType) {
		String result = IndexToStmt.get(nodeType);
		return result;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Term))
			return false;
		Term other = (Term) obj;
		if (this.nodeType != other.nodeType)
			return false;
		if (this.name == null && other.name != null)
			// for the case when the term corresponds to the type of NULL
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

	public String getAbstractName() {
		if (this.abstractName == null) {
			return "";
		}
		return this.abstractName;
	}

	public String getAbstractNameWithoutIndex() {
		if (abstractName == null) {
			return "_"; // the space to show that there should be an operator
						// here
		}
		if (ExactAbsPattern.matcher(abstractName).matches())
			return this.abstractName.substring(0,
					this.abstractName.indexOf('_'));
		return abstractName;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * get a single string containing all suffixes
	 * 
	 * @param name
	 * @return
	 */
	public static String getAllSuffixes(String name) {
		System.out.print("");
		Matcher matcher = Postfix_Pattern.matcher(name);
		if (matcher.find())
			return matcher.group(0);
		return "NOT_FOUND";
	}

	/**
	 * get a list of strings, each of which represents a suffix
	 * 
	 * @param name
	 * @return
	 */
	public static Set<String> getSuffixes(String name) {
		Set<String> suffixes = new HashSet<String>();
		if (U_Pattern.matcher(name).matches()) {
			Matcher matcher = SufPattern.matcher(name);
			while (matcher.find()) {
				suffixes.add(matcher.group());
			}
		}
		return suffixes;
	}

	public TermType getTermType() {
		return this.termType;
	}

	public static Set<VariableTypeBindingTerm> getVTerms(Set<Term> terms) {
		Set<VariableTypeBindingTerm> result = new HashSet<VariableTypeBindingTerm>();
		for (Term t : terms) {
			if (t instanceof VariableTypeBindingTerm) {
				result.add((VariableTypeBindingTerm) t);
			}
		}
		return result;
	}

	public static Map<String, VariableTypeBindingTerm> getVTermMap(
			Set<VariableTypeBindingTerm> set) {
		Map<String, VariableTypeBindingTerm> map = new HashMap<String, VariableTypeBindingTerm>();
		for (VariableTypeBindingTerm t : set) {
			map.put(t.getName(), t);
		}
		return map;
	}

	public static Map<String, Term> getTermMap(Set<Term> set) {
		Map<String, Term> map = new HashMap<String, Term>();
		for (Term t : set) {
			map.put(t.getName(), t);
		}
		return map;
	}

	public int hashCode() {
		if (this.abstractName != null)
			return this.nodeType * 10000 + this.abstractName.hashCode() * 100
					+ this.name.hashCode();
		else
			return this.nodeType * 10000 + this.name.hashCode();
	}

	/**
	 * suf1 and lSuf have the same source lName, while suf2 and rSuf have the
	 * same source rName
	 * 
	 * @param lSuf
	 * @param rSuf
	 * @param suf1
	 * @param suf2
	 * @return
	 */
	public static String integrateSuffixes(String lName, String rName,
			String lSuf, String rSuf) {
		Set<String> suf1 = getSuffixes(lName);
		Set<String> suf2 = getSuffixes(rName);
		Set<String> sufs = new HashSet<String>();
		sufs.addAll(suf1);
		sufs.addAll(suf2);
		if (!lSuf.isEmpty()) {
			sufs.add(lSuf);
		}
		if (!rSuf.isEmpty()) {
			sufs.add(rSuf);
		}
		return constructAllSuffixes(sufs);
	}

	public static String integrateSuffixes(String lName, List<String> rNames,
			String lSuf, List<String> rSufs) {
		Set<String> suf1 = getSuffixes(lName);
		String rName = null;
		for (int i = 0; i < rNames.size(); i++) {
			rName = rNames.get(i);
			suf1.addAll(getSuffixes(rName));
		}
		suf1.add(lSuf);
		suf1.addAll(rSufs);
		return constructAllSuffixes(suf1);
	}

	public boolean isEquivalent(Term other) {
		boolean flag = true;
		if (!this.termType.equals(other.termType))
			return false;
		switch (this.termType) {
		case TypeNameTerm: {
			if (abstractType)
				return flag;
		}
			break;
		case MethodNameTerm: {
			if (abstractMethod)
				return flag;
		}
			break;
		case VariableTypeBindingTerm: {
			if (abstractVariable)
				return flag;
		}
			break;
		default: {
			return flag;
		}
		}
		if (!this.getName().equals(other.getName())) {
			flag = false;
		}
		return flag;
	}

	public static Term normalize(Term term) {
		Term newTerm = (Term) term.clone();
		Term typeTerm = null;
		newTerm.abstractName = "";
		if (term.getTermType().equals(Term.TermType.VariableTypeBindingTerm)) {
			typeTerm = ((VariableTypeBindingTerm) newTerm).getTypeNameTerm();
			typeTerm.abstractName = "";
		}
		return newTerm;
	}

	public static List<Integer> parseTypes(String uName) {
		// u_pattern: u\\$_[0-9]+_([a-zA-Z]+_)+
		String[] segs = uName.split("_");
		// the first two segs should be not considered, since one is for u$_,
		// the other is for #_
		// for instance, given "u$_3_InfixExpression_", we will get "u$", "3",
		// "InfixExpression"
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 2; i < segs.length; i++) {
			result.add(ExprToIndex.get(segs[i]));
		}
		return result;
	}

	public static int parseInt(String abstractName) {
		Matcher matcher = IndexPattern.matcher(abstractName);
		if (matcher.find()) {
			return Integer.valueOf(matcher.group());
		}
		return -1;
	}

	public void setAbstractName(String abstractName) {
		this.abstractName = abstractName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return this.name;
	}
}
