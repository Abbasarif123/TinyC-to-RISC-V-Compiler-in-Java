package tinycc.implementation.expression;

import tinycc.parser.Token;

public class UnaryExpression extends Expression {
    private Token operator;
    private boolean postfix; // for post increment or decrement if i do bonus, keep this here for extendability
    private Expression operand;

    public UnaryExpression(Token operator, boolean postfix, Expression operand) {
        this.operator = operator;
        this.postfix = postfix;
        this.operand = operand;

    }

    @Override
    public String toString() {
        // format required --> unary_op[operand]
        // eg --> &x': Unary_&[Var_x]
        return "Unary_" + operator.getText() + "[" + operand.toString() + "]";
    }
}
