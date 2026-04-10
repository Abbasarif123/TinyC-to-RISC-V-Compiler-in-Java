package tinycc.asmgen;

/**
 * Represents a general-purpose register of the MIPS processor.
 *
 * It is used by the MipsAsmGenerator.
 *
 * @see MipsAsmGenerator
 */
public enum GPRegister {
	ZERO("zero"),
	RA("ra"),
	SP("sp"),
	GP("gp"),
	TP("tp"),
	T0("t0"),
	T1("t1"),
	T2("t2"),
	S0("s0"),
	S1("s1"),
	A0("a0"),
	A1("a1"),
	A2("a2"),
	A3("a3"),
	A4("a4"),
	A5("a5"),
	A6("a6"),
	A7("a7"),
	S2("s2"),
	S3("s3"),
	S4("s4"),
	S5("s5"),
	S6("s6"),
	S7("s7"),
	S8("s8"),
	S9("s9"),
	S10("s10"),
	S11("s11"),
	T3("t3"),
	T4("t4"),
	T5("t5"),
	T6("t6");

	private final String name;

	private GPRegister(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
