package src;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import src.ASM.ASMBuilder;
import src.ASM.ASMPrinter;
import src.AST.ASTBuilder;
import src.IR.IRBuilder;
import src.IR.IRPrinter;
import src.Util.error.Errors;
import src.Util.error.ParserErrorListener;
import src.optimize.ADCE.ADCE;
import src.optimize.Inline.IRInline.IRInline;
import src.optimize.Mem2Reg.Mem2Reg;
import src.optimize.RegAllocation.RegAllocation;
import src.optimize.SCCP.SCCP;
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
            mem2Reg.optimize();
            fileOutputStream = new FileOutputStream("./src/builtin/test_mem2reg.ll");
            printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            irPrint.print();
            ADCE adce = new ADCE(ir.irProgram);
            adce.optimize();
            fileOutputStream = new FileOutputStream("./src/builtin/test_adce.ll");
            printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            irPrint.print();
            SCCP sccp = new SCCP(ir.irProgram);
            sccp.optimize();
            fileOutputStream = new FileOutputStream("./src/builtin/test_sccp.ll");
            printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            irPrint.print();
            var irInline = new IRInline(ir.irProgram);
            irInline.optimize();
            Mem2Reg mem2Reg2 = new Mem2Reg(ir.irProgram);
            mem2Reg2.optimize();
            ADCE adce2 = new ADCE(ir.irProgram);
            adce2.optimize();
            SCCP sccp2 = new SCCP(ir.irProgram);
            sccp2.optimize();
            Mem2Reg mem2Reg3 = new Mem2Reg(ir.irProgram);
            mem2Reg3.optimize();
            fileOutputStream = new FileOutputStream("./src/builtin/test_inline.ll");
            printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            irPrint.print();
            var asm = new ASMBuilder(ir.irProgram, ir.useGlobalVar);
            var asmPrinter = new ASMPrinter(asm.asmProgram);
            fileOutputStream = new FileOutputStream("./src/builtin/test_standard.s");
            printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            asmPrinter.print();
            var reg = new RegAllocation(asm);
            fileOutputStream = new FileOutputStream("src/builtin/test.s");
            printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            asmPrinter.print();
        } catch (Errors errors) {
            System.err.println(errors.toString());
        }
        //testSemantic();
        //testIR();
    }
}

