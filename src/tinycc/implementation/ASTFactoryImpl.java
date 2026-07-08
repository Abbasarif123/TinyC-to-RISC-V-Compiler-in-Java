package tinycc.implementation;

import java.util.List;
import tinycc.diagnostic.Locatable;
import tinycc.parser.ASTFactory;
import tinycc.parser.Token;
import tinycc.parser.TokenKind;
import tinycc.implementation.declaration.ExternalDeclaration;
import tinycc.implementation.declaration.FunctionDefinition;
import tinycc.implementation.expression.BinaryExpression;
import tinycc.implementation.expression.CallExpression;
import tinycc.implementation.expression.ConditionalExpression;
import tinycc.implementation.expression.Expression;
import tinycc.implementation.expression.PrimaryExpression;
import tinycc.implementation.expression.UnaryExpression;
import tinycc.implementation.statement.BlockStatement;
import tinycc.implementation.statement.DeclarationStatement;
import tinycc.implementation.statement.ExpressionStatement;
import tinycc.implementation.statement.IfStatement;
import tinycc.implementation.statement.ReturnStatement;
import tinycc.implementation.statement.Statement;
import tinycc.implementation.statement.WhileStatement;
import tinycc.implementation.type.Type;
import tinycc.implementation.type.BaseType;
import tinycc.implementation.type.FunctionType;
import tinycc.implementation.type.PointerType;

public class ASTFactoryImpl implements ASTFactory {
    // this list acts as the root of the AST, storing all global functions and variables
    private List<Object> programRoots = new java.util.ArrayList<>(); // its like a ledger for our C program

    @Override
    public Type createBaseType(TokenKind kind) {
        // called for int char void
        return new BaseType(kind);
    }

    @Override
    public Type createPointerType(Type pointsTo) {
        // caled when the parser sees a '*'
        return new PointerType(pointsTo);
    }

    @Override
    public Type createFunctionType(Type returnType, List<Type> parameters) {
        // called when parsing a function signature
        return new FunctionType(returnType, parameters);
    }

    @Override
    public Statement createBlockStatement(Locatable loc, List<Statement> statements) {
        // done Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createBlockStatement'");
        // creates a scope block containing multiple sattements
        return new BlockStatement(loc, statements);

    }

    @Override
    public Statement createDeclarationStatement(Type type, Token name, Expression init) {
        // DONE Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createDeclarationStatement'");
        // instantiates a local variable

        return new DeclarationStatement(type, name, init);

    }

    @Override
    public Statement createExpressionStatement(Locatable loc, Expression expression) {
        // DONE Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createExpressionStatement'");
        return new ExpressionStatement(loc, expression);
        // this is gonna be called when the parser sees a standalone expression followed by a semicolon

    }

    @Override
    public Statement createIfStatement(Locatable loc, Expression condition, Statement consequence,
            Statement alternative) {
        // DONE Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createIfStatement'");
        // creates conditionnal branch, alternative can be null
        return new IfStatement(loc, condition, consequence, alternative);

    }

    @Override
    public Statement createReturnStatement(Locatable loc, Expression expression) {
        // DONE Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createReturnStatement'");

        // called for retyrb statements, its gonna handle both return and return expr
        return new ReturnStatement(loc, expression);
    }

    @Override
    public Statement createWhileStatement(Locatable loc, Expression condition, Statement body) {
        // DONE Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createWhileStatement'");

        // creates a looping construct
        return new WhileStatement(loc, condition, body);
    }

    @Override
    public Expression createBinaryExpression(Token operator, Expression left, Expression right) {
        // Done Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createBinaryExpression'");

        // this is gonna be called for any two sided operations like math, comparisons and assignment
        return new BinaryExpression(operator, left, right);

    }

    @Override
    public Expression createCallExpression(Token token, Expression callee, List<Expression> arguments) {
        // Done Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createCallExpression'");
        return new CallExpression(token, callee, arguments);

        // this instantiates a function call node
    }

    @Override
    public Expression createConditionalExpression(Token token, Expression condition, Expression consequence,
            Expression alternative) {
        // Done Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createConditionalExpression'");
        return new ConditionalExpression(token, condition, consequence, alternative);
        // instantiates a ternary operator node

    }

    @Override
    public Expression createUnaryExpression(Token operator, boolean postfix, Expression operand) {
        // Done Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createUnaryExpression'");
        // instantiates a single operand math or logic node

        return new UnaryExpression(operator, postfix, operand);
    }

    @Override
    public Expression createPrimaryExpression(Token token) {
        // DONE Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createPrimaryExpression'");
        // this creates a node for a standalone variable, number, char, or string
        return new PrimaryExpression(token);
    }

    @Override
    public void createExternalDeclaration(Type type, Token name) {
        // DONE Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createExternalDeclaration'");

        // a global variable or function signature (eg int global_var or void foo(int))
        // Store the data for phase 2
        programRoots.add(new ExternalDeclaration(type, name));

    }

    @Override
    public void createFunctionDefinition(Type type, Token name, List<Token> parameterNames, Statement body) {
        // DONE Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'createFunctionDefinition'");

        // a complete function with a body (eg int main() { return 0 })
        programRoots.add(new FunctionDefinition(type, name, parameterNames, body));

    }

    public List<Object> getProgramRoots() {
        return programRoots;
    }

}