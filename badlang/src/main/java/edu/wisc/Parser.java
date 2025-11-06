package edu.wisc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Parser converts a sequence of tokens into an Abstract Syntax Tree (AST).
 * 
 * Implement the complete parser for badlang.
 * 
 * Your parser should:
 * 1. Take a list of tokens from the lexer
 * 2. Build an AST using the provided Expr and Stmt classes
 * 3. Implement recursive descent parsing based on the CFG you define
 * 4. Handle operator precedence correctly
 * 5. Report syntax errors with meaningful messages
 */
public class Parser {
    final List<Token> tokens; // The list of tokens to be parsed
    int position = 0; // The index of the current token being parsed
    int inFunction = 0; // Used to track state for detecting nested functions
    List<String> programLines;

    /**
     * Create a new Parser with a list of tokens to parse
     * 
     * @param tokens The list of tokens to parse
     */
    public Parser(List<Token> tokens, List<String> programLines) {
        this.tokens = tokens;
        this.programLines = programLines;
    }

    /**
	 * Retrieves the next token from the token list and moves the position up one
	 * 
	 * @return The next token from the token list
     */
    private Token getNextToken() {
        return tokens.get(position++);
    }

    /**
	 * Retrieves the next token from the token list, but does not change the position
	 * 
	 * @return The next token from the token list
     */
    private Token peekNextToken() {
        return tokens.get(position);
    }

    /**
     * Retrieves the token two ahead of the current token in the token list,
     * but does not change the position
     * 
     * @return The token two ahead from the current token
     */
    private Token peekTwoTokens() {
        return tokens.get(position+1);
    }

    /**
     * Retrieves the next token and checks that it is of type expected
     * 
     * @param expected The expected type of the token being retrieved
     * @return The token that was retrieved
     * @throws RuntimeException if the token was not of the expected type
     */
    private Token consumeAndCheck(TokenType expected) {
        Token t = getNextToken();
        if (t.getType() != expected) {
            String msg = "Expected token of type " + expected + ", but got " + t.getType() + ".";
            throw new Error(Arrays.asList(new Problem(t.getStartCol(), t.getEndCol(), t.getLineNumber(), t.getLineNumber(), msg)), programLines.subList(t.getLineNumber()-1, t.getLineNumber()), ErrorType.PARSE);
        }
        return t;
    }

    /**
     * Main function for parsing
     * Parses each statement and creates a list of AST nodes to be executed by an interpreter
     * 
     * @return A list of AST nodes
     */
    public List<Stmt> parseProgram() {
        List<Stmt> program = new ArrayList<>(); // Stores the created AST nodes for the program

        while (peekNextToken().getType() != TokenType.EOF) {
            program.add(parseStmt());
        }

        return program;
    }

    /**
     * Parses a generic statement.
     * Will figure out type of statement, and parse based on that.
     * 
     * @return The AST node for the statement
     */
    private Stmt parseStmt() {
        Token t = peekNextToken(); // Check, but don't consume the next token

        // Reserved functions
        if (t.getType() == TokenType.PRINT) return parsePrint();
        else if (t.getType() == TokenType.RETURN) return parseReturn();

        // Control flow
        else if (t.getType() == TokenType.IF) return parseIf();
        else if (t.getType() == TokenType.WHILE) return parseWhile();

        // Declarations
        else if (t.getType() == TokenType.TYPE) return parseVarDecl();
        else if (t.getType() == TokenType.FUN) return parseFunDecl();

        // Blocks
        else if (t.getType() == TokenType.LCURLY) return parseBlock();

        // Identifiers
        else if (t.getType() == TokenType.IDENTIFIER) {
            if (peekTwoTokens().getType() == TokenType.ASGN) { // Check the token 1 ahead of t
                return parseAsgn(); 
            }
            else return parseExprStmt(); // If an identifier is not followed by an '=', it must be an expression
        }

        // Expressions
        else return parseExprStmt();
    }

