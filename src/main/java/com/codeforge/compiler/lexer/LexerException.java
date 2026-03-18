package com.codeforge.compiler.lexer;

public class LexerException extends RuntimeException {

    public LexerException(String message, int line, int column) {
        super(message + " at line " + line + ", column " + column);
    }
}
