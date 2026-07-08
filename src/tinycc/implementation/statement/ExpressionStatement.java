package tinycc.implementation.statement;

import tinycc.diagnostic.Locatable;
import tinycc.implementation.expression.Expression;

public class ExpressionStatement extends Statement {
    private Locatable loc;
    private Expression expression;

    public ExpressionStatement(Locatable loc, Expression expression) {
        this.loc = loc;
        this.expression = expression;
    }

    @Override
    public String toString() {
        // from PDF --> "Expression statements have exactly the same abstract syntax as their expression."
        return expression.toString();
    }
}
