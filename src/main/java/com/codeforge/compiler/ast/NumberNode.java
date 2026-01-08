package com.codeforge.compiler.ast;

public class NumberNode extends Node {
    public final int value;

    public NumberNode(int value) {
        this.value = value;
    }
}
