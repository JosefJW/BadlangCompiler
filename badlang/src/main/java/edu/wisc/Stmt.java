package edu.wisc;
import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitPrintspStmt(Printsp stmt);
    R visitPrintlnStmt(Println stmt);
    R visitReturnStmt(Return stmt);
    R visitVarStmt(Var stmt);
    R visitAssignStmt(Assign stmt);
    R visitWhileStmt(While stmt);
  }

  static record Parameter(String name, VarType type, int startCol, int endCol, int startLine, int endLine) {}


  private final int startCol;
  private final int endCol;
  private final int startLine;
  private final int endLine;

  public Stmt(int startCol, int endCol, int startLine, int endLine) {
    this.startCol = startCol;
    this.endCol = endCol;
    this.startLine = startLine;
    this.endLine = endLine;
  }

  public int getStartCol() { return startCol; }
  public int getEndCol() { return endCol; }
  public int getStartLine() { return startLine; }
  public int getEndLine() { return endLine; }

  static class Block extends Stmt {
    Block(List<Stmt> statements, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.statements = statements;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    final List<Stmt> statements;
  }

  static class Expression extends Stmt {
    Expression(Expr expression, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }

  static class Function extends Stmt {
    Function(String name, VarType returnType, List<Parameter> params, List<Stmt> body, int startCol, int endCol, int startLine, int endLine, int headerStartCol, int headerEndCol, int headerStartLine, int headerEndLine) {
      super(startCol, endCol, startLine, endLine);
      this.name = name;
      this.returnType = returnType;
      this.params = params;
      this.body = body;
      this.headerStartCol = headerStartCol;
      this.headerEndCol = headerEndCol;
      this.headerStartLine = headerStartLine;
      this.headerEndLine = headerEndLine;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    public int getHeaderStartCol() { return headerStartCol; }
    public int getHeaderEndCol() { return headerEndCol; }
    public int getHeaderStartLine() { return headerStartLine; }
    public int getHeaderEndLine() { return headerEndLine; }

    String name;
    final VarType returnType;
    List<Parameter> params;
    final List<Stmt> body;
    final int headerStartCol;
    final int headerEndCol;
    final int headerStartLine;
    final int headerEndLine;
  }

  static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;
  }

  static class Print extends Stmt {
    Print(Expr expression, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }

  static class Printsp extends Stmt {
    Printsp(Expr expression, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintspStmt(this);
    }

    final Expr expression;
  }

  static class Println extends Stmt {
    Println(Expr expression, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintlnStmt(this);
    }

    final Expr expression;
  }

  static class Return extends Stmt {
    Return(Expr value, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Expr value;
  }

  static class Var extends Stmt {
    Var(String name, VarType type, Expr initializer, int startCol, int endCol, int startLine, int endLine, int declaratorStartCol, int declaratorEndCol, int declaratorStartLine, int declaratorEndLine) {
      super(startCol, endCol, startLine, endLine);
      this.name = name;
      this.type = type;
      this.initializer = initializer;
      this.declaratorStartCol = declaratorStartCol;
      this.declaratorEndCol = declaratorEndCol;
      this.declaratorStartLine = declaratorStartLine;
      this.declaratorEndLine = declaratorEndLine;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    public int getDeclaratorStartCol() { return declaratorStartCol; }
    public int getDeclaratorEndCol() { return declaratorEndCol; }
    public int getDeclaratorStartLine() { return declaratorStartLine; }
    public int getDeclaratorEndLine() { return declaratorEndLine; }

    String name;
    final VarType type;
    final Expr initializer;
    final int declaratorStartCol;
    final int declaratorEndCol;
    final int declaratorStartLine;
    final int declaratorEndLine;
  }

  static class Assign extends Stmt {
    Assign(String name, Expr value, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignStmt(this);
    }

    String name;
    final Expr value;
  }

  static class While extends Stmt {
    While(Expr condition, Stmt body, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.condition = condition;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt body;
  }

  abstract <R> R accept(Visitor<R> visitor);
}


