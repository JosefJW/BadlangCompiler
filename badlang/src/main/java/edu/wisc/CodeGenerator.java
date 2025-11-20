package edu.wisc;

import java.util.List;
import java.util.Set;
import java.util.Stack;

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

	private String pushRegister(int registerIndex) {
		StringBuilder sb = new StringBuilder();

		sb.append("# Push register $t").append(registerIndex).append(" onto the stack\n");
		sb.append("addi $sp, $sp, -4\n"); // Make room on the stack
		sb.append("sw $t").append(registerIndex).append(", 0($sp)\n"); // Store the value from the register onto the stack

		return sb.toString();
	}

	private String popToRegister(int registerIndex) {
		StringBuilder sb = new StringBuilder();

		sb.append("# Pop top of the stack into $t").append(registerIndex).append("\n");
		sb.append("lw $t").append(registerIndex).append(", 0($sp)\n"); // Load from top of stack
		sb.append("addi $sp, $sp, 4\n"); // Move the stack pointer down

		return sb.toString();
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitBinaryExpr'");
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
		sb.append(pushRegister(0));

		return sb.toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		StringBuilder sb = new StringBuilder();

		// Get the righthand value on the stack
		sb.append(expr.right.accept(this));

		// Get the righthand value into register $t0
		sb.append(popToRegister(0));

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
		sb.append(pushRegister(0));

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
		sb.append(pushRegister(0));
		
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitBlockStmt'");
	}

	@Override
	public String visitExpressionStmt(Expression stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitExpressionStmt'");
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitPrintStmt'");
	}

	@Override
	public String visitReturnStmt(Return stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitReturnStmt'");
	}

	@Override
	public String visitVarStmt(Var stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitVarStmt'");
	}

	@Override
	public String visitAssignStmt(Assign stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitAssignStmt'");
	}

	@Override
	public String visitWhileStmt(While stmt) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'visitWhileStmt'");
	}
}
