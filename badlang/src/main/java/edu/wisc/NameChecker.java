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

class SpellChecker {

    private final List<String> vocabulary;

    // Constructor — pass in your vocabulary
    public SpellChecker(Collection<String> vocabulary) {
        this.vocabulary = new ArrayList<>(vocabulary);
    }

    // Public method: returns the most likely correct word
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

    // Levenshtein edit distance algorithm
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

public class NameChecker implements Stmt.Visitor<Void>, Expr.Visitor<Void> {
	Environment environment;
	List<String> programLines;
	List<Error> errors = new ArrayList<Error>();
	List<Problem> currentStatementProblems;
	int errorCount = 0;

	public NameChecker(Environment environment, List<String> programLines) {
		this.environment = environment;
		this.programLines = programLines;
	}

	private void addProblems(List<Problem> problems) {
		if (!problems.isEmpty()) {
			int startLine = problems.get(0).startLine;
			int endLine = problems.get(0).endLine;
			for (Problem np : problems) {
				if (np.startLine < startLine) startLine = np.startLine;
				if (np.endLine > endLine) endLine = np.endLine;
			}
			errors.add(new Error(problems, programLines.subList(startLine-1, endLine), ErrorType.NAME));
		} 
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
		if (!environment.isDeclared(expr.name)) {
			SpellChecker sc = new SpellChecker(environment.getAllVariables());
			String closestName = sc.correct(expr.name);
			String msg;
			if (closestName != null) msg = "Variable \'" + expr.name + "\' was used but never declared. Did you mean \'" + closestName + "\'?";
			else msg = "Variable \'" + expr.name + "\' was used but never declared.";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
			errorCount++;
		}
		else if (!environment.isInitialized(expr.name)) {
			String msg = "Variable \'" + expr.name + "\' was used but never initialized.";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getEndCol(), expr.getStartLine(), expr.getEndLine(), msg));
			errorCount++;
		}
		return null;
	}

	@Override
	public Void visitCallExpr(Call expr) {
		if (!environment.isDeclared(expr.name)) {
			SpellChecker sc = new SpellChecker(environment.getAllFunctions());
			String closestName = sc.correct(expr.name);
			String msg;
			if (closestName != null) msg = "Function \'" + expr.name + "\' was used but never declared. Did you mean \'" + closestName + "\'?";
			else msg = "Function \'" + expr.name + "\' was used but never declared.";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getStartCol() + expr.name.length(), expr.getStartLine(), expr.getStartLine(), msg));
			errorCount++;
		}
		else if (!environment.isFunction(expr.name)) {
			String msg = "Identifier \'" + expr.name + "\' was declared as a variable but used as a function.";
			currentStatementProblems.add(new Problem(expr.getStartCol(), expr.getStartCol() + expr.name.length(), expr.getStartLine(), expr.getStartLine(), msg));
			errorCount++;
		}
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
		currentStatementProblems = new ArrayList<Problem>();
		stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		environment.initialize(stmt.name);
		environment = new Environment(environment, stmt.returnType);
		currentStatementProblems = new ArrayList<Problem>();
		if (environment.isDeclared(stmt.name) && !environment.isFunction(stmt.name)) {
			String msg = "Identifier " + stmt.name + " was previously used to define a variable; variables and functions cannot share names.";
			currentStatementProblems.add(new Problem(stmt.getHeaderStartCol(), stmt.getHeaderEndCol(), stmt.getHeaderStartLine(), stmt.getHeaderEndLine(), msg));
			errorCount++;
		}
		for (Stmt.Parameter p : stmt.params) {
			if (!environment.isDeclared(p.name())) {
				environment.declare(p.name(), p.type(), IdentifierType.VARIABLE, null, true);
			}
			else {
				String msg = "Parameter " + p.name() + " is already used for this function; cannot have duplicate parameter names.";
				currentStatementProblems.add(new Problem(p.startCol(), p.endCol(), p.startLine(), p.endLine(), msg));
			}
		}
		addProblems(currentStatementProblems);
		for (Stmt s : stmt.body) {
			s.accept(this);
		}
		environment = environment.getParent();
		return null;
	}

	@Override
	public Void visitIfStmt(If stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.condition.accept(this);
		addProblems(currentStatementProblems);
		stmt.thenBranch.accept(this);
		if (stmt.elseBranch != null) stmt.elseBranch.accept(this);
		return null;
	}

	@Override
	public Void visitPrintStmt(Print stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.expression.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.value.accept(this);
		addProblems(currentStatementProblems);
		return null;
	}

	@Override
	public Void visitVarStmt(Var stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		if (environment.isDeclaredInScope(stmt.name) && (!environment.isFunction(stmt.name) || (environment.isFunction(stmt.name) && environment.isInitialized(stmt.name)))) {
			String msg = "Variable \'" + stmt.name + "\' was previously declared in this scope; cannot redeclare variables.";
			currentStatementProblems.add(new Problem(stmt.getDeclaratorStartCol(), stmt.getDeclaratorEndCol(), stmt.getDeclaratorStartLine(), stmt.getDeclaratorEndLine(), msg));
			errorCount++;
		}
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

	@Override
	public Void visitAssignStmt(Assign stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		if (!environment.isDeclared(stmt.name)) {
			SpellChecker sc = new SpellChecker(environment.getAllVariables());
			String closestName = sc.correct(stmt.name);
			String msg;
			if (closestName != null) msg = "Variable \'" + stmt.name + "\' was used but never declared. Did you mean \'" + closestName + "\'?";
			else msg = "Variable \'" + stmt.name + "\' was used but never declared.";
			currentStatementProblems.add(new Problem(stmt.getStartCol(), stmt.getStartCol() + stmt.name.length(), stmt.getStartLine(), stmt.getStartLine(), msg));
			errorCount++;
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

	@Override
	public Void visitWhileStmt(While stmt) {
		currentStatementProblems = new ArrayList<Problem>();
		stmt.condition.accept(this);
		addProblems(currentStatementProblems);
		stmt.body.accept(this);
		return null;
	}
		
}