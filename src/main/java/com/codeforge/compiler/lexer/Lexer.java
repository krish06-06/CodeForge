package com.codeforge.compiler.lexer;

import com.codeforge.compiler.token.*;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private final String input;
    private int pos = 0;

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            if (Character.isDigit(c)) {
                tokens.add(number());
                continue;
            }

            if (Character.isLetter(c)) {
                tokens.add(identifier());
                continue;
            }

            switch (c) {
                case '+':
                    tokens.add(new Token(TokenType.PLUS, "+"));
                    pos++;
                    break;
                case '-':
                    tokens.add(new Token(TokenType.MINUS, "-"));
                    pos++;
                    break;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, "("));
                    pos++;
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")"));
                    pos++;
                    break;
                default:
                    throw new RuntimeException("Unexpected character: " + c);
            }
        }

        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private Token number() {
        int start = pos;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            pos++;
        }
        return new Token(TokenType.NUMBER, input.substring(start, pos));
    }

    private Token identifier() {
        int start = pos;
        while (pos < input.length() && Character.isLetter(input.charAt(pos))) {
            pos++;
        }
        return new Token(TokenType.IDENTIFIER, input.substring(start, pos));
    }
}

