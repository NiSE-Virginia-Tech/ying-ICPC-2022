package edu.vt.cs.append;

public class ASTUtil {
	public static int NULL_TYPE = 999;
	public static int UNKNOWN_TYPE = 998;
	
	public static final int THEN_TYPE = 200;
	public static final int ELSE_TYPE = 201;
	public static final int DEFAULT_CASE_TYPE = 202;
	public static final int TRY_TYPE = 203;
	public static final int FINALLY_TYPE = 204;
	public static final int CATCH_CLAUSES = 205;
	
	public static final int FRAGMENTS_TYPE = 300;
	public static final int MODIFIERS_TYPE = 301;
	public static final int PARAMETERS_TYPE = 302;
	public static final int SUPER_INTERFACE_TYPES_TYPE = 303;
	public static final int THROWS_TYPE = 304;	
	public static final int TYPE_ARGUMENTS_TYPE = 305;	
	
	public static String convertToStringTypeName(int nodeType){
		String typeName = "";
		switch(nodeType){
//		case 1: typeName = "MALFORMED"; break;
		case 1: typeName = "ANONYMOUS_CLASS_DECLARATION"; break;
//		case 2: typeName = "ORIGINAL"; break;
		case 2: typeName = "ARRAY_ACCESS"; break;
		case 3: typeName = "ARRAY_CREATION"; break;
//		case 4: typeName = "PROTECT"; break;
		case 4: typeName = "ARRAY_INITIALIZER"; break;
		case 5: typeName = "ARRAY_TYPE"; break;
		case 6: typeName = "ASSERT_STATEMENT"; break;
		case 7: typeName = "ASSIGNMENT"; break;
//		case 8: typeName = "RECOVERED"; break;
		case 8: typeName = "BLOCK"; break;
		case 9: typeName = "BOOLEAN_LITERAL"; break;
		case 10:typeName = "BREAK_STATEMENT"; break;
		case 11:typeName = "CAST_EXPRESSION"; break;
		case 12:typeName = "CATCH_CLAUSE"; break;
		case 13:typeName = "CHARACTER_LITERAL"; break;
		case 14:typeName = "CLASS_INSTANCE_CREATION"; break;
		case 15:typeName = "COMPILATION_UNIT"; break;
		case 16:typeName = "CONDITIONAL_EXPRESSION"; break;
		case 17:typeName = "CONSTRUCTOR_INVOCATION"; break;
		case 18:typeName = "CONTINUE_STATEMENT"; break;
		case 19:typeName = "DO_STATEMENT"; break;
		case 20:typeName = "EMPTY_STATEMENT"; break;
		case 21:typeName = "EXPRESSION_STATEMENT"; break;
		case 22:typeName = "FIELD_ACCESS"; break;
		case 23:typeName = "FIELD_DECLARATION"; break;
		case 24:typeName = "FOR_STATEMENT"; break;
		case 25:typeName = "IF_STATEMENT"; break;
		case 26:typeName = "IMPORT_DECLARATION"; break;
		case 27:typeName = "INFIX_EXPRESSION"; break;
		case 28:typeName = "INITIALIZER"; break;
		case 29:typeName = "JAVADOC"; break;
		case 30:typeName = "LABELED_STATEMENT"; break;
		case 31:typeName = "METHOD_DECLARATION"; break;
		case 32:typeName = "METHOD_INVOCATION"; break;
		case 33:typeName = "NULL_LITERAL"; break;
		case 34:typeName = "NUMBER_LITERAL"; break;
		case 35:typeName = "PACKAGE_DECLARATION"; break;
		case 36:typeName = "PARENTHESIZED_EXPRESSION"; break;
		case 37:typeName = "POSTFIX_EXPRESSION"; break;
		case 38:typeName = "PREFIX_EXPRESSION"; break;
		case 39:typeName = "PRIMITIVE_TYPE"; break;
		case 40:typeName = "QUALIFIED_NAME"; break;
		case 41:typeName = "RETURN_STATEMENT"; break;
		case 42:typeName = "SIMPLE_NAME"; break;
		case 43:typeName = "SIMPLE_TYPE"; break;
		case 44:typeName = "SINGLE_VARIABLE_DECLARATION"; break;
		case 45:typeName = "STRING_LITERAL"; break;
		case 46:typeName = "SUPER_CONSTRUCTOR_INVOCATION"; break;
		case 47:typeName = "SUPER_FIELD_ACCESS"; break;
		case 48:typeName = "SUPER_METHOD_INVOCATION"; break;
		case 49:typeName = "SWITCH_CASE"; break;
		case 50:typeName = "SWITCH_STATEMENT"; break;
		case 51:typeName = "SYNCHRONIZED_STATEMENT"; break;
		case 52:typeName = "THIS_EXPRESSION"; break;
		case 53:typeName = "THROW_STATEMENT"; break;
		case 54:typeName = "TRY_STATEMENT"; break;
		case 55:typeName = "TYPE_DECLARATION"; break;
		case 56:typeName = "TYPE_DECLARATION_STATEMENT"; break;
		case 57:typeName = "TYPE_LITERAL"; break;
		case 58:typeName = "VARIABLE_DECLARATION_EXPRESSION"; break;
		case 59:typeName = "VARIABLE_DECLARATION_FRAGMENT"; break;
		case 60:typeName = "VARIABLE_DECLARATION_STATEMENT"; break;
		case 61:typeName = "WHILE_STATEMENT"; break;
		case 62:typeName = "INSTANCEOF_EXPRESSION"; break;
		case 63:typeName = "LINE_COMMENT"; break;
		case 64:typeName = "BLOCK_COMMENT"; break;
		case 65:typeName = "TAG_ELEMENT"; break;
		case 66:typeName = "TEXT_ELEMENT"; break;
		case 67:typeName = "MEMBER_REF"; break;
		case 68:typeName = "METHOD_REF"; break;
		case 69:typeName = "METHOD_REF_PARAMETER"; break;
		case 70:typeName = "ENHANCED_FOR_STATEMENT"; break;
		case 71:typeName = "ENUM_DECLARATION"; break;
		case 72:typeName = "ENUM_CONSTANT_DECLARATION"; break;
		case 73:typeName = "TYPE_PARAMETER"; break;
		case 74:typeName = "PARAMETERIZED_TYPE"; break;
		case 75:typeName = "QUALIFIED_TYPE"; break;
		case 76:typeName = "WILDCARD_TYPE"; break;
		case 77:typeName = "NORMAL_ANNOTATION"; break;
		case 78:typeName = "MARKER_ANNOTATION"; break;
		case 79:typeName = "SINGLE_MEMBER_ANNOTATION"; break;
		case 80:typeName = "MEMBER_VALUE_PAIR"; break;
		case 81:typeName = "ANNOTATION_TYPE_DECLARATION"; break;
		case 82:typeName = "ANNOTATION_TYPE_MEMBER_DECLARATION"; break;
		case 83:typeName = "MODIFIER"; break;
		
		case 100: typeName = "FRAGMENTS"; break;
		case 101: typeName = "MODIFIERS"; break;
		case 102: typeName = "PARAMETERS"; break;
		case 103: typeName = "SUPER_INTERFACE_TYPES"; break;
		case 104: typeName = "THROWS";	break;	
		case 105: typeName = "TYPE_ARGUMENTS"; break;		
		
		case 998: typeName = "UNKNOWN"; break;
		case 999: typeName = "NULL"; break;
		
		}
		return typeName;
	}

}
