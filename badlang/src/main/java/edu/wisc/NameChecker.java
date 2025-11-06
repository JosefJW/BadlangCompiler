package edu.wisc;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.Math;

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

/**
 * Used for checking a word against a vocabulary.
 * Given an identifier and a list of valid identifiers, gives the most similar
 * valid identifier if they are reasonably close to each other.
 */
class SpellChecker {
    private final List<String> vocabulary; // Vocabulary of words to check against

	/**
	 * Initialize a new SpellChecker
	 * 
	 * @param vocabulary The valid words to spell check against
	 */
    public SpellChecker(Collection<String> vocabulary) {
        this.vocabulary = new ArrayList<>(vocabulary);
    }

    // Public method: returns the most likely correct word
	/**
	 * Return the reasonably closest word in the vocabulary to the provided word
	 * or null if no words are reasonably close.
	 * 
	 * @param input The word to correct
	 * @return The best match against the input
	 */
	public String correct(String input) {
		String bestMatch = null;
		int bestDistance = Integer.MAX_VALUE;

		for (String word : vocabulary) {
			int distance = editDistance(input.toLowerCase(), word.toLowerCase());

			if (distance < bestDistance) {
				bestDistance = distance;
				bestMatch = word;
			}
		}

		// Only return a suggestion if it's within the threshold
		if (bestDistance <= Math.max(1, input.length() / 2)) {
			return bestMatch;
		} else {
			return null; // No close match
		}
	}

	/**
	 * Use the Levenshtein edit distance algorithm to determine word similarity
	 * 
	 * @param a The first word to compare
	 * @param b The second word to compare
	 * @return The two words' edit distance
	 */
    private static int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j; // insert all of b
                } else if (j == 0) {
                    dp[i][j] = i; // delete all of a
                } else if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1]; // no change
                } else {
                    dp[i][j] = 1 + Math.min(
                        dp[i - 1][j - 1],           // substitution
                        Math.min(dp[i - 1][j], dp[i][j - 1]) // deletion or insertion
                    );
                }
            }
        }

        return dp[a.length()][b.length()];
    }
}

/**
 * Performs name checking on the Badlang language
 */
