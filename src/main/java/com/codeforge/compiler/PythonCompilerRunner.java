/*package com.codeforge.compiler;

import com.codeforge.compiler.lexer.Lexer;
import com.codeforge.compiler.token.Token;

import java.util.List;

public class PythonCompilerRunner {

    public static void run(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        for (Token t : tokens) {
            System.out.println(t);
        }
    }
}*/
package com.codeforge.compiler;

import com.codeforge.compiler.lexer.Lexer;
import com.codeforge.compiler.parser.Parser;
import com.codeforge.compiler.vm.VirtualMachine;

import com.codeforge.compiler.token.Token;

import java.util.List;

public class PythonCompilerRunner {
    public static void run(String source) {
        // 1️⃣ Lex
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        // 2️⃣ Parse
        Parser parser = new Parser(tokens);
        var ast = parser.parse();

        // 3️⃣ Run VM
        VirtualMachine vm = new VirtualMachine();
        vm.run(ast);
    }
}

