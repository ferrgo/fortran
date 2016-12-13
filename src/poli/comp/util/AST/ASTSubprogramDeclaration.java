package poli.comp.util.AST;

import poli.comp.checker.SemanticException;
import poli.comp.checker.Visitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hgferr on 30/09/16.
 */
public class ASTSubprogramDeclaration extends ASTSubroutineDeclaration{

	private ASTIdentifier id;
	private List<ASTSingleDeclaration> l_params;

	public ASTSubprogramDeclaration( ASTIdentifier subroutineName, List<ASTSingleDeclaration> l_par, List<ASTStatement> l_s) {
       super(null,l_s);
		 this.id = subroutineName;
		 this.l_params = l_par;
	}

    @Override
    public Object visit(Visitor v, ArrayList<AST> scopeTracker) throws SemanticException {
        return v.visitASTSubprogramDeclaration(this, scopeTracker);
    }

	public ASTIdentifier getIdentifier() {
		return id;
	}

	public List<ASTSingleDeclaration> getParams() {
		return l_params;
	}


}
