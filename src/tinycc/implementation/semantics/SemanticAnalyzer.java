package tinycc.implementation.semantics;

import tinycc.implementation.expression.Expression;
import java.util.List;
import tinycc.diagnostic.Diagnostic;
import tinycc.implementation.declaration.ExternalDeclaration;
import tinycc.implementation.declaration.FunctionDefinition;
import tinycc.implementation.type.Type;

//this is gonna traverse AST to perform name and type checking

public class SemanticAnalyzer {
    private SymbolTable symbolTable;
    private Diagnostic diagnostic; // this is gonna be used to emit errors
    private Type currentFunctionReturnType;

    public SemanticAnalyzer(Diagnostic diagnostic) {
        this.symbolTable = new SymbolTable();
        this.diagnostic = diagnostic;
    }

    public void analyze(List<Object> programRoots) {

        // register all global declarations (PASS 1)
        for (Object root : programRoots) {
            if (root instanceof ExternalDeclaration) {
                ExternalDeclaration decl = (ExternalDeclaration) root;
                checkAndInsertGlobal(decl.getNameText(), decl.getType(), decl.getName());
            } else if (root instanceof FunctionDefinition) {
                FunctionDefinition func = (FunctionDefinition) root;
                checkAndInsertGlobal(func.getNameText(), func.getType(), func.getName());
            }
        }

        // PASS 2
        // analyze the actual code inside the fucntions
        for (Object root : programRoots) {
            if (root instanceof FunctionDefinition) {
                FunctionDefinition func = (FunctionDefinition) root;
                analyzeFunction(func);
            }
        }
    }

    // helper method to insert globals and check tinyCrules
    private void checkAndInsertGlobal(String name, Type type, tinycc.parser.Token locationToken) {
        // rule: variables cannot be of type void
        if (!(type instanceof tinycc.implementation.type.FunctionType) && type.toString().equals("Type_void")) {
            diagnostic.printError(locationToken, "Variables cannot be declared with type void.");
            return;
        }

        Type existingType = symbolTable.lookup(name);

        if (existingType != null) {
            // rule if it already exists the types must match exactly
            if (!existingType.toString().equals(type.toString())) {
                diagnostic.printError(locationToken, "Redeclaration of '" + name + "' with a different type.");
            }
        } else {
            // else its new so insert it into the global scope
            symbolTable.insert(name, type);
        }
    }

    private void analyzeFunction(FunctionDefinition func) {
        symbolTable.enterScope(); // open a new local scope for the function

        // DONE: register function parameters into this new scope
        // get the paameter types and names
        tinycc.implementation.type.FunctionType funcType = (tinycc.implementation.type.FunctionType) func.getType();
        List<Type> paramTypes = funcType.getParameters();
        List<tinycc.parser.Token> paramNames = func.getParameterNames();

        // loop througb and register eacg parmeter
        for (int i = 0; i < paramNames.size(); i++) {
            Type pType = paramTypes.get(i);
            tinycc.parser.Token pNameToken = paramNames.get(i);

            // rule: parameters cannot be void
            if (pType.toString().equals("Type_void")) {
                diagnostic.printError(pNameToken, "Function parameter cannot be of type void.");
                continue;
            }

            // attempt to insert into the local scope
            boolean success = symbolTable.insert(pNameToken.getText(), pType);
            if (!success) {
                // rule: parameters must have unique names
                diagnostic.printError(pNameToken, "Duplicate parameter name: " + pNameToken.getText());
            }
        }

        this.currentFunctionReturnType = funcType.getReturnType();

        // recursivelt walk through the func.getBody() and check the statements
        checkStatement(func.getBody());

        // kiill it when the function ends
        this.currentFunctionReturnType = null;

        symbolTable.leaveScope();
    }

