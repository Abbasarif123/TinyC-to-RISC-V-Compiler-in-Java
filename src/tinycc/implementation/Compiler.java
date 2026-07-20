package tinycc.implementation;

import tinycc.implementation.codegen.CodeGenerator;
import tinycc.diagnostic.Diagnostic;
import tinycc.parser.ASTFactory;
import tinycc.parser.Lexer;
import tinycc.parser.Parser;
import tinycc.logic.Formula;
import tinycc.asmgen.AsmGen;
import tinycc.implementation.codegen.CodeGenerator;
import tinycc.implementation.semantics.SemanticAnalyzer;
import tinycc.implementation.semantics.SymbolTable;
import tinycc.asmgen.AsmGen;

/**
 * The main compiler class.
 *
 * An instance of this class will handle a single translation unit (e.g. input
 * file). There will be multiple instances of your class during runtime of your
 * compiler. You can change this class but the given name and signature of
 * methods and the name of the class must not be modified.
 */
public class Compiler {

	Diagnostic diagnostic;
	private ASTFactory factory; // addition

	/**
	 * Initializes the compiler class with the given diagnostic module
	 *
	 * @param diagnostic The diagnostic module to use
	 * @see Diagnostic
	 */
	public Compiler(final Diagnostic diagnostic) {
		this.diagnostic = diagnostic;
		this.factory = new ASTFactoryImpl(); // initialized
	}

	/**
	 * Returns the current ASTFactory which is used internally.
	 *
	 * @return The current ASTFactory which is used internally.
	 * @see ASTFactory
	 */
	public ASTFactory getASTFactory() {
		// throw new UnsupportedOperationException("DONE: implement this");
		return this.factory; // returning the cached instance
	}

	/**
	 * Parses a single translation unit which is given by an instance of the Lexer
	 * class.
	 *
	 * @param lexer The lexer to use
	 * @see Lexer
	 * @remarks This function is invoked only once in each instance of the compiler
	 *          class.
	 */
	public void parseTranslationUnit(final Lexer lexer) {
		Parser parser = new Parser(diagnostic, lexer, this.getASTFactory());
		parser.parseTranslationUnit();
	}

	/**
	 * Checks the semantics of the input program.
	 *
	 * @see ASTFactory
	 * @remarks Use the diagnostics module to report errors. This function is
	 *          invoked only once in each instance of the compiler class.
	 */
	public void checkSemantics() {
		// throw new UnsupportedOperationException("Done: implement this");

		// get the AST
		ASTFactoryImpl myFactory = (ASTFactoryImpl) this.getASTFactory();
		java.util.List<Object> astRoots = myFactory.getProgramRoots();

		// bootup the analyzer and start traversubg the tree
		SemanticAnalyzer analyzer = new SemanticAnalyzer(this.diagnostic);
		analyzer.analyze(astRoots);
	}

	/**
	 * Performs optimizations on the input program.
	 *
	 * @remarks Bonus exercise.
	 */
	public void performOptimizations() {
		throw new UnsupportedOperationException("TODO: implement this");
	}

	/**
	 * Generates code for the input program.
	 *
	 * @param out The target output stream.
	 * @remarks This function is invoked only once in each instance of the compiler
	 *          class. Only necessary if mentioned in the project description.
	 */

	public void generateCode(final AsmGen out) {
		// throw new UnsupportedOperationException("DONE: implement this");
		// 1. Grab the validated AST ledger
		ASTFactoryImpl myFactory = (ASTFactoryImpl) this.getASTFactory();
		java.util.List<Object> astRoots = myFactory.getProgramRoots();

		// 2. Boot up the code generator and pass it the output stream
		CodeGenerator generator = new CodeGenerator(out);
		generator.generate(astRoots);
	}

	/**
	 * Generates verification conditions for the input program.
	 *
	 * @remarks This function is invoked only once in each instance of the compiler
	 *          class. Only necessary if mentioned in the project description.
	 */
	public Formula genVerificationConditions() {
		throw new UnsupportedOperationException("TODO: implement this");
	}
}
