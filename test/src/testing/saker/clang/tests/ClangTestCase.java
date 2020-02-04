package testing.saker.clang.tests;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import saker.build.thirdparty.saker.util.StringUtils;
import testing.saker.clang.tests.mock.ClangMockProcess;
import testing.saker.clang.tests.mock.MockingClangTestMetric;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;

public abstract class ClangTestCase extends RepositoryLoadingVariablesMetricEnvironmentTestCase {
	protected static final String LINE_SEPARATOR = System.lineSeparator();
	protected static final int MULTI_C_X64 = MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_C * 64;
	protected static final int MULTI_C_X86 = MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_C * 86;
	protected static final int MULTI_CPP_X64 = MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP * 64;
	protected static final int MULTI_CPP_X86 = MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP * 86;

	protected static final String LANG_C = "c";
	protected static final String LANG_CPP = "c++";

	protected static final String TARGET_DEFAULT = MockingClangTestMetric.DEFAULT_TARGET;
	protected static final String DEFAULT_VERSION = MockingClangTestMetric.DEFAULT_VERSION;

	public static final String DEFAULT_CLUSTER_NAME = "cluster";

	@Override
	protected MockingClangTestMetric createMetricImpl() {
		return new MockingClangTestMetric();
	}

	protected Path getTestSDKDirectory() {
		Path basedir = getTestingBaseWorkingDirectory();
		if (basedir == null) {
			return null;
		}
		return basedir.resolve("testsdk");
	}

	@Override
	protected MockingClangTestMetric getMetric() {
		return (MockingClangTestMetric) super.getMetric();
	}

	public static String src(String... lines) {
		return StringUtils.toStringJoin(LINE_SEPARATOR, lines);
	}

	public static int langC(int val) {
		return val * MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_C;
	}

	public static int langCpp(int val) {
		return val * MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP;
	}

	public static String linkExe(String arch, int... lines) {
		return linkTypeImpl(DEFAULT_VERSION, MockingClangTestMetric.TYPE_EXE, arch, lines);
	}

	public static String linkDll(String arch, int... lines) {
		return linkTypeImpl(DEFAULT_VERSION, MockingClangTestMetric.TYPE_DLL, arch, lines);
	}

	public static String linkExeVer(String version, String arch, int... lines) {
		return linkTypeImpl(version, MockingClangTestMetric.TYPE_EXE, arch, lines);
	}

	public static String linkDllVer(String version, String arch, int... lines) {
		return linkTypeImpl(version, MockingClangTestMetric.TYPE_DLL, arch, lines);
	}

	private static String linkTypeImpl(String version, String type, String arch, int... lines) {
		int mult = ClangMockProcess.getTargetMultiplier(arch);
		List<String> vals = new ArrayList<>();
		vals.add(MockingClangTestMetric.createFileTypeLine(type, arch));
		vals.add("#version " + version);
		for (int l : lines) {
			vals.add(l * mult + "");
		}

		return StringUtils.toStringJoin(null, LINE_SEPARATOR, vals, LINE_SEPARATOR);
	}

	public static String compile(String lang, String arch, int... lines) {
		return compileVer(DEFAULT_VERSION, lang, arch, lines);
	}

	public static String compileVer(String version, String lang, String arch, int... lines) {
		int mult = ClangMockProcess.getTargetMultiplier(arch) * ClangMockProcess.getLanguageMockMultipler(lang);
		List<String> res = new ArrayList<>();
		res.add(MockingClangTestMetric.createFileTypeLine(MockingClangTestMetric.TYPE_OBJ, arch));
		res.add("#version " + version);
		for (int l : lines) {
			res.add(l * mult + "");
		}
		return StringUtils.toStringJoin(null, LINE_SEPARATOR, res, LINE_SEPARATOR);
	}

	public static String binaryX64Exe1_0(BinaryLine... lines) {
		return binaryImpl(MockingClangTestMetric.TYPE_EXE, "x64", "1.0", lines);
	}

	public static String binaryX86Exe1_0(BinaryLine... lines) {
		return binaryImpl(MockingClangTestMetric.TYPE_EXE, "x86", "1.0", lines);
	}

	private static String binaryImpl(String type, String arch, String version, BinaryLine... lines) {
		List<String> vals = new ArrayList<>();

		vals.add(MockingClangTestMetric.createFileTypeLine(type, arch));
		vals.add("#version " + version);
		for (BinaryLine l : lines) {
			l.process(vals, arch, version);
		}

		return StringUtils.toStringJoin(null, LINE_SEPARATOR, vals, LINE_SEPARATOR);
	}

	public static BinaryLine c(int... vals) {
		return new BinaryLine() {
			@Override
			public void process(List<String> output, String architecture, String version) {
				for (int val : vals) {
					output.add(MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_C * val
							* ClangMockProcess.getTargetMultiplier(architecture) + "");
				}
			}
		};
	}

	public static BinaryLine cpp(int... vals) {
		return new BinaryLine() {
			@Override
			public void process(List<String> output, String architecture, String version) {
				for (int val : vals) {
					output.add(MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP * val
							* ClangMockProcess.getTargetMultiplier(architecture) + "");
				}
			}
		};
	}

	public static BinaryLine lib(int... vals) {
		return new BinaryLine() {
			@Override
			public void process(List<String> output, String architecture, String version) {
				for (int val : vals) {
					output.add(val + "");
				}
			}
		};
	}

	@FunctionalInterface
	public interface BinaryLine {
		public void process(List<String> output, String architecture, String version);
	}

	protected void assertHeaderPrecompilationWasntRun() {
		for (Entry<List<String>, Long> entry : getMetric().getProcessInvocationFrequencies().entrySet()) {
			if (entry.getKey().contains("/Yc")) {
				throw new AssertionError("Header was precompiled: " + entry.getKey());
			}
		}
	}

	protected void assertHeaderPrecompilationRunOnlyOnce() {
		for (Entry<List<String>, Long> entry : getMetric().getProcessInvocationFrequencies().entrySet()) {
			if (entry.getKey().contains("/Yc")) {
				if (entry.getValue() > 1) {
					fail("Precompiled more than once: " + entry.getKey());
				}
			}
		}
	}

}
