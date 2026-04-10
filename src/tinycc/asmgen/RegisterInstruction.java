package tinycc.asmgen;

/**
 * Represents an instruction of the MIPS processor which works on registers.
 *
 * It is used by the MipsAsmGenerator.
 *
 * @see MipsAsmGenerator
 */
public enum RegisterInstruction {
	ADD("add"),
	AND("and"),
	DIV("div"),
	NOT("not"),
	MUL("mul"),
	OR("or"),
	REM("rem"),
	SUB("sub"),
	SLL("sll"),
	SLT("slt"),
	SLTU("sltu"),
	SRL("srl"),
	SRA("sra"),
	XOR("xor");

	private final String name;

	private RegisterInstruction(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
