/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package testing.saker.clang.tests.compile;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;

@SakerTest
public class SimpleForceIncludeTest extends ClangTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.cpp.o");

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath maincpppath = PATH_WORKING_DIRECTORY.resolve("main.cpp");
		SakerPath fihpath = PATH_WORKING_DIRECTORY.resolve("fi.h");

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 987, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(fihpath, "654".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, TARGET_DEFAULT, 654, 123));
	}

}