    /**
     * Parses a statement of the form 'print [expr];'
     * 
     * @return The AST node for the print statement
     */
    private Stmt parsePrint() {
        Token start = consumeAndCheck(TokenType.PRINT);
        int startCol = start.getStartCol();
        int startLine = start.getLineNumber();
        Expr expr = parseExpr();
        int endLine = expr.getEndLine();
        int endCol = expr.getEndCol();
        consumeAndCheck(TokenType.SEMICOLON);
        return new Stmt.Print(expr, startCol, endCol, startLine, endLine);
    }

    /**
     * Parses a statement of the form 'return [expr];'
     * 
     * @return The AST node for the return statement
     */
    private Stmt parseReturn() {
        Token start = consumeAndCheck(TokenType.RETURN);
        int startCol = start.getStartCol();
        int startLine = start.getLineNumber();
        Expr expr = parseExpr();
        int endCol = expr.getEndCol();
        int endLine = expr.getEndLine();
        consumeAndCheck(TokenType.SEMICOLON);
        return new Stmt.Return(expr, startCol, endCol, startLine, endLine);
    }

    /**
     * Parses a statement of the form:
     * 'if ([expr]) [stmt]'
     * or
     * 'if ([expr]) [stmt]
     *  else [stmt]'
     * 
     * @return The AST node for the if statement
     */
    private Stmt parseIf() {
        Token start = consumeAndCheck(TokenType.IF);
        int startCol = start.getStartCol();
        int startLine = start.getStartCol();
        consumeAndCheck(TokenType.LPAREN);
        Expr ifCond = parseExpr();
        consumeAndCheck(TokenType.RPAREN);
        Stmt ifBranch = parseStmt();
        int endCol = ifBranch.getEndCol();
        int endLine = ifBranch.getEndLine();
        Stmt elseBranch = null;

        // If there is an else branch, parse that too
        if (peekNextToken().getType() == TokenType.ELSE) {
            consumeAndCheck(TokenType.ELSE);
            elseBranch = parseStmt();
            endCol = elseBranch.getEndCol();
            endLine = elseBranch.getEndLine();
        }

        return new Stmt.If(ifCond, ifBranch, elseBranch, startCol, endCol, startLine, endLine);
    }

    /**
     * Parse a statement of the form 'while ([expr]) [stmt]'
     * 
     * @return The AST node for the while statement
     */
    private Stmt parseWhile() {
        Token start = consumeAndCheck(TokenType.WHILE);
        int startCol = start.getStartCol();
        int startLine = start.getLineNumber();
        consumeAndCheck(TokenType.LPAREN);
        Expr expr = parseExpr();
        consumeAndCheck(TokenType.RPAREN);
        Stmt body = parseStmt();
        int endCol = body.getEndCol();
        int endLine = body.getEndLine();
        return new Stmt.While(expr, body, startCol, endCol, startLine, endLine);
    }

    /**
     * Parse a statement of the form:
     * '[type] [identifier] = [expr];'
     * or
     * '[type] [identifier];'
     * 
     * @return The AST node for the variable declaration statement
     */
    private Stmt parseVarDecl() {
        Token typeToken = consumeAndCheck(TokenType.TYPE);
        VarType type;
        if (typeToken.getLexeme().equals("int")) type = VarType.INT;
        else if (typeToken.getLexeme().equals("bool")) type = VarType.BOOL;
        else throw new RuntimeException("Parser Exception: Unsupported variable type " + typeToken.getLexeme() + " at line " + typeToken.getLineNumber() + ", col " + typeToken.getStartCol() + ".");
        int startCol = typeToken.getStartCol();
        int startLine = typeToken.getLineNumber();
        
        Token identifier = consumeAndCheck(TokenType.IDENTIFIER);
        String identifierName = identifier.getLexeme();
        int endCol = identifier.getEndCol();
        int endLine = identifier.getLineNumber();

        // Uninitialized variable
        if (peekNextToken().getType() == TokenType.SEMICOLON) {
            consumeAndCheck(TokenType.SEMICOLON);
            return new Stmt.Var(identifierName, type, null, startCol, endCol, startLine, endLine, startCol, identifier.getEndCol(), startLine, identifier.getLineNumber());
        }

        // Initialized variable
        else {
            consumeAndCheck(TokenType.ASGN);
            Expr expr = parseExpr();
            consumeAndCheck(TokenType.SEMICOLON);
            endCol = expr.getEndCol();
            endLine = expr.getEndLine();
            return new Stmt.Var(identifierName, type, expr, startCol, endCol, startLine, endLine, startCol, identifier.getEndCol(), startLine, identifier.getLineNumber());
        }
    }

