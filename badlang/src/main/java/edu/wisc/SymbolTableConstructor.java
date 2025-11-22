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
import edu.wisc.Stmt.Println;
import edu.wisc.Stmt.Printsp;
import edu.wisc.Stmt.Return;
import edu.wisc.Stmt.Var;
import edu.wisc.Stmt.While;

/**
 * SymbolTableConstructor is an AST visitor responsible for constructing symbol tables
 * for a Badlang program. It walks the abstract syntax tree and collects all variable,
 * parameter, and function declarations, recording their types, offsets, and initial values.
 *
 * For functions, it also constructs a local symbol table for their parameters and local
 * variables. By the end of the traversal, every function and global scope has an associated
 * symbol table that can be used for code generation.
 *
 * This visitor assumes that variable renaming has already been performed, so all identifiers
 * are globally unique. It does not perform type checking or name analysis; it only records
 * the structure of identifiers in symbol tables.
 */
public class SymbolTableConstructor implements Stmt.Visitor<Void>, Expr.Visitor<Object> {
	private final SymbolTable globalSymbolTable = new SymbolTable(); // Stores the global symbol table for the program
	private SymbolTable currentSymbolTable = globalSymbolTable; // Holds either the global symbol table or the symbol table for the current function

	/**
	 * Gives the global symbol table to be used for code generation
	 * 
	 * @return The global symbol table
	 */
	public SymbolTable getGlobalSymbolTable() {
		return globalSymbolTable;
	}

	/**
	 * Evaluate the binary expression for global variable initial values.
	 * 
	 * @param expr The binary expression to evaluate
	 */
	@Override
	public Object visitBinaryExpr(Binary expr) {
		Object left = expr.left.accept(this);
		Object right = expr.right.accept(this);

		switch (expr.operator) {
			case PLUS: return (Integer)left + (Integer)right;
			case MINUS: return (Integer)left - (Integer)right;
			case DIVIDE: return (Integer)left / (Integer)right;
			case MULTIPLY: return (Integer)left * (Integer)right;
			case MODULO: return (Integer)left % (Integer)right;

			case AND: return (Boolean)left && (Boolean)right;
			case OR: return (Boolean)left || (Boolean)right;

			case EQUAL: if (left instanceof Integer) return (Integer)left == (Integer)right;
						else if (left instanceof Boolean) return (Boolean)left == (Boolean)right;
			case NOT_EQUAL: if (left instanceof Integer) return (Integer)left != (Integer)right;
							else if (left instanceof Boolean) return (Boolean)left != (Boolean)right;
			
			case LESS: return (Integer)left < (Integer)right;
			case LESS_EQUAL: return (Integer)left <= (Integer)right;
			case GREATER: return (Integer)left > (Integer)right;
			case GREATER_EQUAL: return (Integer)left >= (Integer)right;

			default: return null; // Never reached
		}
	}

	/**
	 * Evaluate the literal expression for global variable initial values.
	 * 
	 * @param expr The literal expression to evaluate
	 */
	@Override
	public Object visitLiteralExpr(Literal expr) {
		return expr.value;
	}

	/**
	 * Evaluate the unary expression for global variable initial values.
	 * 
	 * @param expr The unary expression to evaluate
	 */
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

	/**
	 * Evaluate the variable expression for global variable initial values.
	 * 
	 * @param expr The variable expression to evaluate
	 */
	@Override
	public Object visitVariableExpr(Variable expr) {
		return globalSymbolTable.getVariableInitialValue(expr.name);
	}

	/**
	 * Do nothing; global variable initial values cannot be contingent on executable code.
	 * 
	 * @param expr The call expression
	 */
	@Override
	public Object visitCallExpr(Call expr) {
		return null;
	}

	/**
	 * Traverse the block of statements
	 * 
	 * @param stmt The block statement to traverse
	 */
	@Override
	public Void visitBlockStmt(Block stmt) {
		for (Stmt s : stmt.statements) {
			s.accept(this);
		}
		return null;
	}

	/**
	 * Cannot define variables; do nothing.
	 * 
	 * @param stmt The expression statement
	 */
	@Override
	public Void visitExpressionStmt(Expression stmt) {
		return null;
	}

	/**
	 * Create a local symbol table for a function.
	 * 
	 * @param stmt The function statement to create a local symbol table for
	 */
	@Override
	public Void visitFunctionStmt(Function stmt) {
		// Create the function's local symbol table
		currentSymbolTable = new SymbolTable();
		for (Stmt.Parameter p : stmt.params) {
			currentSymbolTable.putParameter(p.name(), p.type());
		}
		for (Stmt s : stmt.body) {
			s.accept(this);
		}

		// Put the function in the global symbol table
		globalSymbolTable.putFunction(stmt.name, stmt.returnType, currentSymbolTable);
		
		currentSymbolTable = globalSymbolTable;		
		return null;
	}

	/**
	 * Traverse the branches of the if statement to search for local variable declarations.
	 * 
	 * @param stmt The if statement to traverse
	 */
	@Override
	public Void visitIfStmt(If stmt) {
		stmt.thenBranch.accept(this);
		if (stmt.elseBranch != null) stmt.elseBranch.accept(this);
		return null;
	}

	/**
	 * Cannot define variables; do nothing.
	 * 
	 * @param stmt The print statement
	 */
	@Override
	public Void visitPrintStmt(Print stmt) {
		return null;
	}

	/**
	 * Cannot define variables; do nothing.
	 * 
	 * @param stmt The printsp statement
	 */
	@Override
	public Void visitPrintspStmt(Printsp stmt) {
		return null;
	}

	/**
	 * Cannot define variables; do nothing.
	 * 
	 * @param stmt The println statement
	 */
	@Override
	public Void visitPrintlnStmt(Println stmt) {
		return null;
	}

	/**
	 * Cannot define variables; do nothing.
	 * 
	 * @param stmt The return statement
	 */
	@Override
	public Void visitReturnStmt(Return stmt) {
		return null;
	}

	/**
	 * Add the variable to the symbol table.
	 * If the variable is a global variable, check for its initialization value.
	 * 
	 * @param stmt The variable statement
	 */
	@Override
	public Void visitVarStmt(Var stmt) {
		Object initializer = null;
		if (currentSymbolTable == globalSymbolTable && stmt.initializer != null) initializer = stmt.initializer.accept(this);
		currentSymbolTable.putVariable(stmt.name, stmt.type, initializer);
		return null;
	}

	/**
	 * Cannot define variables; do nothing.
	 * 
	 * @param stmt The assign statement
	 */
	@Override
	public Void visitAssignStmt(Assign stmt) {
		return null;
	}

	/**
	 * Traverse the while body searching for local variable declarations.
	 * 
	 * @param stmt The while statement to traverse
	 */
	@Override
	public Void visitWhileStmt(While stmt) {
		stmt.body.accept(this);
		return null;
	}
	
}
