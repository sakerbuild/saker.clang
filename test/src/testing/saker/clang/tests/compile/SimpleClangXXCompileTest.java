package testing.saker.clang.tests.compile;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;

@SakerTest
public class SimpleClangXXCompileTest extends ClangTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.cpp.o");
	private static final SakerPath PATH_EXE = PATH_BUILD_DIRECTORY.resolve("saker.clang.link/default/default");

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 123));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(TARGET_DEFAULT, langCpp(123)));

		getMetric().getRunCommands().keySet().forEach(c -> assertEquals(c.get(0), "clang++"));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("main.cpp"), "456".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 456));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(TARGET_DEFAULT, langCpp(456)));
	}
}
