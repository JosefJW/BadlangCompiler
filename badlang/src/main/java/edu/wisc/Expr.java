package edu.wisc;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitBinaryExpr(Binary expr);
    R visitLiteralExpr(Literal expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
    R visitCallExpr(Call expr);
  }

  private final int startCol;
  private final int endCol;
  private final int startLine;
  private final int endLine;

  public Expr(int startCol, int endCol, int startLine, int endLine) {
    this.startCol = startCol;
    this.endCol = endCol;
    this.startLine = startLine;
    this.endLine = endLine;
  }

  public int getStartCol() { return startCol; }
  public int getEndCol() { return endCol; }
  public int getStartLine() { return startLine; }
  public int getEndLine() { return endLine; }

  static class Binary extends Expr {
    Binary(Expr left, Operator operator, Expr right, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Operator operator;
    final Expr right;
  }

  static class Literal extends Expr {
    Literal(Object value, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }

  static class Unary extends Expr {
    Unary(Operator operator, Expr right, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Operator operator;
    final Expr right;
  }

  static class Variable extends Expr {
    Variable(String name, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    String name;
  }

  static class Call extends Expr {
    Call(String name, List<Expr> arguments, int startCol, int endCol, int startLine, int endLine) {
      super(startCol, endCol, startLine, endLine);
      this.name = name;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    String name;
    final List<Expr> arguments;
  }

  abstract <R> R accept(Visitor<R> visitor);
}


