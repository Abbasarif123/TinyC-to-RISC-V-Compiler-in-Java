package tinycc.implementation.expression;

import tinycc.parser.Token;

//represents a conditional expression: condition ? consequence : alternative.

public class ConditionalExpression extends Expression {
    private Token token; // "?"
    private Expression condition;
    private Expression consequence;
    private Expression alternative;

    public ConditionalExpression(Token token, Expression condition, Expression consequence, Expression alternative) {
        this.token = token;
        this.condition = condition;
        this.consequence = consequence;
        this.alternative = alternative;
    }

    @Override
    public String toString() {
        // format eg: Conditional[Binary_>[Var_x, Const_0], Var_a, Var_b]
        return "Conditional[" + condition.toString() + ", " + consequence.toString() + ", " + alternative.toString()
                + "]";
    }

}
