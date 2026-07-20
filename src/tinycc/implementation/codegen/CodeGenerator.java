package tinycc.implementation.codegen;

import java.util.List;
import tinycc.asmgen.*;
import tinycc.implementation.declaration.FunctionDefinition;
import tinycc.implementation.statement.Statement;
import tinycc.implementation.expression.Expression;

// traverses the validated ast to emit lowlevel riscv assembly instructions via asmgen

public class CodeGenerator {

    private AsmGen out;
    private TextLabel currentEpilogueLabel;
    private ActivationRecord record;

    public CodeGenerator(AsmGen out) {
        this.out = out;
    }

    // main entry point for code generation.

    public void generate(List<Object> programRoots) {
        for (Object root : programRoots) {
            if (root instanceof FunctionDefinition) {
                generateFunction((FunctionDefinition) root);
            }
        }
    }

    private void generateFunction(FunctionDefinition func) {
        String funcName = func.getNameText();

        this.record = new ActivationRecord();

        // the function label
        TextLabel funcLabel = out.makeTextLabel(funcName);
        out.emitLabel(funcLabel);

        // set up the stack frame (storing)
        // allocate 32 bytes on the stack
        out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, -32);
        // save ra and frame poointer s0
        out.emitInstruction(MemoryInstruction.SW, GPRegister.RA, null, 28, GPRegister.SP);
        out.emitInstruction(MemoryInstruction.SW, GPRegister.S0, null, 24, GPRegister.SP);
        // new frame pointer
        out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.S0, GPRegister.SP, 32);

        // create new epilogue label after func so u know where tojump
        currentEpilogueLabel = out.makeUniqueTextLabel("epilogue_" + funcName);

        // generate code for the func body
        generateStatement(func.getBody());

        // kill stack frame and return
        out.emitLabel(currentEpilogueLabel);

        // restore ra and fp
        out.emitInstruction(MemoryInstruction.LW, GPRegister.RA, null, 28, GPRegister.SP);
        out.emitInstruction(MemoryInstruction.LW, GPRegister.S0, null, 24, GPRegister.SP);
        // deallocate stack
        out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.SP, GPRegister.SP, 32);
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
            // Jump to the epilogue to clean up the stack and return
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
                // asssuming addi can handle it if it fits in 12 bits, otherwise LUI will be needed
                out.emitInstruction(ImmediateInstruction.ADDI, GPRegister.A0, GPRegister.ZERO, val);
            } else if (token.getKind() == tinycc.parser.TokenKind.IDENTIFIER) {
                // Get offset from symbol table memory layout
                Integer offset = record.getOffset(token.getText());

                if (offset != null) {
                    // Load the variable's value from memory into A0
                    out.emitInstruction(MemoryInstruction.LW, GPRegister.A0, null, offset, GPRegister.S0);
                }
            }
        } else if (expr instanceof tinycc.implementation.expression.BinaryExpression) {
            tinycc.implementation.expression.BinaryExpression binOp = (tinycc.implementation.expression.BinaryExpression) expr;
            tinycc.parser.Token operator = binOp.getOperator();

            // assignments evaluate right to left
            if (operator.getKind() == tinycc.parser.TokenKind.EQUAL) {
                generateExpression(binOp.getRight());

                // The left side MUST be an identifier
                tinycc.implementation.expression.PrimaryExpression leftPrimary = (tinycc.implementation.expression.PrimaryExpression) binOp
                        .getLeft();

                Integer offset = record.getOffset(leftPrimary.getToken().getText());

                // store A0 into the correct offset
                if (offset != null) {
                    out.emitInstruction(MemoryInstruction.SW, GPRegister.A0, null, offset, GPRegister.S0);
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
                out.emitInstruction(RegisterInstruction.ADD, GPRegister.A0, GPRegister.T0, GPRegister.A0);
            } else if (operator.getKind() == tinycc.parser.TokenKind.LESS) {
                out.emitInstruction(RegisterInstruction.SLT, GPRegister.A0, GPRegister.T0, GPRegister.A0);
            } else if (operator.getKind() == tinycc.parser.TokenKind.EQUAL_EQUAL) {
                // subbtract the two values if they were equal the result is 0
                out.emitInstruction(RegisterInstruction.SUB, GPRegister.A0, GPRegister.T0, GPRegister.A0);
                // we need to set A0 to 1 if a0 == 0
                out.emitInstruction(ImmediateInstruction.SLTIU, GPRegister.A0, GPRegister.A0, 1);
            }
        } else if (expr instanceof tinycc.implementation.expression.CallExpression) {
            tinycc.implementation.expression.CallExpression call = (tinycc.implementation.expression.CallExpression) expr;

            // evaluate arguments and push them to argument registers a0 to a7
            List<Expression> args = call.getArguments();
            for (int i = 0; i < args.size(); i++) {
                generateExpression(args.get(i));

                // map a0 to the correct argument register
                GPRegister targetArgReg = GPRegister.valueOf("A" + i);
                if (i != 0) { // no need to move if its already in a0
                    // move instruction is inherently aaddi target, a0, 0
                    out.emitInstruction(ImmediateInstruction.ADDI, targetArgReg, GPRegister.A0, 0);
                }
            }

            // extract function name and emit call
            tinycc.implementation.expression.PrimaryExpression callee = (tinycc.implementation.expression.PrimaryExpression) call
                    .getCallee();
            TextLabel funcTarget = out.makeTextLabel(callee.getToken().getText());
            out.emitInstruction(JumpInstruction.JAL, funcTarget);

            // return value naturally sits in A0 after the call returns which aligns perfectly with our stack

        }
    }
}