package src.parser;// Generated from src/parser/Mx.g4 by ANTLR 4.13.0
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link MxParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface MxVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link MxParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(MxParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefinition(MxParser.DefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#classTypeDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassTypeDef(MxParser.ClassTypeDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#functionDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDef(MxParser.FunctionDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#suite}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuite(MxParser.SuiteContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(MxParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#constructor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor(MxParser.ConstructorContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#selectStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectStatement(MxParser.SelectStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code WhileLoop}
	 * labeled alternative in {@link MxParser#loopStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileLoop(MxParser.WhileLoopContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ForLoop}
	 * labeled alternative in {@link MxParser#loopStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForLoop(MxParser.ForLoopContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ReturnStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStmt(MxParser.ReturnStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BreakStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStmt(MxParser.BreakStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ContinueStmt}
	 * labeled alternative in {@link MxParser#jumpStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStmt(MxParser.ContinueStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#variableDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDef(MxParser.VariableDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#initVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitVariable(MxParser.InitVariableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ArrayType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayType(MxParser.ArrayTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FoundationType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFoundationType(MxParser.FoundationTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ClassType}
	 * labeled alternative in {@link MxParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassType(MxParser.ClassTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FunctionCallLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCallLhsExp(MxParser.FunctionCallLhsExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BoolExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolExp(MxParser.BoolExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ClassMemberLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassMemberLhsExp(MxParser.ClassMemberLhsExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NewArrayExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewArrayExp(MxParser.NewArrayExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code StringExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringExp(MxParser.StringExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ThisPointerExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisPointerExp(MxParser.ThisPointerExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PrefixLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixLhsExp(MxParser.PrefixLhsExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NullExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullExp(MxParser.NullExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code VariableLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableLhsExp(MxParser.VariableLhsExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ClassMemFunctionLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassMemFunctionLhsExp(MxParser.ClassMemFunctionLhsExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BinaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryExp(MxParser.BinaryExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PrimaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExp(MxParser.PrimaryExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PostfixExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExp(MxParser.PostfixExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AssignExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignExp(MxParser.AssignExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NewClassExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewClassExp(MxParser.NewClassExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TernaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExp(MxParser.TernaryExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ArrayElementLhsExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayElementLhsExp(MxParser.ArrayElementLhsExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExp(MxParser.UnaryExpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NumberExp}
	 * labeled alternative in {@link MxParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumberExp(MxParser.NumberExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#parallelExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallelExp(MxParser.ParallelExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#brackets}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBrackets(MxParser.BracketsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MxParser#bracketsWithIndex}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketsWithIndex(MxParser.BracketsWithIndexContext ctx);
}