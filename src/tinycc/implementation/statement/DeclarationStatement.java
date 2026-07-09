package tinycc.implementation.statement;

import tinycc.implementation.expression.Expression;
import tinycc.implementation.type.Type;
import tinycc.parser.Token;

//deals with a local variable declaration, optionally with an intialiazation
public class DeclarationStatement extends Statement {
    private Type type;
    private Token name;
    private Expression init; // can be null

    public DeclarationStatement(Type type, Token name, Expression init) {
        this.type = type;
        this.name = name;
        this.init = init;
    }

    @Override
    public String toString() {
        // format --> Declaration_x[Type_int, Const_0] or Declaration_y[Type_char]
        StringBuilder sb = new StringBuilder("Declaration_");
        sb.append(name.getText()).append("[").append(type.toString());

        if (init != null) {
            sb.append(", ").append(init.toString());
        }

        sb.append("]");
        return sb.toString();

    }

    // getters
    public Type getType() {
        return type;
    }

    public tinycc.parser.Token getName() {
        return name;
    }

    public Expression getInit() {
        return init;
    }
}
