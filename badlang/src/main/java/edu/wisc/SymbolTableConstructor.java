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

public class SymbolTableConstructor implements Stmt.Visitor<Void>, Expr.Visitor<Void> {

	@Override
	public Void visitBinaryExpr(Binary expr) {
		return null;
	}

	@Override
	public Void visitLiteralExpr(Literal expr) {
		return null;
	}

	@Override
	public Void visitUnaryExpr(Unary expr) {
		return null;
	}

	@Override
	public Void visitVariableExpr(Variable expr) {
		return null;
	}

	@Override
	public Void visitCallExpr(Call expr) {
		return null;
	}

	@Override
	public Void visitBlockStmt(Block stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitBlockStmt'");
	}

	@Override
	public Void visitExpressionStmt(Expression stmt) {
		return null;
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitFunctionStmt'");
	}

	@Override
	public Void visitIfStmt(If stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitVarStmt'");
	}

	@Override
	public Void visitAssignStmt(Assign stmt) {
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
	}
	
}
