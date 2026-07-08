package tinycc.implementation.expression;

import tinycc.parser.Token;
import tinycc.parser.TokenKind;
import venus.fernet.TokenExpiredException;

public class PrimaryExpression extends Expression {
    private Token token;

    public PrimaryExpression(Token token) {
        this.token = token;
    }

    @Override
    public String toString() {
        // if its an identifier (variable), format it as Var_name
        if (token.getKind() == TokenKind.IDENTIFIER) {
            return "Var_" + token.getText();
        } else {
            // otherwise its a constant (number char or string)so format as Const_value
            return "Const_" + token.toString();

        }

    }

}
