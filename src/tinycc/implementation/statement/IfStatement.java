package tinycc.implementation.statement;

import tinycc.diagnostic.Locatable;
import tinycc.implementation.expression.Expression;

//if else
//else block is null when there is no else clause

public class IfStatement extends Statement {
    private Locatable loc;
    private Expression condition;
    private Statement consequence; // the block to execute if true
    private Statement alternative; // the else block (can be null)

    public IfStatement(Locatable loc, Expression condition, Statement consequence, Statement alternative) {
        this.loc = loc;
        this.condition = condition;
        this.consequence = consequence;
        this.alternative = alternative;
    }

    @Override
    public String toString() {
        // formate needed --> If[cond, cons, alt] or If[cond, cons] if no else block
        if (alternative != null) {
            return "If[" + condition.toString() + ", " + consequence.toString() + ", " + alternative.toString() + "]";
        } else {
            return "If[" + condition.toString() + ", " + consequence.toString() + "]";
        }
    }

}
