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
import edu.wisc.Stmt.Println;
import edu.wisc.Stmt.Printsp;
import edu.wisc.Stmt.Return;
import edu.wisc.Stmt.Var;
import edu.wisc.Stmt.While;

/**
 * CodeGenerator is a visitor that walks the AST and generates MIPS assembly code
 * for a Badlang program. 
 *
 * It uses the symbol tables constructed by SymbolTableConstructor to determine
 * offsets, variable locations, and function layouts. The generator handles
 * global variables, function prologues and epilogues, expressions, and control
 * flow statements. Expressions are evaluated using a stack-based approach, and
 * function calls follow a consistent calling convention.
 *
 * This class assumes that the AST has already been validated through name analysis,
 * type analysis, and variable renaming, so all identifiers are unique and
 * semantically correct.
 */
public class CodeGenerator implements Stmt.Visitor<String>, Expr.Visitor<String>{
	private final SymbolTable globalSymbolTable; // Stores the global symbol table for the program
	private SymbolTable currentSymbolTable; // Holds either the global symbol table or the symbol table for the current function
	private String currentFunction = "main"; // Name of the current function for making labels
	private int labelCounter = 0; // Value to use for making unique labels

	public CodeGenerator(SymbolTable globalSymbolTable) {
		this.globalSymbolTable = globalSymbolTable;
		this.currentSymbolTable = globalSymbolTable;
	}

	/**
	 * Start the generation process
	 * 
	 * @param AST The AST to generate MIPS code for
	 * @return A string with the MIPS code
	 */
	public String generate(List<Stmt> AST) {
		StringBuilder sb = new StringBuilder();
		sb.append(dataSection());
		sb.append(textSection(AST));
		return sb.toString();
	}