    /**
     * Converts a Type token to its corresponding VarType
     * 
     * @return The VarType of the token
     * @throws RuntimeException if the token is not a Type token or has an unsupported type
     */
    private VarType parseType() {
        Token typeToken = consumeAndCheck(TokenType.TYPE);
        if (typeToken.getLexeme().equals("int")) return VarType.INT;
        else if (typeToken.getLexeme().equals("bool")) return VarType.BOOL;
        else throw new RuntimeException("Parser Exception: Unsupported variable type " + typeToken.getLexeme() + " at line " + typeToken.getLineNumber() + ", col " + typeToken.getStartCol() + ".");
    }

    /**
     * Parse a statement of the form:
     * 'fun [type] [identifier]([type] [identifier], [type] [identifier], ...) {
     *     [statements]
     * }'
     * 
     * @return The AST node for the function declaration statement
     */
    private Stmt parseFunDecl() {
        Token start = consumeAndCheck(TokenType.FUN);
        int startCol = start.getStartCol();
        int startLine = start.getLineNumber();
        
        // Check for nested function declarations
        if (inFunction > 0) {
            String msg = "Nested functions are not supported.";
            throw new Error(Arrays.asList(new Problem(start.getStartCol(), start.getEndCol(), start.getLineNumber(), start.getLineNumber(), msg)), programLines.subList(startLine-1, startLine), ErrorType.PARSE);
        }
        inFunction++;

        VarType type = parseType();
        Token identifierToken = consumeAndCheck(TokenType.IDENTIFIER);
        String identifierName = identifierToken.getLexeme();
        
        // Parameters
        consumeAndCheck(TokenType.LPAREN);
        List<Stmt.Parameter> paramList = new ArrayList<>();
        if (peekNextToken().getType() != TokenType.RPAREN) paramList = parseParamList();
        consumeAndCheck(TokenType.RPAREN);
        
        // Body
        consumeAndCheck(TokenType.LCURLY);
        List<Stmt> body = new ArrayList<>();
        while (peekNextToken().getType() != TokenType.RCURLY) {
            body.add(parseStmt());
        }
        Token end = consumeAndCheck(TokenType.RCURLY);
        int endCol = end.getEndCol();
        int endLine = end.getLineNumber();
        
        inFunction--;
        return new Stmt.Function(identifierName, type, paramList, body, startCol, endCol, startLine, endLine, startCol, identifierToken.getEndCol(), startLine, identifierToken.getLineNumber());
    }

