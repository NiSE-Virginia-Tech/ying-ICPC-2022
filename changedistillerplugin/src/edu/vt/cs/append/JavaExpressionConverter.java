package edu.vt.cs.append;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import edu.vt.cs.append.terms.MethodNameTerm;
import edu.vt.cs.append.terms.Term;
import edu.vt.cs.append.terms.TypeNameTerm;
import edu.vt.cs.append.terms.VariableTypeBindingTerm;


/**
 * TODO: The way statements are parsed may be different from that in ChangeDistiller. This
 * may cause confusion.
 * @author nm8247
 *
 */
public class JavaExpressionConverter extends ASTVisitor {

	public static int OPERATOR = 106;
	public static int DOT = 107;
	public static int LEFTPARENTHESIS = 108;
	public static int RIGHTPARENTHESIS = 109;
	public static int COMMA = 110;
	public static int NEW = 111;
	public static int LT = 112;
	public static int GT = 113;
	public static int LEFTBRACKET = 114;
	public static int RIGHTBRACKET = 115;
	public static int THIS = 116;
	public static int LEFTBRACE = 117;
	public static int RIGHTBRACE = 118;
	public static int INTERROGATION = 119;// interrogation = ?
	public static int COLON = 120;
	public static int INSTANCEOF = 121;
	public static int SUPER = 122;
	public static int SEMICOLON = 123;
	public static int EQUAL = 124;
	public static int SPACE = 125;
	public static int MODIFIER = 126;
	public static int ELLIPSIS = 127;
	public static int THROWS = 128;
	public static int ASSERT = 129;
	public static int BREAK = 130;
	public static int CATCH = 131;
	public static int CONTINUE = 135;
	public static int DO = 136;
	public static int WHILE = 137;
	public static int FOR = 138;
	public static int IF = 139;
	public static int ELSE = 140;
	public static int RETURN = 141;
	public static int CASE = 142;
	public static int SWITCH = 143;
	public static int SYNCHRONIZED = 144;
	public static int THROW = 145;
	public static int TRY = 146;
	public static int FINALLY = 147;
	public static int LABEL = 148;
	public static int DEFAULT = 149;
	public static int ANONYMOUS_CLASS_DECLARATION = 150;
	public static int LIST = 151;
	public static int CLASS = 152;
	public static int UNKNOWN_STATEMENT = 153;
	public static int INFIX_OPERATORS = 154;
	public static int PREFIX_OPERATORS = 155;
	public static int POSTFIX_OPERATORS = 156;
	public static int LIST_LITERAL = 157;
	public static int THEN = 158;

	public static final String ABSTRACT_TYPE = "t$_";
	public static final String ABSTRACT_METHOD = "m$_";
	public static final String ABSTRACT_VARIABLE = "v$_";
	public static final String ABSTRACT_UNKNOWN = "u$_";
	public static final String ABSTRACT_STATEMENT = "s$_";
	public static final String ARGS_PRE = "args: ";

	private Node root;
	private Stack<Node> fNodeStack;
	//modified by Ying
	public Map<String, VariableTypeBindingTerm> variableTypeMap;
	private Map<String, TypeNameTerm> typeMap;
	private Map<String, MethodNameTerm> methodMap;
	public Map<String, Integer> local_variable_decid; // A map from the local variable's name to it's variable binding ID

	private int abstractVariableIndex, abstractTypeIndex, abstractMethodIndex;

	public JavaExpressionConverter() {
		init();
	}

	public void clear() {
		init();
	}

