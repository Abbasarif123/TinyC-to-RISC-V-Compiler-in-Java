package tinycc.implementation.semantics;

import java.util.List;
import tinycc.diagnostic.Diagnostic;
import tinycc.implementation.declaration.ExternalDeclaration;
import tinycc.implementation.declaration.FunctionDefinition;
import tinycc.implementation.type.Type;

//this is gonna traverse AST to perform name and type checking

public class SemanticAnalyzer {
    private SymbolTable symbolTable;
    private Diagnostic diagnostic; // this is gonna be used to emit errors

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

        // TODO: recursively walk through the func.getbody() and check statements

        symbolTable.leaveScope();
    }

}