    /**
     * Parses the parameters for a function statement
     * 
     * @return A list of AST parameters for the function
     */
    private List<Stmt.Parameter> parseParamList() {
        List<Stmt.Parameter> params = new ArrayList<>();
        Token typeToken = consumeAndCheck(TokenType.TYPE);
        VarType type;
        if (typeToken.getLexeme().equals("int")) type = VarType.INT;
        else if (typeToken.getLexeme().equals("bool")) type = VarType.BOOL;
        else throw new RuntimeException("Parser Exception: Unsupported variable type " + typeToken.getLexeme() + " at line " + typeToken.getLineNumber() + ", col " + typeToken.getStartCol() + ".");
        Token identifier = consumeAndCheck(TokenType.IDENTIFIER);
        String identifierName = identifier.getLexeme();
        params.add(new Stmt.Parameter(identifierName, type, typeToken.getStartCol(), identifier.getEndCol(), typeToken.getLineNumber(), identifier.getLineNumber()));
        while (peekNextToken().getType() == TokenType.COMMA) {
            consumeAndCheck(TokenType.COMMA);
            typeToken = consumeAndCheck(TokenType.TYPE);
            if (typeToken.getLexeme().equals("int")) type = VarType.INT;
            else if (typeToken.getLexeme().equals("bool")) type = VarType.BOOL;
            else throw new RuntimeException("Parser Exception: Unsupported variable type " + typeToken.getLexeme() + " at line " + typeToken.getLineNumber() + ", col " + typeToken.getStartCol() + ".");
            identifier = consumeAndCheck(TokenType.IDENTIFIER);
            identifierName = identifier.getLexeme();
            params.add(new Stmt.Parameter(identifierName, type, typeToken.getStartCol(), identifier.getEndCol(), typeToken.getLineNumber(), identifier.getLineNumber()));
        }
        return params;
    }

    /**
     * Parses a statement of the form:
     * '{
     *      [statements]
     *  }'
     * 
     * @return The AST node for a block statement
     */
    private Stmt parseBlock() {
        Token start = consumeAndCheck(TokenType.LCURLY);
        int startCol = start.getStartCol();
        int startLine = start.getLineNumber();
        List<Stmt> body = new ArrayList<>();
        while (peekNextToken().getType() != TokenType.RCURLY) {
            body.add(parseStmt());
        }
        Token end = consumeAndCheck(TokenType.RCURLY);
        int endCol = end.getEndCol();
        int endLine = end.getLineNumber();
        return new Stmt.Block(body, startCol, endCol, startLine, endLine);
    }

    /**
     * Parses a statement of the form '[identifier] = [expr];'
     * 
     * @return The AST node for an assign statement
     */
    private Stmt parseAsgn() {
        Token start = consumeAndCheck(TokenType.IDENTIFIER);
        int startCol = start.getStartCol();
        int startLine = start.getLineNumber();
        String identifierName = start.getLexeme();
        consumeAndCheck(TokenType.ASGN);
        Expr expr = parseExpr();
        int endCol = expr.getEndCol();
        int endLine = expr.getEndLine();
        consumeAndCheck(TokenType.SEMICOLON);
        return new Stmt.Assign(identifierName, expr, startCol, endCol, startLine, endLine);
    }

    /**
     * Parses a statement of the form '[expr];'
     * 
     * @return The AST node for an expression statement
    */
    private Stmt parseExprStmt() {
        Expr expr = parseExpr(); 
        int startCol = expr.getStartCol();
        int startLine = expr.getStartLine();
        int endCol = expr.getEndCol();
        int endLine = expr.getEndLine();
        consumeAndCheck(TokenType.SEMICOLON);
        return new Stmt.Expression(expr, startCol, endCol, startLine, endLine);
    }

    /**
     * Parses an expression
     * Order of precedence:
     * 1. Primary (Literal, Identifier)
     * 2. Unary (!, +, -)
     * 3. Factor (*, /)
     * 4. Term (+, -)
     * 5. Comparison (>, >=, <, <=)
     * 6. Equality (==, !=)
     * 7. And (&&)
     * 8. Or (||)
     * 
     * @return The AST node for an expression
     */
    private Expr parseExpr() {
        return parseOr();
    }

