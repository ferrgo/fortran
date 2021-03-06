package poli.comp.checker;

import poli.comp.util.AST.*; //importing all AST node classes
import poli.comp.util.symbolsTable.IdentificationTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Checker implements Visitor{

	private IdentificationTable idt;
	//TODO dar um jeito de acessar o tipo de um ID por uma decoracao no ASTId.

	public Checker(){
			this.idt = new IdentificationTable();
	}


	public AST check(AST a) throws SemanticException {

		ArrayList<AST> scopeTracker = new ArrayList<AST>();

		//TODO: Even though we expect the parser to work reliably (i.e. throw an exception if at runtime it realizes the input code is wrong)
		//, would it be conceptually *wrong* to check for "a instanceof ASTProgram" here in this method?

		//So what we receive is an AST node which happens to be the root of the AST.
		//Assuming the Parser is working correctly, what we get in a is an ASTProgram node,
		// so the call to a.visit below will in turn call (this checker object, provided to a via argument).visitProgram
		a.visit(this,scopeTracker);

		//The tree is now decorated, let's return it.
		return a;

	}

	public Object visitASTProgram(ASTProgram p, ArrayList<AST> scopeTracker) throws SemanticException{

		//Checks globals
		for (ASTDeclarationGroup dg : p.getGlobalDeclarationGroups()){
			dg.visit(this, scopeTracker);//TODO change o
		}

		//Checks function declarations
		for (ASTFunctionDeclaration fd: p.getFunctionDeclarations()){
			fd.visit(this, scopeTracker);
		}

		//Checks subprogram declarations
		for(ASTSubprogramDeclaration sd : p.getSubprogramDeclarations()){
			sd.visit(this, scopeTracker);
		}

		//Checks main program
		return p.getMainProgram().visit(this,scopeTracker);

	}


	public Object visitASTSingleDeclaration(ASTSingleDeclaration sd, ArrayList<AST> scopeTracker) throws SemanticException{
		//We are linking the idt key to the ASTSingleDeclaration and not the ASTIdentifier object, so that when
		//we wanna check type consistency we can have access to the ASTType object contained in the ASTSingleDeclaration object.

		//We don't need to check if the key already exists cause idt.enter does that and raises exceptions internally.

		String idSpelling = sd.getIdentifier().getSpelling();
		idt.enter(idSpelling,sd);
		return null;
	}


	public Object visitASTDeclarationGroup(ASTDeclarationGroup dg, ArrayList<AST> scopeTracker) throws SemanticException{
			//TODO: check for rules:

			String dgType = dg.getType().getSpelling();

			List<ASTSingleDeclaration> declarations = dg.getDeclarations();
			Map<ASTSingleDeclaration,ASTExpression> assignmentMap = dg.getAssignmentMap();

			ASTExpression currentExpression=null;
			for (ASTSingleDeclaration dec: declarations){
				dec.visit(this, scopeTracker);

				//Checking the assigned expression (if appicable)
				if(assignmentMap.containsKey(dec)){
					currentExpression = assignmentMap.get(dec); //exp can be null
					if(currentExpression!=null){
						currentExpression.visit(this,scopeTracker);

						if (!currentExpression.getTypeString().equals(dgType)){
							throw new SemanticException("Assigning an expresson with return type "+
							currentExpression.getTypeString()+
							" to the variable"+
							dec.getIdentifier().getSpelling()+
							", which is of type "+dgType);
						}
					}

				}
			}
			return null;
	}

	public Object visitASTFunctionDeclaration(ASTFunctionDeclaration fd, ArrayList<AST> scopeTracker) throws SemanticException{
		//TODO: Check for rules:
		scopeTracker.add(fd); //Adding the function block to the scope tracker

		ASTType functionType = fd.getType();
		ASTIdentifier functionName = fd.getIdentifier();
		List<ASTSingleDeclaration> l_params = fd.getParams();
		List<ASTStatement> functionStatements = fd.getStatements();
		boolean hasReturn = false;

		//Checking name and putting it on global scope
		idt.enter(functionName.getSpelling(),fd);

		// Increasing the scope, cause we will get into local scope
		idt.openScope();

		//Checking params
		for (ASTSingleDeclaration dec: l_params) {
			dec.visit(this,scopeTracker);
		}

		//Checking statements, have an if / else ifs with types, check return etc
		for( ASTStatement stt : functionStatements ){
			stt.visit(this,scopeTracker);
			if (stt instanceof ASTReturnStatement) {
				hasReturn = true;
			}
		}
		if(!hasReturn){
			throw new SemanticException("Function "+functionName.getSpelling()+" has no return statement!");
		}
		idt.closeScope();
		scopeTracker.remove(fd);

		return fd; //Really dont know what to return here ¯\_(ツ)_/¯
	}

	//This is basically a copy of the above, without the type because its a Subprogram
	//TODO maybe merge those two and have if statements for the functionType part and for the AST classes?
	public Object visitASTSubprogramDeclaration(ASTSubprogramDeclaration spd, ArrayList<AST> scopeTracker) throws SemanticException{
		scopeTracker.add(spd); //Adding the function block to the scope tracker

		ASTIdentifier subprogramName = spd.getIdentifier();
		List<ASTSingleDeclaration> l_params = spd.getParams();
		List<ASTStatement> subprogramStatements = spd.getStatements();
//		boolean hasReturn = false;

		//Checking name and putting it on global scope
		idt.enter(subprogramName.getSpelling(),spd);

		// Increasing the scope, cause we will get into local scope
		idt.openScope();

		//Checking params
		for (ASTSingleDeclaration dec: l_params) {
			dec.visit(this,scopeTracker);
		}


		//Checking statements, have an if / else ifs with types, check return etc
		for( ASTStatement stt : subprogramStatements ){
			stt.visit(this,scopeTracker);
//			if (stt instanceof ASTReturnStatement) {
//				hasReturn = true;
//			}

		}
//		if(!hasReturn){
//			throw new SemanticException("Subprogram "+subprogramName.getSpelling()+" has no return statement!");
//		}

		idt.closeScope();
		scopeTracker.remove(spd);

		return spd; //Really dont know what to return here ¯\_(ツ)_/¯
	}

	//Checks the main body of the program
	public Object visitASTMainProgram(ASTMainProgram mp, ArrayList<AST> scopeTracker) throws SemanticException{
		//TODO ter como regra extra que o main pode ter return?
		scopeTracker.add(mp);
		idt.openScope();
		ASTIdentifier programName = mp.getIdentifier();
		List<ASTStatement> programStatements = mp.getStatements();


		idt.enter(programName.getSpelling(), mp);
		for(ASTStatement stt : programStatements){
			stt.visit(this,scopeTracker);
		}

		idt.closeScope();
		scopeTracker.remove(mp);

		return mp;
	}

	@Override
	public Object visitASTOperator(ASTOperator op, ArrayList<AST> scopeTracker) throws SemanticException {
		return op.getSpelling();
	}

	@Override
	public Object visitASTType(ASTType astType, ArrayList<AST> scopeTracker) {
		return null;
	}

	//Checks a declaration, adding it to the idt or raising an exception if there is a prvious variable with the same id


	public Object visitASTLoop(ASTLoop loopStt, ArrayList<AST> scopeTracker) throws SemanticException{
		//check expression //TODO add return to function/sbp boolean return
		ASTExpression condition = loopStt.getCondition();
		String conditionType = condition.getTypeString();
		if(!conditionType.equals("LOGICAL")){
			throw new SemanticException("Loop conditon must be evaluated to a boolean value, but it's being evaluated to a "+conditionType+" value");
		}

		idt.openScope();
		scopeTracker.add(loopStt);
		List<ASTStatement> statements = loopStt.getStatements();
		for(ASTStatement stt : statements){
			stt.visit(this,scopeTracker);
		}
		idt.closeScope();
		scopeTracker.remove(loopStt);

		return loopStt; // Agin, what the hell should I even return? :(
	}



	public Object visitASTLoopControl(ASTLoopControl lc, Object scopeTracker) throws SemanticException{
		//TODO check for rules:

		//Checks if break/continue statement is inside loop
		List<AST> cScopeTracker = (List<AST>) scopeTracker; //c for Casted
		for(AST node : cScopeTracker){
			if (node instanceof ASTLoop) return lc; //I wish this was a void method
		}

		//If we get here, the loop control statemet is not in a loop
		throw new SemanticException("The "+((lc instanceof ASTLoopExit)?("EXIT"):("CONTINUE"))+" statement is not inside of a loop!");
	}

	Object visitASTFunctionReturnStatement(ASTReturnStatement rs, ArrayList<AST> scopeTracker) throws SemanticException{
		ASTExpression returnValue = rs.getExpression();
		returnValue.visit(this,scopeTracker);
		String returnValueType = returnValue.getTypeString();

		List<AST> cScopeTracker = (List<AST>) scopeTracker; //c for Casted
		for(AST node : cScopeTracker){
			if(node instanceof ASTFunctionDeclaration){
				ASTFunctionDeclaration tmp = (ASTFunctionDeclaration) node;
				String functionType = tmp.getType().getSpelling();
				if(!returnValueType.equals(functionType)){
					throw new SemanticException("Returning an expression of type "+returnValueType+" in a function of type "+functionType);
				}
				tmp.foundReturn();
				return rs; //Retorne risos (huehuebrbr)
			}else if (node instanceof ASTMainProgram){
				return rs; //Our rule 10: MainProgram is allowed to return an expression.
			}
		}
		throw new SemanticException("Trying to return an expression outside of a function scope!");
	}

	Object visitASTSubprogramReturnStatement(ASTReturnStatement rs, Object scopeTracker) throws SemanticException{

		List<AST> cScopeTracker = (List<AST>) scopeTracker; //c for Casted
		for(AST node : cScopeTracker){
			if(node instanceof ASTSubprogramDeclaration){
				((ASTSubprogramDeclaration) node).foundReturn();
				return rs; //Retorne risos (huehuebrbr)
			}
		}
		throw new SemanticException("Trying to return from a subprogram outside of a subprogram scope!");
	}

	public Object visitASTPrintStatement(ASTPrintStatement pstt, ArrayList<AST> scopeTracker) throws SemanticException{

		ASTExpression printExpression = pstt.getExpression();
		printExpression.visit(this,scopeTracker);

		return null;
	}



	public Object visitASTFunctionCall(ASTFunctionCall fc, ArrayList<AST> scopeTracker) throws SemanticException{
		//TODO (low priority): We named this SubroutineCall but it also works for subprogarms. Perhaps we
		// should refactor this to SubroutineCall?
		//TODO (low priority): arent they called params when we call and args on the declaration?
		// Maybe we switched the right names. This is just an "readability/maintenance" problem tho, cause the
		// compiler will work either way as long as we're consistent.

		//Checking if function was declared
		ASTIdentifier functionId = fc.getFunctionId();
		functionId.visit(this,scopeTracker);

		if(idt.retrieve(functionId.getSpelling())==null){
			throw new SemanticException("Trying to call subroutine "+ functionId.getSpelling() +", but it was not declared yet!");
		}else if(! (idt.retrieve(functionId.getSpelling()) instanceof ASTSubroutineDeclaration )){
			throw new SemanticException("Trying to call subroutine "+ functionId.getSpelling() +", but this identifier does not return a subroutine!");
		}

		ASTFunctionDeclaration declaration = (ASTFunctionDeclaration) idt.retrieve(functionId.getSpelling());
		//Checking if number of call arguments matches the number from the declaration
		List<ASTSingleDeclaration> declarationArguments = declaration.getParams();

//		ASTFunctionArgs callArguments = fc.getFunctionArgs();
		List<ASTExpression> callArgumentList = fc.getFunctionArgs().getArgumentList();

		if(callArgumentList.size()!=declarationArguments.size()){
			throw new SemanticException("Function "+functionId.getSpelling()+" accepts "+ declarationArguments.size()
					+" arguments, but you're calling it with "+callArgumentList.size()+" arguments");
		}

		//Checking the argument expressions, and cheking if they have the right return types
		for(int i=0; i<fc.getFunctionArgs().getArgumentList().size(); i++ ){

			ASTExpression currentExp = callArgumentList.get(i);
			String currentExpType = (String) currentExp.visit(this,scopeTracker);

			ASTSingleDeclaration currentDeclaration = declarationArguments.get(i);
			String currentDeclarationType = currentDeclaration.getType().getSpelling();

			if(!currentExpType.equals(currentDeclarationType)){
					throw new SemanticException("The argument " + currentDeclaration.getIdentifier().getSpelling()
														 +" of function "+ functionId.getSpelling() +" accepts values of type "
														 + currentDeclarationType+", but was given a value of type "
														 + currentExpType+" instead");
			}

		}

		return declaration;
	}

	public Object visitASTIfStatement(ASTIfStatement ifStt, ArrayList<AST> scopeTracker) throws SemanticException{
		ASTExpression condition = ifStt.getCondition();
		condition.visit(this,scopeTracker);

		if(!condition.getTypeString().equals("LOGICAL")){
			throw new SemanticException("If statement condition must evaluate to a boolean value!");
		}

		List<ASTStatement> ifBlockStatements = ifStt.getIfBlockStatements();
		List<ASTStatement> elseBlockStatements = ifStt.getElseBlockStatements();

		for(ASTStatement stt : ifBlockStatements){
			stt.visit(this,scopeTracker);
		}
		if(elseBlockStatements !=null) {
			for (ASTStatement stt : elseBlockStatements) {
				stt.visit(this, scopeTracker);
			}
		}

		return null;
	}



	@Override
	public Object visitASTLiteral(ASTLiteral l, ArrayList<AST> scopeTracker) throws SemanticException {
		return null;
	}

	@Override
	public Object visitASTLoopContinue(ASTLoopContinue astLoopContinue, ArrayList<AST> scopeTracker) throws SemanticException {
		return null;
	}

	@Override
	public Object visitASTLoopExit(ASTLoopExit astLoopExit, ArrayList<AST> scopeTracker) {
		return null;
	}

	public Object visitASTAssignment(ASTAssignment asg, ArrayList<AST> scopeTracker) throws SemanticException{

		ASTIdentifier target = asg.getTarget();
		target.visit(this,scopeTracker);

		ASTExpression expression = asg.getExpression();
		expression.visit(this,scopeTracker);
		String expType = expression.getTypeString();

		ASTSingleDeclaration targetDeclaration = (ASTSingleDeclaration) idt.retrieve(target.getSpelling());
		if(targetDeclaration==null){
			throw new SemanticException("Couldnt find the variable "+target.getSpelling());
		}else if(!(targetDeclaration instanceof ASTSingleDeclaration)){
			throw new SemanticException(target.getSpelling()+" is not a variable!");
		}else{
			targetDeclaration =(ASTSingleDeclaration) targetDeclaration;

			if(!(expType.equals(targetDeclaration.getTypeString()))){
			throw new SemanticException(target.getSpelling()+" is of type"+
												 targetDeclaration.getTypeString()+
												 "but its being assigned a value of type"+
												 expType);
			}

		}

		return asg;
	}


	public Object visitASTExpression(ASTExpression e, ArrayList<AST> scopeTracker) throws SemanticException{
		//We dont need to verify if e.getOpComp() indeer returns an OP_COMP, because
		//the parser will only save it if that is the case.
		if (e.getOpComp()==null){
			String ts = (String) e.getExp1().visit(this,scopeTracker);
			e.setTypeString(ts);
			return ts;
		}else{
			e.getExp1().visit(this,scopeTracker);
			e.getExp2().visit(this,scopeTracker);
			e.setTypeString("LOGICAL");
			return "LOGICAL";
		}

	}

	public Object visitASTArithmeticExpression(ASTArithmeticExpression e, ArrayList<AST> scopeTracker) throws SemanticException{
		String term1Type = (String) e.getTerm().visit(this,scopeTracker);

		for(Map.Entry<ASTOperatorArit,ASTTerm> entry : e.getOpTerms().entrySet()){
			String opSpelling = (String) entry.getKey().visit(this,scopeTracker);
			if(!(opSpelling.equals("+") || opSpelling.equals("-"))){
				throw new SemanticException("Youre using a wrong operator here. A + or a - is expected");
			}

			String currentTermType = (String) entry.getValue().visit(this,scopeTracker);
			if(!currentTermType.equals("INTEGER")){
				throw new SemanticException("Youre trying to do an arithmetic operation with a " +currentTermType + " value.");
			}
		}

		if ( term1Type.equals("LOGICAL")){
			if (e.getOpTerms().size()==0 ){
				return "LOGICAL";
			}else{
				throw new SemanticException("Doing arithmetic operations with a boolean value");
			}
		}else{
			return "INTEGER";
		}
	}

	public Object visitASTOperatorArit(ASTOperatorArit     opa  ,Object o) throws SemanticException{
		String a = opa.getSpelling(); //a for alias
		if(!(a.equals("+") || a.equals("-") || a.equals("*") || a.equals("/"))){
			throw new SemanticException("You're using "+a+"where an arithmetic operator is expected");
		}

		return a;
	}

	public Object visitASTOperatorComp(ASTOperatorComp opc , ArrayList<AST> scopeTracker) throws SemanticException{
		String a = opc.getSpelling(); //a for alias
		if(!(a.equals("==") || a.equals("/=") || a.equals(">") || a.equals("<")|| a.equals("<=")|| a.equals(">="))){
			throw new SemanticException("You're using "+a+"where a comparative operator is expected");
		}

		return a;
	}

	@Override
	public Object visitASTReturnStatement(ASTReturnStatement rs, ArrayList<AST> scopeTracker) throws SemanticException {
		return null;
	}


	public Object visitASTTerm(ASTTerm t, ArrayList<AST> scopeTracker) throws SemanticException{
		String factorType = (String) t.getFactor().visit(this,scopeTracker);

		for(Map.Entry<ASTOperatorArit,ASTFactor> entry : t.getOpFactors().entrySet()){
			String opSpelling = (String) entry.getKey().visit(this,scopeTracker);
			if(!(opSpelling.equals("*") || opSpelling.equals("/"))){
				throw new SemanticException("Youre using a wrong operator here. A * or a / is expected");
			}

			String currentFactorType = (String) entry.getValue().visit(this,scopeTracker);
			if(!currentFactorType.equals("INTEGER")){
				throw new SemanticException("Youre trying to do an arithmetic operation with a void or logical value");
			}
		}

		if ( factorType.equals("LOGICAL")){
			if (t.getOpFactors().size()==0 ){
				return "LOGICAL";
			}else{
				throw new SemanticException("Doing arithmetic operations with a boolean value");
			}
		}else{
			return "INTEGER";
		}
	}



	@Override
	public Object visitASTFactorExpression(ASTFactorExpression fe, ArrayList<AST> scopeTracker) throws SemanticException {
		String ae1, ae2, op;
		String resultType;

		ae1 = (String) fe.getExpression().getExp1().visit(this, scopeTracker);

		if(fe.getExpression().getExp2() != null){
			ae2 = (String) fe.getExpression().getExp2().visit(this, scopeTracker);
			op = (String) fe.getExpression().getOpComp().visit(this, scopeTracker);
			//TODO
			//REGRA 7
			if(!ae1.equals(ae2)){
				throw new SemanticException("Cannot do operation between two different types");
			}
			if(ae1.equals("LOGICAL") || ae2.equals("LOGICAL")){
				throw new SemanticException("This operation ("+ fe.getExpression().getOpComp().getSpelling() + ") is not defined between Bool expressions");
			}
			resultType = "LOGICAL";
		} else {
			resultType = ae1;
		}

		fe.getExpression().setTypeString(resultType);
		return fe.getExpression().getTypeString();

	}

	@Override
	public Object visitASTFactorLiteral(ASTFactorLiteral fl, ArrayList<AST> o) throws SemanticException {
		ASTLiteral l = fl.getLiteral();
		return l.getTypeString();
	}

	public Object visitASTFactorSubroutineCall(ASTFactorSubroutineCall fsc, ArrayList<AST> scopeTracker) throws SemanticException {
//		ASTSubroutineDeclaration dec = (ASTSubroutineDeclaration) fsc.visit(this,scopeTracker);
		//Checking if function was declared
		ASTIdentifier subroutineId = fsc.getId();
		subroutineId.visit(this,scopeTracker);

		if(idt.retrieve(subroutineId.getSpelling())==null){
			throw new SemanticException("Trying to call subroutine "+ subroutineId.getSpelling() +", but it was not declared yet!");
		}else if(! (idt.retrieve(subroutineId.getSpelling()) instanceof ASTSubroutineDeclaration )){
			throw new SemanticException("Trying to call subroutine "+ subroutineId.getSpelling() +", but this identifier does not return a subroutine!");
		}
		ASTSubroutineDeclaration dec = (ASTSubroutineDeclaration) idt.retrieve(subroutineId.getSpelling());
		if (dec.getType()==null){
			return "VOID";
		}else{
			return dec.getType().getSpelling();
		}
	}

	@Override
	public Object visitASTFunctionArgs(ASTFunctionArgs fa, ArrayList<AST> o) throws SemanticException {
		return null;
	}


	//TODO UNFINISHED VISITS:

	//Checks an ID that was found in the code. Doesn't add it to IDT - this is done on visitSingleDeclaration.
	public Object visitASTIdentifier(ASTIdentifier id, ArrayList<AST> scopeTracker) throws SemanticException{
		ASTSingleDeclaration sDec = null;
		String idSpelling = id.getSpelling();

		if(!idt.containsKey(id.getSpelling())){
			throw new SemanticException("Trying to refer to identifier "+id.getSpelling()
			+",which is not declared within that scope");
		}


		// Recupera a declaração do id na tabela de símbolos
		// Decora a AST de ID com declaração
		AST declaration = idt.retrieve(id.getSpelling());
		id.setDec(declaration);

		// Checa se o id proveio de uma declaração de variável ou de função
		// Decora a AST de ID com o tipo
		if (declaration instanceof ASTSingleDeclaration) {
			sDec = (ASTSingleDeclaration) idt.retrieve(id.getSpelling());
			id.setType(sDec.getType().getSpelling());
		}

		//Returnig type so we can check for correctness
		return id.getType();
	}

}
