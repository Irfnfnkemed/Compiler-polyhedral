package src;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import src.AST.ASTBuilder;
import src.IR.IRBuilder;
import src.IR.IRPrinter;
import src.Util.error.Errors;
import src.Util.error.ParserErrorListener;
import src.optimize.Mem2Reg.Mem2Reg;
import src.parser.MxLexer;
import src.parser.MxParser;
import src.semantic.Semantic;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

//import static test.TestIR.testIR;
import static test.TestIR.testIR;
import static test.TestSemantic.testSemantic;


public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        try {
            String name = "./src/test";
            InputStream inputStream = new FileInputStream(name);
            MxLexer lexer = new MxLexer(CharStreams.fromStream(inputStream));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ParserErrorListener());
            MxParser parser = new MxParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(new ParserErrorListener());
            ParseTree ctx = parser.program();
            ASTBuilder AST = new ASTBuilder(ctx);
            Semantic semantic = new Semantic(AST.ASTProgram);
            semantic.check();
            var ir = new IRBuilder(AST.ASTProgram, semantic.globalScope, semantic.inlineGlobalVar);
            var irPrint = new IRPrinter(ir.irProgram);
            FileOutputStream fileOutputStream = new FileOutputStream("./src/builtin/test_standard.ll");
            PrintStream printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            irPrint.print();
            Mem2Reg mem2Reg = new Mem2Reg(ir.irProgram);
            fileOutputStream = new FileOutputStream("./src/builtin/test_mem2reg.ll");
            printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            irPrint.print();
        } catch (Errors errors) {
            System.err.println(errors.toString());
        }
        //testSemantic();
        //testIR();
    }
}

