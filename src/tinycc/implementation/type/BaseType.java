package tinycc.implementation.type;

import tinycc.implementation.type.Type;
import tinycc.parser.TokenKind;

public class BaseType extends Type {
    private TokenKind kind;

    public BaseType(TokenKind kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        // gettext returns int char void
        // we expect type char, type int or type void
        return "Type_" + kind.getText();
    }
}
