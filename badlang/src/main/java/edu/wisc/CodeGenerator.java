package edu.wisc;

import java.util.List;
import java.util.Set;

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

public class CodeGenerator implements Stmt.Visitor<String>, Expr.Visitor<String>{
	private final SymbolTable globalSymbolTable;
	private SymbolTable currentSymbolTable;

	public CodeGenerator(SymbolTable globalSymbolTable) {
		this.globalSymbolTable = globalSymbolTable;
		this.currentSymbolTable = globalSymbolTable;
	}

	public String generate(List<Stmt> AST) {
		StringBuilder sb = new StringBuilder();
		sb.append(dataSection());
		sb.append(textSection(AST));
		return "";
	}

	private String dataSection() {
		StringBuilder sb = new StringBuilder(".data\n");
		Set<Identifier> globalVariables = globalSymbolTable.getVariables();
		for (Identifier var : globalVariables) {
			sb.append("_");
			sb.append(var.getName());
			sb.append(": ");
			sb.append(".word ");
			switch(var.getType()) {
				case INT: {
					sb.append(var.getInitial() != null ? var.getInitial() : 0);
					break;
				}
				case BOOL: {
					sb.append(var.getInitial() != null && (Boolean)var.getInitial() ? 1 : 0);
					break;
				}
				default: throw new RuntimeException("Invalid variable type"); // Should never reach
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	private String textSection(List<Stmt> AST) {
		StringBuilder sb = new StringBuilder(".text\n");
		sb.append("j _main\n");

		for (Stmt s : AST) {
			if (s instanceof Stmt.Function) {
				sb.append(s.accept(this));
			}
		}

		return sb.toString();
	}

	private String pushRegister(String register) {
		StringBuilder sb = new StringBuilder();

		sb.append("# Push register ").append(register).append(" onto the stack\n");
		sb.append("addi $sp, $sp, -4\n"); // Make room on the stack
		sb.append("sw ").append(register).append(", 0($sp)\n"); // Store the value from the register onto the stack

		return sb.toString();
	}

	private String popToRegister(String register) {
		StringBuilder sb = new StringBuilder();

		sb.append("# Pop top of the stack into ").append(register).append("\n");
		sb.append("lw ").append(register).append(", 0($sp)\n"); // Load from top of stack
		sb.append("addi $sp, $sp, 4\n"); // Move the stack pointer down

		return sb.toString();
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		StringBuilder sb = new StringBuilder();

		sb.append("# Push literal value ").append(expr.value).append(" onto the stack.\n");

		// Load value into register
		sb.append("li $t0, ");
		if (expr.value instanceof Integer) {
			sb.append(expr.value);
		}
		else if (expr.value instanceof Boolean) {
			if ((Boolean)expr.value) sb.append("1");
			else sb.append("0");
		}
		sb.append("\n");

		// Push register onto the stack
		sb.append(pushRegister("$t0"));

		return sb.toString();
	}

	@Override
	public String visitVariableExpr(Variable expr) {
		StringBuilder sb = new StringBuilder();

		sb.append("# Push variable ").append(expr.name).append(" onto the stack.\n");

		// Load variable into register
		if (currentSymbolTable.contains(expr.name)) {
			// Either a local variable or parameter
			Identifier var = currentSymbolTable.get(expr.name);
			if (var.isParameter()) {
				sb.append("lw $t0, ").append(8 + var.getOffset()).append("($fp)\n");
			}
			else if (var.isVariable()) {
				sb.append("lw $t0, ").append(-4 - var.getOffset()).append("($fp)\n");
			}
		}
		else {
			// A global variable
			sb.append("lw $t0, _").append(expr.name).append("\n");
		}

		// Push register onto the stack
		sb.append(pushRegister("$t0"));
		
		return sb.toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		StringBuilder sb = new StringBuilder();

		// Get the righthand value on the stack
		sb.append(expr.right.accept(this));

		// Get the righthand value into register $t0
		sb.append(popToRegister("$t0"));

		if (expr.operator == Operator.PLUS) {
			// Register stays the same, no additional instructions needed
		}
		else if (expr.operator == Operator.MINUS) {
			// Negate register
			sb.append("sub $t0, $zero, $t0\n");
		}
		else if (expr.operator == Operator.NOT) {
			// Not register
			sb.append("xori $t0, $t0, 1\n");
		}
				
		// Push register onto stack
		sb.append(pushRegister("$t0"));

		return sb.toString();
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		StringBuilder sb = new StringBuilder();

		// Get the first value on the stack
		sb.append(expr.left.accept(this));

		// Get the second value on the stack
		sb.append(expr.right.accept(this));

		// Pop the values into registers
		sb.append(popToRegister("$t1")); // Put the second value in register $t1
		sb.append(popToRegister("$t0")); // Put the first value in register $t0
		
		// Perform the operation, saving the result in register $t0
		switch(expr.operator) {
			case PLUS: sb.append("addu $t0, $t0, $t1\n"); break;
			case MINUS: sb.append("subu $t0, $t0, $t1\n"); break;
			case DIVIDE: sb.append("divu $t0, $t1\n"); sb.append("mflo $t0\n"); break;
			case MULTIPLY: sb.append("multu $t0, $t1\n"); sb.append("mflo $t0\n"); break;

			case AND: sb.append("and $t0, $t0, $t1\n"); break;
			case OR: sb.append("or $t0, $t0, $1\n"); break;

			case EQUAL: {
				sb.append("slt $t2, $t0, $t1\n"); // If $t0 < $t1, set to 1, else 0
				sb.append("slt $t3, $t1, $t0\n"); // If $t1 < $t0, set to 1, else 0
				sb.append("or $t0, $t2, $t3\n"); // If $t0 != $t1, set to 1, else 0
				sb.append("xori $t0, $t0, 1\n"); // Negate $t0
				break;
			}
			case NOT_EQUAL: {
				sb.append("slt $t2, $t0, $t1\n"); // If $t0 < $t1, set to 1, else 0
				sb.append("slt $t3, $t1, $t0\n"); // If $t1 < $t0, set to 1, else 0
				sb.append("or $t0, $t2, $t3\n"); // If $t0 != $t1, set to 1, else 0
				break;
			}

			case LESS: sb.append("slt $t0, $t0, $t1\n"); break;
			case LESS_EQUAL: {
				// Check for less
				sb.append("slt $t2, $t0, $t1\n"); // Set $t2 to 1 if $t0 < $t1, else 0
				
				// Check for equal (follows same process as EQUAL case)
				sb.append("slt $t3, $t0, $t1\n");
				sb.append("slt $t4, $t1, $t0\n");
				sb.append("or $t3, $t3, $t4\n");
				sb.append("xori $t3, $t3, 1\n");

				// Less or equal
				sb.append("or $t0, $t2, $t3\n"); // Set $t0 to 1 if $t2 or $t3
				break;
			}
			case GREATER: {
				// Check for less
				sb.append("slt $t2, $t0, $t1\n"); // Set $t2 to 1 if $t0 < $t1, else 0
				
				// Check for equal (follows same process as EQUAL case)
				sb.append("slt $t3, $t0, $t1\n");
				sb.append("slt $t4, $t1, $t0\n");
				sb.append("or $t3, $t3, $t4\n");
				sb.append("xori $t3, $t3, 1\n");

				// Less or equal
				sb.append("or $t0, $t2, $t3\n"); // Set $t0 to 1 if $t2 or $t3

				// NOT the result
				sb.append("xori $t0, $t0, 1\n");
				break;
			}
			case GREATER_EQUAL: {
				sb.append("slt $t0, $t0, $t1\n"); // Set $t0 to 1 if $t0 < $t1, else 0
				sb.append("xori $t0, $t0, 1\n"); // NOT $t0
				break;
			}

			default: throw new RuntimeException("Operator not allowed!"); // Should never reach
		}

		sb.append(pushRegister("$t0"));

		return sb.toString();
	}

	@Override
	public String visitCallExpr(Call expr) {
		SymbolTable previousSymbolTable = currentSymbolTable;
		// current = new symbol table


		currentSymbolTable = previousSymbolTable;
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitCallExpr'");
	}

	@Override
	public String visitBlockStmt(Block stmt) {
		StringBuilder sb = new StringBuilder();
		
		for (Stmt s : stmt.statements) {
			sb.append(s.accept(this));
		}

		return sb.toString();
	}

	@Override
	public String visitExpressionStmt(Expression stmt) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(stmt.expression.accept(this));

		return sb.toString();
	}

	@Override
	public String visitFunctionStmt(Function stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitFunctionStmt'");
	}

	@Override
	public String visitIfStmt(If stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitIfStmt'");
	}

	@Override
	public String visitPrintStmt(Print stmt) {
		StringBuilder sb = new StringBuilder();

		// Get the value placed on the stack
		sb.append(stmt.expression.accept(this));

		// Set the syscall for printing
		sb.append("li $v0, 1\n");

		// Pop the value into the $a0 register
		sb.append(popToRegister("$a0"));

		// Execute the syscall
		sb.append("syscall\n");

		return sb.toString();
	}

	@Override
	public String visitReturnStmt(Return stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitReturnStmt'");
	}

	@Override
	public String visitVarStmt(Var stmt) {
		StringBuilder sb = new StringBuilder();

		if (stmt.initializer != null) {
			// Get the value on the stack
			sb.append(stmt.initializer.accept(this));

			// Pop the value into $t0
			sb.append(popToRegister("$t0"));

			if (currentSymbolTable.contains(stmt.name)) {
				// Local variable or parameter
				Identifier var = currentSymbolTable.get(stmt.name);
				if (var.isParameter()) {
					// Load address of the parameter into $t1
					sb.append("la $t1, ").append(8 + var.getOffset()).append("($fp)\n");
				}
				else {
					// Load the address of the local variable into $t1
					sb.append("la $t1, ").append(-4 - var.getOffset()).append("($fp)\n");
				}
			}
			else {
				// Global variable
				// Load the address of the global variable into $t1
				sb.append("la $t1, _").append(stmt.name).append("\n");
			}

			// Store the value in $t0 to the address in $t1
			sb.append("sw $t0, $t1\n");
		}

		return sb.toString();
	}

	@Override
	public String visitAssignStmt(Assign stmt) {
		StringBuilder sb = new StringBuilder();

		// Get the value on the stack
		sb.append(stmt.value.accept(this));

		// Pop the value into $t0
		sb.append(popToRegister("$t0"));

		if (currentSymbolTable.contains(stmt.name)) {
			// Local variable or parameter
			Identifier var = currentSymbolTable.get(stmt.name);
			if (var.isParameter()) {
				// Load address of the parameter into $t1
				sb.append("la $t1, ").append(8 + var.getOffset()).append("($fp)\n");
			}
			else {
				// Load the address of the local variable into $t1
				sb.append("la $t1, ").append(-4 - var.getOffset()).append("($fp)\n");
			}
		}
		else {
			// Global variable
			// Load the address of the global variable into $t1
			sb.append("la $t1, _").append(stmt.name).append("\n");
		}

		// Store the value in $t0 to the address in $t1
		sb.append("sw $t0, $t1\n");
		
		return sb.toString();
	}

	@Override
	public String visitWhileStmt(While stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
	}
}
