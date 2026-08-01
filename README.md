# TinyC-to-RISC-V Compiler in Java

Developed as part of the Programming 2 course at Saarland University, this project is a multi-stage compiler written in Java for TinyC—a restricted variant of C supporting primitive types (char, int, void), pointers, control flow, global variables, and multi-argument functions. The compiler processes source code through syntactic analysis, static type/scope enforcement, and target machine code generation.  

## Key Features:
    AST Construction: Implements the ASTFactory interface to build a structured Abstract Syntax Tree (AST) from lexed/parsed TinyC tokens and outputs standardized abstract syntax string representations.  

    Static Semantic Analysis: Performs comprehensive compile-time verification across TinyC's type hierarchy (scalar, object, and function types), L-value checks, implicit conversions, and strict scope rules (handling global vs. nested local scopes and variable shadowing).  

    Diagnostics System: Emits precise source-location error reports upon encountering semantic rule violations, such as illegal type operations, missing symbols, or invalid scope redeclarations.  

    RISC-V Machine Code Generation: Emits standard 32-bit RISC-V assembly code targeting standard runtime environments (like Venus). Features include stack-frame management for local variables, parameter passing via system registers ($a0–$a7), temporary register allocation ($t0–$t6), pointer arithmetic, and strict calling convention compliance.
