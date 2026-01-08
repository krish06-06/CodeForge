package com.codeforge.compiler.parser;

import com.codeforge.compiler.ast.*;
import com.codeforge.compiler.token.*;

import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) { this.tokens = tokens; }

    private Token current() { return tokens.get(pos); }

    private void eat(TokenType type) {
        if (current().type == type) pos++;
        else throw new RuntimeException("Unexpected token: " + current().type);
    }

    public Node parse() {
        // Only handles print statements with simple arithmetic: print(NUMBER + NUMBER)
        if (current().type == TokenType.PRINT) {
            eat(TokenType.PRINT);
            eat(TokenType.LPAREN);
            NumberNode left = new NumberNode(Integer.parseInt(current().lexeme));
            eat(TokenType.NUMBER);
            String op = current().lexeme;
            eat(TokenType.PLUS);
            NumberNode right = new NumberNode(Integer.parseInt(current().lexeme));
            eat(TokenType.NUMBER);
            eat(TokenType.RPAREN);
            return new PrintNode(new BinOpNode(left, op, right));
        }
        return null;
    }
}
