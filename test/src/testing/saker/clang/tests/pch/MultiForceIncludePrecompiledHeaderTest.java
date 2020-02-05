package testing.saker.clang.tests.pch;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.clang.tests.ClangTestCase;

@SakerTest
public class MultiForceIncludePrecompiledHeaderTest extends ClangTestCase {

	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.cpp.o");

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath maincpppath = PATH_WORKING_DIRECTORY.resolve("main.cpp");
		SakerPath secondhpath = PATH_WORKING_DIRECTORY.resolve("second.h");
		SakerPath pchpath = PATH_WORKING_DIRECTORY.resolve("pch.h");

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 1, 2, 222, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(pchpath, "333");
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 1, 2, 333, 123));
		assertHeaderPrecompilationRunOnlyOnce();

		files.putFile(maincpppath, files.getAllBytes(maincpppath).toString().replace("123", "456"));
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 1, 2, 333, 456));
		assertHeaderPrecompilationWasntRun();

		files.putFile(secondhpath, "22");
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 1, 22, 333, 456));
		assertHeaderPrecompilationRunOnlyOnce();

	}

}
