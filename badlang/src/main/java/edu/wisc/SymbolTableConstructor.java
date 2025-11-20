package edu.wisc;

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
import edu.wisc.Stmt.Return;
import edu.wisc.Stmt.Var;
import edu.wisc.Stmt.While;

public class SymbolTableConstructor implements Stmt.Visitor<Void>, Expr.Visitor<Object> {
	private final SymbolTable globalSymbolTable = new SymbolTable();
	private SymbolTable currentSymbolTable = globalSymbolTable;

	public SymbolTable getGlobalSymbolTable() {
		return globalSymbolTable;
	}

	@Override
	public Object visitBinaryExpr(Binary expr) {
		Object left = expr.left.accept(this);
		Object right = expr.right.accept(this);

		switch (expr.operator) {
			case PLUS: return (Integer)left + (Integer)right;
			case MINUS: return (Integer)left - (Integer)right;
			case DIVIDE: return (Integer)left / (Integer)right;
			case MULTIPLY: return (Integer)left * (Integer)right;

			case AND: return (Boolean)left && (Boolean)right;
			case OR: return (Boolean)left || (Boolean)right;

			case EQUAL: if (left instanceof Integer) return (Integer)left == (Integer)right;
						else if (left instanceof Boolean) return (Boolean)left == (Boolean)right;
			case NOT_EQUAL: if (left instanceof Integer) return (Integer)left != (Integer)right;
							else if (left instanceof Boolean) return (Boolean)left == (Boolean)right;
			
			case LESS: return (Integer)left < (Integer)right;
			case LESS_EQUAL: return (Integer)left <= (Integer)right;
			case GREATER: return (Integer)left > (Integer)right;
			case GREATER_EQUAL: return (Integer)left >= (Integer)right;

			default: return null; // Never reached
		}
	}

	@Override
	public Object visitLiteralExpr(Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitUnaryExpr(Unary expr) {
		Object right = expr.right.accept(this);
		switch (expr.operator) {
			case PLUS: return (Integer)right;
			case MINUS: return -(Integer)right;
			case NOT: return !(Boolean)right;
			default: return null; // Never reached
		}
	}

	@Override
	public Object visitVariableExpr(Variable expr) {
		return globalSymbolTable.getVariableInitialValue(expr.name);
	}

	@Override
	public Object visitCallExpr(Call expr) {
		return null;
	}

	@Override
	public Void visitBlockStmt(Block stmt) {
		for (Stmt s : stmt.statements) {
			s.accept(this);
		}
		return null;
	}

	@Override
	public Void visitExpressionStmt(Expression stmt) {
		return null;
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		currentSymbolTable = new SymbolTable();
		for (Stmt.Parameter p : stmt.params) {
			currentSymbolTable.putParameter(p.name(), p.type());
		}
		for (Stmt s : stmt.body) {
			s.accept(this);
		}
		globalSymbolTable.putFunction(stmt.name, stmt.returnType, currentSymbolTable);
		currentSymbolTable = globalSymbolTable;		
		return null;
	}

	@Override
	public Void visitIfStmt(If stmt) {
		stmt.thenBranch.accept(this);
		stmt.elseBranch.accept(this);
		return null;
	}

	@Override
	public Void visitPrintStmt(Print stmt) {
		return null;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		return null;
	}

	@Override
	public Void visitVarStmt(Var stmt) {
		Object initializer = null;
		if (currentSymbolTable == globalSymbolTable && stmt.initializer != null) initializer = stmt.initializer.accept(this);
		currentSymbolTable.putVariable(stmt.name, stmt.type, initializer);
		return null;
	}

	@Override
	public Void visitAssignStmt(Assign stmt) {
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		stmt.body.accept(this);
		return null;
	}
	
}
