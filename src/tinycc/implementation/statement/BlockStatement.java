package tinycc.implementation.statement;

import java.util.List;
import tinycc.diagnostic.Locatable;

//represents a block of code in curly braces
//it contains a list of stateemnts which can be empty as well
public class BlockStatement extends Statement {
    private Locatable loc;
    private List<Statement> statements;

    public BlockStatement(Locatable loc, List<Statement> statements) {
        this.loc = loc;
        this.statements = statements;
    }

    @Override
    public String toString() {
        // formate --> Block[stmt1, stmt2, ...] or if empty , Block[]

        StringBuilder sb = new StringBuilder("Block[");

        for (int i = 0; i < statements.size(); i++) {
            sb.append(statements.get(i).toString());

            // add coma after every statment except the last one
            if (i < statements.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }
}
