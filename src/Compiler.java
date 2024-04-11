//package src;
//
//import org.antlr.v4.runtime.CharStreams;
//import org.antlr.v4.runtime.CommonTokenStream;
//import org.antlr.v4.runtime.tree.ParseTree;
////import src.ASM.ASMBuilder;
////import src.ASM.ASMPrinter;
//import src.ASM.ASMBuilder;
//import src.ASM.ASMPrinter;
//import src.AST.ASTBuilder;
//import src.IR.IRBuilder;
//import src.Util.error.ParserErrorListener;
//import src.optimize.Mem2Reg.Mem2Reg;
//import src.optimize.RegAllocation.RegAllocation;
//import src.parser.MxLexer;
//import src.parser.MxParser;
//import src.semantic.Semantic;
//
//import java.io.FileOutputStream;
//import java.io.InputStream;
//
//
//public class Compiler {
//    public static void main(String[] args) throws Exception {
//        if (args.length == 0) {
//            System.out.println("Usage: java src.Compiler <filename>");
//            return;
//        } else if (args[0].equals("-fsyntax-only")) {
//            System.out.println("Semantic check");
//            InputStream is = System.in;
//            MxLexer lexer = new MxLexer(CharStreams.fromStream(is));
//            lexer.removeErrorListeners();
//            lexer.addErrorListener(new ParserErrorListener());
//            MxParser parser = new MxParser(new CommonTokenStream(lexer));
//            parser.removeErrorListeners();
//            parser.addErrorListener(new ParserErrorListener());
//            ParseTree ctx = parser.program();
//            ASTBuilder AST = new ASTBuilder(ctx);
//            new Semantic(AST.ASTProgram).check();
//        } else if (args[0].equals("-S")) {
//            //System.out.println("Generate assembly code");
//            InputStream is = System.in;
//            MxLexer lexer = new MxLexer(CharStreams.fromStream(is));
//            lexer.removeErrorListeners();
//            lexer.addErrorListener(new ParserErrorListener());
//            MxParser parser = new MxParser(new CommonTokenStream(lexer));
//            parser.removeErrorListeners();
//            parser.addErrorListener(new ParserErrorListener());
//            ParseTree ctx = parser.program();
//            ASTBuilder AST = new ASTBuilder(ctx);
//            Semantic semantic = new Semantic(AST.ASTProgram);
//            semantic.check();
//            IRBuilder irBuilder = new IRBuilder(AST.ASTProgram, semantic.globalScope, semantic.inlineGlobalVar);
//            Mem2Reg mem2Reg = new Mem2Reg(irBuilder.irProgram);
//            ASMBuilder asmBuilder = new ASMBuilder(irBuilder.irProgram, irBuilder.useGlobalVar);
//            ASMPrinter asmPrinter = new ASMPrinter(asmBuilder.asmProgram);
//            RegAllocation regAllocation = new RegAllocation(asmBuilder);
//            asmPrinter.print();
//            return;
//        } else {
//            System.out.println("Unknown option");
//            return;
//        }
//    }
//}
//
