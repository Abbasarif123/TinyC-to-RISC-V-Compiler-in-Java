package prog2.tests;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility class used to run MIPS assembly code.
 */
public class VenusUtil {
	private static final int STEPS = 10_000_000;
	private final static String preload =
			/* data segment */
			"	.data\n" +

			/* scratch space */
			"_scratch:\n" +
			"	.space 64\n" +

			/* code segment */
			"	.text\n" +
			"	.globl __crt0:\n" +

			/* wrapper that calls main and the exit syscall */
			"__crt0:\n" +
			"	jal main\n" +
			"	or a1 a0 zero\n" +
			"	jal print_integer\n" +
			"	li a0 10\n" +
			"	ecall\n" +

			/* wrapper for print_integer */
			"print_integer:\n" +
			"	li a0 1\n" +
			"	ecall\n" +
			"	jr ra\n" +

			/* wrapper for print_string */
			"print_string:\n" +
			"	li a0 4\n" +
			"	ecall\n" +
			"	jr ra\n" +

			/* wrapper for print_char */
			"print_char:\n" +
			"	li a0 11\n" +
			"	ecall\n" +
			"	jr ra\n" +

			/* wrapper for sbrk */
			"malloc:\n" +
			"	li a0 9\n" +
			"	ecall\n" +
			"	jr ra\n" +

			"get_scratch:\n" +
			"	la a0 _scratch\n" +
			"	jr ra\n";

	private final String code;

	/**
	 * Constructs the simulator from a given string of MIPS assembly code.
	 */
	public VenusUtil(final String code) {
		this.code = code;
	}

	/**
	 * Runs the Mars simulator and returns the result returned by the main function.
	 * 
	 * @throws MarsException
	 */
	public synchronized int run() throws IOException, InterruptedException {
		String jarPath = "libs/venus.jar";
		String preloadPath = "src/prog2/tests/preload.s";
		String tmpPath = "src/prog2/tests/tmp_2547981358.s";
		// we have to replace main functions with another name to avoid venus jumping there directly
		final String mainScramble = "mainuidtanxgflhvcfirdn";
		final String mainReplaced = code.replace("main", mainScramble);
		final String prefix = "student_version/";
		
		try (FileWriter writer = new FileWriter(tmpPath)) {
			writer.write(mainReplaced);
		} catch (FileNotFoundException e) {
			jarPath = prefix + jarPath;
			preloadPath = prefix + preloadPath;
			tmpPath = prefix + tmpPath;
			try (FileWriter writer = new FileWriter(tmpPath)) {
				writer.write(mainReplaced);
			}
		} catch (IOException e) {
			throw e;
		}
		
		ProcessBuilder processBuilder;
		if (!"prog2".equals(System.getProperty("user.name"))) {
			processBuilder = new ProcessBuilder("java", "-jar", jarPath, preloadPath);
		} else {
			processBuilder = new ProcessBuilder("/usr/sbin/java", "-jar", jarPath, preloadPath);
		}
		//processBuilder.directory(new File(tmpPath));

		String cwd = System.getProperty("user.dir");
		System.out.println(cwd);

		Process process = processBuilder.start();
		String content = "";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
				if (CompilerTests.PRINT_ASM_CODE)
                	System.out.println("OUTPUT: " + line); // Print the output to the console
				content += line;
            }
        }
		
		int _exitCode = process.waitFor();
		return Integer.parseInt(content);
	}
}
