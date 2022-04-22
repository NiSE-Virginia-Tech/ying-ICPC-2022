package ch.uzh.ifi.seal.changedistiller.model.classifiers.java;

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

import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;

/**
 * Implementation of {@link EntityType} for the Java language.
 * 
 * @author Beat Fluri
 */
public enum JavaEntityType implements EntityType {
    ARGUMENTS(false),
    ARRAY_ACCESS(true),
    ARRAY_CREATION(true),
    ARRAY_INITIALIZER(true),
    ARRAY_TYPE(true),
    ASSERT_STATEMENT(true),
    ASSIGNMENT(true),
    FIELD(false),
    BLOCK(false),
    BLOCK_COMMENT(true),
    BODY(false),
    BOOLEAN_LITERAL(true),
    BREAK_STATEMENT(true),
    CAST_EXPRESSION(true),
    CATCH_CLAUSE(true),
    CATCH_CLAUSES(false),
    CHARACTER_LITERAL(true),
    CLASS(false),
    CLASS_INSTANCE_CREATION(true),
    COMPILATION_UNIT(true),
    CONDITIONAL_EXPRESSION(true),
    CONSTRUCTOR_INVOCATION(true),
    CONTINUE_STATEMENT(true),
    DO_STATEMENT(true),
    ENHANCED_FOR_STATEMENT(true), //added by nameng
    EXPRESSION_STATEMENT(true), //added by nameng
    ELSE_STATEMENT(true),
    EMPTY_STATEMENT(true),
    FOREACH_STATEMENT(true),
    FIELD_ACCESS(true),
    FIELD_DECLARATION(true),
    FINALLY(false),
    FOR_STATEMENT(true),
    IF_STATEMENT(true),
    INFIX_EXPRESSION(true),
    INSTANCEOF_EXPRESSION(true),
    JAVADOC(true),
    LABELED_STATEMENT(true),
    LINE_COMMENT(true),
    METHOD(false),
    METHOD_DECLARATION(true),
    METHOD_INVOCATION(true),
    MODIFIER(true),
    MODIFIERS(false),
    NULL_LITERAL(true),
    NUMBER_LITERAL(true),
    PARAMETERIZED_TYPE(true),
    PARAMETERS(false),
    PARAMETER(true),
    PARENTHESIZED_EXPRESSION(true), //added by nameng
    POSTFIX_EXPRESSION(true),
    PREFIX_EXPRESSION(true),
    PRIMITIVE_TYPE(true),
    QUALIFIED_NAME(true),
    QUALIFIED_TYPE(true),
    RETURN_STATEMENT(true),
    ROOT_NODE(true),
    SIMPLE_NAME(true),
    SINGLE_TYPE(true),
    SINGLE_VARIABLE_DECLARATION(true), //added by nameng
    STRING_LITERAL(true),
    SUPER_INTERFACE_TYPES(false),
    SUPER_CLASS_TYPES(false), // added by shengzhe Apr-8, 2017
    SUPER_CONSTRUCTOR_INVOCATION(true), //added by nameng
    SUPER_FIELD_ACCESS(true), //added by nameng
    SUPER_METHOD_INVOCATION(true), //added by nameng
    SWITCH_CASE(true),
    SWITCH_STATEMENT(true),
    SYNCHRONIZED_STATEMENT(true),
    THEN_STATEMENT(true),
    THIS_EXPRESSION(true), //added by nameng
    THROW(false),
    THROW_STATEMENT(true),
    TRY_STATEMENT(true),
    TYPE_PARAMETERS(false),
    TYPE_DECLARATION(true),
    TYPE_LITERAL(true),
    TYPE_PARAMETER(true),
    VARIABLE_DECLARATION_EXPRESSION(true), //added by nameng
    VARIABLE_DECLARATION_FRAGMENT(true), //added by nameng
    VARIABLE_DECLARATION_STATEMENT(true),
    WHILE_STATEMENT(true),
    WILDCARD_TYPE(true),
    FOR_INIT(true),
    FOR_INCR(true),//added by nameng
    OP(true), //added by nameng
    RESERVE(true),// added by nameng
    LABEL(true); // added by nameng

    private final boolean fIsValidChange;

    JavaEntityType(boolean isValidChange) {
        fIsValidChange = isValidChange;
    }

    @Override
	public boolean isUsableForChangeExtraction() {
        return fIsValidChange;
    }

    @Override
    public boolean isComment() {
        switch (this) {
            case BLOCK_COMMENT:
            case JAVADOC:
            case LINE_COMMENT:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isStructureEntityType() {
        switch (this) {
        	case COMPILATION_UNIT: //added by nameng
            case CLASS:
            case FIELD:
            case BLOCK: //added by shengzhe
            case METHOD:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isType() {
        switch (this) {
            case ARRAY_TYPE:
            case PARAMETERIZED_TYPE:
            case PRIMITIVE_TYPE:
            case QUALIFIED_TYPE:
            case SINGLE_TYPE:
            case WILDCARD_TYPE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isStatement() {
        switch (this) {
            case ASSERT_STATEMENT:
            case ASSIGNMENT:
            case BREAK_STATEMENT:
            case CATCH_CLAUSE:
            case CLASS_INSTANCE_CREATION:
            case CONSTRUCTOR_INVOCATION:
            case CONTINUE_STATEMENT:
            case DO_STATEMENT:
            case FINALLY:
            case FOR_STATEMENT:
            case IF_STATEMENT:
            case LABELED_STATEMENT:
            case METHOD_INVOCATION:
            case RETURN_STATEMENT:
            case SWITCH_CASE:
            case SWITCH_STATEMENT:
            case SYNCHRONIZED_STATEMENT:
            case THROW_STATEMENT:
            case TRY_STATEMENT:
            case VARIABLE_DECLARATION_STATEMENT:
            case WHILE_STATEMENT:
            case FOREACH_STATEMENT:
            // Fix for https://bitbucket.org/sealuzh/tools-changedistiller/issue/22
            case PREFIX_EXPRESSION:
            case POSTFIX_EXPRESSION:
            //
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isField() {
        return this == FIELD;
    }

    @Override
    public boolean isClass() {
        return this == CLASS;
    }

    @Override
    public boolean isMethod() {
        return this == METHOD;
    }

    @Override
    public boolean isBlock() {
    	// add by shengzhe
        return this == BLOCK;
    }
    
	@Override
	public boolean isQualifiedName() {
		// add by shengzhe
		return this == QUALIFIED_NAME;
	}
	
	

}
