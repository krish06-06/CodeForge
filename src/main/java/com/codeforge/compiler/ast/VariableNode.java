package com.codeforge.compiler.ast;

public class VariableNode extends Node {
    public final String name;

    public VariableNode(String name) {
        this.name = name;
    }
}
