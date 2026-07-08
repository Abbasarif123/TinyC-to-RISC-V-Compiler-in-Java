package tinycc.implementation.expression;

import tinycc.parser.Token;

public class BinaryExpression extends Expression {
    private Token operator;
    private Expression left;
    private Expression right;

    public BinaryExpression(Token operator, Expression left, Expression right) {
        this.operator = operator;
        this.left = left;
        this.right = right;

    }

    @Override
    public String toString() {
        // we need format Binary_op[left, right]
        // such as Binary_+[Var_x, Const_2]
        return "Binary_" + operator.getText() + "[" + left.toString() + ", " + right.toString() + "]";
    }

}
