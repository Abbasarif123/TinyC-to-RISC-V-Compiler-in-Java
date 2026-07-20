package tinycc.implementation.codegen;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

// tracks memory offsets for local variables relative to the frame pointer

public class ActivationRecord {

    private Stack<Map<String, Integer>> scopes;
    private int currentOffset;

    public ActivationRecord() {
        this.scopes = new Stack<>();
        // bugfix remove the 8 bytes needed by ra and s0 will be skiped
        // start at 0 and move in negative offsets of 4
        this.currentOffset = -8;
        enterScope();
    }

    // called when entering a new block

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    // called when exiting a block
    // comment to check if git push is working
    public void leaveScope() {
        if (!scopes.isEmpty()) {
            scopes.pop();
        }
    }

    // allocates 4 bytes for a new variable
    public int allocateLocal(String variableName) {
        currentOffset -= 4; // mpve the pointer by 4
        scopes.peek().put(variableName, currentOffset);
        return currentOffset;
    }

    // retrieves the offset for an exxisting variable by checking from the innermost scope outward
    public Integer getOffset(String variableName) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Integer> scope = scopes.get(i);
            if (scope.containsKey(variableName)) {
                return scope.get(variableName);
            }
        }
        return null; // should never happen if semantic analysis passed
    }
}