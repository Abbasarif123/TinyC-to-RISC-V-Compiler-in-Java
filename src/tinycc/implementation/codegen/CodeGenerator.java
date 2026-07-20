package tinycc.implementation.codegen;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import tinycc.asmgen.*;
import tinycc.implementation.declaration.FunctionDefinition;
import tinycc.implementation.statement.Statement;
import tinycc.implementation.expression.Expression;

// traverses the validated ast to emit lowlevel riscv assembly instructions via asmgen

public class CodeGenerator {

    private AsmGen out;
    private TextLabel currentEpilogueLabel;
    private ActivationRecord record;
    private Set<String> intPointers = new HashSet<>(); // BUGFIX: Track int pointers for arithmetic
    private Map<String, TextLabel> functionLabels = new HashMap<>(); // BUGFIX: Cache function labels to prevent
                                                                     // duplicates

    public CodeGenerator(AsmGen out) {
        this.out = out;
    }

    // helper method to ensure we only create one label per function name
    private TextLabel getFunctionLabel(String name) {
        if (!functionLabels.containsKey(name)) {
            functionLabels.put(name, out.makeTextLabel(name));
        }
        return functionLabels.get(name);
    }

    // main entry point for code generation.

    public void generate(List<Object> programRoots) {
        // PASS 1: scan for global int pointers to handle pointer math scaling
        for (Object root : programRoots) {
            if (root instanceof tinycc.implementation.declaration.ExternalDeclaration) {
                tinycc.implementation.declaration.ExternalDeclaration decl = (tinycc.implementation.declaration.ExternalDeclaration) root;
                if (decl.getType().toString().equals("Pointer[Type_int]")) {
                    intPointers.add(decl.getNameText());
                }
            } else if (root instanceof FunctionDefinition) {
                FunctionDefinition func = (FunctionDefinition) root;
                if (func.getType() instanceof tinycc.implementation.type.FunctionType) {
                    tinycc.implementation.type.FunctionType fType = (tinycc.implementation.type.FunctionType) func
                            .getType();
                    List<tinycc.implementation.type.Type> pTypes = fType.getParameters();
                    List<tinycc.parser.Token> pNames = func.getParameterNames();
                    for (int i = 0; i < pNames.size(); i++) {
                        if (pTypes.get(i).toString().equals("Pointer[Type_int]")) {
                            intPointers.add(pNames.get(i).getText());
                        }
                    }
                }
            }
        }

        for (Object root : programRoots) {
            if (root instanceof FunctionDefinition) {
                generateFunction((FunctionDefinition) root);
            }
        }
    }

    // helper method to safely grab a registers without relying on string reflection
    private GPRegister getArgRegister(int index) {
        switch (index) {
            case 0:
                return GPRegister.A0;
            case 1:
                return GPRegister.A1;
            case 2:
                return GPRegister.A2;
            case 3:
                return GPRegister.A3;
            case 4:
                return GPRegister.A4;
            case 5:
                return GPRegister.A5;
            case 6:
                return GPRegister.A6;
            case 7:
                return GPRegister.A7;
            default:
                throw new RuntimeException("Too many arguments");
        }
    }

