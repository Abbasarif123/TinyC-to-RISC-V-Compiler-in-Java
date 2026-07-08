package tinycc.implementation.semantics;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import tinycc.implementation.type.Type;

//tracks variable and function declarations across different scopes

public class SymbolTable {
    // stack of scopes
    // each scope maps a variable name to its type
    private Stack<Map<String, Type>> scopes;

    public SymbolTable() {
        this.scopes = new Stack<>();
        // push the global scope when the table is created
        enterScope();
    }

    // its gonna be called whenever we enter a new block or function body
    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    // called whenever we hit the end of a block so all the local variables will be killed
    public void leaveScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    // this will attempt to register a new variable in the current scope
    // returns true if successful and false if its illegal redeclaration
    public boolean insert(String name, Type type) {
        Map<String, Type> currentScope = scopes.peek();

        // variables may only be declared only once in a local scope
        if (currentScope.containsKey(name)) {
            return false;
        }
        currentScope.put(name, type);
        return true;
    }

    // lookup looks for a variable, starting from the innermost and goes outword
    // it returns the type of variable or null if it was never declared
    public Type lookup(String name) {
        // iterate backwards from the top of the stack (innermost) to the bottom (global)
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Type> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null; // Error: Unknown identifier

    }

}
