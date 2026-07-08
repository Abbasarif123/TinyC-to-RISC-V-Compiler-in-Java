package tinycc.implementation.declaration;

import java.util.List;
import tinycc.implementation.statement.Statement;
import tinycc.implementation.type.Type;
import tinycc.parser.Token;

//deals with a complete function definition, inculding its body
public class FunctionDefinition {
    private Type type; // this will be a function type
    private Token name;
    private List<Token> parameterNames;
    private Statement body; // this is a block statement

    public FunctionDefinition(Type type, Token name, List<Token> parameterNames, Statement body) {
        this.type = type;
        this.name = name;
        this.parameterNames = parameterNames;
        this.body = body;
    }

    // getters
    public Type getType() {
        return type;
    }

    public Token getName() {
        return name;
    }

    public String getNameText() {
        return name.getText();
    }

    public List<Token> getParameterNames() {
        return parameterNames;
    }

    public Statement getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "FunctionDefinition[" + name.getText() + "]";
    }
}
