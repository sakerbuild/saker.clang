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
package testing.saker.clang.tests.cluster.compile;

import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import testing.saker.SakerTest;
import testing.saker.build.tests.EnvironmentTestCaseConfiguration;
import testing.saker.clang.tests.compile.ClangTestCase;
import testing.saker.clang.tests.mock.MockingClangTestMetric;

@SakerTest
public class ClusterSDKModifyTest extends ClangTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.clang.compile/default/main.c.o");

	private String clusterDefaultTarget = TARGET_DEFAULT;
	private MockingClangTestMetric baseMetric;

	@Override
	protected Set<EnvironmentTestCaseConfiguration> getTestConfigurations() {
		return EnvironmentTestCaseConfiguration.builder(super.getTestConfigurations())
				.setClusterNames(ImmutableUtils.singletonSet(DEFAULT_CLUSTER_NAME)).build();
	}

	@Override
	public void executeRunning() throws Exception {
		baseMetric = createMetricImpl();
		testing.saker.build.flag.TestFlag.set(baseMetric);
		super.executeRunning();
	}

	@Override
	protected MockingClangTestMetric createMetricImpl() {
		MockingClangTestMetric result = new MockingClangTestMetric();
		result.setDefaultTarget(null);
		result.setClusterDefaultTarget(DEFAULT_CLUSTER_NAME, clusterDefaultTarget);
		return result;
	}

	@Override
	protected void runTestImpl() throws Throwable {
		clusterDefaultTarget = TARGET_DEFAULT;
		baseMetric.setClusterDefaultTarget(DEFAULT_CLUSTER_NAME, clusterDefaultTarget);

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, clusterDefaultTarget, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		clearClusterEnvironmentCachedProperties(DEFAULT_CLUSTER_NAME);
		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		clusterDefaultTarget = MockingClangTestMetric.DIFF_TARGET;
		baseMetric.setClusterDefaultTarget(DEFAULT_CLUSTER_NAME, clusterDefaultTarget);
		clearClusterEnvironmentCachedProperties(DEFAULT_CLUSTER_NAME);
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, clusterDefaultTarget, 123));
	}

}
