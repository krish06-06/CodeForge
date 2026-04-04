package com.codeforge.compiler.ast;

public class AssignmentNode extends Node {
    public final String name;
    public final Node expression;

    public AssignmentNode(String name, Node expression) {
        this.name = name;
        this.expression = expression;
    }
}
