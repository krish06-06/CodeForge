package com.codeforge.compiler.ast;

public class StringNode extends Node {
    public final String value;

    public StringNode(String value) {
        this.value = value;
    }
}
