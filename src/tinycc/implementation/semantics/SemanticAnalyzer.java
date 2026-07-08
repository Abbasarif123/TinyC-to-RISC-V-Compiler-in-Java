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
                // TODO: insert decl into global scope
            } else if (root instanceof FunctionDefinition) {
                FunctionDefinition func = (FunctionDefinition) root;
                // TODO: insert func signature into the global scope

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

    private void analyzeFunction(FunctionDefinition func) {
        symbolTable.enterScope(); // open a new local scope for the function

        // TODO: register function parameters into this new scope
        // TODO: recursively walk through the func.getbody() and check statements

        symbolTable.leaveScope();
    }

}
