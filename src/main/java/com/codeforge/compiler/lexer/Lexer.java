package com.codeforge.compiler.lexer;

import com.codeforge.compiler.token.Token;
import com.codeforge.compiler.token.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("elif", TokenType.ELIF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("def", TokenType.DEF);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("True", TokenType.TRUE);
        KEYWORDS.put("False", TokenType.FALSE);
        KEYWORDS.put("None", TokenType.NONE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final Deque<Integer> indentStack = new ArrayDeque<>();

    private int index;
    private int line = 1;
    private int column = 1;
    private boolean atLineStart = true;

    public Lexer(String source) {
        this.source = source == null ? "" : source;
        indentStack.push(0);
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            if (atLineStart) {
                consumeIndentation();
            }

            if (isAtEnd()) {
                break;
            }

            char current = peek();

            if (current == '\r') {
                advance();
                continue;
            }

            if (current == '\n') {
                emit(TokenType.NEWLINE, "\\n", line, column);
                advance();
                atLineStart = true;
                continue;
            }

            if (current == ' ' || current == '\t') {
                advance();
                continue;
            }

            if (current == '#') {
                skipComment();
                continue;
            }

            if (Character.isDigit(current)) {
                tokens.add(readNumber());
                continue;
            }

            if (isIdentifierStart(current)) {
                tokens.add(readIdentifierOrKeyword());
                continue;
            }

            if (current == '"' || current == '\'') {
                tokens.add(readString());
                continue;
            }

            switch (current) {
                case '+':
                    emit(TokenType.PLUS, "+", line, column);
                    advance();
                    break;
                case '-':
                    emit(TokenType.MINUS, "-", line, column);
                    advance();
                    break;
                case '*':
                    emit(TokenType.STAR, "*", line, column);
                    advance();
                    break;
                case '/':
                    emit(TokenType.SLASH, "/", line, column);
                    advance();
                    break;
                case '%':
                    emit(TokenType.PERCENT, "%", line, column);
                    advance();
                    break;
                case '(':
                    emit(TokenType.LPAREN, "(", line, column);
                    advance();
                    break;
                case ')':
                    emit(TokenType.RPAREN, ")", line, column);
                    advance();
                    break;
                case ':':
                    emit(TokenType.COLON, ":", line, column);
                    advance();
                    break;
                case ',':
                    emit(TokenType.COMMA, ",", line, column);
                    advance();
                    break;
                case '=':
                    tokens.add(match('=') ? twoCharToken(TokenType.DOUBLE_EQUAL, "==") : oneCharToken(TokenType.EQUAL, "="));
                    break;
                case '!':
                    if (match('=')) {
                        tokens.add(twoCharToken(TokenType.NOT_EQUAL, "!="));
                    } else {
                        throw error("Unexpected '!'");
                    }
                    break;
                case '<':
                    tokens.add(match('=') ? twoCharToken(TokenType.LESS_EQUAL, "<=") : oneCharToken(TokenType.LESS, "<"));
                    break;
                case '>':
                    tokens.add(match('=') ? twoCharToken(TokenType.GREATER_EQUAL, ">=") : oneCharToken(TokenType.GREATER, ">"));
                    break;
                default:
                    throw error("Unexpected character '" + current + "'");
            }
        }

        if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).type != TokenType.NEWLINE) {
            emit(TokenType.NEWLINE, "\\n", line, column);
        }

        while (indentStack.size() > 1) {
            indentStack.pop();
            emit(TokenType.DEDENT, "<DEDENT>", line, column);
        }

        emit(TokenType.EOF, "", line, column);
        return List.copyOf(tokens);
    }

    private void consumeIndentation() {
        int indentation = 0;
        int startColumn = column;
        int startIndex = index;

        while (!isAtEnd()) {
            char current = peek();
            if (current == ' ') {
                indentation++;
                advance();
            } else if (current == '\t') {
                indentation += 4;
                advance();
            } else {
                break;
            }
        }

        if (isAtEnd()) {
            return;
        }

        char current = peek();

        if (current == '\n' || current == '\r' || current == '#') {
            atLineStart = false;
            return;
        }

        int previousIndent = indentStack.peek();
        if (indentation > previousIndent) {
            indentStack.push(indentation);
            emit(TokenType.INDENT, "<INDENT>", line, startColumn);
        } else if (indentation < previousIndent) {
            while (indentStack.size() > 1 && indentation < indentStack.peek()) {
                indentStack.pop();
                emit(TokenType.DEDENT, "<DEDENT>", line, startColumn);
            }

            if (indentStack.peek() != indentation) {
                index = startIndex;
                column = startColumn;
                throw error("Inconsistent indentation");
            }
        }

        atLineStart = false;
    }

    private void skipComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private Token readNumber() {
        int tokenLine = line;
        int tokenColumn = column;
        int start = index;
        boolean seenDot = false;

        while (!isAtEnd()) {
            char current = peek();
            if (Character.isDigit(current)) {
                advance();
            } else if (current == '.' && !seenDot && Character.isDigit(peekNext())) {
                seenDot = true;
                advance();
            } else {
                break;
            }
        }

        return new Token(TokenType.NUMBER, source.substring(start, index), tokenLine, tokenColumn);
    }

    private Token readIdentifierOrKeyword() {
        int tokenLine = line;
        int tokenColumn = column;
        int start = index;

        while (!isAtEnd() && isIdentifierPart(peek())) {
            advance();
        }

        String lexeme = source.substring(start, index);
        TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);
        return new Token(type, lexeme, tokenLine, tokenColumn);
    }

    private Token readString() {
        char quote = peek();
        int tokenLine = line;
        int tokenColumn = column;
        advance();

        StringBuilder value = new StringBuilder();
        while (!isAtEnd() && peek() != quote) {
            if (peek() == '\n') {
                throw error("Unterminated string literal");
            }

            if (peek() == '\\') {
                advance();
                if (isAtEnd()) {
                    throw error("Unterminated string escape");
                }
                value.append(readEscapeSequence(advance()));
                continue;
            }

            value.append(advance());
        }

        if (isAtEnd()) {
            throw error("Unterminated string literal");
        }

        advance();
        return new Token(TokenType.STRING, value.toString(), tokenLine, tokenColumn);
    }

    private char readEscapeSequence(char escaped) {
        return switch (escaped) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case '\\' -> '\\';
            case '"' -> '"';
            case '\'' -> '\'';
            default -> escaped;
        };
    }

    private Token oneCharToken(TokenType type, String lexeme) {
        Token token = new Token(type, lexeme, line, column);
        advance();
        return token;
    }

    private Token twoCharToken(TokenType type, String lexeme) {
        int tokenLine = line;
        int tokenColumn = column;
        advance();
        advance();
        return new Token(type, lexeme, tokenLine, tokenColumn);
    }

    private boolean match(char expected) {
        return !isAtEnd() && peekNext() == expected;
    }

    private void emit(TokenType type, String lexeme, int tokenLine, int tokenColumn) {
        tokens.add(new Token(type, lexeme, tokenLine, tokenColumn));
    }

    private char advance() {
        char current = source.charAt(index++);
        if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return current;
    }

    private char peek() {
        return source.charAt(index);
    }

    private char peekNext() {
        return index + 1 >= source.length() ? '\0' : source.charAt(index + 1);
    }

    private boolean isAtEnd() {
        return index >= source.length();
    }

    private boolean isIdentifierStart(char current) {
        return Character.isLetter(current) || current == '_';
    }

    private boolean isIdentifierPart(char current) {
        return Character.isLetterOrDigit(current) || current == '_';
    }

    private LexerException error(String message) {
        return new LexerException(message, line, column);
    }
}
