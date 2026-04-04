package com.codeforge.compiler.ast;

public class UnaryOpNode extends Node {
    public final String op;
    public final Node expression;

    public UnaryOpNode(String op, Node expression) {
        this.op = op;
        this.expression = expression;
    }
}