    /**
     * Parse all top level expressions of form '[expr] || [expr]',
     * then pass to parseAnd()
     * 
     * @return The AST node for the expression
     */
    private Expr parseOr() {
        Expr expr = parseAnd();
        int startCol = expr.getStartCol();
        int startLine = expr.getStartLine();
        while (peekNextToken().getType() == TokenType.OR) {
            consumeAndCheck(TokenType.OR);
            Expr right = parseAnd();
            int endCol = right.getEndCol();
            int endLine = right.getEndLine();
            expr = new Expr.Binary(expr, Operator.OR, right, startCol, endCol, startLine, endLine);
        }
        return expr;
    }

    /**
     * Parse all top level expressions of form '[expr] && [expr]',
     * then pass to parseEquality().
     * 
     * @return The AST node for the expression
     */
    private Expr parseAnd() {
        Expr expr = parseEquality();
        int startCol = expr.getStartCol();
        int startLine = expr.getStartLine();
        while (peekNextToken().getType() == TokenType.AND) {
            consumeAndCheck(TokenType.AND);
            Expr right = parseEquality();
            int endCol = right.getEndCol();
            int endLine = right.getEndLine();
            expr = new Expr.Binary(expr, Operator.AND, right, startCol, endCol, startLine, endLine);
        }
        return expr;
    }

    /**
     * Parse all top level expressions of form `[expr] [== OR !=] [expr]`,
     * then pass to parseComparison().
     * 
     * @return The AST node for the expression
     */
    private Expr parseEquality() {
        Expr expr = parseComparison();
        int startCol = expr.getStartCol();
        int startLine = expr.getStartLine();
        int endCol;
        int endLine;
        while (peekNextToken().getType() == TokenType.EQUALS || peekNextToken().getType() == TokenType.NOT_EQUALS) {
            if (peekNextToken().getType() == TokenType.EQUALS) {
                consumeAndCheck(TokenType.EQUALS);
                Expr right = parseComparison();
                endCol = right.getEndCol();
                endLine = right.getEndLine();
                expr = new Expr.Binary(expr, Operator.EQUAL, right, startCol, endCol, startLine, endLine);
            }
            else if (peekNextToken().getType() == TokenType.NOT_EQUALS) {
                consumeAndCheck(TokenType.NOT_EQUALS);
                Expr right = parseComparison();
                endCol = right.getEndCol();
                endLine = right.getEndLine();
                expr = new Expr.Binary(expr, Operator.NOT_EQUAL, right, startCol, endCol, startLine, endLine);
            }
        }
        return expr;
    }

    /**
     * Parse all top level expressions of form `[expr] [>, >=, <, OR <=] [expr]`,
     * then pass to parseTerm().
     * 
     * @return The AST node for the expression
     */
    private Expr parseComparison() {
        Expr expr = parseTerm();
        int startCol = expr.getStartCol();
        int startLine = expr.getStartLine();
        int endCol;
        int endLine;
        while (peekNextToken().getType() == TokenType.GREATER || peekNextToken().getType() == TokenType.GREATER_EQUAL
               || peekNextToken().getType() == TokenType.LESS || peekNextToken().getType() == TokenType.LESS_EQUAL) {
            switch (peekNextToken().getType()) {
                case GREATER: {
                    consumeAndCheck(TokenType.GREATER);
                    Expr right = parseTerm();
                    endCol = right.getEndCol();
                    endLine = right.getEndLine();
                    expr = new Expr.Binary(expr, Operator.GREATER, right, startCol, endCol, startLine, endLine);
                    break;
                }
                case GREATER_EQUAL: {
                    consumeAndCheck(TokenType.GREATER_EQUAL);
                    Expr right = parseTerm();
                    endCol = right.getEndCol();
                    endLine = right.getEndLine();
                    expr = new Expr.Binary(expr, Operator.GREATER_EQUAL, right, startCol, endCol, startLine, endLine);
                    break;
                }
                case LESS: {
                    consumeAndCheck(TokenType.LESS);
                    Expr right = parseTerm();
                    endCol = right.getEndCol();
                    endLine = right.getEndLine();
                    expr = new Expr.Binary(expr, Operator.LESS, right, startCol, endCol, startLine, endLine);
                    break;
                }
                case LESS_EQUAL: {
                    consumeAndCheck(TokenType.LESS_EQUAL);
                    Expr right = parseTerm();
                    endCol = right.getEndCol();
                    endLine = right.getEndLine();
                    expr = new Expr.Binary(expr, Operator.LESS_EQUAL, right, startCol, endCol, startLine, endLine);
                    break;
                }
                default:
                    break;
            }
        }

        return expr;
    }

