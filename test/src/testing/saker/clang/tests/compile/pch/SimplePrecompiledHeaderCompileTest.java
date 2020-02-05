package testing.saker.clang.tests.compile.pch;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.clang.tests.compile.ClangTestCase;

@SakerTest
public class SimplePrecompiledHeaderCompileTest extends ClangTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.cpp.o");

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath maincpppath = PATH_WORKING_DIRECTORY.resolve("main.cpp");

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 222, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("pch.h"), "333".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 123));

		files.putFile(maincpppath, files.getAllBytes(maincpppath).toString().replace("123", "456"));
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 456));
	}

}
