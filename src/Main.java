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
import src.polyhedral.matrix.Fraction;
import src.polyhedral.matrix.Matrix;
import src.semantic.Semantic;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

//import static test.TestIR.testIR;
//import static test.TestIR.testIR;
//import static test.TestSemantic.testSemantic;


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

//
//
//import ilog.concert.IloException;
//import ilog.concert.IloLinearNumExpr;
//import ilog.concert.IloNumVar;
//import ilog.cplex.IloCplex;
//
//public class Main {
//
//    public static void main(String[] args) throws IloException {
//
//        // 创建cplex对象，往后基于此对象进行模型的建立与求解
//        IloCplex cplex = new IloCplex();
//
//        // 声明决策变量 x1，x2，x3
//        // x1 的取值范围是 -10 ~ 50
//        IloNumVar x1 = cplex.intVar(-10,50);
//        // x2 的取值范围是 0 ~ 正无穷(这里用Double类型能接受的最大值代替正无穷)
//        IloNumVar x2 = cplex.intVar(0,Integer.MAX_VALUE);
//        // x3 被限定为 等于 5 (相当于取值范围是5~5)
//        IloNumVar x3 = cplex.intVar(5,5);
//
//        // 定义目标函数表达式
//        IloLinearNumExpr target = cplex.linearNumExpr();
//        target.addTerm(1,x1);  // addTerm(a,b) 是指将 a*b 追加到表达式中
//        target.addTerm(2,x2);
//        target.addTerm(3,x3);
//
//        // 声明求解目标函数的最大值,将目标函数加入到cplex模型中
//        cplex.addMaximize(target);
//
//        // 添加约束
//        // 约束1：X1+2*X2+X3 <= 100   用表达式添加约束
//        IloLinearNumExpr expr1 = cplex.linearNumExpr();
//        expr1.addTerm(1,x1);
//        expr1.addTerm(2,x2);
//        expr1.addTerm(1,x3);
//        cplex.addLe(expr1,100);  // addLe(a,b)  代表令 a <= b
//        // 约束2：X1+X2-2*X3 >= 10
//        IloLinearNumExpr expr2 = cplex.linearNumExpr();
//        expr2.addTerm(1,x1);
//        expr2.addTerm(1,x2);
//        expr2.addTerm(-2,x3);
//        cplex.addGe(expr2,10);   // addGe(a,b)  代表令 a >= b
//        // 约束3 ： x3 = 5
//        // （由于声明x3变量的时候范围已经限制在5~5之间，所以这里其实没有必有再写了
//        // 但是为了让大家了解addEq的用法，在这里还是演示一下）
//        cplex.addEq(x3,5);    // addGe(a,b)  代表令 a = b
//
//        // 激动人心的求解时刻!
//        // 只需要调用cplex.solve()即可 ，返回值为是否找到解
//        boolean isSolve = cplex.solve();
//
//        if(isSolve){
//            // 如果找到了解
//            double result = cplex.getObjValue();  // 获取解（目标函数最大值）
//            System.out.println("目标函数最大值为："+result);
//
//            // 我们还可以看看x1，x2，x3分别取什么的情况下，使得目标函数达到最值
//            double x1_value = cplex.getValue(x1);
//            double x2_value = cplex.getValue(x2);
//            double x3_value = cplex.getValue(x3);
//            System.out.println("x1 = "+x1_value);
//            System.out.println("x2 = "+x2_value);
//            System.out.println("x3 = "+x3_value);
//
//        }else{
//            // 如果找不到解
//            System.err.println("此题无解");
//        }
//    }
//
//}