    /**
     * Parse all top level expressions of form `[expr] [+ OR -] [expr]`,
     * then pass to parseFactor().
     * 
     * @return The AST node for the expression
     */
    private Expr parseTerm() {
        Expr expr = parseFactor();
        int startCol = expr.getStartCol();
        int startLine = expr.getStartLine();
        int endCol;
        int endLine;
        while (peekNextToken().getType() == TokenType.PLUS || peekNextToken().getType() == TokenType.MINUS) {
            switch (peekNextToken().getType()) {
                case MINUS: {
                    consumeAndCheck(TokenType.MINUS);
                    Expr right = parseFactor();
                    endCol = right.getEndCol();
                    endLine = right.getEndLine();
                    expr = new Expr.Binary(expr, Operator.MINUS, right, startCol, endCol, startLine, endLine);
                    break;
                }
                case PLUS: {
                    consumeAndCheck(TokenType.PLUS);
                    Expr right = parseFactor();
                    endCol = right.getEndCol();
                    endLine = right.getEndLine();
                    expr = new Expr.Binary(expr, Operator.PLUS, right, startCol, endCol, startLine, endLine);
                    break;
                }
                default:
                    break;
            }
        }
        return expr;
    }

    /**
     * Parse all top level expressions of the form `[expr] [* OR /] [expr]`,
     * then pass to parseUnary().
     * 
     * @return The AST node for the expression
     */
    private Expr parseFactor() {
        Expr expr = parseUnary();
        int startCol = expr.getStartCol();
        int startLine = expr.getStartLine();
        int endCol;
        int endLine;
        while (peekNextToken().getType() == TokenType.STAR || peekNextToken().getType() == TokenType.SLASH) {
            switch (peekNextToken().getType()) {
                case STAR: {
                    consumeAndCheck(TokenType.STAR);
                    Expr right = parseUnary();
                    endCol = right.getEndCol();
                    endLine = right.getEndLine();
                    expr = new Expr.Binary(expr, Operator.MULTIPLY, right, startCol, endCol, startLine, endLine);
                    break;
                }
                case SLASH: {
                    consumeAndCheck(TokenType.SLASH);
                    Expr right = parseUnary();
                    endCol = right.getEndCol();
                    endLine = right.getEndLine();
                    expr = new Expr.Binary(expr, Operator.DIVIDE, right, startCol, endCol, startLine, endLine);
                    break;
                }
                default:
                    break;
            }
        }
        return expr;
    }

    /**
     * Parse all top level expressions of the form `[!, -, OR +] [expr]`,
     * then pass to parsePrimary().
     * 
     * @return The AST node for the expression
     */
    private Expr parseUnary() {
        switch (peekNextToken().getType()) {
            case NOT: {
                Token start = consumeAndCheck(TokenType.NOT);
                int startCol = start.getStartCol();
                int startLine = start.getLineNumber();
                Expr expr = parseUnary();
                int endCol = expr.getEndCol();
                int endLine = expr.getEndLine();
                return new Expr.Unary(Operator.NOT, expr, startCol, endCol, startLine, endLine);
            }
            case MINUS: {
                Token start = consumeAndCheck(TokenType.MINUS);
                int startCol = start.getStartCol();
                int startLine = start.getLineNumber();
                Expr expr = parseUnary();
                int endCol = expr.getEndCol();
                int endLine = expr.getEndLine();
                return new Expr.Unary(Operator.MINUS, expr, startCol, endCol, startLine, endLine);
            }
            case PLUS: {
                Token start = consumeAndCheck(TokenType.PLUS);
                int startCol = start.getStartCol();
                int startLine = start.getLineNumber();
                Expr expr = parseUnary();
                int endCol = expr.getEndCol();
                int endLine = expr.getEndLine();
                return new Expr.Unary(Operator.PLUS, expr, startCol, endCol, startLine, endLine);
            }
            default: return parsePrimary();
        }
    }

