package com.codeforge.compiler.ast;

import java.util.List;

public class ProgramNode extends Node {
    public final List<Node> statements;

    public ProgramNode(List<Node> statements) {
        this.statements = statements;
    }
}
