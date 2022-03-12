package ch.uzh.ifi.seal.changedistiller.structuredifferencing.java;

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

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

//import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;

import ch.uzh.ifi.seal.changedistiller.ast.java.JavaCompilation;
import ch.uzh.ifi.seal.changedistiller.ast.java.JavaCompilationUtils;
import ch.uzh.ifi.seal.changedistiller.structuredifferencing.java.JavaStructureNode.Type;
import ch.uzh.ifi.seal.changedistiller.ast.ASTHelper;
import edu.vt.cs.append.CommonValue;
/**
 * Creates a tree of {@link JavaStructureNode}s.
 * 
 * @author Beat Fluri
 */
public class JavaStructureTreeBuilder extends ASTVisitor {

    private Stack<JavaStructureNode> fNodeStack;
    private Stack<char[]> fQualifiers;
 	private List<org.eclipse.jdt.internal.compiler.ast.MethodDeclaration> methods = new LinkedList<org.eclipse.jdt.internal.compiler.ast.MethodDeclaration>();
 	private int init_start, init_end;
 	private String init_str = "public void methodA() {";
 	
    /**
     * Creates a new Java structure tree builder.
     * 
     * @param root
     *            of the structure tree
     */
    public JavaStructureTreeBuilder(JavaStructureNode root) {
        fNodeStack = new Stack<JavaStructureNode>();
        fNodeStack.push(root);
        fQualifiers = new Stack<char[]>();
        init_start = -1;
    }


	@Override
    public boolean visit(CompilationUnitDeclaration compilationUnitDeclaration, CompilationUnitScope scope) {
//        System.out.println("comp!!!");
    	if (compilationUnitDeclaration.currentPackage != null) {
            for (char[] qualifier : compilationUnitDeclaration.currentPackage.tokens) {
                fQualifiers.push(qualifier);
            }
        }
        return true;
    }

    @Override
    public boolean visit(FieldDeclaration fieldDeclaration, MethodScope scope) {
        StringBuffer name = new StringBuffer();
        name.append(fieldDeclaration.name);
        name.append(" : ");
        if (fieldDeclaration.type == null &&  fNodeStack.peek().getType().compareTo(JavaStructureNode.Type.ENUM) == 0) {
        	name.append(fNodeStack.peek().getName());
        } else {
        	fieldDeclaration.type.print(0, name);
        }
        push(Type.FIELD, name.toString(), fieldDeclaration);
        return false;
    }
    
    @Override
    public void endVisit(FieldDeclaration fieldDeclaration, MethodScope scope) {
        pop();
    }
    
    //add by shengzhe
    @Override
    public boolean visit(Initializer initializer, MethodScope scope) {
//    	System.out.println("");
    	String name = getInitializerSignature(initializer);
//    	Parser parser = new MatchLocatorParser(
//    			new ProblemReporter(
//    					DefaultErrorHandlingPolicies.proceedWithAllProblems(), null, null),
//    			new );
    	ASTParser parser = ASTParser.newParser(AST.JLS8);
    	parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
    
//		for (int i = 0; i<initializer.block.statements.length; i++) {
//			visit(initializer.block.statements[i],(BlockScope) null );
//		}
		
//		JavaStructureNode node_sub = new JavaStructureNode(Type.METHOD, null, name, initializer.block);
//		JavaStructureTreeBuilder builder_sub = new JavaStructureTreeBuilder(node_sub);
//		initializer.block.traverse(builder_sub, (MethodScope) null);
    	
		String init_str = initializer.block.toString();
//		initializer.sourceStart(), initializer.sourceEnd();
		String file_name = CommonValue.workspace + "ast_tmp.java";
    	String str = "class Test{public void methodA() "+init_str+"}";
    	str = str.replaceAll(";;", ";");
    	parser.setSource(str.toCharArray());
//    	System.out.println(parser.createAST(null));

        appendMethodB(file_name, str);
//    	CompilationUnit encapsulated_method = (CompilationUnit) parser.createAST(null);
//    	
		long versionNumber = ClassFileConstants.JDK1_7; 
		System.out.println();
    	JavaCompilation fCompilation = JavaCompilationUtils.compile(str, file_name);
		CompilationUnitDeclaration sub_cu = fCompilation.getCompilationUnit();
    	JavaStructureNode sub_node = new JavaStructureNode(Type.CU, null, file_name, sub_cu);
    	System.out.println();
    	fCompilation.getCompilationUnit().traverse(new JavaStructureTreeBuilder(sub_node) {
    		MethodDeclaration m =null;
    		@Override
    		public boolean visit(MethodDeclaration node, ClassScope scope) {
    			methods.add(node);
//    			return super.visit(node, scope);
    			return false;
    		}
		},(CompilationUnitScope) null);
//        CompilationUnitDeclaration cu = fCompilation.getCompilationUnit();
//        String name = "111";        
//        JavaStructureNode node = new JavaStructureNode(Type.CU, null, name, cu);
//        JavaStructureTreeBuilder builder = new JavaStructureTreeBuilder(node);
//        cu.traverse(builder, (CompilationUnitScope) null);
//        node.setQualifier(builder.getQualifier());//added by nameng
        
//    	System.out.println(encapsulated_method.toString()+" "+encapsulated_method.getNodeType());
//    	ASTNode newnode. = (ASTNode) encapsulated_method.;
//    	
//    	encapsulated_method.accept(new ASTVisitor() {
//    		MethodDeclaration m =null;
//    		@Override
//    		public boolean visit(MethodDeclaration node, ClassScope scope) {
//    			methods.add(node);
//    			return super.visit(node, scope);
//    		}
//		});
//    	
    	MethodDeclaration method = methods.get(0);

		init_start = initializer.declarationSourceStart;
		init_end = initializer.declarationSourceEnd;
		
//    	
    	push(Type.INITIALIZER, "Initializer1", method );
		return false;
    }
    
