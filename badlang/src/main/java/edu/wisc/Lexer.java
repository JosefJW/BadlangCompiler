package edu.wisc;

import java.util.ArrayList;
import java.util.List;

/**
 * The Lexer (also known as Scanner or Tokenizer) converts source code into tokens.
 * 
 * Implement the complete lexer for badlang.
 * 
 * Your lexer should:
 * 1. Read the source code character by character
 * 2. Recognize all tokens defined in the language (keywords, identifiers, literals, operators, etc.)
 * 3. Handle whitespace appropriately
 * 4. Report errors with meaningful messages
 * 5. Track line numbers for error reporting
 */
public class Lexer {
    String code; // The code that is being lexed
	Integer position = 0; // The current char position in the string code
	Integer lineNumber = 1; // The current line number in the code file
	Integer colNumber = 0; // The current column number in the code file

	/**
	 * Create a new lexer
	 * 
	 * @param code The code that will be lexed
	 */
	public Lexer(String code) {
		this.code = code;
	}

	/**
	 * Retrieves the next character from the code and moves the position up one
	 * 
	 * @return The next character from the code
	 */
	private char getNextChar() {
		colNumber++;
		return code.charAt(position++);
	}

	/**
	 * Retrieves the next character from the code, but does not change the position
	 * 
	 * @return The next character from the code
	 */
	private char peekNextChar() {
		return code.charAt(position);
	}

