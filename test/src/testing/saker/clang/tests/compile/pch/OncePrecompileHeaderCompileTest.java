package testing.saker.clang.tests.compile.pch;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.clang.tests.compile.ClangTestCase;

@SakerTest
public class OncePrecompileHeaderCompileTest extends ClangTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.cpp.o");
	private static final SakerPath PATH_MAINCPP2_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main2.cpp.o");
	private static final SakerPath PATH_MAINCPP3_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main3.cpp.o");
	private static final SakerPath PATH_MAINCPP4_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main4.cpp.o");

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath maincpppath = PATH_WORKING_DIRECTORY.resolve("main.cpp");

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 222, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP2_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 222, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP3_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 222, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP4_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 222, 123));
		assertHeaderPrecompilationRunOnlyOnce();

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("pch.h"), "333".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP2_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP3_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP4_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 123));
		assertHeaderPrecompilationRunOnlyOnce();

		files.putFile(maincpppath, files.getAllBytes(maincpppath).toString().replace("123", "456"));
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 456));
		assertEquals(files.getAllBytes(PATH_MAINCPP2_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP3_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP4_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333, 123));
	}

}