public class NameChecker implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
	Environment environment; // The current scope
	List<String> programLines; // A list containing each line of code that is being checked
	List<Error> errors = new ArrayList<Error>(); // A list of errors found in the code
	List<Problem> currentStatementProblems; // Used to track problems found in the current statement being checked
	int errorCount = 0; // The number of problems found

	/**
	 * Initialize a new name checker
	 * 
	 * @param environment  The environment to use as a global environmnet
	 * @param programLines A list containing each line of code that is being checked
	 */
	public NameChecker(Environment environment, List<String> programLines) {
		this.environment = environment;
		this.programLines = programLines;
	}

	/**
	 * Take a list of Problems, make them into one Error, and add them to the list of errors
	 * 
	 * @param problems The Problems to make into an Error
	 */
	private void addProblems(List<Problem> problems) {
		if (!problems.isEmpty()) {
			int startLine = problems.get(0).getStartLine();
			int endLine = problems.get(0).getEndLine();
			for (Problem np : problems) {
				if (np.getStartLine() < startLine) startLine = np.getStartLine();
				if (np.getEndLine() > endLine) endLine = np.getEndLine();
			}
			errors.add(new Error(problems, programLines.subList(startLine-1, endLine), ErrorType.NAME));
		} 
	}

	/**
	 * Explore a binary expression ('[expr] [operator] [expr]').
	 * No direct name checking is needed, so just go deeper in the tree.
	 * 
	 * @param expr The expression to explore
	 */
	@Override
	public Void visitBinaryExpr(Binary expr) {
		expr.left.accept(this);
		expr.right.accept(this);
		return null;
	}

	/**
	 * Do nothing.
	 * Cannot go deeper in the tree and literals cannot be name checked.
	 * 
	 * @param expr The literal expression
	 */
	@Override
	public Void visitLiteralExpr(Literal expr) {
		return null;
	}

	/**
	 * Explore a unary expression ('[operator] [expr]').
	 * No direct name checking is needed, so just go deeper in the tree.
	 * 
	 * @param expr The unary expression to explore
	 */
	@Override
	public Void visitUnaryExpr(Unary expr) {
		expr.right.accept(this);
		return null;
	}

	/**
	 * Check that a variable name is valid when being called in the code.
	 * 
	 * @param expr The variable to check
	 */
	@Override
	public Void visitVariableExpr(Variable expr) {
		if (!environment.isDeclared(expr.name)) { // Variables need to be declared before being used
			// Check for a typo
			SpellChecker sc = new SpellChecker(environment.getAllVariables());
			String closestName = sc.correct(expr.name);
			
			// Create error
			String msg;
			if (closestName != null) msg = "Variable \'" + expr.name + "\' was used but never declared. Did you mean \'" + closestName + "\'?";
			else msg = "Variable \'" + expr.name + "\' was used but never declared.";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
			errorCount++;
		}
		else if (!environment.isInitialized(expr.name)) { // Variables need to be initialized before being used
			// Create error
			String msg = "Variable \'" + expr.name + "\' was used but never initialized.";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
			errorCount++;
		}

		return null;
	}

	/**
	 * Check that a function name is valid when being called in the code.
	 * Additionally, explore its child argument expressions.
	 * 
	 * @param expr The function to check
	 */
	@Override
	public Void visitCallExpr(Call expr) {
		if (!environment.isDeclared(expr.name)) { // Functions need to be declared before being used
			// Check for a typo
			SpellChecker sc = new SpellChecker(environment.getAllFunctions());
			String closestName = sc.correct(expr.name);
			
			// Create error
			String msg;
			if (closestName != null) msg = "Function \'" + expr.name + "\' was used but never declared. Did you mean \'" + closestName + "\'?";
			else msg = "Function \'" + expr.name + "\' was used but never declared.";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getStartCol() + expr.name.length(), expr.getStartLine(), expr.getStartLine(), msg));
			errorCount++;
		}
		else if (!environment.isFunction(expr.name)) { // Variables cannot be called
			// Create error
			String msg = "Identifier \'" + expr.name + "\' was declared as a variable but used as a function.";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getStartCol() + expr.name.length(), expr.getStartLine(), expr.getStartLine(), msg));
			errorCount++;
		}

		// Perform name checking on all arguments
		for (Expr arg : expr.arguments) {
			arg.accept(this);
		}

		return null;
	}

	/**
	 * Explore each statement in a block statement ('{ [statements] }').
	 * 
	 * @param stmt The block statement to explore
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
	 * Explore the expression in an expression statement ('[expression];').
	 * 
	 * @param stmt The expression statement to explore
	 */
	@Override
	public Void visitExpressionStmt(Expression stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Explore a function declaration ('fun [type] [identifier]([parameters]) { [statements] }')
	 * 
	 * @param stmt The function declaration to explore
	 */
	@Override
	public Void visitFunctionStmt(Function stmt) {
		environment.initialize(stmt.name);
		environment = new Environment(environment, stmt.returnType);
		currentStatementProblems = new ArrayList<Problem>();
		
		// Functions cannot use names that previously declared variables already used
		if (environment.isDeclared(stmt.name) && !environment.isFunction(stmt.name)) {
			String msg = "Identifier " + stmt.name + " was previously used to define a variable; variables and functions cannot share names.";
			currentStatementProblems.add(new Problem(stmt.getHeaderStartCol(), stmt.getHeaderEndCol(), stmt.getHeaderStartLine(), stmt.getHeaderEndLine(), msg));
			errorCount++;
		}

		// Perform name checking on all parameters
		for (Stmt.Parameter p : stmt.params) {
			// Check that a parameter does not take a function name
			if (environment.isDeclared(p.name()) && environment.isFunction(p.name())) {
				String msg = "Parameter " + p.name() + " shares an identifier with a function; parameters and functions cannot share names.";
				currentStatementProblems.add(new Problem(p.startCol(), p.endCol(), p.startLine(), p.endLine(), msg));
				errorCount++;
			}
			// Check that parameter names are unique
			else if (environment.isDeclaredInScope(p.name())) {
				String msg = "Parameter " + p.name() + " is already used for this function; cannot have duplicate parameter names.";
				currentStatementProblems.add(new Problem(p.startCol(), p.endCol(), p.startLine(), p.endLine(), msg));
				errorCount++;
			}
			// Parameter is good
			else {
				environment.declare(p.name(), p.type(), IdentifierType.VARIABLE, null, true);
			}
		}
		addProblems(currentStatementProblems);

		// Perform name checking on all statement in the function body
		for (Stmt s : stmt.body) {
			s.accept(this);
		}

		environment = environment.getParent();
		return null;
	}

	/**
	 * Explore an if statement ('if ([expr]) [statement] else [statement]')
	 * 
	 * @param stmt The if statement to explore
	 */
	@Override
	public Void visitIfStmt(If stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		
		stmt.condition.accept(this);
		addProblems(currentStatementProblems);
		
		stmt.thenBranch.accept(this);
		if (stmt.elseBranch != null) stmt.elseBranch.accept(this);
		
		return null;
	}

	/**
	 * Explore a print statement ('print [expression];')
	 * 
	 * @param stmt The print statement to explore
	 */
	@Override
	public Void visitPrintStmt(Print stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Explore a return statement ('return [expression];')
	 * 
	 * @param stmt The return statement to explore
	 */
	@Override
	public Void visitReturnStmt(Return stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.value.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Ensure the new variable has a unique identifier and explore its initializer if it has one.
	 * 
	 * @param stmt The var stmt to explore
	 */
	@Override
	public Void visitVarStmt(Var stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		
		// Check that the variable was not previously declared in the same scope
		if (environment.isDeclaredInScope(stmt.name) && (!environment.isFunction(stmt.name) || (environment.isFunction(stmt.name) && environment.isInitialized(stmt.name)))) {
			String msg = "Variable \'" + stmt.name + "\' was previously declared in this scope; cannot redeclare variables.";
			currentStatementProblems.add(new Problem(stmt.getDeclaratorStartCol(), stmt.getDeclaratorEndCol(), stmt.getDeclaratorStartLine(), stmt.getDeclaratorEndLine(), msg));
			errorCount++;
		}

		// Check that the identifier was not previously used for a function
		else if (environment.isDeclared(stmt.name) && environment.isFunction(stmt.name) && environment.isInitialized(stmt.name)) {
			String msg = "Variable \'" + stmt.name + "\' was previously declared as a function; variables and functions cannot share identifiers.";
			currentStatementProblems.add(new Problem(stmt.getDeclaratorStartCol(), stmt.getDeclaratorEndCol(), stmt.getDeclaratorStartLine(), stmt.getDeclaratorEndLine(), msg));
			errorCount++;
		}

		// Variable is good
		else if (stmt.initializer == null) {
			environment.declare(stmt.name, stmt.type, IdentifierType.VARIABLE, null, false);
		}
		else {
			stmt.initializer.accept(this);
			environment.declare(stmt.name, stmt.type, IdentifierType.VARIABLE, null, true);
		}

		addProblems(currentStatementProblems);
		return null;
	}

	/**
	 * Ensure the variable being assigned to was declared previously and explore its value.
	 * 
	 * @param stmt The assign statement to explore
	 */
	@Override
	public Void visitAssignStmt(Assign stmt) {
		currentStatementProblems = new ArrayList<Problem>();

		// Check that variable was previously declared
		if (!environment.isDeclared(stmt.name)) {
			// Check for a typo
			SpellChecker sc = new SpellChecker(environment.getAllVariables());
			String closestName = sc.correct(stmt.name);
			
			// Create error
			String msg;
			if (closestName != null) msg = "Variable \'" + stmt.name + "\' was used but never declared. Did you mean \'" + closestName + "\'?";
			else msg = "Variable \'" + stmt.name + "\' was used but never declared.";
			currentStatementProblems.add(new Problem(stmt.getStartCol(), stmt.getStartCol() + stmt.name.length(), stmt.getStartLine(), stmt.getStartLine(), msg));
			errorCount++;

			// Check for name errors in the value
			stmt.value.accept(this);
			addProblems(currentStatementProblems);
		}
		else {
			stmt.value.accept(this);
			addProblems(currentStatementProblems);
			environment.initialize(stmt.name);
		}
		return null;
	}

	/**
	 * Explore a while statement ('while ([expression]) [statement]')
	 * 
	 * @param stmt The while statement to explore
	 */
	@Override
	public Void visitWhileStmt(While stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.condition.accept(this);
		addProblems(currentStatementProblems);
		stmt.body.accept(this);
		return null;
	}		
}