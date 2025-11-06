package edu.wisc;

import java.util.ArrayList;
import java.util.List;

class Problem {
	int startCol;
	int endCol;
	int startLine;
	int endLine;
	String message;

	public Problem(int startCol, int endCol, int startLine, int endLine, String message) {
		this.startCol = startCol;
		this.endCol = endCol;
		this.startLine = startLine;
		this.endLine = endLine;
		this.message = message;
	}
}

enum ErrorType { NAME, TYPE, PARSE, LEX }

class Error extends RuntimeException {
	String error;
    List<Problem> problems;

	public Error(List<Problem> problems, List<String> lines, ErrorType errorType) {
		error = makeError(problems, lines, errorType);
        this.problems = problems;
	}

    public Integer getStartLine() {
        int startLine = problems.get(0).startLine;
        for (Problem problem : problems) {
            if (problem.startLine < startLine) startLine = problem.startLine;
        }
        return startLine;
    }

	private String makeError(List<Problem> problems, List<String> lines, ErrorType errorType) {
		if (lines.size() == 1) return makeSingleLineError(problems, lines.get(0), errorType);
		else return makeMultiLineError(problems, lines, errorType);
	}

	private String makeSingleLineError(List<Problem> problems, String line, ErrorType errorType) {
		int errorCount = problems.size();
		StringBuilder sb = new StringBuilder();
		switch (errorType) {
			case NAME: sb.append("Name Error").append("\n"); break;
			case TYPE: sb.append("Type Error").append("\n"); break;
			case PARSE: sb.append("Syntax Error").append("\n"); break;
			case LEX: sb.append("Lexical Error").append("\n"); break;
			default: sb.append("UNKNOWN ERROR").append("\n"); break;
		}
		/*
		for (Problem prob : problems) {
			sb.append(" - ").append(prob.message).append("\n");
		}
		*/
		sb.append("~~~~~~~~~~~~~~~~~~~").append("\n");

		sb.append(problems.get(0).startLine).append(" | ").append(line).append("\n");

		sb.append(" ".repeat(String.valueOf(problems.get(0).startLine).length()));
		sb.append(" | ");
		for (int i = 0; i < line.length(); i++) {
			char c = ' ';
			for (Problem prob : problems) {
				if (i >= prob.startCol - 1 && i < prob.endCol - 1) {
					c = '^';
					break;
				}
			}
			sb.append(c);
		}
		sb.append("\n");

		// Messages stacked with decreasing number of '|' above
		for (int j = 0; j < errorCount; j++) {
			sb.append(" ".repeat(String.valueOf(problems.get(0).startLine).length())).append(" | ");
			int errorNum = 0;
			for (int i = 0; i < line.length(); i++) {
				boolean errorHere = false;
				for (Problem prob : problems) {
					if (i == prob.startCol - 1) {
						errorHere = true;
						break;
					}
				}
				if (errorHere) {
					// Only print '|' if this is above a later message
					if (errorNum < errorCount - 1 - j) {
						sb.append("|");
					} else if (errorNum == errorCount - 1 - j) {
						sb.append(problems.get(errorNum).message);
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

		return sb.toString();
	}


	private String makeMultiLineError(List<Problem> problems, List<String> lines, ErrorType errorType) {
		StringBuilder sb = new StringBuilder();
		switch (errorType) {
			case NAME: sb.append("Name Error").append("\n"); break;
			case TYPE: sb.append("Type Error").append("\n"); break;
			case PARSE: sb.append("Syntax Error").append("\n"); break;
			case LEX: sb.append("Lexical Error").append("\n"); break;
			default: sb.append("UNKNOWN ERROR").append("\n"); break;
		}
		/*
		for (Problem prob : problems) {
			sb.append(" - ").append(prob.message).append("\n");
		}
		*/
		sb.append("~~~~~~~~~~~~~~~~~~~\n");

		for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			int lineNumber = problems.get(0).startLine + lineIndex;
			String line = lines.get(lineIndex);

			// Print the code line
			sb.append(lineNumber).append(" | ").append(line).append("\n");

            // Caret line
            sb.append(" ".repeat(String.valueOf(lineNumber).length())).append(" | ");
            for (int i = 0; i < line.length(); i++) {
                char c = ' ';
                for (Problem prob : problems) {
                    if (lineNumber >= prob.startLine && lineNumber <= prob.endLine) {
                        int caretStart = (lineNumber == prob.startLine) ? prob.startCol - 1 : 0;
                        int caretEnd = (lineNumber == prob.endLine) ? prob.endCol - 1 : line.length();
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
				if (lineNumber == prob.startLine) {
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
						if (i == prob.startCol - 1) {
							errorHere = true;
							break;
						}
					}
					if (errorHere) {
						if (errorNum < errorCount - 1 - j) {
							sb.append("|");
						} else if (errorNum == errorCount - 1 - j) {
							sb.append(problemsOnLine.get(errorNum).message);
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



	@Override
	public String getMessage() {
		return error;
	}

	@Override
	public String toString() {
		return error;
	}
}
