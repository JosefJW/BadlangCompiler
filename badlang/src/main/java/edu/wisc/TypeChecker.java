package edu.wisc;

import java.util.ArrayList;
import java.util.List;

import edu.wisc.Expr.Binary;
import edu.wisc.Expr.Call;
import edu.wisc.Expr.Literal;
import edu.wisc.Expr.Unary;
import edu.wisc.Expr.Variable;
import edu.wisc.Stmt.Assign;
import edu.wisc.Stmt.Block;
import edu.wisc.Stmt.Expression;
import edu.wisc.Stmt.Function;
import edu.wisc.Stmt.If;
import edu.wisc.Stmt.Print;
import edu.wisc.Stmt.Println;
import edu.wisc.Stmt.Printsp;
import edu.wisc.Stmt.Return;
import edu.wisc.Stmt.Var;
import edu.wisc.Stmt.While;

/**
 * Performs type checking on the Badlang language
 */
public class TypeChecker implements Stmt.Visitor<Void>, Expr.Visitor<VarType>{
	Environment environment = new Environment(null, null); // The current scope
	List<String> programLines; // List of the lines of code being type checked
	List<Error> errors = new ArrayList<Error>(); // List of all errors found
	List<Problem> currentStatementProblems; // List of problems found in the current statement being checked
	int errorCount = 0; // The number of type problems found in total

	/**
	 * Initialize a new type checker
	 * 
	 * @param environment The global environment for this type checker to use
	 * @param programLines A list containing the lines of code that are being type checked
	 */
	public TypeChecker(Environment environment, List<String> programLines) {
		this.environment = environment;
		this.programLines = programLines;
	}

	/**
	 * Congregates all problems in the input into one error and stores it
	 * in the global errors list
	 * 
	 * @param problems List of problems to make into an error
	 */
	private void addProblems(List<Problem> problems) {
		if (!problems.isEmpty()) {
			int startLine = problems.get(0).getStartLine();
			int endLine = problems.get(0).getEndLine();
			for (Problem np : problems) {
				if (np.getStartLine() < startLine) startLine = np.getStartLine();
				if (np.getEndLine() > endLine) endLine = np.getEndLine();
			}
			errors.add(new Error(problems, programLines.subList(startLine-1, endLine), ErrorType.TYPE));
		} 
	}