	private void init() {
		this.fNodeStack = new Stack<Node>();
		this.variableTypeMap = new HashMap<String, VariableTypeBindingTerm>();
		this.typeMap = new HashMap<String, TypeNameTerm>();
		this.methodMap = new HashMap<String, MethodNameTerm>();
		this.abstractVariableIndex = this.abstractTypeIndex = this.abstractMethodIndex = 0;
		this.local_variable_decid = new HashMap<String, Integer>();
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(ArrayAccess node) {
		// format: Expression[Expression]
		push(JavaEntityType.ARRAY_ACCESS, node);

		node.getArray().accept(this);

		pushOpNode("[");
		pop();

		node.getIndex().accept(this);// Expression inside

		pushOpNode("]");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(ArrayCreation node) {
		// for the reason that this is very complicated, I prefer to realize it
		// in a simpler way
		// new PrimitiveType [ Expression ] { [ Expression ] } { [ ] }
		// new TypeName [ Expression ] { [ Expression ] } { [ ] }
		// new PrimitiveType [ ] { [ ] } ArrayInitializer
		// new TypeName [ ] { [ ] } ArrayInitializer
		push(JavaEntityType.ARRAY_CREATION, node);

		pushReserveNode("new ");
		pop();

		int numOfDimensions = node.getType().getDimensions();
		Type elementType = node.getType().getElementType();
		elementType.accept(this);

		List<Expression> dimensions = node.dimensions();

		for (int i = 0; i < numOfDimensions; i++) {
			pushOpNode("[");
			pop();

			if (i < dimensions.size()) {
				dimensions.get(i).accept(this);
			}

			pushOpNode("]");
			pop();
		}

		if (node.getInitializer() != null) {
			node.getInitializer().accept(this);
		}

		pop();
		return false;
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		// format like: {[ Expression { , Expression} [ , ]]}
		push(JavaEntityType.ARRAY_INITIALIZER, node);

		pushOpNode("{");
		pop();

		List<ASTNode> list = node.expressions();
		visitList(list);

		pushOpNode("}");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(ArrayType node) {
		// format like: Type [ ]
		push(JavaEntityType.ARRAY_TYPE, node);

		node.getElementType().accept(this);

		pushOpNode("[");
		pop();

		pushOpNode("]");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(AssertStatement node) {
		// format like: assert Expression [ : Expression ] ;
		push(JavaEntityType.ASSERT_STATEMENT, node);

		pushReserveNode("assert ");
		pop();

		node.getExpression().accept(this);

		Expression message = node.getMessage();
		if (message != null) {
			pushOpNode(":");
			pop();

			message.accept(this);
		}

		pushOpNode(";");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(Assignment node) {
		push(JavaEntityType.ASSIGNMENT, node);

		node.getLeftHandSide().accept(this);

		pushOpNode(node.getOperator().toString());
		pop();

		node.getRightHandSide().accept(this);

		pop();
		return false;
	}

	@Override
	public boolean visit(Block node) {
		push(JavaEntityType.BLOCK, node);

		pushOpNode("{");
		pop();

		List<Statement> statements = node.statements();
		for (Statement stat : statements) {
			stat.accept(this);
		}

		pushOpNode("}");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(BooleanLiteral node) {
		ITypeBinding tBinding = node.resolveTypeBinding();
		String typeName = tBinding.getName();
		String qName = tBinding.getQualifiedName();
		pushValueTypeNode(String.valueOf(node.booleanValue()),
				typeName, qName, JavaEntityType.BOOLEAN_LITERAL, node);
		pop();
		return false;
	}

	@Override
	public boolean visit(BreakStatement node) {
		// format like:break [ Identifier ] ;
		push(JavaEntityType.BREAK_STATEMENT, node);

		pushReserveNode("break");
		pop();

		SimpleName label = node.getLabel();
		if (label != null) {
			pushOpNode(" ");
			pop();

			pushLabelNode(label.getIdentifier());
			pop();
		}

		pushOpNode(";");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(CastExpression node) {
		// (Type) Expression
		push(JavaEntityType.CAST_EXPRESSION, node);

		try {
			pushOpNode("(");
			pop();

			node.getType().accept(this);

			pushOpNode(")");
			pop();

			node.getExpression().accept(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		pop();
		return false;
	}

	// format like: catch ( FormalParameter ) Block
	@Override
	public boolean visit(CatchClause node) {
		push(JavaEntityType.CATCH_CLAUSE, node);

		pushReserveNode("catch");
		pop();

		pushOpNode("(");
		pop();

		node.getException().accept(this);

		pushOpNode(")");
		pop();

		node.getBody().accept(this);

		pop();
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral node) {
		ITypeBinding tBinding = node.resolveTypeBinding();
		String tName = tBinding.getName();
		String qName = tBinding.getQualifiedName();
		pushValueTypeNode(node.getEscapedValue(), tName, qName,
				JavaEntityType.CHARACTER_LITERAL, node);
		pop();
		return false;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		// format like "[ Expression . ] new Name
		// ( [ Expression { , Expression } ] )
		// [ AnonymousClassDeclaration ]--this part is only considered as string
		push(JavaEntityType.CLASS_INSTANCE_CREATION, node);
		IMethodBinding binding = node.resolveConstructorBinding();
		List<Integer> paramIndexes = new ArrayList<Integer>();
		int index = 0;

		try {
			if (node.getExpression() != null) {
				node.getExpression().accept(this);
				index++;
				pushOpNode(".");
				index++;
				pop();
			}

			pushReserveNode("new ");
			index++;
			pop();

			node.getType().accept(this);
			index++;

			pushOpNode("(");
			index++;
			pop();

			List<ASTNode> list = node.arguments();
			visitList(list);
			index = updateParamIndexes(list.size(), paramIndexes, index);

			List<Object> info = new ArrayList<Object>();
			info.add(binding);
			info.add(paramIndexes);
			getCurrentParent().setUserObject(info);

			pushOpNode(")");
			pop();

			AnonymousClassDeclaration anonymous = node.getAnonymousClassDeclaration();
			if (anonymous != null) {
				// do nothing temporarily
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		pop();
		return false;
	}

	@Override
	public boolean visit(ConditionalExpression node) {
		// format like "Expression ? Expression : Expression"
		push(JavaEntityType.CONDITIONAL_EXPRESSION, node);
		node.getExpression().accept(this);

		pushOpNode(" ? ");
		pop();

		node.getThenExpression().accept(this);
		pop();

		pushOpNode(" : ");
		pop();

		node.getElseExpression().accept(this);

		pop();
		return false;
	}

	// format like: [ < Type { , Type } > ]
	// this ( [ Expression { , Expression } ] ) ;
	@Override
	public boolean visit(ConstructorInvocation node) {
		push(JavaEntityType.CONSTRUCTOR_INVOCATION, node);
		IMethodBinding binding = node.resolveConstructorBinding();
		List<Integer> paramIndexes = new ArrayList<Integer>();
		int index = 0;
		List<ASTNode> list = node.typeArguments();
		visitTypeList(list);
		index = updateIndex(list.size(), index);

		pushOpNode(" ");
		index++;
		pop();

		pushReserveNode("this");
		index++;
		pop();

		pushOpNode("(");
		index++;
		pop();

		list = node.arguments();
		visitList(list);
		index = updateParamIndexes(list.size(), paramIndexes, index);

		List<Object> info = new ArrayList<Object>();
		info.add(binding);
		info.add(paramIndexes);
		try {
			getCurrentParent().setUserObject(info);
		}
		catch (Exception e) {
			System.out.println(e);
		}
		finally {
			pushOpNode(")");
			pop();

			pushOpNode(";");
			pop();

			pop();
			return false;
		}
	}

	@Override
	public boolean visit(ContinueStatement node) {
		push(JavaEntityType.CONTINUE_STATEMENT, node);
		pushReserveNode("continue");
		pop();

		SimpleName label = node.getLabel();
		if (label != null) {
			pushOpNode(" ");
			pop();
			pushLabelNode(label.getIdentifier());
			pop();
		}

		pushOpNode(";");
		pop();

		pop();
		return false;
	}

	// format like: do Statement while ( Expression ) ;
	@Override
	public boolean visit(DoStatement node) {
		push(JavaEntityType.DO_STATEMENT, node);

		pushReserveNode("do");
		pop();

		node.getBody().accept(this);

		pushReserveNode("while");
		pop();

		pushOpNode("(");
		pop();

		node.getExpression().accept(this);

		pushOpNode(")");
		pop();

		pushOpNode(";");
		pop();

		pop();
		return false;
	}

	// format like: for ( FormalParameter : Expression )Statement
	@Override
	public boolean visit(EnhancedForStatement node) {
		push(JavaEntityType.ENHANCED_FOR_STATEMENT, node);
		pushReserveNode("for");
		pop();

		pushOpNode("(");
		pop();

		node.getParameter().accept(this);

		pushOpNode(":");
		pop();

		node.getExpression().accept(this);

		pushOpNode(")");
		pop();

		node.getBody().accept(this);

		pop();
		return false;
	}

	// format like: StatementExpression ;
	@Override
	public boolean visit(ExpressionStatement node) {
		push(JavaEntityType.EXPRESSION_STATEMENT, node);

		node.getExpression().accept(this);

		pushOpNode(";");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(FieldAccess node) {
		// format like: Expression . Identifier
		push(JavaEntityType.FIELD_ACCESS, node);

		if (node.getExpression() != null) {
			node.getExpression().accept(this);

			pushOpNode(".");
			pop();
		}

		node.getName().accept(this);

		pop();
		return false;
	}

	/*
	 * format like: ForStatement: for ( [ ForInit ]; [ Expression ] ; [
	 * ForUpdate ] ) Statement ForInit: Expression { , Expression } ForUpdate:
	 * Expression { , Expression }
	 */
	@Override
	public boolean visit(ForStatement node) {
		push(JavaEntityType.FOR_STATEMENT, node);

		pushReserveNode("for");
		pop();

		pushOpNode("(");
		pop();

		visitList(node.initializers());

		pushOpNode(";");
		pop();

		node.getExpression().accept(this);

		pushOpNode(";");
		pop();

		visitList(node.updaters());

		pushOpNode(")");
		pop();

		node.getBody().accept(this);

		pop();
		return false;
	}

	// format like: if ( Expression ) Statement [ else Statement]
	@Override
	public boolean visit(IfStatement node) {
		push(JavaEntityType.IF_STATEMENT, node);

		pushReserveNode("if");
		pop();

		pushOpNode("(");
		pop();

		node.getExpression().accept(this);

		pushOpNode(")");
		pop();

		pushReserveNode("then");
		pop();

		node.getThenStatement().accept(this);

		Statement elseStat = node.getElseStatement();
		if (elseStat != null) {
			pushReserveNode("else");
			pop();

			elseStat.accept(this);
		}

		pop();
		return false;
	}

	@Override
	public boolean visit(InfixExpression node) {
		// format like
		// "Expression InfixOperator Expression {InfixOperation Expression}"
		push(JavaEntityType.INFIX_EXPRESSION, node);

		node.getLeftOperand().accept(this);

		pushOpNode(node.getOperator().toString());
		pop();

		node.getRightOperand().accept(this);

		if (node.hasExtendedOperands()) {
			List<ASTNode> list = node.extendedOperands();
			for (ASTNode nodeInList : list) {
				pushOpNode(node.getOperator().toString());
				pop();
				nodeInList.accept(this);
			}
		}

		pop();
		return false;
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		// format like: Expression instanceof Type
		push(JavaEntityType.INSTANCEOF_EXPRESSION, node);

		node.getLeftOperand().accept(this);

		pushReserveNode("instanceof");
		pop();

		node.getRightOperand().accept(this);

		pop();
		return false;
	}

	// format like: Identifier : Statement
	@Override
	public boolean visit(LabeledStatement node) {
		push(JavaEntityType.LABELED_STATEMENT, node);

		pushLabelNode(node.getLabel().getIdentifier());
		pop();

		pushOpNode(":");
		pop();

		node.getBody().accept(this);

		pop();
		return false;
	}

	// format like "MethodDeclaration:
	// [ Javadoc ] { Modifier } ( Type | void ) Identifier (
	// [ FormalParameter
	// { , FormalParameter } ] ) {[ ] }
	// [ throws TypeName { , TypeName } ] ( Block | ; )"
	@Override
	public boolean visit(MethodDeclaration node) {
		push(JavaEntityType.METHOD_DECLARATION, node);

		node.getReturnType2().accept(this);

		pushOpNode(" ");
		pop();

		SimpleName mName = node.getName();
		IMethodBinding binding = node.resolveBinding();
		pushMethodNode(mName.getNodeType(), mName.toString(), node, binding);

		pushOpNode("(");
		pop();

		visitList(node.parameters());

		pushOpNode(")");
		pop();

		// [][]
		int extraDimensions = node.getExtraDimensions();
		for (int i = 0; i < extraDimensions; i++) {
			pushOpNode("[");
			pop();

			pushOpNode("]");
			pop();
		}

		pushOpNode(" ");
		pop();

		// exceptions
		try {
			List<ASTNode> exceptions = node.thrownExceptions();
			if (exceptions != null) {
				pushReserveNode("throws ");
				pop();

				visitTypeList(exceptions);
			}

			node.getBody().accept(this);

			pop();
		}
		finally {
			return false;
		}
	}

	@Override
	public boolean visit(MethodInvocation node) {
		// format like "MethodInvocation:
		// [ Expression . ]
		// [ < Type { , Type } > ]
		// Identifier ( [ Expression { , Expression } ] )		
		push(JavaEntityType.METHOD_INVOCATION, node);
		IMethodBinding binding = node.resolveMethodBinding();
		List<Integer> paramIndexes = new ArrayList<Integer>();
		int index = 0;

		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			paramIndexes.add(index);
			index++;

			pushOpNode(".");
			index++;
			pop();
		}

		if (node.typeArguments().size() > 0) {
			pushOpNode("<");
			index++;
			pop();

			List<ASTNode> list = node.typeArguments();
			visitTypeList(list);
			index+= list.size() * 2 - 1;
			pushOpNode(">");
			index++;
			pop();
		}

		SimpleName mName = node.getName();
		pushMethodNode(mName.getNodeType(), mName.toString(), node, binding);
		index++;
		pop();

		pushOpNode("(");
		index++;
		pop();

		//add by Ying, get the node arguments
		System.out.println("node.arguments" + node.arguments());
		visitList(node.arguments());
		index = updateParamIndexes(node.arguments().size(), paramIndexes, index);

		List<Object> info = new ArrayList<Object>();
		info.add(binding);
		info.add(paramIndexes);
		try {
			getCurrentParent().setUserObject(info);
		}
		catch (Exception e) {
			System.out.println(e);
		}
		finally {
			pushOpNode(")");
			pop();

			pop();
			return false;
		}
	}

	@Override
	public boolean visit(NullLiteral node) {
		pushReserveNode("null");
		pop();
		return false;
	}

	@Override
	public boolean visit(NumberLiteral node) {
		ITypeBinding tBinding = node.resolveTypeBinding();
		String tName = tBinding.getName();
		String qName = tName;
		pushValueTypeNode(node.getToken(), tName, qName,
				JavaEntityType.NUMBER_LITERAL, node);
		pop();
		return false;
	}

	@Override
	public boolean visit(ParameterizedType node) {
		pushTypeNode(node.resolveBinding(), node);
		pop();
		return false;
	}

	@Override
	public boolean visit(ParenthesizedExpression node) {
		// format like: (Expression)
		push(JavaEntityType.PARAMETERIZED_TYPE, node);

		pushOpNode("(");
		pop();

		node.getExpression().accept(this);

		pushOpNode(")");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(PostfixExpression node) {
		push(JavaEntityType.POSTFIX_EXPRESSION, node);

		node.getOperand().accept(this);

		pushOpNode(node.getOperator().toString());
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(PrefixExpression node) {
		// format like : PrefixOperator Expression
		push(JavaEntityType.PREFIX_EXPRESSION, node);

		pushOpNode(node.getOperator().toString());
		pop();

		node.getOperand().accept(this);

		pop();
		return false;
	}

	@Override
	public boolean visit(QualifiedName node) {
		push(JavaEntityType.QUALIFIED_NAME, node);
		IBinding binding = node.resolveBinding();
		this.getCurrentParent().setUserObject(binding);

		node.getQualifier().accept(this);

		pushOpNode(".");
		pop();

		node.getName().accept(this);

		pop();
		return false;
	}

	@Override
	public boolean visit(QualifiedType node) {
		// format like: QualifiedType:
		// Type . SimpleName
		pushTypeNode(node.resolveBinding(), node);
		pop();
		return false;
	}

	// format like: return [ Expression ] ;
	@Override
	public boolean visit(ReturnStatement node) {
		push(JavaEntityType.RETURN_STATEMENT, node);

		pushReserveNode("return");
		pop();

		Expression expr = node.getExpression();
		if (expr != null) {
			pushOpNode(" ");
			pop();
			expr.accept(this);
		}

		pushOpNode(";");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(SimpleName node) {
		ITypeBinding tBinding = node.resolveTypeBinding();
		if (tBinding == null) {
			VariableTypeBindingTerm vTerm = pushValueTypeNode(node.getIdentifier(), node.getIdentifier(),
					node.getIdentifier(),
					JavaEntityType.SIMPLE_NAME, node);
			vTerm.binding = node.resolveBinding();
		} else {
			if (node.getIdentifier().equals(tBinding.getName())) {
				pushTypeNode(tBinding, node);
			} else {
				VariableTypeBindingTerm vTerm = pushValueTypeNode(node.getIdentifier(), tBinding.getName(),
						tBinding.getQualifiedName(),
						JavaEntityType.SIMPLE_NAME, node);
				vTerm.binding = node.resolveBinding();
				// Feb-26, 2018, Shengzhe
				String xyz;
				try {
					xyz = vTerm.binding.toString();
				}
				catch (Exception e) {
					xyz = "cantfind";
				}
				Integer idno = get_decid_map(xyz);
				if (!idno.equals(Integer.valueOf(-1))) {
					local_variable_decid.put(vTerm.getName(), idno );
//					System.out.println("extracted pos-id: " + vTerm.getName() + " " +node.getStartPosition() + " -> "+ idno);
				}
			}
		}
		pop();
		return false;
	}

	// to fix: get the id from the vbinding by getsubstring
	public Integer get_decid_map(String vbinding) {
		boolean lable = false; String result = "";
		for (int i = 2; i < vbinding.length(); i++) {
			if (vbinding.charAt(i-2) == 'i' && vbinding.charAt(i-1) == 'd' && vbinding.charAt(i) == ':') {
				lable = true;
				continue;
			}
			if (vbinding.charAt(i) == ']') {
				lable = false;
				continue;
			}
			if (lable == true) {
				result += vbinding.charAt(i);
			}
		}
		if (StringUtils.isNumeric(result)) {
			return Integer.valueOf(result);
		}
		else {
			return Integer.valueOf("-1");
		}
	}

	@Override
	public boolean visit(SimpleType node) {
		pushTypeNode(node.resolveBinding(), node);
		pop();
		return false;
	}

	// format like: SingleVariableDeclaration:
	// { ExtendedModifier } Type [ ... ] Identifier { [] } [ = Expression ]
	@Override
	public boolean visit(SingleVariableDeclaration node) {
		push(JavaEntityType.SINGLE_VARIABLE_DECLARATION, node);
		// Type
		node.getType().accept(this);
		// [...]
		if (node.isVarargs()) {
			pushOpNode("... ");
			pop();
		}
		// Identifier
		node.getName().accept(this);
		// extra dimensions
		int dimensions = node.getExtraDimensions();
		for (int i = 0; i < dimensions; i++) {
			pushOpNode("[");
			pop();

			pushOpNode("]");
			pop();
		}
		// initializer
		Expression init = node.getInitializer();
		if (init != null) {
			pushOpNode("=");
			pop();
			init.accept(this);
		}
		pop();
		return false;
	}

	@Override
	public boolean visit(StringLiteral node) {
		ITypeBinding tBinding = node.resolveTypeBinding();
		String tName = tBinding.getName();
		String qName = tBinding.getQualifiedName();
		pushValueTypeNode(node.getEscapedValue(), tName, qName,
				JavaEntityType.STRING_LITERAL, node);
		pop();
		return false;
	}

	/*
	 * format like: [ Expression . ] [ < Type { , Type } > ] super ( [
	 * Expression { , Expression } ] ) ;
	 */
	@Override
	public boolean visit(SuperConstructorInvocation node) {
		push(JavaEntityType.SUPER_CONSTRUCTOR_INVOCATION, node);
		IMethodBinding binding = node.resolveConstructorBinding();
		List<Integer> paramIndexes = new ArrayList<Integer>();
		int index = 0;
		Expression expr = node.getExpression();
		if (expr != null) {
			expr.accept(this);
			index++;

			pushOpNode(".");
			index++;
			pop();
		}
		// <Type {, Type}>
		List<ASTNode> types = node.typeArguments();
		if (types != null && types.size() > 0) {
			pushOpNode("<");
			index++;
			pop();

			visitTypeList(types);
			index += types.size() * 2 - 1;

			pushOpNode(">");
			index++;
			pop();
		}
		pushReserveNode("super");
		index++;
		pop();

		pushOpNode("(");
		index++;
		pop();

		visitList(node.arguments());
		index = updateParamIndexes(node.arguments().size(), paramIndexes, index);
		List<Object> info = new ArrayList<Object>();
		info.add(binding);
		info.add(paramIndexes);
		getCurrentParent().setUserObject(info);

		pushOpNode(")");
		pop();

		pushOpNode(";");
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		// format like:
		// SuperFieldAccess:
		// [ ClassName . ] super . Identifier
		push(JavaEntityType.SUPER_FIELD_ACCESS, node);

		Name qualifier = node.getQualifier();
		if (qualifier != null) {
			pushTypeNode(qualifier.resolveTypeBinding(), qualifier);
			pop();

			pushOpNode(".");
			pop();
		}

		pushReserveNode("super");
		pop();

		pushOpNode(".");
		pop();

		SimpleName className = node.getName();
		pushTypeNode(className.resolveTypeBinding(), className);
		pop();

		pop();
		return false;
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		// format like: [ ClassName . ] super .
		// [ < Type { , Type } > ]
		// Identifier ( [ Expression { , Expression } ] )
		push(JavaEntityType.SUPER_METHOD_INVOCATION, node);
		IMethodBinding binding = node.resolveMethodBinding();
		List<Integer> paramIndexes = new ArrayList<Integer>();
		int index = 0;

		Name qualifier = node.getQualifier();
		if (qualifier != null) {
			pushTypeNode(qualifier.resolveTypeBinding(), qualifier);
			index++;
			pop();

			pushOpNode(".");
			index++;
			pop();
		}

		pushReserveNode("super");
		index++;
		pop();

		pushOpNode(".");
		index++;
		pop();

		if (node.typeArguments().size() > 0) {
			pushOpNode("<");
			index++;
			pop();

			visitTypeList(node.typeArguments());
			index+= node.typeArguments().size() * 2 - 1;

			pushOpNode(">");
			index++;
			pop();
		}

		SimpleName mName = node.getName();
		pushMethodNode(mName.getNodeType(), mName.getIdentifier(), node, binding);
		index++;
		pop();

		pushOpNode("(");
		index++;
		pop();

		visitList(node.arguments());
		index = updateParamIndexes(node.arguments().size(), paramIndexes, index);
		List<Object> info = new ArrayList<Object>();
		info.add(binding);
		info.add(paramIndexes);
		getCurrentParent().setUserObject(info);

		pushOpNode(")");
		pop();

		pop();
		return false;
	}

	/*
	 * case Expression : default :
	 */
	@Override
	public boolean visit(SwitchCase node) {
		push(JavaEntityType.SWITCH_CASE, node);

		boolean isDefault = node.isDefault();
		if (isDefault) {
			pushReserveNode("default");
			pop();
		} else {
			pushReserveNode("case ");
			pop();

			node.getExpression().accept(this);
		}
		pushOpNode(":");
		pop();

		pop();
		return false;
	}

	/*
	 * format like: switch ( Expression ) { { SwitchCase | Statement } } }
	 */
	@Override
	public boolean visit(SwitchStatement node) {
		push(JavaEntityType.SWITCH_STATEMENT, node);

		pushReserveNode("switch");
		pop();

		pushOpNode("(");
		pop();

		node.getExpression().accept(this);

		pushOpNode(")");
		pop();

		pushOpNode("{");
		pop();

		visitList(node.statements());

		pushOpNode("}");
		pop();

		pop();
		return false;
	}

	// format like: synchronized ( Expression ) Block
	@Override
	public boolean visit(SynchronizedStatement node) {
		push(JavaEntityType.SYNCHRONIZED_STATEMENT, node);

		pushReserveNode("synchronized");
		pop();

		pushOpNode("(");
		pop();

		node.getExpression().accept(this);

		pushOpNode(")");
		pop();

		node.getBody().accept(this);

		pop();
		return false;
	}

	@Override
	public boolean visit(ThisExpression node) {
		// format like: [ClassName.]this
		push(JavaEntityType.THIS_EXPRESSION, node);

		Name qualifier = node.getQualifier();
		if (qualifier != null) {
			pushTypeNode(qualifier.resolveTypeBinding(), qualifier);
			pop();

			pushOpNode(".");
			pop();
		}
		pushReserveNode("this");
		pop();

		pop();
		return false;
	}

	// format like: throw Expression ;
	@Override
	public boolean visit(ThrowStatement node) {
		push(JavaEntityType.THROW_STATEMENT, node);

		pushReserveNode("throw ");
		pop();

		node.getExpression().accept(this);

		pushOpNode(";");
		pop();

		pop();
		return false;
	}

	/*
	 * format like: try Block { CatchClause } [ finally Block ]
	 */
	@Override
	public boolean visit(TryStatement node) {
		push(JavaEntityType.TRY_STATEMENT, node);

		pushReserveNode("try");
		pop();

		node.getBody().accept(this);

		List<ASTNode> catches = node.catchClauses();
		for (ASTNode catchClause : catches) {
			catchClause.accept(this);
		}

		pushReserveNode("finally");
		pop();

		if (node != null && node.getFinally() != null)
			node.getFinally().accept(this);

		pop();
		return false;
	}

	@Override
	public boolean visit(TypeLiteral node) {
		// format like: (Type|void).class
//		if (node.getType()!=null) {
//			pushTypeNode(qualifier.resolveTypeBinding(), qualifier);
//			pop();
//			
//			pushOpNode(".");
//			pop();
//		}
		System.out.println(node);
		if (node.getType()!=null) {
			pushTypeNode(node.getType().resolveBinding(), node.getType());
			pop();

			pushOpNode(".");
			pop();
		}
		pushTypeNode(node.resolveTypeBinding(), node);
		pop();
		return false;
	}
	//added by shengzx Aug-24
//	public void endVisit(TypeLiteral node) {
//		if (node!=null && node.getType()!=null) {
//			pushOpNode(".");
//			index++;
//			pop();
//		}
//		pushTypeNode(node.resolveTypeBinding(), node);
//		pop();
//	}

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		// format like: {ExtendedModifier}Type VariableDeclarationFragment{,
		// VariableDeclarationFragment}
		// since the structure is complicated, and it is uncommon; I don't parse
		// out information carefully from it
		push(JavaEntityType.VARIABLE_DECLARATION_EXPRESSION, node);

		node.getType().accept(this);

		pushOpNode(" ");
		pop();

		visitList(node.fragments());

		pop();
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		push(JavaEntityType.VARIABLE_DECLARATION_FRAGMENT, node);

		node.getName().accept(this);

		Expression initializer = node.getInitializer();
		if (initializer != null) {
			pushOpNode("=");
			pop();

			initializer.accept(this);
		}
		pop();
		return false;
	}

	/**
	 * { ExtendedModifier } Type VariableDeclarationFragment { ,
	 * VariableDeclarationFragment } ;
	 */
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		push(JavaEntityType.VARIABLE_DECLARATION_STATEMENT, node);

		List<IExtendedModifier> modifiers = node.modifiers();
		if (modifiers == null || modifiers.size() == 0) {
			// do nothing
		} else {
			for (int i = 0; i < modifiers.size(); i++) {
				pushReserveNode(((Modifier)modifiers.get(i)).getKeyword().toString());
				pop();

				pushOpNode(" ");
				pop();
			}
		}
		node.getType().accept(this);

		pushOpNode(" ");
		pop();

		visitList(node.fragments());

		pop();
		return false;
	}

	// format like: while ( Expression ) Statement
	@Override
	public boolean visit(WhileStatement node) {
		push(JavaEntityType.WHILE_STATEMENT, node);

		pushReserveNode("while");
		pop();

		pushOpNode("(");
		pop();

		node.getExpression().accept(this);

		pushOpNode(")");
		pop();

		node.getBody().accept(this);

		pop();
		return false;
	}

	public Node getRoot() {
		return root;
	}

	private Node getCurrentParent() {
		if (!fNodeStack.isEmpty())
			return fNodeStack.peek();
		else return null;
	}

	private TypeNameTerm getTypeNameTerm(String typeName, String qName) {
		return getTypeNameTerm(typeName, qName, 0);
	}

	private TypeNameTerm getTypeNameTerm(String typeName, String qName,
										 int nodeType) {
		TypeNameTerm tTerm = null;
		for (Entry<String, TypeNameTerm> entry : typeMap.entrySet()) {
			if (entry.getValue().getQualifiedName().equals(qName)) {
				tTerm = entry.getValue();
				break;
			}
		}
		if (tTerm == null) {
			for (Entry<String, TypeNameTerm> entry : typeMap.entrySet()) {
				if (entry.getKey().equals(typeName)) {
					tTerm = entry.getValue();
					break;
				}
			}
		}
		if (tTerm == null) {
			tTerm = new TypeNameTerm(nodeType, typeName, qName);
			tTerm.setAbstractTypeName(Term.createAbstractName(ABSTRACT_TYPE,
					this.abstractTypeIndex++));
			typeMap.put(typeName, tTerm);
		}
		return tTerm;
	}



	private void push(JavaEntityType et, ASTNode node) {
		String label = Term.IndexToExpr.get(node.getNodeType());
		push(et, label, node);
	}

	private Node push(JavaEntityType et, String label, ASTNode node) {
		Node n = new Node(et, label);
		SourceCodeEntity entity = new SourceCodeEntity();
		entity.setSourceRange(new SourceRange(node.getStartPosition(), node.getLength()));
		n.setEntity(entity);
		if (!fNodeStack.isEmpty()) {
			getCurrentParent().add(n);
		} else {
			root = n;
		}
		fNodeStack.push(n);
		return n;
	}

	private void pushLabelNode(String label) {
		Node n = new Node(JavaEntityType.LABEL, label);
		pushNodeDefault(n);
	}

	private void pushMethodNode(int nodeType, String label, ASTNode node,
								IMethodBinding binding) {
		MethodNameTerm mTerm = methodMap.get(label);
		if (mTerm == null) {
			mTerm = new MethodNameTerm(nodeType, label,
					Term.createAbstractName(ABSTRACT_METHOD,
							this.abstractMethodIndex++));
			mTerm.setMethodBinding(binding);
			methodMap.put(label, mTerm);
		}
		Node n = push(JavaEntityType.METHOD, label, node);
		n.setUserObject(mTerm);
	}

	private void pushNodeDefault(Node n) {
		SourceCodeEntity entity = new SourceCodeEntity();
		entity.setSourceRange(new SourceRange(0, 0));
		n.setEntity(entity);
		if (!fNodeStack.isEmpty()) {
			getCurrentParent().add(n);
		}	else {
			root = n;
		}
		fNodeStack.push(n);
	}

	private void pushOpNode(String label) {
		Node n = new Node(JavaEntityType.OP, label);
		pushNodeDefault(n);
	}

	private void pushReserveNode(String label) {
		Node n = new Node(JavaEntityType.RESERVE, label);
		pushNodeDefault(n);
	}

	private void pushTypeNode(ITypeBinding tBinding, ASTNode node) {
		String tName = tBinding.getName();
		String qName = tBinding.getQualifiedName();
		TypeNameTerm tTerm = getTypeNameTerm(tName, qName, node.getNodeType());
		tTerm.setTypeBinding(tBinding);
		Node n = push(JavaEntityType.TYPE_PARAMETER, tName, node);
		n.setUserObject(tTerm);
	}

	private VariableTypeBindingTerm pushValueTypeNode(String vName, String tName, String qName,
													  JavaEntityType et,
													  ASTNode node) {
		String keyValue = vName + "+" + tName;
		VariableTypeBindingTerm vTerm = variableTypeMap.get(keyValue);
		if (vTerm == null) {
			vTerm = new VariableTypeBindingTerm(node.getNodeType(), vName,
					Term.createAbstractName(ABSTRACT_VARIABLE,
							this.abstractVariableIndex++));
			TypeNameTerm tTerm = getTypeNameTerm(tName, qName);
			vTerm.setTypeNameTerm(tTerm);
			variableTypeMap.put(keyValue, vTerm);
		}
		Node n = push(et, vName, node);
		n.setUserObject(vTerm);
		return vTerm;
	}

	private void pop() {
		if (!fNodeStack.isEmpty())
			fNodeStack.pop();
	}

	private void visitList(List<ASTNode> list) {
		int numOfComma = list.size() - 1;
		int counter = 0;
		for (ASTNode nodeInList : list) {
			// if(nodeInList instanceof Expression){
			nodeInList.accept(this); // Argument'n'
			if (counter++ < numOfComma) {
				pushOpNode(",");// ,
				pop();
			}
			// }
		}
	}

	private void visitTypeList(List<ASTNode> typeList) {
		int numOfComma = typeList.size() - 1;
		int counter = 0;
		for (ASTNode type : typeList) {
			Type t = (Type)type;
			pushTypeNode(t.resolveBinding(), t);
			pop();
			if (counter++ < numOfComma) {
				pushOpNode(",");
				pop();
			}
		}
	}

	public MethodNameTerm getMethodNameTerm(String apiName) {
		return methodMap.get(apiName);
	}

	public VariableTypeBindingTerm getVariableTerm(String vKey) {
		return variableTypeMap.get(vKey);
	}

	private int updateParamIndexes(int argNum, List<Integer> paramIndexes, int index) {
		if (argNum != 0) {
			if (argNum == 1) {
				paramIndexes.add(index++);
			} else {
				paramIndexes.add(index++);
				for (int i = 1; i < argNum; i++) {
					index++;
					paramIndexes.add(index++);
				}
			}
		}
		return index;
	}

	private int updateIndex(int argNum, int index) {
		if (argNum != 0) {
			index += argNum * 2 - 1;
		}
		return index;
	}

	public static void markSubStmts(Node n, ASTNode astNode) {
		if (n == null) {
			return;
		}
		int childCount = n.getChildCount();
		if (childCount < 2)
			return;
		Enumeration<Node> cEnum = n.children();
		int index = 0;
		Node child = null;
		n.subStmtStarts = new ArrayList<Integer>();
		n.subStmtEnds = new ArrayList<Integer>();
		switch(astNode.getNodeType()) {
			case ASTNode.DO_STATEMENT:
				cEnum.nextElement();
				index++;
				n.subStmtStarts.add(index);
				while(cEnum.hasMoreElements()) {
					child = cEnum.nextElement();
					if (child == null || child.getValue() == null) continue;
					if (child.getValue().equals("while"))
						break;
					index++;
				}
				n.subStmtEnds.add(index);
				break;
			case ASTNode.TRY_STATEMENT:
				cEnum.nextElement();
				index++;
				n.subStmtStarts.add(index);//try
				n.subStmtEnds.add(index);
				do {
					child = cEnum.nextElement();
					index++;
					if (child == null || child.getValue() == null) continue;
					if (!child.getValue().equals("finally")) {
						n.subStmtStarts.add(index);
						n.subStmtEnds.add(index);
					} else {
						break;
					}
				} while(cEnum.hasMoreElements());
				if (cEnum.hasMoreElements()) {
					index++;
					n.subStmtStarts.add(index);
					n.subStmtEnds.add(index);
				}
				break;
			case ASTNode.ENHANCED_FOR_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.IF_STATEMENT:
			case ASTNode.SWITCH_STATEMENT:
			case ASTNode.SYNCHRONIZED_STATEMENT:
			case ASTNode.WHILE_STATEMENT:
				while(cEnum.hasMoreElements()) {
					child = cEnum.nextElement();
					if (child == null || child.getValue() == null) continue;
					index++;
					if (child.getValue().equals(")"))
						break;
				}
				n.subStmtStarts.add(index);
				n.subStmtEnds.add(index);
				break;
			default:
				n.subStmtStarts = null;
				n.subStmtEnds = null;
		}
	}

	public static List<Node> getChildren(Node n) {
		Enumeration<Node> cEnum = n.children();
		List<Node> list = new ArrayList<Node>();
		while (cEnum.hasMoreElements()) {
			list.add(cEnum.nextElement());
		}
		return list;
	}
}
