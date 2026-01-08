package com.codeforge.compiler.ast;

public class PrintNode extends Node {
    public final Node expression;

    public PrintNode(Node expression) {
        this.expression = expression;
    }
}
