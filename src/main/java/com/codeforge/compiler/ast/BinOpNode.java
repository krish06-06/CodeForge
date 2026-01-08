package com.codeforge.compiler.ast;

public class BinOpNode extends Node {
    public final Node left;
    public final String op;
    public final Node right;

    public BinOpNode(Node left, String op, Node right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
}
