package prog2.tests.pub.codegen;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import prog2.tests.CodegenExercise;
import prog2.tests.CompilerTests;
import prog2.tests.MarsException;
import prog2.tests.PublicTest;
import prog2.tests.VenusUtil;

public class CodeGenTests extends CompilerTests implements PublicTest, CodegenExercise {

	@Test
	public void testSimple() throws IOException, InterruptedException {
		final String code = "\n"
				+ "int main() {\n"
				+ "	return 54;\n"
				+ "}\n";
		final VenusUtil mars = prepareCode(code);
		assertEquals(54, mars.run());
	}

	@Test
	public void testFactorial() throws IOException, InterruptedException {
		final String code = "\n"
				+ "int fact(int n) {\n"
				+ "	if (n == 0) {\n"
				+ "		return 1;\n"
				+ "	} else {\n"
				+ "		return n * fact(n - 1);\n"
				+ "	}\n"
				+ "}\n"
				+ "int main() {\n"
				+ "	return fact(7);\n"
				+ "}\n";
		final VenusUtil mars = prepareCode(code);
		assertEquals(5040, mars.run());
	}

	@Test
	public void testIf() throws MarsException, IOException, InterruptedException {
		final String code = "\n"
				+ "int main() {\n"
				+ "	int x = 1;\n"
				+ "	if (x) return 97;\n"
				+ "	else return 21;\n"
				+ "	 return 0;\n"
				+ "}\n";
		final VenusUtil mars = prepareCode(code);
		assertEquals(97, mars.run());
	}

	@Test
	public void testWhile() throws MarsException, IOException, InterruptedException {
		final String code = "\n"
				+ "int main() {\n"
				+ "	int x = 0;\n"
				+ "	while (x != 37) x = x + 1;\n"
				+ "	return x;\n"
				+ "}\n";
		final VenusUtil mars = prepareCode(code);
		assertEquals(37, mars.run());
	}

	@Test
	public void testCall() throws MarsException, IOException, InterruptedException {
		final String code = "\n"
				+ "int foo(int a, int b) {"
				+ "	return a + b;"
				+ "}"
				+ "int main() {\n"
				+ "	return foo(20, 4);"
				+ "}\n";
		final VenusUtil mars = prepareCode(code);
		assertEquals(24, mars.run());
	}

	@Test
	public void testPointerSimple() throws MarsException, IOException, InterruptedException {
		final String code = ""
				+ "void* get_scratch();\n"
				+ "int main() {\n"
				+ "	void *scratch = get_scratch();"
				+ "	int  *a       = scratch;\n"
				+ "	char *b       = scratch;\n"
				+ "	*(b + 4) = 100;\n"
				+ "	return *(a + 1);\n"
				+ "}\n";
		final VenusUtil mars = prepareCode(code);
		assertEquals(100, mars.run());
	}

	@Test
	public void testLocalVar() throws MarsException, IOException, InterruptedException {
		final String code = "\n"
				+ "int main() {\n"
				+ "	int x = 68;\n"
				+ "	return x;\n"
				+ "}\n";
		final VenusUtil mars = prepareCode(code);
		assertEquals(68, mars.run());
	}

	@Test
	public void testGlobalVar() throws MarsException, IOException, InterruptedException {
		final String code = "\n"
				+ "int x;\n"
				+ "int main() {\n"
				+ "	x = 93;\n"
				+ "	return x;\n"
				+ "}\n";
		final VenusUtil mars = prepareCode(code);
		assertEquals(93, mars.run());
	}
}
