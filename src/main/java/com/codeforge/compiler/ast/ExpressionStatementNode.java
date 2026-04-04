package com.codeforge.compiler.ast;

public class ExpressionStatementNode extends Node {
    public final Node expression;

    public ExpressionStatementNode(Node expression) {
        this.expression = expression;
    }
}
