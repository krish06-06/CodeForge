package com.codeforge.compiler.vm;

import com.codeforge.compiler.ast.*;

public class VirtualMachine {
    public void run(Node node) {
        if (node instanceof PrintNode printNode) {
            int value = evaluate(printNode.expression);
            System.out.println(value);
        }
    }

    private int evaluate(Node node) {
        if (node instanceof NumberNode numberNode) return numberNode.value;
        if (node instanceof BinOpNode bin) {
            int left = evaluate(bin.left);
            int right = evaluate(bin.right);
            return switch (bin.op) {
                case "+" -> left + right;
                default -> 0;
            };
        }
        return 0;
    }
}
