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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import testing.saker.SakerTest;

@SakerTest
public class FailFastCompileTest extends ClangTestCase {
	private static final SakerPath PATH_BASEOBJ = PATH_BUILD_DIRECTORY.resolve("saker.clang.compile/default/");

	@Override
	protected void runTestImpl() throws Throwable {
		Set<SakerPath> validfilepaths = new TreeSet<>();
		int count = 20;
		for (int i = 0; i < count; i++) {
			SakerPath fp = PATH_WORKING_DIRECTORY.resolve("f" + i + ".cpp");
			files.putFile(fp, "#error");
		}

		int c = 0;
		while (true) {
			++c;
			System.out.println("Run " + c);
			int s = validfilepaths.size();
			if (s == count) {
				//should succeed
				runScriptTask("build");
				break;
			}
			assertTaskException(Exception.class, () -> runScriptTask("build"));
			Map<SakerPath, String> compiledfiles = ImmutableUtils
					.makeImmutableNavigableMap(getMetric().getCompiledFileClusterNames());
			assertNotEmpty(compiledfiles);

			//should not rerun once more as nothing changed
			assertTaskException(Exception.class, () -> runScriptTask("build"));
			assertEmpty(getMetric().getRunTaskIdFactories());

			for (SakerPath fpath : compiledfiles.keySet()) {
				files.putFile(fpath, "123");
				validfilepaths.add(fpath);
			}
		}
		assertTrue(c > 2);

		for (SakerPath fp : validfilepaths) {
			assertEquals(files.getAllBytes(PATH_BASEOBJ.resolve(fp.getFileName() + ".o")).toString(),
					compile(LANG_CPP, TARGET_DEFAULT, 123));
		}

	}

}