	/**
	 * Perform type checking on a return statement
	 * 
	 * @param stmt The return statement to type check
	 */
	@Override
	public Void visitReturnStmt(Return stmt) {
		// Make sure return type matches with function type
		currentStatementProblems = new ArrayList<Problem>();
		VarType returnType = stmt.value.accept(this);
		VarType functionType = environment.getReturnType();
		if (functionType == null) {
			// Handle error: Return outside of function
			String msg = "Return statements can only be used within functions.";
			currentStatementProblems.add(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg));
			errorCount++;
		}
		else if (returnType != functionType && returnType != VarType.ERR && functionType != VarType.ERR) {
			// Handle error: Return type not matching function type
			String msg = "Function is of type " + functionType + ", but return value is of type " + returnType + ".";
			currentStatementProblems.add(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg));
			errorCount++;
		}
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Perform type checking on a print statement
	 * 
	 * @param stmt The print statement to type check
	 */
	@Override
	public Void visitPrintStmt(Print stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Perform type checking on a printsp statement
	 * 
	 * @param stmt The printsp statement to type check
	 */
	@Override
	public Void visitPrintspStmt(Printsp stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		if (stmt.expression != null) stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Perform type checking on a println statement
	 * 
	 * @param stmt The println statement to type check
	 */
	@Override
	public Void visitPrintlnStmt(Println stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		if (stmt.expression != null) stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Perform type checking on each statement in a block statement
	 * 
	 * @param stmt The block of statements to type check
	 */
	@Override
	public Void visitBlockStmt(Block stmt) {
		for (Stmt s : stmt.statements) {
			s.accept(this);
		}
		return null;
	}

	/**
	 * Perform type checking on an if statement
	 * 
	 * @param stmt The if statement to type check
	 */
	@Override
	public Void visitIfStmt(If stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		VarType condType = stmt.condition.accept(this);
		if (condType == VarType.INT) {
			// Handle error: If condition not being of type bool
			String msg = "Conditional expressions need to be of type bool, but this expression is of type int.";
			currentStatementProblems.add(new Problem(stmt.condition.getStartCol(), stmt.condition.getEndCol(), stmt.condition.getStartLine(), stmt.condition.getEndLine(), msg));
			errorCount++;
		}
		addProblems(currentStatementProblems);
		// Go down both branches
		stmt.thenBranch.accept(this);
		if (stmt.elseBranch != null) stmt.elseBranch.accept(this);
		return null;
	}

	/**
	 * Perform type checking on a variable declaration
	 * 
	 * @param stmt The variable declaration to type check
	 */ 
	@Override
	public Void visitVarStmt(Var stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		if (stmt.initializer != null) {
			// Check initializer type
			VarType initializerType = stmt.initializer.accept(this);
			environment.declare(stmt.name, stmt.type, IdentifierType.VARIABLE, null, true);
			if (initializerType != stmt.type && initializerType != VarType.ERR && stmt.type != VarType.ERR) {
				// Handle error: type mismatch
				String msg = "Variable \'" + stmt.name + "\' expected value of type " + stmt.type + ", but value is of type " + initializerType + ".";
				currentStatementProblems.add(new Problem(stmt.initializer.getStartCol(), stmt.initializer.getEndCol(), stmt.initializer.getStartLine(), stmt.initializer.getEndLine(), msg));
				errorCount++;
			}
		}
		else environment.declare(stmt.name, stmt.type, IdentifierType.VARIABLE, null, false);
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Perform type checking on a while loop
	 * 
	 * @param stmt The while loop to type check
	 */
	@Override
	public Void visitWhileStmt(While stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		VarType condType = stmt.condition.accept(this);
		if (condType == VarType.INT) {
			//Handle error: While condition not being of type bool
			String msg = "Conditional expressions need to be of type bool, but this expression is of type int.";
			currentStatementProblems.add(new Problem(stmt.condition.getStartCol(), stmt.condition.getEndCol(), stmt.condition.getStartLine(), stmt.condition.getEndLine(), msg));
			errorCount++;
		}
		addProblems(currentStatementProblems);
		// Go down body
		stmt.body.accept(this);
		return null;
	}

	/**
	 * Perform type checking on an assign statement
	 * 
	 * @param stmt The assign statement to type check
	 */
	@Override
	public Void visitAssignStmt(Assign stmt) {
		if (!environment.isDeclared(stmt.name)) return null;
		currentStatementProblems = new ArrayList<Problem>();
		VarType varType = environment.getType(stmt.name);
		VarType rightType = stmt.value.accept(this);
		if (rightType != varType && rightType != VarType.ERR && varType != VarType.ERR) {
			// Handle error: Right side not matching variable type
			String msg = "Variable " + stmt.name + " expected value of type " + varType + ", but value is of type " + rightType + ".";
			currentStatementProblems.add(new Problem(stmt.value.getStartCol(), stmt.value.getEndCol(), stmt.value.getStartLine(), stmt.value.getEndLine(), msg));
			errorCount++;
		}
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Perform type checking on an expression statement
	 * 
	 * @param stmt The expression statement to type check
	 */
	@Override
	public Void visitExpressionStmt(Expression stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Perform type checking on a function declaration
	 * 
	 * @param stmt The function declaration to type check
	 */
	@Override
	public Void visitFunctionStmt(Function stmt) {
		// Visit all lines in function body
		environment = new Environment(environment, stmt.returnType);
		for (Stmt s : stmt.body) {
			s.accept(this);
		}
		return null;
	}


	/**
	 * Perform type checking on a unary expression
	 * 
	 * @param expr The unary expression to type check
	 */
	@Override
	public VarType visitUnaryExpr(Unary expr) {
		VarType eType = expr.right.accept(this);
		if (eType == VarType.ERR) return VarType.ERR; // Do not cascade errors
		switch (expr.operator) {
			// Take an int
			case PLUS:
			case MINUS: {
				if (eType != VarType.INT) {
					String msg = "Operator \'" + expr.operator + "\' expects expression of type int, but got expression of type bool.";
					currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
					errorCount++;
					return VarType.ERR;
				}
				else {
					return VarType.INT;
				}
			}

			// Takes a bool
			case NOT: {
				if (eType != VarType.BOOL) {
					String msg = "Operator \'" + expr.operator + "\' expects expression of type bool, but got expression of type int.";
					currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
					errorCount++;
					return VarType.ERR;
				}
				else {
					return VarType.BOOL;
				}
			}

			default: {
				String msg = "Unsupported operator \'" + expr.operator + "\' used on unary expression.";
				currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
				errorCount++;
				return VarType.ERR;
			}
		}
	}

	/**
	 * Perform type checking on a binary expression
	 * 
	 * @param expr The binary expression to type check
	 */
	@Override
	public VarType visitBinaryExpr(Binary expr) {
		VarType leftType = expr.left.accept(this);
		VarType rightType = expr.right.accept(this);

		if (leftType == VarType.ERR && rightType == VarType.ERR) return VarType.ERR; // Do not cascade errors
		switch (expr.operator) {
			// Take int, int return int
			case PLUS:
			case MINUS:
			case MULTIPLY:
			case DIVIDE: 
			case MODULO: {
				if (leftType == VarType.ERR) {
					if (rightType != VarType.INT) {
						// Handle error: VarType needs to be Integer to use any of the above
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.ERR;
				}
				else if (rightType == VarType.ERR) {
					if (leftType != VarType.INT) {
						// Handle error: VarType needs to be Integer to use any of the above
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.ERR;
				}
				else {
					if (leftType != VarType.INT && rightType != VarType.INT) {
						// Handle error: Both sides being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount += 2;
						return VarType.ERR;
					}
					else if (leftType != VarType.INT) {
						// Handle error: Left side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					else if (rightType != VarType.INT) {
						// Handle error: Right side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.INT; // All good
				}
			}

			// Take int, int return bool
			case GREATER:
			case GREATER_EQUAL:
			case LESS:
			case LESS_EQUAL: {
				if (leftType == VarType.ERR) {
					if (rightType != VarType.INT) {
						// Handle error: VarType needs to be Integer to use any of the above
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.ERR;
				}
				else if (rightType == VarType.ERR) {
					if (leftType != VarType.INT) {
						// Handle error: VarType needs to be Integer to use any of the above
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.ERR;
				}
				else {
					if (leftType != VarType.INT && rightType != VarType.INT) {
						// Handle error: Both sides being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount += 2;
						return VarType.ERR;
					}
					else if (leftType != VarType.INT) {
						// Handle error: Left side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					else if (rightType != VarType.INT) {
						// Handle error: Right side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.BOOL; // All good
				}
			}

			// Take bool, bool return bool
			case AND:
			case OR: {
				if (leftType == VarType.ERR) {
					if (rightType != VarType.BOOL) {
						// Handle error: Right side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.ERR;
				}
				else if (rightType == VarType.ERR) {
					if (leftType != VarType.BOOL) {
						// Handle error: Left side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.ERR;
				}

				else { // Neither side is VarType.ERR
					if (leftType != VarType.BOOL && rightType != VarType.BOOL) {
						// Handle error: Both sides being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount += 2;
						return VarType.ERR;
					}
					else if (leftType != VarType.BOOL) {
						// Handle error: Left side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					else if (rightType != VarType.BOOL) {
						// Handle error: Right side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.BOOL; // All good
				}
			}

			// Both sides can be any type, but they need to match, return bool
			case EQUAL:
			case NOT_EQUAL: {
				if (leftType == VarType.ERR || rightType == VarType.ERR) return VarType.ERR;
				else if (leftType != rightType) {
					// Handle error: Sides not matching
					String msg = "Operator \'" +expr.operator + "\' expects expressions of the same type, but left expression is of type " + leftType + " while right expression is of type " + rightType + ".";
					currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
					errorCount++;	
					return VarType.ERR;
				}
				return VarType.BOOL;
			}

			default: {
				// Handle error: Unsupported operator used on binary expression
				String msg = "Unsupported operator used in binary expression.";
				currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
				errorCount++;
				return VarType.ERR;
			}
		}
	}

	/**
	 * Return the type of the variable in a variable expression
	 * 
	 * @param expr The variable to get the type of
	 */
	@Override
	public VarType visitVariableExpr(Variable expr) {
		if (!environment.isDeclared(expr.name)) return VarType.ERR;
		else if (environment.isFunction(expr.name)) {
			// Handle error: Function was referenced without being called
			String msg = "Function \'" + expr.name + "\' was referenced without being called. Must use \'()\' to call a function (i.e., \'[identifier]()\').";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
			errorCount++;
			return VarType.ERR;
		}
		return environment.getType(expr.name);
	}

	/**
	 * Return the type of a literal
	 * 
	 * @param expr The literal to get the type of
	 */
	@Override
	public VarType visitLiteralExpr(Literal expr) {
		if (expr.value instanceof Boolean) {
			return VarType.BOOL;
		}
		else if (expr.value instanceof Integer) {
			return VarType.INT;
		}
		return VarType.ERR;
	}

	/**
	 * Perform type checking on a function call
	 * 
	 * @param expr The function call to type check
	 */
	@Override
	public VarType visitCallExpr(Call expr) {
		VarType type;
		if (!environment.isDeclared(expr.name) || !environment.isFunction(expr.name)) type = VarType.ERR;
		else {
			type = environment.getType(expr.name);
			int exprArgCount = expr.arguments.size();
			int funcParamCount = environment.getParameters(expr.name) == null ? 0 : environment.getParameters(expr.name).size();
			if (exprArgCount != funcParamCount) {
				// Handle error: Incorrect number of parameters
				String msg = "Function " + expr.name + " expects " + funcParamCount + " parameters, but was given " + exprArgCount + ".";
				currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
				errorCount++;
			}
			else {
				// Check all parameters
				for (int i = 0; i < expr.arguments.size(); i++) {
					VarType argType = expr.arguments.get(i).accept(this);
					if (argType != VarType.ERR && argType != environment.getParameters(expr.name).get(i).type()) {
						// Handle error: Parameter has incorrect type
						String msg = "Parameter \'" + environment.getParameters(expr.name).get(i).name() + "\' is of type " + environment.getParameters(expr.name).get(i).type() + ", but was given value of type " + argType + ".";
						currentStatementProblems.add(new Problem(expr.arguments.get(i).getStartCol(), expr.arguments.get(i).getEndCol(), expr.arguments.get(i).getStartLine(), expr.arguments.get(i).getEndLine(), msg));
						errorCount++;
					}
				}
			}
		}
		
		return type;
	}
}
