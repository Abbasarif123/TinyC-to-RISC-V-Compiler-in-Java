package tinycc.implementation.declaration;

import tinycc.implementation.type.Type;
import tinycc.parser.Token;

//dealing with global variable or a function prototype

public class ExternalDeclaration {
    private Type type;
    private Token name;

    public ExternalDeclaration(Type type, Token name) {
        this.type = type;
        this.name = name;
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

    @Override
    public String toString() {
        return "ExternalDeclaration[" + type.toString() + ", " + name.getText() + "]";
    }
}
