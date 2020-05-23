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
public class RecompileWithFailureTest extends ClangTestCase {
	private static final SakerPath PATH_BASEOBJ = PATH_BUILD_DIRECTORY.resolve("saker.clang.compile/default/");

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath pf1 = PATH_WORKING_DIRECTORY.resolve("f1.cpp");
		SakerPath pf2 = PATH_WORKING_DIRECTORY.resolve("f2.cpp");
		SakerPath pfailer = PATH_WORKING_DIRECTORY.resolve("failer.cpp");

		SakerPath f1obj = PATH_BASEOBJ.resolve("f1.cpp.o");
		SakerPath f2obj = PATH_BASEOBJ.resolve("f2.cpp.o");
		SakerPath failerobh = PATH_BASEOBJ.resolve("failer.cpp.o");

		files.putFile(pf1, "123");

		runScriptTask("build");
		assertEquals(files.getAllBytes(f1obj).toString(), compile(LANG_CPP, TARGET_DEFAULT, 123));
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pf1));

		files.putFile(pfailer, "#error");
		assertTaskException(Exception.class, () -> runScriptTask("build"));
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pfailer));
		assertEquals(files.getAllBytes(f1obj).toString(), compile(LANG_CPP, TARGET_DEFAULT, 123));

		//nothing is compiled because we had an error 
		files.putFile(pf2, "456");
		assertTaskException(Exception.class, () -> runScriptTask("build"));
		assertEmpty(getMetric().getCompiledFileClusterNames());

		files.putFile(pfailer, "987");
		runScriptTask("build");
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pfailer, pf2));
		assertEquals(files.getAllBytes(f1obj).toString(), compile(LANG_CPP, TARGET_DEFAULT, 123));
		assertEquals(files.getAllBytes(f2obj).toString(), compile(LANG_CPP, TARGET_DEFAULT, 456));
		assertEquals(files.getAllBytes(failerobh).toString(), compile(LANG_CPP, TARGET_DEFAULT, 987));

		files.putFile(pfailer, "#error");
		assertTaskException(Exception.class, () -> runScriptTask("build"));
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pfailer));

		files.putFile(pf2, "111");
		assertTaskException(Exception.class, () -> runScriptTask("build"));
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pf2));

		files.putFile(pf2, "#error");
		assertTaskException(Exception.class, () -> runScriptTask("build"));
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pf2));

		files.putFile(pf2, "#error again");
		assertTaskException(Exception.class, () -> runScriptTask("build"));
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pf2));

		files.delete(f1obj);
		assertTaskException(Exception.class, () -> runScriptTask("build"));
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pf1));

		files.putFile(pf2, "222");
		files.putFile(pfailer, "333");
		runScriptTask("build");
		assertEquals(getMetric().getCompiledFileClusterNames().keySet(), setOf(pf2, pfailer));
		assertEquals(files.getAllBytes(f1obj).toString(), compile(LANG_CPP, TARGET_DEFAULT, 123));
		assertEquals(files.getAllBytes(f2obj).toString(), compile(LANG_CPP, TARGET_DEFAULT, 222));
		assertEquals(files.getAllBytes(failerobh).toString(), compile(LANG_CPP, TARGET_DEFAULT, 333));
	}

}
