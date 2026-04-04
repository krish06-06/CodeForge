package com.codeforge.language;

import com.codeforge.compiler.lexer.Lexer;
import com.codeforge.compiler.lexer.LexerException;
import com.codeforge.compiler.parser.Parser;
import com.codeforge.compiler.parser.ParserException;
import com.codeforge.compiler.token.Token;
import com.codeforge.runner.PythonRunner;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class LanguageService {

    public List<Diagnostic> validate(String source) {
        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            parser.parse();
            return Collections.emptyList();
        } catch (LexerException ex) {
            return List.of(new Diagnostic(
                DiagnosticSeverity.ERROR,
                "lexer",
                ex.getMessage(),
                ex.getLine(),
                ex.getColumn()
            ));
        } catch (ParserException ex) {
            return List.of(new Diagnostic(
                DiagnosticSeverity.ERROR,
                "parser",
                ex.getMessage(),
                ex.getLine(),
                ex.getColumn()
            ));
        } catch (Exception ex) {
            return List.of(new Diagnostic(
                DiagnosticSeverity.ERROR,
                "language-service",
                ex.getMessage(),
                -1,
                -1
            ));
        }
    }

    public Process runExternally(Path filePath, String source) throws Exception {
        return PythonRunner.run(filePath, source);
    }
}
