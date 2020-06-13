package testing.saker.clang.tests.compile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;

@SakerTest
public class SimpleCompileTest extends ClangTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.c.o");
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.cpp.o");
	private static final SakerPath PATH_EXE = PATH_BUILD_DIRECTORY.resolve("saker.clang.link/default/default");

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, TARGET_DEFAULT, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 123));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(TARGET_DEFAULT, langC(123), langCpp(123)));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("main.c"), "456".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, TARGET_DEFAULT, 456));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(TARGET_DEFAULT, langC(456), langCpp(123)));
		if (isPosixFilePermissionsSupported()) {
			assertEquals(files.getPosixFilePermissions(PATH_EXE), EnumSet.allOf(PosixFilePermission.class));
		}

		files.putFile(PATH_WORKING_DIRECTORY.resolve("main.cpp"), "456".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 456));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(TARGET_DEFAULT, langC(456), langCpp(456)));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("add.c"), "1".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_EXE).toString(),
				linkExe(TARGET_DEFAULT, langC(1), langC(456), langCpp(456)));
	}

	private boolean isPosixFilePermissionsSupported() throws IOException {
		try {
			Files.getPosixFilePermissions(getWorkingDirectory().resolve("saker.build"));
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}
}
