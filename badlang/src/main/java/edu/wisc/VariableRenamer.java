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

public class VariableRenamer implements Stmt.Visitor<Void>, Expr.Visitor<Void>{
    Environment environment; // The current scope
	int varCount = 0; // Used for give unique labels to each variable

	/**
	 * Initialize a new variable renamer
	 * 
	 * @param environment  The environment to use as a global environmnet
	 */
	public VariableRenamer(Environment environment) {
		this.environment = environment;
	}


    @Override
    public Void visitBinaryExpr(Binary expr) {
        expr.left.accept(this);
        expr.right.accept(this);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Literal expr) {
        return null;
    }

    @Override
    public Void visitUnaryExpr(Unary expr) {
        expr.right.accept(this);
        return null;
    }

    @Override
    public Void visitVariableExpr(Variable expr) {
        String uniqueLabel = environment.getUniqueLabel(expr.name);
        expr.name = uniqueLabel;
        return null;
    }

    @Override
    public Void visitCallExpr(Call expr) {
        expr.name = environment.getUniqueLabel(expr.name);
        for (Expr arg : expr.arguments) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        environment = new Environment(environment, null);
        for (Stmt s : stmt.statements) {
            s.accept(this);
        }
        environment = environment.getParent();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Function stmt) {
        if (!stmt.name.equals("main")) {
            environment.setUniqueLabel(stmt.name, stmt.name + "_" + varCount++);
            stmt.name = environment.getUniqueLabel(stmt.name);
        }
        else {
            environment.setUniqueLabel(stmt.name, stmt.name);
        }

        environment = new Environment(environment, stmt.returnType);
        
        List<Stmt.Parameter> newParams = new ArrayList<>();
        for (int i = 0; i < stmt.params.size(); i++) {
            Stmt.Parameter p = stmt.params.get(i);
            environment.declare(p.name(), p.type(), IdentifierType.VARIABLE, null, true);
            environment.setUniqueLabel(p.name(), p.name() + "_" + varCount++);
            Stmt.Parameter newP = new Stmt.Parameter(environment.getUniqueLabel(p.name()), p.type(), p.startCol(), p.endCol(), p.startLine(), p.endLine());
            newParams.add(newP);
        }
        stmt.params = newParams;

        for (Stmt s : stmt.body) {
            s.accept(this);
        }

        environment = environment.getParent();
        return null;
    }

    @Override
    public Void visitIfStmt(If stmt) {
        stmt.condition.accept(this);
        stmt.thenBranch.accept(this);
        if (stmt.elseBranch != null) stmt.elseBranch.accept(this);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visitPrintspStmt(Printsp stmt) {
        if (stmt.expression != null) stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visitPrintlnStmt(Println stmt) {
        if (stmt.expression != null) stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) {
        stmt.value.accept(this);
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        if (stmt.initializer != null) stmt.initializer.accept(this);
        environment.declare(stmt.name, stmt.type, IdentifierType.VARIABLE, null, false);
        environment.setUniqueLabel(stmt.name, stmt.name + "_" + varCount++);
        stmt.name = environment.getUniqueLabel(stmt.name);
        return null;
    }

    @Override
    public Void visitAssignStmt(Assign stmt) {
        stmt.value.accept(this);
        stmt.name = environment.getUniqueLabel(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        stmt.condition.accept(this);
        stmt.body.accept(this);
        return null;
    }
    
}
