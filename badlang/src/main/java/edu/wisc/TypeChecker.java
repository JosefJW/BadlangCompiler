package edu.wisc;

import java.util.ArrayList;
import java.util.List;

public class TypeChecker implements Stmt.Visitor<Void>, Expr.Visitor<VarType>{
	Environment environment = new Environment(null, null);
	List<String> programLines;
	List<Error> errors = new ArrayList<Error>();
	List<Problem> currentStatementProblems;
	int errorCount = 0;

	public TypeChecker(Environment environment, List<String> programLines) {
		this.environment = environment;
		this.programLines = programLines;
	}

	private void addProblems(List<Problem> problems) {
		if (!problems.isEmpty()) {
			int startLine = problems.get(0).startLine;
			int endLine = problems.get(0).endLine;
			for (Problem np : problems) {
				if (np.startLine < startLine) startLine = np.startLine;
				if (np.endLine > endLine) endLine = np.endLine;
			}
			errors.add(new Error(problems, programLines.subList(startLine-1, endLine), ErrorType.TYPE));
		} 
	}

	// Statements
	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		// Make sure return type matches with function type
		currentStatementProblems = new ArrayList<Problem>();
		VarType returnType = stmt.value.accept(this);
		VarType functionType = environment.getReturnType();
		if (functionType == null) {
			// Handle error: Message for return outside of function
			String msg = "Return statements can only be used within functions.";
			currentStatementProblems.add(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg));
			errorCount++;
		}
		else if (returnType != functionType && returnType != VarType.ERR && functionType != VarType.ERR) {
			// Handle error: Message for return type not matching function type
			String msg = "Function is of type " + functionType + ", but return value is of type " + returnType + ".";
			currentStatementProblems.add(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg));
			errorCount++;
		}
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		for (Stmt s : stmt.statements) {
			s.accept(this);
		}
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		VarType condType = stmt.condition.accept(this);
		if (condType == VarType.INT) {
			//Handle error: Message for if condition not being of type bool
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

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		if (stmt.initializer != null) {
			VarType initializerType = stmt.initializer.accept(this);
			environment.declare(stmt.name, stmt.type, IdentifierType.VARIABLE, null, true);
			if (initializerType != stmt.type && initializerType != VarType.ERR && stmt.type != VarType.ERR) {
				String msg = "Variable \'" + stmt.name + "\' expected value of type " + stmt.type + ", but value is of type " + initializerType + ".";
				currentStatementProblems.add(new Problem(stmt.initializer.getStartCol(), stmt.initializer.getEndCol(), stmt.initializer.getStartLine(), stmt.initializer.getEndLine(), msg));
				errorCount++;
			}
		}
		else environment.declare(stmt.name, stmt.type, IdentifierType.VARIABLE, null, false);
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		VarType condType = stmt.condition.accept(this);
		if (condType == VarType.INT) {
			//Handle error: Message for while condition not being of type bool
			String msg = "Conditional expressions need to be of type bool, but this expression is of type int.";
			currentStatementProblems.add(new Problem(stmt.condition.getStartCol(), stmt.condition.getEndCol(), stmt.condition.getStartLine(), stmt.condition.getEndLine(), msg));
			errorCount++;
		}
		// Go down body
		stmt.body.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitAssignStmt(Stmt.Assign stmt) {
		if (!environment.isDeclared(stmt.name)) return null;
		currentStatementProblems = new ArrayList<Problem>();
		VarType varType = environment.getType(stmt.name);
		VarType rightType = stmt.value.accept(this);
		if (rightType != varType && rightType != VarType.ERR && varType != VarType.ERR) {
			// Handle error: Message for right side not matching variable type
			String msg = "Variable " + stmt.name + " expected value of type " + varType + ", but value is of type " + rightType + ".";
			currentStatementProblems.add(new Problem(stmt.value.getStartCol(), stmt.value.getEndCol(), stmt.value.getStartLine(), stmt.value.getEndLine(), msg));
			errorCount++;
		}
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		// Visit all lines in function body
		environment = new Environment(environment, stmt.returnType);
		for (Stmt s : stmt.body) {
			s.accept(this);
		}
		return null;
	}


	// Expressions
	@Override
	public VarType visitUnaryExpr(Expr.Unary expr) {
		VarType eType = expr.right.accept(this);
		if (eType == VarType.ERR) return VarType.ERR; // Do not cascade errors
		switch (expr.operator) {
			case PLUS:
			case MINUS: {
				if (eType != VarType.INT) {
					String msg = "Expected expression of type int, but got expression of type bool.";
					currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
					errorCount++;
					return VarType.ERR;
				}
				else {
					return VarType.INT;
				}
			}
			case NOT: {
				if (eType != VarType.BOOL) {
					String msg = "Expected expression of type bool, but got expression of type int.";
					currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
					errorCount++;
					return VarType.ERR;
				}
				else {
					return VarType.BOOL;
				}
			}
			default: {
				String msg = "Unsupported operator used on unary expression.";
				currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
				errorCount++;
				return VarType.ERR;
			}
		}
	}

	@Override
	public VarType visitBinaryExpr(Expr.Binary expr) {
		VarType leftType = expr.left.accept(this);
		VarType rightType = expr.right.accept(this);

		if (leftType == VarType.ERR && rightType == VarType.ERR) return VarType.ERR; // Do not cascade errors
		switch (expr.operator) {
			// Take int, int return int
			case PLUS:
			case MINUS:
			case MULTIPLY:
			case DIVIDE: {
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
						//Handle error: Error message for both sides being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount += 2;
						return VarType.ERR;
					}
					else if (leftType != VarType.INT) {
						// Handle error: Error message for left side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					else if (rightType != VarType.INT) {
						// Handle error: Error message for right side being incorrect type
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
						// Handle error: Error message for both sides being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount += 2;
						return VarType.ERR;
					}
					else if (leftType != VarType.INT) {
						// Handle error: Error message for left side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type int, but got expression of type bool.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					else if (rightType != VarType.INT) {
						// Handle error: Error message for right side being incorrect type
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
						// Handle error: Error message for right side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.ERR;
				}
				else if (rightType == VarType.ERR) {
					if (leftType != VarType.BOOL) {
						// Handle error: Error message for left side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					return VarType.ERR;
				}
				else {
					if (leftType != VarType.BOOL && rightType != VarType.BOOL) {
						// Handle error: Error message for both sides being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						currentStatementProblems.add(new Problem(expr.right.getStartCol(), expr.right.getEndCol(), expr.right.getStartLine(), expr.right.getEndLine(), msg));
						errorCount += 2;
						return VarType.ERR;
					}
					else if (leftType != VarType.BOOL) {
						// Handle error: Error message for left side being incorrect type
						String msg = "Operator \'" +expr.operator + "\' expects expressions of type bool, but got expression of type int.";
						currentStatementProblems.add(new Problem(expr.left.getStartCol(), expr.left.getEndCol(), expr.left.getStartLine(), expr.left.getEndLine(), msg));
						errorCount++;
						return VarType.ERR;
					}
					else if (rightType != VarType.BOOL) {
						// Handle error: Error message for right side being incorrect type
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
					// Handle error: Error message for sides not matching
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

	@Override
	public VarType visitVariableExpr(Expr.Variable expr) {
		if (!environment.isDeclared(expr.name)) return VarType.ERR;
		return environment.getType(expr.name);
	}

	@Override
	public VarType visitLiteralExpr(Expr.Literal expr) {
		if (expr.value instanceof Boolean) {
			return VarType.BOOL;
		}
		else if (expr.value instanceof Integer) {
			return VarType.INT;
		}
		return null;
	}

	@Override
	public VarType visitCallExpr(Expr.Call expr) {
		VarType type;
		if (!environment.isDeclared(expr.name) || !environment.isFunction(expr.name)) type = VarType.ERR;
		else {
			type = environment.getType(expr.name);
			int exprArgCount = expr.arguments.size();
			int funcParamCount = environment.getParameters(expr.name) == null ? 0 : environment.getParameters(expr.name).size();
			if (exprArgCount != funcParamCount) {
				String msg = "Function " + expr.name + " expects " + funcParamCount + " parameters, but was given " + exprArgCount + ".";
				currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
				errorCount++;
			}
			else {
				for (int i = 0; i < expr.arguments.size(); i++) {
					VarType argType = expr.arguments.get(i).accept(this);
					if (argType != VarType.ERR && argType != environment.getParameters(expr.name).get(i).type()) {
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
