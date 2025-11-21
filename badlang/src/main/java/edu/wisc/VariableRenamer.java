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
 * Performs the variable and function renaming phase of the Badlang compiler.
 *
 * This pass assumes that Name Analysis and Type Analysis have succeeded, and
 * therefore the program is semantically valid. Its purpose is to transform all
 * identifiers (variables, parameters, and function names) into globally unique
 * versions. This allows the compiler to use an unscoped symbol table during
 * code generation, simplifying offset assignment and lookup.
 *
 * The VariableRenamer walks the AST while maintaining full scope information.
 * Each time a new binding is introduced, the renamer generates a unique
 * identifier (for example: x_0, x_1, ...). Any reference to that identifier
 * within the same scope resolves to the same unique name. Shadowed variables
 * automatically receive different unique identifiers.
 *
 * After this pass, all identifiers in the AST are globally unique, and the
 * structure and meaning of the program remain unchanged; only the names are
 * rewritten.
 */
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


    /**
     * Continue down the AST by traversing the left and right sides of the expression.
     * 
     * @param expr The binary expression to traverse
     */
    @Override
    public Void visitBinaryExpr(Binary expr) {
        expr.left.accept(this);
        expr.right.accept(this);
        return null;
    }

    /**
     * At a leaf node of the AST with no variables, so no work is needed to be done.
     * 
     * @param expr The literal expression
     */
    @Override
    public Void visitLiteralExpr(Literal expr) {
        return null;
    }

    /**
     * Continue down the AST by traversing the right side of the expression.
     * 
     * @param expr The unary expression to traverse
     */
    @Override
    public Void visitUnaryExpr(Unary expr) {
        expr.right.accept(this);
        return null;
    }

    /**
     * Because we assume the code is semantically correct at this step,
     * we know this variable should already have a unique label generated.
     * We simply replace the variable name with this unique label.
     * 
     * @param expr The variable expression to edit
     */
    @Override
    public Void visitVariableExpr(Variable expr) {
        String uniqueLabel = environment.getUniqueLabel(expr.name);
        expr.name = uniqueLabel;
        return null;
    }

    /**
     * Because we assume the code is semantically correct at this step,
     * we know this function call should already have a unique label generated.
     * We simply replace the function call with a call to the unique label.
     * 
     * We also perform variable renaming in each of the call's arguments.
     * 
     * @param expr The call expression to edit
     */
    @Override
    public Void visitCallExpr(Call expr) {
        expr.name = environment.getUniqueLabel(expr.name);
        for (Expr arg : expr.arguments) {
            arg.accept(this);
        }
        return null;
    }

    /**
     * Create a new environment to preserve scope while doing variable renaming
     * and traverse the statements within the block.
     * 
     * @param stmt The block statement to traverse
     */
    @Override
    public Void visitBlockStmt(Block stmt) {
        environment = new Environment(environment, null);
        for (Stmt s : stmt.statements) {
            s.accept(this);
        }
        environment = environment.getParent();
        return null;
    }

    /**
     * Continue down the AST by traversing the expression.
     * 
     * @param stmt The expression statement to traverse
     */
    @Override
    public Void visitExpressionStmt(Expression stmt) {
        stmt.expression.accept(this);
        return null;
    }

    /**
     * Because we assume the code is semantically correct at this step,
     * we know this function signature should be valid.
     * We simply replace the function signature (including the function name and parameter names)
     * with unique identifiers.
     * 
     * Note: The main function will not have its name replaced.
     * 
     * @param stmt The function statement to traverse
     */
    @Override
    public Void visitFunctionStmt(Function stmt) {
        // Replace the function name with a unique identifiers
        if (!stmt.name.equals("main")) {
            environment.setUniqueLabel(stmt.name, stmt.name + "_" + varCount++);
            stmt.name = environment.getUniqueLabel(stmt.name);
        }
        else {
            environment.setUniqueLabel(stmt.name, stmt.name);
        }

        // Create a new environment for scoping
        environment = new Environment(environment, stmt.returnType);
        
        // Replace the parameters with new ones with unique identifiers
        List<Stmt.Parameter> newParams = new ArrayList<>();
        for (int i = 0; i < stmt.params.size(); i++) {
            Stmt.Parameter p = stmt.params.get(i);
            environment.declare(p.name(), p.type(), IdentifierType.VARIABLE, null, true);
            environment.setUniqueLabel(p.name(), p.name() + "_" + varCount++);
            Stmt.Parameter newP = new Stmt.Parameter(environment.getUniqueLabel(p.name()), p.type(), p.startCol(), p.endCol(), p.startLine(), p.endLine());
            newParams.add(newP);
        }
        stmt.params = newParams;

        // Traverse the function body
        for (Stmt s : stmt.body) {
            s.accept(this);
        }

        environment = environment.getParent();
        return null;
    }

    /**
     * Continue down the AST by traversing the condition and all branches of the if statement.
     * 
     * @param stmt The if statement to traverse
     */
    @Override
    public Void visitIfStmt(If stmt) {
        stmt.condition.accept(this);
        stmt.thenBranch.accept(this);
        if (stmt.elseBranch != null) stmt.elseBranch.accept(this);
        return null;
    }

    /**
     * Continue down the AST by traversing the statement's expression.
     * 
     * @param stmt The print statement to traverse
     */
    @Override
    public Void visitPrintStmt(Print stmt) {
        stmt.expression.accept(this);
        return null;
    }

    /**
     * Continue down the AST by traversing the statement's expression.
     * 
     * @param stmt The printsp statement to traverse
     */
    @Override
    public Void visitPrintspStmt(Printsp stmt) {
        if (stmt.expression != null) stmt.expression.accept(this);
        return null;
    }

    /**
     * Continue down the AST by traversing the statement's expression.
     * 
     * @param stmt The println statement to traverse
     */
    @Override
    public Void visitPrintlnStmt(Println stmt) {
        if (stmt.expression != null) stmt.expression.accept(this);
        return null;
    }

    /**
     * Continue down the AST by traversing the statement's expression.
     * 
     * @param stmt The return statement to traverse
     */
    @Override
    public Void visitReturnStmt(Return stmt) {
        stmt.value.accept(this);
        return null;
    }

    /**
     * Replace the variable node's name with a unique label generated for it.
     * 
     * @param stmt The variable statement to handle
     */
    @Override
    public Void visitVarStmt(Var stmt) {
        // Continue traversing the AST if there is an expression
        if (stmt.initializer != null) stmt.initializer.accept(this);

        // Generate and assign a unique label for this variable
        environment.declare(stmt.name, stmt.type, IdentifierType.VARIABLE, null, false);
        environment.setUniqueLabel(stmt.name, stmt.name + "_" + varCount++);
        stmt.name = environment.getUniqueLabel(stmt.name);

        return null;
    }

    /**
     * Because we assume the code is semantically correct at this step,
     * we know this variable should already have a unique label generated.
     * We simply replace the variable name with this unique label.
     * 
     * @param expr The assign statement to edit
     */
    @Override
    public Void visitAssignStmt(Assign stmt) {
        stmt.value.accept(this);
        stmt.name = environment.getUniqueLabel(stmt.name);
        return null;
    }

    /**
     * Continue down the AST by traversing both parts of the while statement.
     * 
     * @param stmt The while statement to traverse
     */
    @Override
    public Void visitWhileStmt(While stmt) {
        stmt.condition.accept(this);
        stmt.body.accept(this);
        return null;
    }
    
}
