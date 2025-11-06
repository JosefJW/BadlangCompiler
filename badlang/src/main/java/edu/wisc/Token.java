package edu.wisc;

enum TokenType {
    TYPE, IDENTIFIER, FUN, NUMBER, BOOLEAN,
    IF, ELSE, WHILE, RETURN, PRINT,
    PLUS, MINUS, STAR, SLASH, 
    AND, OR, NOT,
    ASGN, EQUALS, NOT_EQUALS,
    GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
    LPAREN, RPAREN, LCURLY, RCURLY,
    COMMA, SEMICOLON, 
    EOF
}

/**
 * Represents a single token produced by the lexer.
 * 
 * Implement this class according to your lexical analysis needs.
 */
public class Token {
    // Add fields for token type, lexeme, literal value, line number, etc.
    private final TokenType type;
    private final String lexeme;
    private final Object literalValue;
    private final int lineNumber;
    private final int startColumn;
    private final int endColumn;


    // Add constructor(s)
    public Token(TokenType type, String lexeme, Object literalValue, Integer lineNumber, Integer startColumn, Integer endColumn) {
        this.type = type;
        this.lexeme = lexeme;
        this.literalValue = literalValue;
        this.lineNumber = lineNumber;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
    }

    public Token(TokenType type, String lexeme, Integer lineNumber, Integer startColumn, Integer endColumn) {
        this.type = type;
        this.lexeme = lexeme;
        this.literalValue = null;
        this.lineNumber = lineNumber;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
    }

    
    // Add getter methods
    public TokenType getType() {
        return this.type;
    }

    public String getLexeme() {
        return this.lexeme;
    }

    public Object getLiteralValue() {
        return this.literalValue;
    }

    public Integer getLineNumber() {
        return this.lineNumber;
    }

    public Integer getStartCol() {
        return this.startColumn;
    }

    public Integer getEndCol() {
        return this.endColumn;
    }
    
    
    @Override
    public String toString() {
        String literalStr = (literalValue != null) ? literalValue.toString() : "null";
        return String.format(
            "[%s] '%s'%s (line %d, cols %d - %d)",
            type,
            lexeme,
            literalValue != null ? " => " + literalStr : "",
            lineNumber,
            startColumn,
            endColumn
        );
    }
}


