package com.codeforge.compiler.parser;

import com.codeforge.compiler.ast.AssignmentNode;
import com.codeforge.compiler.ast.PrintNode;
import com.codeforge.compiler.ast.ProgramNode;
import com.codeforge.compiler.lexer.Lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ParserTest {

    @Test
    void parsesAssignmentsAndPrintStatements() {
        String source = """
            x = 6
            print(x + 2)
            """;

        Parser parser = new Parser(new Lexer(source).tokenize());
        ProgramNode program = assertInstanceOf(ProgramNode.class, parser.parse());

        assertEquals(2, program.statements.size());
        assertInstanceOf(AssignmentNode.class, program.statements.get(0));
        assertInstanceOf(PrintNode.class, program.statements.get(1));
    }
}
