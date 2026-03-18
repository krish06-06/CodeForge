package com.codeforge.compiler.lexer;

import com.codeforge.compiler.token.Token;
import com.codeforge.compiler.token.TokenType;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LexerTest {

    @Test
    void tokenizesIndentedPythonLikeSource() {
        String source = """
            if value == 10:
                print("ok")
            print(value)
            """;

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        assertEquals(TokenType.IF, tokens.get(0).type);
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals(TokenType.DOUBLE_EQUAL, tokens.get(2).type);
        assertEquals(TokenType.NUMBER, tokens.get(3).type);
        assertEquals(TokenType.COLON, tokens.get(4).type);
        assertEquals(TokenType.NEWLINE, tokens.get(5).type);
        assertEquals(TokenType.INDENT, tokens.get(6).type);
        assertEquals(TokenType.PRINT, tokens.get(7).type);
        assertEquals(TokenType.DEDENT, tokens.get(12).type);
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).type);
    }
}
