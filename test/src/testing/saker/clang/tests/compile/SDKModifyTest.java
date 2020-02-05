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
import saker.build.thirdparty.saker.util.function.Functionals;
import testing.saker.SakerTest;
import testing.saker.clang.tests.mock.MockingClangTestMetric;

@SakerTest
public class SDKModifyTest extends ClangTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.c.o");

	private String target;

	@Override
	protected MockingClangTestMetric createMetricImpl() {
		MockingClangTestMetric result = super.createMetricImpl();
		result.setDefaultTarget(target);
		return result;
	}

	@Override
	protected void runTestImpl() throws Throwable {
		target = MockingClangTestMetric.DEFAULT_TARGET;

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, target, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		environment.invalidateEnvironmentPropertiesWaitExecutions(Functionals.alwaysPredicate());
		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		target = MockingClangTestMetric.DIFF_TARGET;
		environment.invalidateEnvironmentPropertiesWaitExecutions(Functionals.alwaysPredicate());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, target, 123));
	}

}