    private void generateFunction(FunctionDefinition func) {
        String funcName = func.getNameText();

        this.record = new ActivationRecord();

        // the function label (using the cache to prevent duplicates)
        TextLabel funcLabel = getFunctionLabel(funcName);
        out.emitLabel(funcLabel);

        // set up the stack frame (storing)
        // allocate 128 bytes on the stack for plenty of local variable space
        out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, -128);
        // save ra and frame poointer s0 at the very top of the new frame
        out.emitInstruction(MemoryInstruction.SW, GPRegister.RA, null, 124, GPRegister.SP);
        out.emitInstruction(MemoryInstruction.SW, GPRegister.S0, null, 120, GPRegister.SP);
        // new frame pointer points to the top of the frame
        out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.S0, GPRegister.SP, 128);

        // create new epilogue label after func so u know where tojump
        currentEpilogueLabel = out.makeUniqueTextLabel("epilogue_" + funcName);

        // save incoming parameters to the stack
        List<tinycc.parser.Token> paramNames = func.getParameterNames();
        for (int i = 0; i < paramNames.size(); i++) {
            // allocate a stack slot for the parameter
            int paramOffset = record.allocateLocal(paramNames.get(i).getText());
            // grab the corresponding argument register safely
            GPRegister argReg = getArgRegister(i);
            // store the registers value into the new memory slot
            out.emitInstruction(MemoryInstruction.SW, argReg, null, paramOffset, GPRegister.S0);
        }

        // generate code for the func body
        generateStatement(func.getBody());

        // kill stack frame and return
        out.emitLabel(currentEpilogueLabel);

        // restore ra and fp
        out.emitInstruction(MemoryInstruction.LW, GPRegister.RA, null, 124, GPRegister.SP);
        out.emitInstruction(MemoryInstruction.LW, GPRegister.S0, null, 120, GPRegister.SP);
        // deallocate stack
        out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, 128);
        // returnto caller
        out.emitInstruction(JumpRegisterInstruction.JR, GPRegister.RA);

        currentEpilogueLabel = null;
    }

    private void generateStatement(Statement stmt) {
        if (stmt == null)
            return;

        if (stmt instanceof tinycc.implementation.statement.BlockStatement) {
            tinycc.implementation.statement.BlockStatement block = (tinycc.implementation.statement.BlockStatement) stmt;

            record.enterScope();

            for (Statement innerStmt : block.getStatements()) {
                generateStatement(innerStmt);
            }

            record.leaveScope();

        } else if (stmt instanceof tinycc.implementation.statement.ReturnStatement) {
            tinycc.implementation.statement.ReturnStatement ret = (tinycc.implementation.statement.ReturnStatement) stmt;
            if (ret.getExpression() != null) {
                // evaluates expresion and returns the result in a0
                generateExpression(ret.getExpression());
            }
            // jump to the epilogue to clean up the stack and return
            out.emitInstruction(JumpInstruction.J, currentEpilogueLabel);
        } else if (stmt instanceof tinycc.implementation.statement.ExpressionStatement) {
            tinycc.implementation.statement.ExpressionStatement exprStmt = (tinycc.implementation.statement.ExpressionStatement) stmt;
            generateExpression(exprStmt.getExpression());
        } else if (stmt instanceof tinycc.implementation.statement.IfStatement) {
            tinycc.implementation.statement.IfStatement ifStmt = (tinycc.implementation.statement.IfStatement) stmt;

            TextLabel elseLabel = out.makeUniqueTextLabel("else");
            TextLabel endLabel = out.makeUniqueTextLabel("endIf");

            // evaluate condition and result in A0
            generateExpression(ifStmt.getCondition());

            // if condition is false (A0 == 0), jump to else (or end if no else block exists)
            out.emitInstruction(BranchInstruction.BEQ, GPRegister.A0, GPRegister.ZERO, elseLabel);

            // generate consequence block
            generateStatement(ifStmt.getConsequence());
            out.emitInstruction(JumpInstruction.J, endLabel);

            // generate alternative else block
            out.emitLabel(elseLabel);
            if (ifStmt.getAlternative() != null) {
                generateStatement(ifStmt.getAlternative());
            }

            out.emitLabel(endLabel);
        } else if (stmt instanceof tinycc.implementation.statement.WhileStatement) {
            tinycc.implementation.statement.WhileStatement whileStmt = (tinycc.implementation.statement.WhileStatement) stmt;

            TextLabel loopStart = out.makeUniqueTextLabel("whileStart");
            TextLabel loopEnd = out.makeUniqueTextLabel("whileEnd");

            out.emitLabel(loopStart);

            // evaluate condition
            generateExpression(whileStmt.getCondition());

            // exit loop if condition is false
            out.emitInstruction(BranchInstruction.BEQ, GPRegister.A0, GPRegister.ZERO, loopEnd);

            // loop body
            generateStatement(whileStmt.getBody());

            // jump back to start
            out.emitInstruction(JumpInstruction.J, loopStart);

            out.emitLabel(loopEnd);
        } else if (stmt instanceof tinycc.implementation.statement.DeclarationStatement) {
            tinycc.implementation.statement.DeclarationStatement decl = (tinycc.implementation.statement.DeclarationStatement) stmt;

            // BUGFIX: keep track of local int pointers for scaling
            if (decl.getType().toString().equals("Pointer[Type_int]")) {
                intPointers.add(decl.getName().getText());
            }

            // allocate space on the stack for this new variable
            int offset = record.allocateLocal(decl.getName().getText());

            if (decl.getInit() != null) {
                // evaluate initialization expression
                generateExpression(decl.getInit());

                // store A0 into the variable's specific memory slot
                out.emitInstruction(MemoryInstruction.SW, GPRegister.A0, null, offset, GPRegister.S0);
            }
        }
    }

    private void generateExpression(Expression expr) {
        if (expr == null)
            return;

        if (expr instanceof tinycc.implementation.expression.PrimaryExpression) {
            tinycc.implementation.expression.PrimaryExpression primary = (tinycc.implementation.expression.PrimaryExpression) expr;
            tinycc.parser.Token token = primary.getToken();

            if (token.getKind() == tinycc.parser.TokenKind.NUMBER) {
                int val = Integer.parseInt(token.getText());
                // load immediate value into return register A0
                out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.A0, GPRegister.ZERO, val);
            } else if (token.getKind() == tinycc.parser.TokenKind.IDENTIFIER) {
                // get offset from symbol table memory layout
                Integer offset = record.getOffset(token.getText());

                if (offset != null) {
                    // load the variable's value from memory into A0
                    out.emitInstruction(MemoryInstruction.LW, GPRegister.A0, null, offset, GPRegister.S0);
                }
            }
        } else if (expr instanceof tinycc.implementation.expression.BinaryExpression) {
            tinycc.implementation.expression.BinaryExpression binOp = (tinycc.implementation.expression.BinaryExpression) expr;
            tinycc.parser.Token operator = binOp.getOperator();

            // assignments evaluate right to left
            if (operator.getKind() == tinycc.parser.TokenKind.EQUAL) {
                generateExpression(binOp.getRight()); // right side value is now in a0

                if (binOp.getLeft() instanceof tinycc.implementation.expression.PrimaryExpression) {
                    // standard variable assignment
                    tinycc.implementation.expression.PrimaryExpression leftPrimary = (tinycc.implementation.expression.PrimaryExpression) binOp
                            .getLeft();
                    Integer offset = record.getOffset(leftPrimary.getToken().getText());
                    if (offset != null) {
                        out.emitInstruction(MemoryInstruction.SW, GPRegister.A0, null, offset, GPRegister.S0);
                    }
                } else if (binOp.getLeft() instanceof tinycc.implementation.expression.UnaryExpression) {
                    // pointer assignment
                    tinycc.implementation.expression.UnaryExpression leftUnary = (tinycc.implementation.expression.UnaryExpression) binOp
                            .getLeft();
                    if (leftUnary.getOperator().getText().equals("*")) {
                        // push right side to the stack temporarily
                        out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, -4);
                        out.emitInstruction(MemoryInstruction.SW, GPRegister.A0, null, 0, GPRegister.SP);

                        // evaluate the pointer address goes into a0
                        generateExpression(leftUnary.getOperand());

                        // pop the right side value back into t0
                        out.emitInstruction(MemoryInstruction.LW, GPRegister.T0, null, 0, GPRegister.SP);
                        out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, 4);

                        // store the value into the address
                        out.emitInstruction(MemoryInstruction.SW, GPRegister.T0, null, 0, GPRegister.A0);
                    }
                }
                return;
            }

            // generate left side
            generateExpression(binOp.getLeft());

            // push left side to stack
            out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, -4);
            out.emitInstruction(MemoryInstruction.SW, GPRegister.A0, null, 0, GPRegister.SP);

            // generate right side
            generateExpression(binOp.getRight());

            // pop left side into a temporary register
            out.emitInstruction(MemoryInstruction.LW, GPRegister.T0, null, 0, GPRegister.SP);
            out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, 4);

            // perform operation t0 has left a0 has right
            if (operator.getKind() == tinycc.parser.TokenKind.PLUS) {
                // BUGFIX: Check if left side is an int* so we can scale the integer by 4
                boolean scalePointer = false;
                if (binOp.getLeft() instanceof tinycc.implementation.expression.PrimaryExpression) {
                    String leftName = ((tinycc.implementation.expression.PrimaryExpression) binOp.getLeft()).getToken()
                            .getText();
                    if (intPointers.contains(leftName))
                        scalePointer = true;
                }
                if (scalePointer) {
                    // Shift Left Logical by 2 multiplies A0 by exactly 4
                    out.emitInstruction(ImmediateInstruction.SLLI, GPRegister.A0, GPRegister.A0, 2);
                }
                out.emitInstruction(RegisterInstruction.ADD, GPRegister.A0, GPRegister.T0, GPRegister.A0);
            } else if (operator.getKind() == tinycc.parser.TokenKind.MINUS) {
                boolean scalePointer = false;
                if (binOp.getLeft() instanceof tinycc.implementation.expression.PrimaryExpression) {
                    String leftName = ((tinycc.implementation.expression.PrimaryExpression) binOp.getLeft()).getToken()
                            .getText();
                    if (intPointers.contains(leftName))
                        scalePointer = true;
                }
                if (scalePointer) {
                    out.emitInstruction(ImmediateInstruction.SLLI, GPRegister.A0, GPRegister.A0, 2);
                }
                out.emitInstruction(RegisterInstruction.SUB, GPRegister.A0, GPRegister.T0, GPRegister.A0);
            } else if (operator.getKind() == tinycc.parser.TokenKind.ASTERISK) {
                out.emitInstruction(RegisterInstruction.MUL, GPRegister.A0, GPRegister.T0, GPRegister.A0);
            } else if (operator.getKind() == tinycc.parser.TokenKind.LESS) {
                out.emitInstruction(RegisterInstruction.SLT, GPRegister.A0, GPRegister.T0, GPRegister.A0);
            } else if (operator.getKind() == tinycc.parser.TokenKind.EQUAL_EQUAL) {
                // subtract the two values if they were equal the result is 0
                out.emitInstruction(RegisterInstruction.SUB, GPRegister.A0, GPRegister.T0, GPRegister.A0);
                // we need to set A0 to 1 if a0 == 0
                out.emitInstruction(ImmediateInstruction.SLTIU, GPRegister.A0, GPRegister.A0, 1);
            } else if (operator.getText().equals("!=")) {
                // subtract the two values. if they are not equal, the result is non-zero
                out.emitInstruction(RegisterInstruction.SUB, GPRegister.A0, GPRegister.T0, GPRegister.A0);
                // sltu a0, zero, a0 sets a0 to 1 if a0 > 0 (not equal)
                out.emitInstruction(RegisterInstruction.SLTU, GPRegister.A0, GPRegister.ZERO, GPRegister.A0);
            }
        } else if (expr instanceof tinycc.implementation.expression.CallExpression) {
            tinycc.implementation.expression.CallExpression call = (tinycc.implementation.expression.CallExpression) expr;

            List<Expression> args = call.getArguments();

            // evaluate all arguments and push them to the stack to prevent clobbering a0
            for (int i = 0; i < args.size(); i++) {
                generateExpression(args.get(i));
                out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, -4);
                out.emitInstruction(MemoryInstruction.SW, GPRegister.A0, null, 0, GPRegister.SP);
            }

            // pop them into their respective argument registers in reverse order
            for (int i = args.size() - 1; i >= 0; i--) {
                GPRegister targetArgReg = getArgRegister(i);
                out.emitInstruction(MemoryInstruction.LW, targetArgReg, null, 0, GPRegister.SP);
                out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, 4);
            }

            // extract function name and emit call using the label cache
            tinycc.implementation.expression.PrimaryExpression callee = (tinycc.implementation.expression.PrimaryExpression) call
                    .getCallee();
            String targetName = callee.getToken().getText();
            TextLabel funcTarget = getFunctionLabel(targetName);
            out.emitInstruction(JumpInstruction.JAL, funcTarget);

            // return value naturally sits in A0 after the call returns which aligns perfectly with our stack

        } else if (expr instanceof tinycc.implementation.expression.UnaryExpression) {
            tinycc.implementation.expression.UnaryExpression unary = (tinycc.implementation.expression.UnaryExpression) expr;
            tinycc.parser.Token op = unary.getOperator();

            if (op.getText().equals("&")) {
                // get the offset of the variable
                tinycc.implementation.expression.PrimaryExpression primary = (tinycc.implementation.expression.PrimaryExpression) unary
                        .getOperand();
                Integer offset = record.getOffset(primary.getToken().getText());

                // calculate the exact memory address and store it in a0
                if (offset != null) {
                    out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.A0, GPRegister.S0, offset);
                }
            } else if (op.getText().equals("*")) {
                // evaluate the pointer expression the address will end up in a0
                generateExpression(unary.getOperand());
                // load the actual value from that address
                out.emitInstruction(MemoryInstruction.LW, GPRegister.A0, null, 0, GPRegister.A0);
            } else if (op.getText().equals("-")) {
                // evaluate the operand (result in a0)
                generateExpression(unary.getOperand());
                // negate a0 by subtracting it from zero (0 - a0)
                out.emitInstruction(RegisterInstruction.SUB, GPRegister.A0, GPRegister.ZERO, GPRegister.A0);
            } else if (op.getText().equals("!")) {
                // evaluate the operand (result in a0)
                generateExpression(unary.getOperand());
                // if a0 == 0, set a0 to 1. if a0 != 0, set a0 to 0 (set less than immediate unsigned)
                out.emitInstruction(ImmediateInstruction.SLTIU, GPRegister.A0, GPRegister.A0, 1);
            }
        }
    }
}