package edu.wisc;

import java.util.ArrayList;
import java.util.List;

/**
 * A Problem contains the information relevant to the location of a problematic part of the code.
 * It also contains a message detailing what the problem is.
 */
class Problem {
	private final int startCol, endCol, startLine, endLine;
	private final String message;

	/**
	 * Initializes a new Problem
	 * 
	 * @param startCol  Starting column of the problematic part of code
	 * @param endCol    Ending column of the problematic part of code
	 * @param startLine Line that the problematic part of code starts on
	 * @param endLine   Line that the problematic part of code ends on
	 * @param message   Message detailing what the problem is
	 */
	public Problem(int startCol, int endCol, int startLine, int endLine, String message) {
		this.startCol = startCol;
		this.endCol = endCol;
		this.startLine = startLine;
		this.endLine = endLine;
		this.message = message;
	}

	public int getStartCol() { return startCol; }
	public int getEndCol() { return endCol; }
	public int getStartLine() { return startLine; }
	public int getEndLine() { return endLine; }
	public String getMessage() { return message; }
}

/**
 * Provides types for all errors that are being checked
 */
enum ErrorType { NAME, TYPE, PARSE, LEX }

/**
 * An Error represents a collection of Problems for a given statement in the code.
 * An Error can be one of any of the error types defined by ErrorType.
 * An Error will hold all Problems for a given statement that are of the same ErrorType.
 */
class Error extends RuntimeException {
	String error; // The error report
    List<Problem> problems; // The list of problems making up the error
	ErrorType errorType; // The type of this error

	/**
	 * Initialize a new error
	 * 
	 * @param problems  The list of problems making up the error
	 * @param lines     The relevant lines of code for this error
	 * @param errorType The type of error this is
	 */
	public Error(List<Problem> problems, List<String> lines, ErrorType errorType) {
		error = makeError(problems, lines, errorType); // Create the error report
        this.problems = problems;
		this.errorType = errorType;
	}

	/**
	 * Gets the number of the first line that appears in this error
	 * 
	 * @return The number of the first line
	 */
    public Integer getStartLine() {
        int startLine = problems.get(0).getStartLine();
        for (Problem problem : problems) {
            if (problem.getStartLine() < startLine) startLine = problem.getStartLine();
        }
        return startLine;
    }

	/**
	 * Creates the error report
	 * 
	 * @param problems  The list of problems making up this error
	 * @param lines     The relevant lines for this error
	 * @param errorType The type of error this is
	 * @return The error report
	 */
	private String makeError(List<Problem> problems, List<String> lines, ErrorType errorType) {
		StringBuilder sb = new StringBuilder();

		// Put the type of error
		switch (errorType) {
			case NAME: sb.append("Name Error").append("\n"); break;
			case TYPE: sb.append("Type Error").append("\n"); break;
			case PARSE: sb.append("Syntax Error").append("\n"); break;
			case LEX: sb.append("Lexical Error").append("\n"); break;
			default: sb.append("UNKNOWN ERROR").append("\n"); break;
		}
		sb.append("~~~~~~~~~~~~~~~~~~~\n");

		// Print each line of relevant code with Problem information where needed
		for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			int lineNumber = problems.get(0).getStartLine() + lineIndex;
			String line = lines.get(lineIndex);

			// Print the code line
			sb.append(lineNumber).append(" | ").append(line).append("\n");

            // Caret line
            sb.append(" ".repeat(String.valueOf(lineNumber).length())).append(" | ");
            for (int i = 0; i < line.length(); i++) {
                char c = ' ';
                for (Problem prob : problems) {
                    if (lineNumber >= prob.getStartLine() && lineNumber <= prob.getEndLine()) {
                        int caretStart = (lineNumber == prob.getStartLine()) ? prob.getStartCol() - 1 : 0;
                        int caretEnd = (lineNumber == prob.getEndLine()) ? prob.getEndCol() - 1 : line.length();
                        if (i >= caretStart && i < caretEnd) {
                            c = '^';
                            break;
                        }
                    }
                }
                sb.append(c);
            }
            sb.append("\n");

			// Collect all problems for this line
			List<Problem> problemsOnLine = new ArrayList<>();
			for (Problem prob : problems) {
				if (lineNumber == prob.getStartLine()) {
					problemsOnLine.add(prob);
				}
			}

			int errorCount = problemsOnLine.size();

			// Print stacked messages with decreasing '|'s
			for (int j = 0; j < errorCount; j++) {
				sb.append(" ".repeat(String.valueOf(lineNumber).length())).append(" | ");
				int errorNum = 0;
				for (int i = 0; i < line.length(); i++) {
					boolean errorHere = false;
					for (Problem prob : problemsOnLine) {
						if (i == prob.getStartCol() - 1) {
							errorHere = true;
							break;
						}
					}
					if (errorHere) {
						if (errorNum < errorCount - 1 - j) {
							sb.append("|");
						} else if (errorNum == errorCount - 1 - j) {
							sb.append(problemsOnLine.get(errorNum).getMessage());
						} else {
							sb.append(" ");
						}
						errorNum++;
					} else {
						sb.append(" ");
					}
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	/**
	 * @return The error report
	 */
	@Override
	public String getMessage() {
		return error;
	}

	/**
	 * @return The error report
	 */
	@Override
	public String toString() {
		return error;
	}
}
