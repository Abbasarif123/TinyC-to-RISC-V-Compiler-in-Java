package tinycc.implementation.statement;

import tinycc.diagnostic.Locatable;
import tinycc.implementation.expression.Expression;

public class ReturnStatement extends Statement {
    private Locatable loc;
    private Expression expression; // can be null

    public ReturnStatement(Locatable loc, Expression expression) {
        this.loc = loc;
        this.expression = expression;
    }

    @Override
    public String toString() {
        // format --> Return[expr] or Return[] if there is no expression
        if (expression != null) {
            return "Return[" + expression.toString() + "]";
        } else {
            return "Return[]";
        }

    }

    // getter
    public Expression getExpression() {
        return expression;
    }
}