    // recursively walks through statments and checks their semantics
    private void checkStatement(tinycc.implementation.statement.Statement stmt) {
        if (stmt == null) {
            return;
        }

        if (stmt instanceof tinycc.implementation.statement.BlockStatement) { // check for block statement
            tinycc.implementation.statement.BlockStatement block = (tinycc.implementation.statement.BlockStatement) stmt;
            // true so its a block

            symbolTable.enterScope(); // a new local scope is declared

            // recursively check every statement inside this block
            for (tinycc.implementation.statement.Statement innerStmt : block.getStatements()) {
                checkStatement(innerStmt);
            }

            symbolTable.leaveScope(); // leave scope ie destroy it when the block ends

        }
        // now check for different rules
        else if (stmt instanceof tinycc.implementation.statement.DeclarationStatement) {
            // DONE: check for local var decl
            tinycc.implementation.statement.DeclarationStatement decl = (tinycc.implementation.statement.DeclarationStatement) stmt;

            // rule: loacl variable cannot be of void type
            if (decl.getType().toString().equals("Type_void")) {
                diagnostic.printError(decl.getName(), "Variable cannot be of type void.");
                return;
            }

            // now attempt add it to a local scope
            boolean success = symbolTable.insert(decl.getName().getText(), decl.getType());
            if (!success) {
                // Rule: Cannot declare two variables with the same name in the EXACT same scope
                diagnostic.printError(decl.getName(),
                        "Variable '" + decl.getName().getText() + "' is already declared in this scope.");
            }

            // if there is an initialization, we need to type check that
            if (decl.getInit() != null) {
                Type initType = checkExpression(decl.getInit());
                // DONE: Verify that the type of decl.getInit() matches decl.getType()

                // strict exact math check for now
                if (initType != null && !initType.toString().equals(decl.getType().toString())) {
                    // implicit char to int and int to char
                    boolean isImplicitCharToInt = decl.getType().toString().equals("Type_int")
                            && initType.toString().equals("Type_char");
                    boolean isImplicitIntToChar = decl.getType().toString().equals("Type_char")
                            && initType.toString().equals("Type_int");

                    // implicit void* to any pointer
                    boolean isVoidPointerCast = decl.getType() instanceof tinycc.implementation.type.PointerType
                            && initType.toString().equals("Pointer[Type_void]");

                    if (!isImplicitCharToInt && !isImplicitIntToChar && !isVoidPointerCast) {
                        diagnostic.printError(decl.getName(), "Type mismatch in declaration. Expected "
                                + decl.getType().toString() + " but got " + initType.toString());
                    }
                }
            }

        } else if (stmt instanceof tinycc.implementation.statement.ExpressionStatement) {
            // DONE: check standalone expressions
            tinycc.implementation.statement.ExpressionStatement exprStmt = (tinycc.implementation.statement.ExpressionStatement) stmt;
            // just check it to find any errors in this standalone expression
            checkExpression(exprStmt.getExpression());

        } else if (stmt instanceof tinycc.implementation.statement.ReturnStatement) {
            // DONE: check return types
            // DONE: Verify the returned expression matches the function's return type

            tinycc.implementation.statement.ReturnStatement ret = (tinycc.implementation.statement.ReturnStatement) stmt;

            if (ret.getExpression() != null) {
                Type actualRetType = checkExpression(ret.getExpression());

                // compare the actual returned type to the function's expected return type
                if (currentFunctionReturnType != null && actualRetType != null) {
                    if (!actualRetType.toString().equals(currentFunctionReturnType.toString())) {

                        // Allow implicit char <-> int conversion
                        boolean isImplicitCharToInt = currentFunctionReturnType.toString().equals("Type_int")
                                && actualRetType.toString().equals("Type_char");
                        boolean isImplicitIntToChar = currentFunctionReturnType.toString().equals("Type_char")
                                && actualRetType.toString().equals("Type_int");

                        // Allow implicit void* to any pointer
                        boolean isVoidPointerCast = currentFunctionReturnType instanceof tinycc.implementation.type.PointerType
                                && actualRetType.toString().equals("Pointer[Type_void]");

                        if (!isImplicitCharToInt && !isImplicitIntToChar && !isVoidPointerCast) {
                            diagnostic.printError(null, "Return type mismatch. Expected "
                                    + currentFunctionReturnType.toString() + " but got " + actualRetType.toString());
                        }
                    }
                }
            } else {
                // handle empty return
                if (currentFunctionReturnType != null && !currentFunctionReturnType.toString().equals("Type_void")) {
                    diagnostic.printError(null, "Missing return value. Function is expected to return "
                            + currentFunctionReturnType.toString());
                }
            }
        } else if (stmt instanceof tinycc.implementation.statement.IfStatement) {
            // DONE: check conditions
            tinycc.implementation.statement.IfStatement ifStmt = (tinycc.implementation.statement.IfStatement) stmt;

            // check the condition
            Type condType = checkExpression(ifStmt.getCondition());
            if (condType != null && condType.toString().equals("Type_void")) {
                // check if its void ie if it aint null then see if it returns void
                diagnostic.printError(null, "Condition in 'if' statement cannot be void.");
            }

            // recursively check the if then block
            checkStatement(ifStmt.getConsequence());

            // recursively check the 'else' block (if it exists)
            if (ifStmt.getAlternative() != null) {
                checkStatement(ifStmt.getAlternative());
            }

        } else if (stmt instanceof tinycc.implementation.statement.WhileStatement) {
            // Done: check loops
            tinycc.implementation.statement.WhileStatement whileStmt = (tinycc.implementation.statement.WhileStatement) stmt;

            // check the condition
            Type condType = checkExpression(whileStmt.getCondition());
            if (condType != null && condType.toString().equals("Type_void")) {
                diagnostic.printError(null, "Condition in 'while' statement cannot be void.");
            }

            // recursively check the loop body
            checkStatement(whileStmt.getBody());
        } else {
            throw new UnsupportedOperationException("Unknown statement type: " + stmt.getClass().getSimpleName());
        }
    }

