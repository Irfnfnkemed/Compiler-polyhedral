package src.parser;// Generated from src/parser/Mx.g4 by ANTLR 4.13.0
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MxParser}.
 */
public interface MxListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MxParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(MxParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(MxParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#definition}.
	 * @param ctx the parse tree
	 */
	void enterDefinition(MxParser.DefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#definition}.
	 * @param ctx the parse tree
	 */
	void exitDefinition(MxParser.DefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#classTypeDef}.
	 * @param ctx the parse tree
	 */
	void enterClassTypeDef(MxParser.ClassTypeDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#classTypeDef}.
	 * @param ctx the parse tree
	 */
	void exitClassTypeDef(MxParser.ClassTypeDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#functionDef}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDef(MxParser.FunctionDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#functionDef}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDef(MxParser.FunctionDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#suite}.
	 * @param ctx the parse tree
	 */
	void enterSuite(MxParser.SuiteContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#suite}.
	 * @param ctx the parse tree
	 */
	void exitSuite(MxParser.SuiteContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(MxParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(MxParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#constructor}.
	 * @param ctx the parse tree
	 */
	void enterConstructor(MxParser.ConstructorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#constructor}.
	 * @param ctx the parse tree
	 */
	void exitConstructor(MxParser.ConstructorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void enterSelectStatement(MxParser.SelectStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void exitSelectStatement(MxParser.SelectStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WhileLoop}
	 * labeled alternative in {@link MxParser#loopStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileLoop(MxParser.WhileLoopContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WhileLoop}
	 * labeled alternative in {@link MxParser#loopStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileLoop(MxParser.WhileLoopContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ForLoop}
	 * labeled alternative in {@link MxParser#loopStatement}.
	 * @param ctx the parse tree
	 */
	void enterForLoop(MxParser.ForLoopContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ForLoop}
	 * labeled alternative in {@link MxParser#loopStatement}.
	 * @param ctx the parse tree
	 */
	void exitForLoop(MxParser.ForLoopContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ReturnStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStmt(MxParser.ReturnStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ReturnStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStmt(MxParser.ReturnStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BreakStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 */
	void enterBreakStmt(MxParser.BreakStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BreakStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 */
	void exitBreakStmt(MxParser.BreakStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ContinueStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 */
	void enterContinueStmt(MxParser.ContinueStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ContinueStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 */
	void exitContinueStmt(MxParser.ContinueStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#variableDef}.
	 * @param ctx the parse tree
	 */
	void enterVariableDef(MxParser.VariableDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#variableDef}.
	 * @param ctx the parse tree
	 */
	void exitVariableDef(MxParser.VariableDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#initVariable}.
	 * @param ctx the parse tree
	 */
	void enterInitVariable(MxParser.InitVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#initVariable}.
	 * @param ctx the parse tree
	 */
	void exitInitVariable(MxParser.InitVariableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArrayType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterArrayType(MxParser.ArrayTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArrayType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitArrayType(MxParser.ArrayTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FoundationType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterFoundationType(MxParser.FoundationTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FoundationType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitFoundationType(MxParser.FoundationTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ClassType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterClassType(MxParser.ClassTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ClassType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitClassType(MxParser.ClassTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FunctionCallLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallLhsExp(MxParser.FunctionCallLhsExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FunctionCallLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallLhsExp(MxParser.FunctionCallLhsExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BoolExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBoolExp(MxParser.BoolExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BoolExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBoolExp(MxParser.BoolExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ClassMemberLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterClassMemberLhsExp(MxParser.ClassMemberLhsExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ClassMemberLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitClassMemberLhsExp(MxParser.ClassMemberLhsExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NewArrayExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterNewArrayExp(MxParser.NewArrayExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NewArrayExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitNewArrayExp(MxParser.NewArrayExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StringExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterStringExp(MxParser.StringExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StringExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitStringExp(MxParser.StringExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ThisPointerExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterThisPointerExp(MxParser.ThisPointerExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ThisPointerExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitThisPointerExp(MxParser.ThisPointerExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PrefixLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterPrefixLhsExp(MxParser.PrefixLhsExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PrefixLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitPrefixLhsExp(MxParser.PrefixLhsExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NullExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterNullExp(MxParser.NullExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NullExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitNullExp(MxParser.NullExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code VariableLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterVariableLhsExp(MxParser.VariableLhsExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code VariableLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitVariableLhsExp(MxParser.VariableLhsExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ClassMemFunctionLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterClassMemFunctionLhsExp(MxParser.ClassMemFunctionLhsExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ClassMemFunctionLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitClassMemFunctionLhsExp(MxParser.ClassMemFunctionLhsExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BinaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryExp(MxParser.BinaryExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BinaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryExp(MxParser.BinaryExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PrimaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExp(MxParser.PrimaryExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PrimaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExp(MxParser.PrimaryExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PostfixExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExp(MxParser.PostfixExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PostfixExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExp(MxParser.PostfixExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AssignExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterAssignExp(MxParser.AssignExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AssignExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitAssignExp(MxParser.AssignExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NewClassExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterNewClassExp(MxParser.NewClassExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NewClassExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitNewClassExp(MxParser.NewClassExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TernaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExp(MxParser.TernaryExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TernaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExp(MxParser.TernaryExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArrayElementLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterArrayElementLhsExp(MxParser.ArrayElementLhsExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArrayElementLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitArrayElementLhsExp(MxParser.ArrayElementLhsExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExp(MxParser.UnaryExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExp(MxParser.UnaryExpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumberExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterNumberExp(MxParser.NumberExpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumberExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitNumberExp(MxParser.NumberExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#parallelExp}.
	 * @param ctx the parse tree
	 */
	void enterParallelExp(MxParser.ParallelExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#parallelExp}.
	 * @param ctx the parse tree
	 */
	void exitParallelExp(MxParser.ParallelExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#brackets}.
	 * @param ctx the parse tree
	 */
	void enterBrackets(MxParser.BracketsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#brackets}.
	 * @param ctx the parse tree
	 */
	void exitBrackets(MxParser.BracketsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MxParser#bracketsWithIndex}.
	 * @param ctx the parse tree
	 */
	void enterBracketsWithIndex(MxParser.BracketsWithIndexContext ctx);
	/**
	 * Exit a parse tree produced by {@link MxParser#bracketsWithIndex}.
	 * @param ctx the parse tree
	 */
	void exitBracketsWithIndex(MxParser.BracketsWithIndexContext ctx);
}