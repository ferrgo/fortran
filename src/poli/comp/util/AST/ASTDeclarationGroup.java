package poli.comp.util.AST;

import java.util.Map;

/**
 * Created by hgferr on 30/09/16.
 */

 // DECLARATION_GROUP ::= TYPE :: ID (= EXPRESSION)? (,ID (= EXPRESSION)? )*
public class ASTDeclarationGroup extends ASTStatement{

  private ASTType tp;
  private Map<ASTIdentifier,ASTExpression> declarations;

  public ASTDeclarationGroup(ASTType tp, Map<ASTIdentifier,ASTExpression> declarations) {
      this.tp = tp;
      this.declarations = declarations;
  }

  	@Override
  	public String toString(int level) {
  			return null;
  	}

	public ASTType getType(){
		return this.tp;
	}

	public Map<ASTIdentifier,ASTExpression> getDeclarations(){
		return this.declarations;
	}

}
