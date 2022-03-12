package ch.uzh.ifi.seal.changedistiller.ast.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class ASTAnonymousClassDeclarationFinder extends ASTVisitor {
    private ASTNode result = null;

    public ASTNode findACD(ASTNode node) {
        result = null;
        node.accept(this);
        return result;
    }

    @Override
    public void preVisit(ASTNode node) {
        if (result == null
                && node.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
            result = node;
        }
    }
}