    // recursively evaluate an expression and return its type
    private Type checkExpression(tinycc.implementation.expression.Expression expr) {
        if (expr == null) {
            return null;
        }

        if (expr instanceof tinycc.implementation.expression.PrimaryExpression) {
            tinycc.implementation.expression.PrimaryExpression primary = (tinycc.implementation.expression.PrimaryExpression) expr;
            tinycc.parser.Token token = primary.getToken();

            // is it a variable,so check memory
            if (token.getKind() == tinycc.parser.TokenKind.IDENTIFIER) {
                Type declaredType = symbolTable.lookup(token.getText());
                if (declaredType == null) {
                    diagnostic.printError(token, "Undeclared variable: '" + token.getText() + "'");
                    // return a dummy type to prevent a crash by the compilor on cascading errors
                    return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);
                }
                return declaredType;
            }

            // is it a number or char, then the tyoe is int
            else if (token.getKind() == tinycc.parser.TokenKind.NUMBER
                    || token.getKind() == tinycc.parser.TokenKind.CHARACTER) {
                return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);
            }
            // is it a string literal the type is char*
            else if (token.getKind() == tinycc.parser.TokenKind.STRING) {
                tinycc.implementation.type.BaseType charType = new tinycc.implementation.type.BaseType(
                        tinycc.parser.TokenKind.CHAR);
                return new tinycc.implementation.type.PointerType(charType);
            }
        }

        else if (expr instanceof tinycc.implementation.expression.BinaryExpression) {
            // DONE:handle math assignments
            tinycc.implementation.expression.BinaryExpression binOp = (tinycc.implementation.expression.BinaryExpression) expr;

            // recursively find the types on the left and right sides
            Type leftType = checkExpression(binOp.getLeft());
            Type rightType = checkExpression(binOp.getRight());
            tinycc.parser.Token operator = binOp.getOperator();

            if (leftType == null || rightType == null) {
                return null; // error prevention
            }

            // dealing with = (assignment)
            if (operator.getKind() == tinycc.parser.TokenKind.EQUAL) {
                // rule left side must be an assignable location L evaluable
                // Done: verify binOp.getLeft() is an Lvalue (like a variable or dereferenced pointer)

                // now we enforce the Lvalue rule
                if (!isLValue(binOp.getLeft())) {
                    diagnostic.printError(operator,
                            "Left side of assignment must be a variable or a dereferenced pointer.");
                }

                // check type compatibility (allowing implicit char <-> int conversion)
                if (!leftType.toString().equals(rightType.toString())) {
                    boolean isImplicitCharToInt = leftType.toString().equals("Type_int")
                            && rightType.toString().equals("Type_char");
                    boolean isImplicitIntToChar = leftType.toString().equals("Type_char")
                            && rightType.toString().equals("Type_int");

                    // implicit void* to any pointer
                    boolean isVoidPointerCast = leftType instanceof tinycc.implementation.type.PointerType
                            && rightType.toString().equals("Pointer[Type_void]");

                    if (!isImplicitCharToInt && !isImplicitIntToChar && !isVoidPointerCast) {
                        diagnostic.printError(operator,
                                "Type mismatch in assignment. Cannot assign " + rightType + " to " + leftType);
                    }
                }
                return leftType; // Assignments return the type of their left operand
            }

            // handle math (+, -, *, /)
            else if (operator.getKind() == tinycc.parser.TokenKind.PLUS ||
                    operator.getKind() == tinycc.parser.TokenKind.MINUS ||
                    operator.getKind() == tinycc.parser.TokenKind.ASTERISK ||
                    operator.getKind() == tinycc.parser.TokenKind.SLASH) {

                // int op int = int ie normal math
                if (leftType.toString().equals("Type_int") && rightType.toString().equals("Type_int")) {
                    return leftType;
                }
                // pointer +/- int = pointer ie pointer artihmetic
                else if (leftType instanceof tinycc.implementation.type.PointerType
                        && rightType.toString().equals("Type_int")
                        && (operator.getKind() == tinycc.parser.TokenKind.PLUS
                                || operator.getKind() == tinycc.parser.TokenKind.MINUS)) {
                    return leftType;
                }
                // int + pointer = pointer ie pointer arithemetic but the ther way around
                else if (leftType.toString().equals("Type_int")
                        && rightType instanceof tinycc.implementation.type.PointerType
                        && operator.getKind() == tinycc.parser.TokenKind.PLUS) {
                    return rightType;
                } else {
                    diagnostic.printError(operator,
                            "Invalid types for math operation: " + leftType + " and " + rightType);
                    return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);
                }
            }

            // handle comparison operatorrs (eg ==, < , >)
            else if (operator.getKind() == tinycc.parser.TokenKind.EQUAL_EQUAL
                    || operator.getKind() == tinycc.parser.TokenKind.LESS) {
                // comparisons always evaluate to an integer (0 for false, 1 for true)
                return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);
            }
            // fallback for other operators
            return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);

        } else if (expr instanceof tinycc.implementation.expression.CallExpression) {
            tinycc.implementation.expression.CallExpression call = (tinycc.implementation.expression.CallExpression) expr;

            // get the function name ie callee
            Expression calleeExpr = call.getCallee();
            if (!(calleeExpr instanceof tinycc.implementation.expression.PrimaryExpression)) {
                diagnostic.printError(call.getToken(), "Invalid function call format.");
                return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT); // fallback
            }

            tinycc.parser.Token funcToken = ((tinycc.implementation.expression.PrimaryExpression) calleeExpr)
                    .getToken();
            String funcName = funcToken.getText();

            // look up the function in the lookup table
            Type funcType = symbolTable.lookup(funcName);
            if (funcType == null) {
                diagnostic.printError(funcToken, "Call to undeclared function: '" + funcName + "'");
                return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);
            }

            // verify for safety that its a function
            if (!(funcType instanceof tinycc.implementation.type.FunctionType)) {
                diagnostic.printError(funcToken, "Called object '" + funcName + "' is not a function.");
                return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);
            }

            tinycc.implementation.type.FunctionType fType = (tinycc.implementation.type.FunctionType) funcType;

            // check if the argument count matches up with the parameter count
            List<Expression> args = call.getArguments();
            List<Type> params = fType.getParameters();
            if (args.size() != params.size()) {
                diagnostic.printError(funcToken,
                        "Function '" + funcName + "' expects " + params.size() + " arguments, but got " + args.size());
            } else {
                // verify the type of every argument, make sure it matches
                for (int i = 0; i < args.size(); i++) {
                    Type argType = checkExpression(args.get(i));
                    Type paramType = params.get(i);

                    if (argType != null && !argType.toString().equals(paramType.toString())) {
                        // Allow implicit char <-> int promotion
                        boolean isImplicitCharToInt = paramType.toString().equals("Type_int")
                                && argType.toString().equals("Type_char");
                        boolean isImplicitIntToChar = paramType.toString().equals("Type_char")
                                && argType.toString().equals("Type_int");

                        // Allow implicit void* to any pointer
                        boolean isVoidPointerCast = paramType instanceof tinycc.implementation.type.PointerType
                                && argType.toString().equals("Pointer[Type_void]");

                        if (!isImplicitCharToInt && !isImplicitIntToChar && !isVoidPointerCast) {
                            diagnostic.printError(funcToken, "Argument " + (i + 1) + " type mismatch. Expected "
                                    + paramType + " but got " + argType);
                        }
                    }
                }
            }
            // a function call will evaluate to its return type
            return fType.getReturnType();

        }
        // for unary operations
        else if (expr instanceof tinycc.implementation.expression.UnaryExpression) {
            tinycc.implementation.expression.UnaryExpression unary = (tinycc.implementation.expression.UnaryExpression) expr;
            tinycc.parser.Token operator = unary.getOperator();

            // get the type of the single operand
            Type operandType = checkExpression(unary.getOperand());
            if (operandType == null) {
                return null;
            }

            // Addressof operator ie &, this creates a pointer
            if (operator.getText().equals("&")) {
                // rule:can only take the address of an lvalue (a specific memory location)
                if (!isLValue(unary.getOperand())) {
                    diagnostic.printError(operator, "Cannot take the address of a non-LValue.");
                    return new tinycc.implementation.type.PointerType(operandType); // return anyway to prevent
                                                                                    // cascading errors
                }
                // taking the address of an 'int' gives you an 'int*'
                return new tinycc.implementation.type.PointerType(operandType);
            }

            // dereference operator ie *
            else if (operator.getText().equals("*")) {
                // rule: you can only dereference a pointer type
                if (!(operandType instanceof tinycc.implementation.type.PointerType)) {
                    diagnostic.printError(operator, "Cannot dereference a non-pointer type: " + operandType);
                    return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);
                }
                // dereferencing an 'int*' gives you the underlying 'int'
                tinycc.implementation.type.PointerType ptrType = (tinycc.implementation.type.PointerType) operandType;
                return ptrType.getPointsTo();
            }

            // math or logic operators which will always return int
            else if (operator.getText().equals("-") || operator.getText().equals("!")) {
                return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);
            }

            // fall back for sizeof and stuff
            return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);

        }
        // fallback for unimplemented expressions
        return new tinycc.implementation.type.BaseType(tinycc.parser.TokenKind.INT);

    }

    // helper
    // determines if an expression is a valid L-value (can be assigned to)
    private boolean isLValue(tinycc.implementation.expression.Expression expr) {
        if (expr == null) {
            return false;
        }

        // check if its a variable
        if (expr instanceof tinycc.implementation.expression.PrimaryExpression) {
            tinycc.implementation.expression.PrimaryExpression primary = (tinycc.implementation.expression.PrimaryExpression) expr;
            // it is only an lvalue if the primary expression is an identifier (variable name)
            return primary.getToken().getKind() == tinycc.parser.TokenKind.IDENTIFIER;
        }

        // check if itsa dereferenced pointer like *p = 2
        else if (expr instanceof tinycc.implementation.expression.UnaryExpression) {
            tinycc.implementation.expression.UnaryExpression unary = (tinycc.implementation.expression.UnaryExpression) expr;
            // check if the unary operator is a *
            return unary.getOperator().getText().equals("*");
        }

        // numbers strings math eqs and func calls are not lvalues since not Levalable
        return false;
    }
}