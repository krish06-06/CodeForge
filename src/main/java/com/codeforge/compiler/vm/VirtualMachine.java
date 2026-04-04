package com.codeforge.compiler.vm;

import com.codeforge.compiler.ast.AssignmentNode;
import com.codeforge.compiler.ast.BinOpNode;
import com.codeforge.compiler.ast.ExpressionStatementNode;
import com.codeforge.compiler.ast.Node;
import com.codeforge.compiler.ast.NumberNode;
import com.codeforge.compiler.ast.PrintNode;
import com.codeforge.compiler.ast.ProgramNode;
import com.codeforge.compiler.ast.StringNode;
import com.codeforge.compiler.ast.UnaryOpNode;
import com.codeforge.compiler.ast.VariableNode;

import java.util.HashMap;
import java.util.Map;

public class VirtualMachine {

    private final Map<String, Object> variables = new HashMap<>();

    public void run(Node node) {
        execute(node);
    }

    private void execute(Node node) {
        if (node instanceof ProgramNode programNode) {
            for (Node statement : programNode.statements) {
                execute(statement);
            }
            return;
        }

        if (node instanceof PrintNode printNode) {
            System.out.println(evaluate(printNode.expression));
            return;
        }

        if (node instanceof AssignmentNode assignmentNode) {
            variables.put(assignmentNode.name, evaluate(assignmentNode.expression));
            return;
        }

        if (node instanceof ExpressionStatementNode expressionStatementNode) {
            evaluate(expressionStatementNode.expression);
        }
    }

    private Object evaluate(Node node) {
        if (node instanceof NumberNode numberNode) {
            return numberNode.value;
        }

        if (node instanceof StringNode stringNode) {
            return stringNode.value;
        }

        if (node instanceof VariableNode variableNode) {
            return variables.getOrDefault(variableNode.name, 0);
        }

        if (node instanceof UnaryOpNode unaryNode) {
            Object value = evaluate(unaryNode.expression);
            if (value instanceof Integer intValue && "-".equals(unaryNode.op)) {
                return -intValue;
            }
            return value;
        }

        if (node instanceof BinOpNode binNode) {
            Object left = evaluate(binNode.left);
            Object right = evaluate(binNode.right);
            return applyBinaryOperator(left, right, binNode.op);
        }

        return null;
    }

    private Object applyBinaryOperator(Object left, Object right, String op) {
        if ("+".equals(op) && (left instanceof String || right instanceof String)) {
            return String.valueOf(left) + right;
        }

        int leftValue = (int) left;
        int rightValue = (int) right;

        return switch (op) {
            case "+" -> leftValue + rightValue;
            case "-" -> leftValue - rightValue;
            case "*" -> leftValue * rightValue;
            case "/" -> rightValue == 0 ? 0 : leftValue / rightValue;
            case "%" -> rightValue == 0 ? 0 : leftValue % rightValue;
            default -> 0;
        };
    }
}
