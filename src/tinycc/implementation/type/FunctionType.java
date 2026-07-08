package tinycc.implementation.type;

import java.lang.reflect.Parameter;
import java.util.List;
import tinycc.implementation.type.Type;

public class FunctionType extends Type {
    private Type returnType;
    private List<Type> parameters;

    public FunctionType(Type returnType, List<Type> parameters) {
        this.returnType = returnType;
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        // we need FunctionType[returnType, param1, param2]

        StringBuilder sb = new StringBuilder("FunctionType[");
        sb.append(returnType.toString());

        for (Type param : parameters) {
            sb.append(",").append(param.toString());
        }

        sb.append("]");
        return sb.toString();

    }

    // getters
    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getParameters() {
        return parameters;
    }
}