    /**
     * Parse all top level expressions of form `[NUMBER, BOOLEAN, OR IDENTIFIER]`.
     * If parentheses are found, restart the expression parsing for the values inside the parentheses.
     * 
     * @return The AST node for the expression
     */
    private Expr parsePrimary() {
        // Literals
        if (peekNextToken().getType() == TokenType.NUMBER) {
            Token start = consumeAndCheck(TokenType.NUMBER);
            return new Expr.Literal(start.getLiteralValue(), start.getStartCol(), start.getEndCol(), start.getLineNumber(), start.getLineNumber());
        }
        else if (peekNextToken().getType() == TokenType.BOOLEAN) {
            Token start = consumeAndCheck(TokenType.BOOLEAN);
            return new Expr.Literal(start.getLiteralValue(), start.getStartCol(), start.getEndCol(), start.getLineNumber(), start.getLineNumber());
        }

        // Identifiers
        else if (peekNextToken().getType() == TokenType.IDENTIFIER) {
            // Function identifiers
            if (peekTwoTokens().getType() == TokenType.LPAREN) {
                return parseFunCall();
            }

            // Variable identifiers
            Token start = consumeAndCheck(TokenType.IDENTIFIER);
            return new Expr.Variable(start.getLexeme(), start.getStartCol(), start.getEndCol(), start.getLineNumber(), start.getLineNumber());
        }

        // Nested expressions (inside paranetheses)
        else if (peekNextToken().getType() == TokenType.LPAREN) {
            consumeAndCheck(TokenType.LPAREN);
            Expr expr = parseExpr();
            consumeAndCheck(TokenType.RPAREN);
            return expr;
        }

        // Invalid
        else {
            Token t = peekNextToken();
            String msg = "Unexpected token '" + t.getLexeme() + "' of type " + t.getType() + ". Expected one of: NUMBER, BOOLEAN, IDENTIFIER, or \'(\' expression \')\'.";
            throw new Error(Arrays.asList(new Problem(t.getStartCol(), t.getEndCol(), t.getLineNumber(), t.getLineNumber(), msg)), programLines.subList(t.getLineNumber()-1, t.getLineNumber()), ErrorType.PARSE);
        }
    }

    /**
     * Parse expressions of the form `[IDENTIFIER]([ARGUMENTS])`
     * 
     * @return The AST node for the function call
     */
    private Expr parseFunCall() {
        Token start = consumeAndCheck(TokenType.IDENTIFIER);
        int startCol = start.getStartCol();
        int startLine = start.getLineNumber();
        String identifierName = start.getLexeme();
        consumeAndCheck(TokenType.LPAREN);
        List<Expr> args = new ArrayList<>();
        if (peekNextToken().getType() != TokenType.RPAREN) args = parseArgList();
        Token end = consumeAndCheck(TokenType.RPAREN);
        int endCol = end.getEndCol();
        int endLine = end.getLineNumber();
        return new Expr.Call(identifierName, args, startCol, endCol, startLine, endLine);
    }

    /**
     * Parse expressions that are found within a function call's arguments.
     * 
     * @return A list of expressions making up the function call's arguments
     */
    private List<Expr> parseArgList() {
        List<Expr> args = new ArrayList<>();
        args.add(parseExpr());
        while (peekNextToken().getType() == TokenType.COMMA) {
            consumeAndCheck(TokenType.COMMA);
            args.add(parseExpr());
        }
        return args;
    }
}


