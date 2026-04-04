package com.codeforge.compiler.parser;

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
import com.codeforge.compiler.token.Token;
import com.codeforge.compiler.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Node parse() {
        List<Node> statements = new ArrayList<>();
        skipLineBreaks();

        while (!isAtEnd()) {
            statements.add(statement());
            skipLineBreaks();
        }

        return new ProgramNode(statements);
    }

    private Node statement() {
        if (match(TokenType.PRINT)) {
            return printStatement();
        }

        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.EQUAL)) {
            return assignmentStatement();
        }

        return new ExpressionStatementNode(expression());
    }

    private Node printStatement() {
        Token keyword = previous();
        consume(TokenType.LPAREN, "Expected '(' after print", keyword);
        Node expression = expression();
        consume(TokenType.RPAREN, "Expected ')' after print expression", current());
        return new PrintNode(expression);
    }

    private Node assignmentStatement() {
        Token identifier = consume(TokenType.IDENTIFIER, "Expected identifier", current());
        consume(TokenType.EQUAL, "Expected '=' after identifier", current());
        return new AssignmentNode(identifier.lexeme, expression());
    }

    private Node expression() {
        return additive();
    }

    private Node additive() {
        Node node = multiplicative();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Node right = multiplicative();
            node = new BinOpNode(node, operator.lexeme, right);
        }
        return node;
    }

    private Node multiplicative() {
        Node node = unary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token operator = previous();
            Node right = unary();
            node = new BinOpNode(node, operator.lexeme, right);
        }
        return node;
    }

    private Node unary() {
        if (match(TokenType.MINUS)) {
            return new UnaryOpNode(previous().lexeme, unary());
        }
        return primary();
    }

    private Node primary() {
        if (match(TokenType.NUMBER)) {
            Token token = previous();
            return new NumberNode((int) Double.parseDouble(token.lexeme));
        }

        if (match(TokenType.STRING)) {
            return new StringNode(previous().lexeme);
        }

        if (match(TokenType.IDENTIFIER)) {
            return new VariableNode(previous().lexeme);
        }

        if (match(TokenType.LPAREN)) {
            Node grouped = expression();
            consume(TokenType.RPAREN, "Expected ')' after grouped expression", current());
            return grouped;
        }

        throw error(current(), "Unexpected token '" + current().lexeme + "'");
    }

    private void skipLineBreaks() {
        while (match(TokenType.NEWLINE, TokenType.INDENT, TokenType.DEDENT)) {
            // Skip formatting tokens for the current starter grammar.
        }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message, Token fallback) {
        if (check(type)) {
            return advance();
        }
        throw error(fallback, message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return type == TokenType.EOF;
        }
        return current().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (pos + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(pos + 1).type == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            pos++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return current().type == TokenType.EOF;
    }

    private Token current() {
        return tokens.get(pos);
    }

    private Token previous() {
        return tokens.get(pos - 1);
    }

    private ParserException error(Token token, String message) {
        return new ParserException(message, token.line, token.column);
    }
}
