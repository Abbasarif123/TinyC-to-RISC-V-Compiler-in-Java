package tinycc.implementation.expression;

import java.util.List;
import tinycc.parser.Token;

//this deals with function calls, including the function being called and its arguments
public class CallExpression extends Expression {

    private Token token; // opening parenthesis token(use this for error location tracking)
    private Expression callee; // the function being called
    private List<Expression> arguments; // the list of arguments passed to func

    public CallExpression(Token token, Expression callee, List<Expression> arguments) {
        this.token = token;
        this.callee = callee;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        // formate --> Call[name, args...]
        // eg --> Call[Var_foo, Const_42, Var_x]

        StringBuilder sb = new StringBuilder("Call[");
        sb.append(callee.toString()); // add the func name

        // loop through args and append them eparated by commas
        for (Expression arg : arguments) {
            sb.append(", ").append(arg.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    // getters //extraction of the line number for error reporting
    public tinycc.parser.Token getToken() {
        return token;
    }

    public Expression getCallee() {
        return callee;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

}