	/**
	 * Checks if a string is a numerical string or not
	 * 
	 * @param lexeme The string to check
	 * @return true if the string is numerical; false otherwise
	 */
	private boolean isNumeric(String lexeme) {
		try {
			Double.parseDouble(lexeme);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Given a complete lexeme, returns a token with all properties set according to that lexeme
	 * 
	 * @param lexeme   The lexeme to tokenize
	 * @param startCol The starting column position of the lexeme
	 * @return A token with all properties set according to the lexeme
	 */
	private Token tokenize(String lexeme, int startCol) {
		TokenType type; // Set according to the lexeme
		Object literal = null; // Set if applicable for the lexeme

		// Numeric literals
		if (isNumeric(lexeme)) { type = TokenType.NUMBER; literal = Integer.parseInt(lexeme); }

		// Boolean literals
		else if (lexeme.equals("true")) { type = TokenType.BOOLEAN; literal = true; }
		else if (lexeme.equals("false")) { type = TokenType.BOOLEAN; literal = false; } 

		// Types
		else if (lexeme.equals("int") || lexeme.equals("bool")) type = TokenType.TYPE;

		// Control flow
		else if (lexeme.equals("if")) type = TokenType.IF;
		else if (lexeme.equals("else")) type = TokenType.ELSE;
		else if (lexeme.equals("while")) type = TokenType.WHILE;

		// Reserved functions
		else if (lexeme.equals("return")) type = TokenType.RETURN;
		else if (lexeme.equals("print")) type = TokenType.PRINT;
		else if (lexeme.equals("printsp")) type = TokenType.PRINTSP;
		else if (lexeme.equals("println")) type = TokenType.PRINTLN;

		// Function declarations
		else if (lexeme.equals("fun")) type = TokenType.FUN;

		// Identifiers
		else type = TokenType.IDENTIFIER;

		// Create and return a new Token with all properties set according to the lexeme
		return new Token(type, lexeme, literal, lineNumber, startCol, colNumber);
	}

	/**
	 * Lex the provided code from the beginning, returning a list of tokens representing the code
	 * 
	 * @return A list of Tokens representing the code
	 */
	public List<Token> lex() {
		// Reset the lexer
		position = 0;
		lineNumber = 1;
		colNumber = 0;

		List<Token> tokens = new ArrayList<>(); // Stores all tokens made from the code
		StringBuilder curLexeme = new StringBuilder(); // Stores the current lexeme being built
		int startCol = 0; // Stores the column number of the first character in the current lexeme

		while (position < code.length()) {
			char curChar = getNextChar();
			// White space
			if (curChar == ' ' || curChar == '\t') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				continue;
			}
			// Newlines
			else if (curChar == '\r') {
				if (peekNextChar() == '\n') getNextChar();
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				colNumber = 0;
				lineNumber++;
				continue;
			}
			else if (curChar == '\n') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				colNumber = 0;
				lineNumber++;
				continue;
			}
			// Comments and Slash
			else if (curChar == '/') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				if (peekNextChar() == '/') {
					while (position < code.length() && getNextChar() != '\n');
					lineNumber++;
					colNumber = 0;
				}
				else if (peekNextChar() == '*') {
					getNextChar();
					while (true) {
						char c = getNextChar();
						if (c == '\n') {
							lineNumber++;
							colNumber = 0;
						}
						else if (c == '*' && peekNextChar() == '/') {
							getNextChar();
							break;
						}
						if (position >= code.length() - 1) {
							throw new RuntimeException("Unterminated multi-line comment.");
						}
					}
				}
				else tokens.add(new Token(TokenType.SLASH, "/", lineNumber, colNumber, colNumber+1));
				continue;
			}
			// Parentheses
			else if (curChar == '(') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.LPAREN, "(", lineNumber, colNumber, colNumber+1));
			}
			else if (curChar == ')') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.RPAREN, ")", lineNumber, colNumber, colNumber+1));
			}
			// Curly Braces
			else if (curChar == '{') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.LCURLY, "{", lineNumber, colNumber, colNumber+1));
			}
			else if (curChar == '}') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.RCURLY, "}", lineNumber, colNumber, colNumber+1));
			}
			// Comma
			else if (curChar == ',') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.COMMA, ",", lineNumber, colNumber, colNumber+1));
			}
			// Semicolon
			else if (curChar == ';') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.SEMICOLON, ";", lineNumber, colNumber, colNumber+1));
			}
			// Equals
			else if (curChar == '=') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				if (peekNextChar() == '=') {
					tokens.add(new Token(TokenType.EQUALS, "==", lineNumber, colNumber, colNumber+2));
					getNextChar();
				}
				else tokens.add(new Token(TokenType.ASGN, "=", lineNumber, colNumber, colNumber+1));
			}
			// Not [Equals]
			else if (curChar == '!') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				if (peekNextChar() == '=') {
					tokens.add(new Token(TokenType.NOT_EQUALS, "!=", lineNumber, colNumber, colNumber+2));
					getNextChar();
				}
				else tokens.add(new Token(TokenType.NOT, "!", lineNumber, colNumber, colNumber+1));
			}
			// Greater
			else if (curChar == '>') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				if (peekNextChar() == '=') {
					tokens.add(new Token(TokenType.GREATER_EQUAL, ">=", lineNumber, colNumber, colNumber+2));
					getNextChar();
				}
				else tokens.add(new Token(TokenType.GREATER, ">", lineNumber, colNumber, colNumber+1));
			}
			// Less
			else if (curChar == '<') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				if (peekNextChar() == '=') {
					tokens.add(new Token(TokenType.LESS_EQUAL, "<=", lineNumber, colNumber, colNumber+2));
					getNextChar();
				}
				else tokens.add(new Token(TokenType.LESS, "<", lineNumber, colNumber, colNumber+1));
			}
			// Plus
			else if (curChar == '+') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.PLUS, "+", lineNumber, colNumber, colNumber+1));
			}
			// Minus
			else if (curChar == '-') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.MINUS, "-", lineNumber, colNumber, colNumber+1));
			}
			// Star
			else if (curChar == '*') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.STAR, "*", lineNumber, colNumber, colNumber+1));
			}
			// Modulo
			else if (curChar == '%') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				tokens.add(new Token(TokenType.MODULO, "%", lineNumber, colNumber, colNumber+1));
			}
			// And
			else if (curChar == '&') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				if (peekNextChar() == '&') {
					tokens.add(new Token(TokenType.AND, "&&", lineNumber, colNumber, colNumber+1));
					getNextChar();
				}
				else {
					throw new RuntimeException("Undefined token '&' at line " + lineNumber + ", col " + colNumber + ".");
				}
			}
			// Or
			else if (curChar == '|') {
				if (curLexeme.length() > 0) tokens.add(tokenize(curLexeme.toString(), startCol));
				curLexeme.setLength(0);
				if (peekNextChar() == '|') {
					tokens.add(new Token(TokenType.OR, "||", lineNumber, colNumber, colNumber+2));
					getNextChar();
				}
				else {
					throw new RuntimeException("Undefined token '|' at line " + lineNumber + ", col " + colNumber + ".");
				}
			}
			// Letter or Digit
			else if (Character.isLetterOrDigit(curChar) || curChar == '_') {
				if (curLexeme.length() == 0) startCol = colNumber;
				curLexeme.append(curChar);
			}
			// Invalid Character
			else {
				throw new RuntimeException("Lexer Exception: Unexpected character [ASCII = " + (int)curChar + "] '" + curChar + "' at line " + lineNumber + ", col " + colNumber + ".");
			}
		}

		tokens.add(new Token(TokenType.EOF, "", lineNumber, colNumber+1, colNumber+1));
		return tokens;
	}
}


