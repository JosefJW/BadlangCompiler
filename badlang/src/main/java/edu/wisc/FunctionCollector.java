package edu.wisc;

import java.util.ArrayList;
import java.util.Arrays;
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

public class FunctionCollector implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
    private Environment environment = new Environment(null, null);
	private List<String> programLines;
	List<Error> errors = new ArrayList<Error>();
    int errorCount = 0;

    public FunctionCollector(List<String> programLines) {
        this.programLines = programLines;
    }

    public Environment getGlobalEnvironment() { return environment; }

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
        String msg = "Global variable initial values must be constant; this is not a constant value.";
        errors.add(new Error(Arrays.asList(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg)), programLines.subList(expr.getStartLine()-1, expr.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitFunctionStmt(Function stmt) {
        if (environment.isDeclared(stmt.name)) {
            String msg = "Function \'" + stmt.name + "\' was previously declared; functions cannot be redeclared.";
            errors.add(new Error(Arrays.asList(new Problem(stmt.getHeaderStartCol(), stmt.getHeaderEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getHeaderStartLine()-1, stmt.getHeaderEndLine()), ErrorType.NAME));
            errorCount++;
        }
        else environment.declare(stmt.name, stmt.returnType, IdentifierType.FUNCTION, stmt.params, false);
        return null;
    }

    @Override
    public Void visitIfStmt(If stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitPrintspStmt(Printsp stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitPrintlnStmt(Println stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitReturnStmt(Return stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        return null;
    }

    @Override
    public Void visitAssignStmt(Assign stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        String msg = "Global statements are not allowed; all executable statements must appear inside of a function.";
        errors.add(new Error(Arrays.asList(new Problem(stmt.getStartCol(), stmt.getEndCol(), stmt.getStartLine(), stmt.getEndLine(), msg)), programLines.subList(stmt.getStartLine()-1, stmt.getEndLine()), ErrorType.SCOPE));
        errorCount++;
        return null;
    }
    
}