	/**
	 * Generate the .data section based off the class variable globalSymbolTable.
	 * 
	 * @return A string with the .data section
	 */
	private String dataSection() {
		StringBuilder sb = new StringBuilder(".data\n");


		// Reserve space for all global variables
		Set<STIdentifier> globalVariables = globalSymbolTable.getVariables();
		for (STIdentifier var : globalVariables) {
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

	/**
	 * Generate the .text section based off the AST and the global symbol table.
	 * 
	 * @param AST The AST to generate the .text section for
	 * @return A string with the .text section
	 */
	private String textSection(List<Stmt> AST) {
		StringBuilder sb = new StringBuilder(".text\n");

		// Jump to main
		sb.append("jal _main\n");

		// After returning from main, terminate the program
		sb.append("li $v0, 10\n");
		sb.append("syscall\n");

		// Write function code
		for (Stmt s : AST) {
			if (s instanceof Stmt.Function) {
				sb.append(s.accept(this));
			}
		}

		return sb.toString();
	}

	/**
	 * Push the value in a given register onto the stack.
	 * 
	 * @param register A string with the register name (e.g., "$t0")
	 * @return A string with the MIPS code for pushing the register onto the stack
	 */
	private String pushRegister(String register) {
		StringBuilder sb = new StringBuilder();

		sb.append("addi $sp, $sp, -4\n"); // Make room on the stack
		sb.append("sw ").append(register).append(", 0($sp)\n"); // Store the value from the register onto the stack

		return sb.toString();
	}

	/**
	 * Pop the top value on the stack into a given register.
	 * 
	 * @param register A string with the register name (e.e., "$t0")
	 * @return A string with the MIPS code for popping into the register
	 */
	private String popToRegister(String register) {
		StringBuilder sb = new StringBuilder();

		sb.append("lw ").append(register).append(", 0($sp)\n"); // Load from top of stack
		sb.append("addi $sp, $sp, 4\n"); // Move the stack pointer down

		return sb.toString();
	}

	/**
	 * Push the literal value onto the stack.
	 * 
	 * @param expr The literal value
	 * @return The MIPS code for the literal expression
	 */
	@Override
	public String visitLiteralExpr(Literal expr) {
		StringBuilder sb = new StringBuilder();

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

	/**
	 * Load the variable's value and push it onto the stack.
	 * 
	 * @param expr The variable
	 * @return The MIPS code for the variable expression
	 */
	@Override
	public String visitVariableExpr(Variable expr) {
		StringBuilder sb = new StringBuilder();

		// Load variable into register
		if (currentSymbolTable.contains(expr.name)) {
			// Either a local variable or parameter
			STIdentifier var = currentSymbolTable.get(expr.name);
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

	/**
	 * Evaluate the right side of the expression.
	 * The result will be on the top of the stack.
	 * Pop the result and perform the operation, pushing the new result onto the stack.
	 * 
	 * @param expr The unary expression
	 * @return The MIPS code for the unary expression
	 */
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

	/**
	 * Evaluate the left and right sides of the expression.
	 * The results will be the top two values on the stack.
	 * Pop the results, perform the operation, and push the new result onto the stack.
	 * 
	 * @param expr The binary expression
	 * @return The MIPS code for the binary expression
	 */
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
			case MODULO: sb.append("divu $t0, $t1\n"); sb.append("mfhi $t0\n"); break;

			case AND: sb.append("and $t0, $t0, $t1\n"); break;
			case OR: sb.append("or $t0, $t0, $t1\n"); break;

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

	/**
	 * Push the call parameters onto the stack in reverse order.
	 * Jump to the called function.
	 * Deallocate the parameters from the stack.
	 * 
	 * @param expr The call expression
	 * @return The MIPS code for the call expression
	 */
	@Override
	public String visitCallExpr(Call expr) {
		StringBuilder sb = new StringBuilder();
		
		// PROLOGUE
		// Push parameters
		for (int i = expr.arguments.size()-1; i >= 0; i--) {
			// Evaluate expression (automatically placed onto stack)
			sb.append(expr.arguments.get(i).accept(this));
		}

		// EXECUTION
		// Jump to function
		sb.append("jal _").append(expr.name).append("\n");

		// EPILOGUE
		// Deallocate arguments
		sb.append("addi $sp, $sp, ").append(expr.arguments.size() * 4).append("\n");

		// Push return value onto stack
		sb.append(pushRegister("$v0"));

		return sb.toString();
	}

	/**
	 * Visit each statement in the block
	 * 
	 * @param stmt The block statement
	 * @return The MIPS code for the block statement
	 */
	@Override
	public String visitBlockStmt(Block stmt) {
		StringBuilder sb = new StringBuilder();
		
		for (Stmt s : stmt.statements) {
			sb.append(s.accept(this));
		}

		return sb.toString();
	}

	/**
	 * Evaluate the expression and pop the result off of the stack.
	 * 
	 * @param stmt The expression statement
	 * @return The MIPS code for the expression statement
	 */
	@Override
	public String visitExpressionStmt(Expression stmt) {
		StringBuilder sb = new StringBuilder();
		
		// Evaluate expression
		sb.append(stmt.expression.accept(this));

		// Pop the value off the stack
		sb.append(popToRegister("$t0"));

		return sb.toString();
	}

	/**
	 * Create a function label, code for saving $ra and $fp, and make room for locals.
	 * Create the code for the function body.
	 * Create an epilogue label, deallocate the local variables, and restore $ra and $fp.
	 * 
	 * @param stmt The function statement
	 * @return The MIPS code for the function statement
	 */
	@Override
	public String visitFunctionStmt(Function stmt) {
		StringBuilder sb = new StringBuilder();

		// PROLOGUE
		// Function label
		sb.append("_").append(stmt.name).append(":\n");

		// Save return address
		sb.append(pushRegister("$ra"));

		// Save old frame pointer
		sb.append(pushRegister("$fp"));

		// Set new frame pointer
		sb.append("move $fp, $sp\n");

		// Make room for locals
		Integer size = globalSymbolTable.get(stmt.name).getLocalVarsSize();
		sb.append("addi $sp, $sp, -").append(size).append("\n");

		// Save bookkeeping data
		SymbolTable previousSymbolTable = currentSymbolTable;
		String previousFunction = currentFunction;

		// Set new bookkeeping data
		currentSymbolTable = globalSymbolTable.get(stmt.name).getLocalVars();
		currentFunction = stmt.name;

		// BODY
		for (Stmt s : stmt.body) {
			sb.append(s.accept(this));
		}

		// EPILOGUE
		// Epilogue label
		sb.append("_").append(stmt.name).append("_epilogue").append(":\n");

		// Deallocate local variable space
		sb.append("addi $sp, $sp, ").append(size).append("\n");

		// Restore return address
		sb.append("lw $ra, 4($fp)\n");

		// Restore old frame pointer
		sb.append("lw $fp, 0($fp)\n");

		// Deallocate area from stack
		sb.append("addi $sp, $sp, 8\n");

		// Return to caller
		sb.append("jr $ra\n");

		// Restore bookkeeping
		currentSymbolTable = previousSymbolTable;
		currentFunction = previousFunction;

		return sb.toString();
	}

	/**
	 * Load the expression value to print and perform the syscall.
	 * 
	 * @param stmt The print statement
	 * @return The MIPS code for the print statement
	 */
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

	/**
	 * Load the expression value to print, perform the syscall, and then load a space to print and perform the syscall.
	 * 
	 * @param stmt The printsp statement
	 * @return The MIPS code for the printsp statement
	 */
	@Override
	public String visitPrintspStmt(Printsp stmt) {
		StringBuilder sb = new StringBuilder();

		if (stmt.expression != null) {
			// Get the value placed on the stack
			sb.append(stmt.expression.accept(this));

			// Set the syscall for printing
			sb.append("li $v0, 1\n");

			// Pop the value into the $a0 register
			sb.append(popToRegister("$a0"));

			// Execute the syscall
			sb.append("syscall\n");
		}

		// Set the syscall for printing a char
		sb.append("li $v0, 11\n");

		// Store a space for printing
		sb.append("li $a0, 32\n");

		// Print the space
		sb.append("syscall\n");

		return sb.toString();
	}

	/**
	 * Load the expression value to print, perform the syscall, and then load a newline to print and perform the syscall.
	 * 
	 * @param stmt The println statement
	 * @return The MIPS code for the println statement
	 */
	@Override
	public String visitPrintlnStmt(Println stmt) {
		StringBuilder sb = new StringBuilder();

		if (stmt.expression != null) {
			// Get the value placed on the stack
			sb.append(stmt.expression.accept(this));

			// Set the syscall for printing
			sb.append("li $v0, 1\n");

			// Pop the value into the $a0 register
			sb.append(popToRegister("$a0"));

			// Execute the syscall
			sb.append("syscall\n");
		}

		// Set the syscall for printing a char
		sb.append("li $v0, 11\n");

		// Store "\n" for printing
		sb.append("li $a0, 10\n");

		// Print the newline
		sb.append("syscall\n");

		return sb.toString();
	}

	/**
	 * Evaluate the return expression, put the value into $v0, and then jump to the function epilogue.
	 * 
	 * @param stmt The return statement
	 * @return The MIPS code for the return statement
	 */
	@Override
	public String visitReturnStmt(Return stmt) {
		StringBuilder sb = new StringBuilder();

		// Evaluate return value
		sb.append(stmt.value.accept(this));

		// Put the return value in $v0
		sb.append(popToRegister("$v0"));

		// Jump to the function epilogue
		sb.append("j _").append(currentFunction).append("_epilogue\n");

		return sb.toString();
	}

	/**
	 * Load the variable's address and store the initial value at that address.
	 * 
	 * @param stmt The variable statement
	 * @return The MIPS code for the variable statement
	 */
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
				STIdentifier var = currentSymbolTable.get(stmt.name);
				if (var.isParameter()) {
					// Load address of the parameter into $t1
					sb.append("addi $t1, $fp, ").append(8 + var.getOffset()).append("\n");
				}
				else {
					// Load the address of the local variable into $t1
					sb.append("addi $t1, $fp, ").append(-4 - var.getOffset()).append("\n");
				}
			}
			else {
				// Global variable
				// Load the address of the global variable into $t1
				sb.append("la $t1, _").append(stmt.name).append("\n");
			}

			// Store the value in $t0 to the address in $t1
			sb.append("sw $t0, 0($t1)\n");
		}

		return sb.toString();
	}

	/**
	 * Load the variable's address and store the new value at that address.
	 * 
	 * @param stmt The assign statement
	 * @return The MIPS code for the assign statement
	 */
	@Override
	public String visitAssignStmt(Assign stmt) {
		StringBuilder sb = new StringBuilder();

		// Get the value on the stack
		sb.append(stmt.value.accept(this));

		// Pop the value into $t0
		sb.append(popToRegister("$t0"));

		if (currentSymbolTable.contains(stmt.name)) {
			// Local variable or parameter
			STIdentifier var = currentSymbolTable.get(stmt.name);
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
		sb.append("sw $t0, 0($t1)\n");
		
		return sb.toString();
	}

	/**
	 * Evaluate the if condition and branch accordingly.
	 * 
	 * @param stmt The if statement
	 * @return The MIPS code for the if statement
	 */
	@Override
	public String visitIfStmt(If stmt) {
		StringBuilder sb = new StringBuilder();

		int labelNum = labelCounter++; // Get unique label number for making unique labels

		// Get condition value on stack
		sb.append(stmt.condition.accept(this));

		// Get condition value in register $t0
		sb.append(popToRegister("$t0"));

		// Check if condition is true
		// If $t0 == 1, then condition is true, continue with then branch
		// If $t0 == 0, then condition is false, jump to else branch
		sb.append("beq $t0, $zero, if_else_").append(labelNum).append("\n");

		// Put if statement then branch code
		sb.append(stmt.thenBranch.accept(this));

		// Jump to exit after then branch
		sb.append("j if_exit_").append(labelNum).append("\n");

		// Else branch label
		sb.append("if_else_").append(labelNum).append(":\n");

		// Put if statement else branch code
		if (stmt.elseBranch != null) {
			sb.append(stmt.elseBranch.accept(this));
		}

		// Exit label
		sb.append("if_exit_").append(labelNum).append(":\n");

		return sb.toString();
	}

	/**
	 * Evaluate the while condition and branch accordingly.
	 * 
	 * @param stmt The while statement
	 * @return The MIPS code for the while statement
	 */
	@Override
	public String visitWhileStmt(While stmt) {
		StringBuilder sb = new StringBuilder();

		int labelNum = labelCounter++; // Get unique label number for making unique labels

		// Label for while loop
		sb.append("while_start_").append(labelNum).append(":\n");

		// Get condition value on stack
		sb.append(stmt.condition.accept(this));

		// Get condition value in register $t0
		sb.append(popToRegister("$t0"));

		// Check if condition is true
		// If $t0 == 1, then condition is true, continue with while loop
		// If $t0 == 0, then condition is false, break out of while loop
		sb.append("beq $t0, $zero, while_exit_").append(labelNum).append("\n");

		// Put while loop body code
		sb.append(stmt.body.accept(this));

		// After while loop body, jump back to while loop start
		sb.append("j while_start_").append(labelNum).append("\n");

		// Label for where to break out of while loop
		sb.append("while_exit_").append(labelNum).append(":\n");

		return sb.toString();
	}
}