    @Override
    public void endVisit(Initializer initializer, MethodScope scope) {
    	pop();
    }
    
    @Override
    public boolean visit(Assignment assignment, BlockScope scope) {
    	push(Type.FIELD, String.valueOf(assignment.lhs), assignment);
    	return false;
    }
    
    @Override
    public void endVisit(Assignment assignment, BlockScope scope) {
    	pop();
    }
//    
//    //add by shengzhe
//    @Override
//    public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
//    	push(Type.FIELD, String.valueOf(localDeclaration.name), localDeclaration);
//		return false;
//    	
//    }
//    
//    @Override
//    public void endVisit(LocalDeclaration localDeclaration, BlockScope scope) {
//    	pop();
//    }

    @Override
    public boolean visit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
        push(Type.CONSTRUCTOR, getMethodSignature(constructorDeclaration), constructorDeclaration);
        return false;
    }

    @Override
    public void endVisit(ConstructorDeclaration constructorDeclaration, ClassScope scope) {
        pop();
    }

    @Override
    public boolean visit(MethodDeclaration methodDeclaration, ClassScope scope) {
        push(Type.METHOD, getMethodSignature(methodDeclaration), methodDeclaration);
        return false;
    }

    @Override
    public void endVisit(MethodDeclaration methodDeclaration, ClassScope scope) {
        pop();
    }

    @Override
    public boolean visit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
//    	System.out.println("TypeDecl!!!");
        return visit(localTypeDeclaration, (CompilationUnitScope) null);
    }

    @Override
    public void endVisit(TypeDeclaration localTypeDeclaration, BlockScope scope) {
        endVisit(localTypeDeclaration, (CompilationUnitScope) null);
    }

    @Override
    public boolean visit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
        return visit(memberTypeDeclaration, (CompilationUnitScope) null);
    }

    @Override
    public void endVisit(TypeDeclaration memberTypeDeclaration, ClassScope scope) {
        endVisit(memberTypeDeclaration, (CompilationUnitScope) null);
    }
    
    @Override
    public boolean visit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
        int kind = TypeDeclaration.kind(typeDeclaration.modifiers);
        Type type = null;
//        System.out.println("wow!!!!");
        switch (kind) {
            case TypeDeclaration.INTERFACE_DECL:
                type = Type.INTERFACE;
                break;
            case TypeDeclaration.CLASS_DECL:
                type = Type.CLASS;
//                System.out.println("have!!!!");
                break;
            case TypeDeclaration.ANNOTATION_TYPE_DECL:
                type = Type.ANNOTATION;
                break;
            case TypeDeclaration.ENUM_DECL:
                type = Type.ENUM;
                break;
            default:
                assert (false);
        }
        push(type, String.valueOf(typeDeclaration.name), typeDeclaration);
        fQualifiers.push(typeDeclaration.name);
        return true;
    }

    @Override
    public void endVisit(TypeDeclaration typeDeclaration, CompilationUnitScope scope) {
        pop();
        fQualifiers.pop();
    }

    private String getMethodSignature(AbstractMethodDeclaration methodDeclaration) {
        StringBuffer signature = new StringBuffer();
        signature.append(methodDeclaration.selector);
        signature.append('(');
        if (methodDeclaration.arguments != null) {
            for (int i = 0; i < methodDeclaration.arguments.length; i++) {
                if (i > 0) {
                    signature.append(',');
                }
                methodDeclaration.arguments[i].type.print(0, signature);
            }
        }
        signature.append(')');
        return signature.toString();
    }
    
    // new
    private String getInitializerSignature(Initializer initializer) {
        StringBuffer signature = new StringBuffer();
//        System.out.println(initializer.name);
        signature.append("Initial1");
        return signature.toString();
    }

    private void push(Type type, String name, ASTNode astNode) {
//    	System.out.println(getQualifier());
//    	System.out.println("initializer Check Point");
    	if (type == Type.INITIALIZER) {
    		
            JavaStructureNode node = new JavaStructureNode(Type.METHOD, getQualifier(), name, astNode);
            node.setRange(get_init_start(), get_init_end());
            fNodeStack.peek().addChild(node);
            fNodeStack.push(node);
    	}
    	else {
    		JavaStructureNode node = new JavaStructureNode(type, getQualifier(), name, astNode);
    		fNodeStack.peek().addChild(node);
            fNodeStack.push(node);
    	}
    }

    //modified by nameng from private to public
    public String getQualifier() {
        if (!fQualifiers.isEmpty()) {
            StringBuilder qualifier = new StringBuilder();
            for (int i = 0; i < fQualifiers.size(); i++) {
                qualifier.append(fQualifiers.get(i));
                if (i < fQualifiers.size() - 1) {
                    qualifier.append('.');
                }
            }
            return qualifier.toString();
        }
        return null;
    }

    private void pop() {
        fNodeStack.pop();
    }

    public int get_init_start() {
    	return init_start;
    }
    
    public int get_init_end() {
    	return init_end;
    }
    
    public String get_init_str() {
    	return init_str;
    }
    
    public void appendMethodB(String fileName, String content) {
        try {
            FileWriter writer = new FileWriter(fileName, false);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
