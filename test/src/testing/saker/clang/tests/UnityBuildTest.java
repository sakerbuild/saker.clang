package testing.saker.clang.tests;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;

@SakerTest
public class UnityBuildTest extends ClangTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.cpp.o");

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath maincpppath = PATH_WORKING_DIRECTORY.resolve("main.cpp");
		SakerPath src1cpppath = PATH_WORKING_DIRECTORY.resolve("src/src1.cpp");
		SakerPath src2cpppath = PATH_WORKING_DIRECTORY.resolve("src/src2.cpp");
		SakerPath h1hpath = PATH_WORKING_DIRECTORY.resolve("inc/h1.h");
		SakerPath h2hpath = PATH_WORKING_DIRECTORY.resolve("inc/h2.h");

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(),
				compile(LANG_CPP, TARGET_DEFAULT, 1, 11, 2, 22, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(maincpppath, "456");
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(),
				compile(LANG_CPP, TARGET_DEFAULT, 1, 11, 2, 22, 456));

		files.putFile(h1hpath, "8");
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(),
				compile(LANG_CPP, TARGET_DEFAULT, 8, 11, 2, 22, 456));

		files.putFile(src2cpppath, files.getAllBytes(src2cpppath).toString().replace("22", "99"));
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(),
				compile(LANG_CPP, TARGET_DEFAULT, 8, 11, 2, 99, 456));
	}

}
