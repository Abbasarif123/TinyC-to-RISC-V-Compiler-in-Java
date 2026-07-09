package tinycc.implementation.type;

import tinycc.implementation.type.Type;

public class PointerType extends Type {
    private Type pointsTo;

    public PointerType(Type pointsTo) {
        this.pointsTo = pointsTo;
    }

    @Override
    public String toString() {
        // we need Pointer[innerType]
        return "Pointer[" + pointsTo.toString() + "]";
    }

    // getters
    public Type getPointsTo() {
        return this.pointsTo;
    }
}
