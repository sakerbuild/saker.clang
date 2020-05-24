package testing.saker.clang.tests.mock;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

import saker.build.file.path.SakerPath;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.thirdparty.saker.util.ObjectUtils;
import testing.saker.build.tests.CollectingTestMetric;
import testing.saker.build.tests.EnvironmentTestCase;
import testing.saker.clang.ClangTestMetric;

public class MockingClangTestMetric extends CollectingTestMetric implements ClangTestMetric {
	public static final String DIFF_VERSION = "7.0.0-1ubuntu3";
	public static final String DIFF_TARGET = "x86-pc-linux-gnu";
	public static final String DIFF_THREADMODEL = "single";

	public static final SakerPath CLANG_EXECUTABLE_PATH_DEFAULT = SakerPath.valueOf("clang");
	public static final SakerPath CLANGXX_EXECUTABLE_PATH_DEFAULT = SakerPath.valueOf("clang++");
	public static final String DEFAULT_VERSION = "6.0.0-1ubuntu2";
	public static final String DEFAULT_TARGET = "x86_64-pc-linux-gnu";
	public static final String DEFAULT_THREADMODEL = "posix";

	public static final SakerPath CLANG_EXECUTABLE_PATH_VERSION_DIFF = SakerPath.valueOf("vdiff/clang");
	public static final SakerPath CLANG_EXECUTABLE_PATH_TARGET_DIFF = SakerPath.valueOf("targetdiff/clang");
	public static final SakerPath CLANG_EXECUTABLE_PATH_THREADMODEL_DIFF = SakerPath.valueOf("threadmodeldiff/clang");

	public static final int MOCK_MULTIPLIER_LANGUAGE_C = 29;
	public static final int MOCK_MULTIPLIER_LANGUAGE_CPP = 31;

	public static final String TYPE_OBJ = "obj";
	public static final String TYPE_DLL = "dll";
	public static final String TYPE_EXE = "exe";
	public static final String TYPE_LIB = "lib";

	protected ConcurrentSkipListMap<SakerPath, String> compiledFileClusterNames = new ConcurrentSkipListMap<>();
	protected ConcurrentHashMap<List<String>, LongAdder> runCommands = new ConcurrentHashMap<>();

	protected Map<String, String> clusterClangDefaultTargets = new ConcurrentSkipListMap<>();

	public MockingClangTestMetric() {
		clusterClangDefaultTargets.put("", DEFAULT_TARGET);
	}

	public void setDefaultTarget(String defaultTarget) {
		if (defaultTarget == null) {
			clusterClangDefaultTargets.remove("");
		} else {
			clusterClangDefaultTargets.put("", defaultTarget);
		}
	}

	public void setClusterDefaultTarget(String clustername, String defaulttarget) {
		if (defaulttarget == null) {
			clusterClangDefaultTargets.remove(clustername);
		} else {
			clusterClangDefaultTargets.put(clustername, defaulttarget);
		}
	}

	public ConcurrentHashMap<List<String>, LongAdder> getRunCommands() {
		return runCommands;
	}

	@Override
	public int runProcess(SakerEnvironment environment, List<String> command, boolean mergestderr,
			MetricProcessIOConsumer stdoutconsumer, MetricProcessIOConsumer stderrconsumer) throws IOException {
		runCommands.computeIfAbsent(command, x -> new LongAdder()).increment();
		System.out.println("MockingClangTestMetric.startProcess() " + command);
		SakerPath exepath = SakerPath.valueOf(command.get(0));
		String defaulttarget = getEnvironmentDefaultTarget(environment);
		String compilerversion = DEFAULT_VERSION;
		String defaultthreadmodel = DEFAULT_THREADMODEL;

		if (CLANG_EXECUTABLE_PATH_DEFAULT.equals(exepath) || CLANGXX_EXECUTABLE_PATH_DEFAULT.equals(exepath)) {
			//keep
		} else if (CLANG_EXECUTABLE_PATH_VERSION_DIFF.equals(exepath)) {
			compilerversion = DIFF_VERSION;
		} else if (CLANG_EXECUTABLE_PATH_TARGET_DIFF.equals(exepath)) {
			defaulttarget = DIFF_TARGET;
		} else if (CLANG_EXECUTABLE_PATH_THREADMODEL_DIFF.equals(exepath)) {
			defaultthreadmodel = DIFF_THREADMODEL;
		} else {
			throw new IOException("Exe not found: " + exepath);
		}
		return ClangMockProcess.run(command, mergestderr, stdoutconsumer, stderrconsumer, defaulttarget,
				compilerversion, defaultthreadmodel);
	}

	@Override
	public void compiling(SakerPath path, SakerEnvironment environment) {
		compiledFileClusterNames.put(path, ObjectUtils.nullDefault(getEnvironmentClusterName(environment), ""));
	}

	private static String getEnvironmentClusterName(SakerEnvironment environment) {
		return environment.getUserParameters().get(EnvironmentTestCase.TEST_CLUSTER_NAME_ENV_PARAM);
	}

	@Override
	public String getClangVersionString(String clangExe, SakerEnvironment environment) throws IOException {
		SakerPath exepath = SakerPath.valueOf(clangExe);
		String defaultTarget = getEnvironmentDefaultTarget(environment);
		if (CLANG_EXECUTABLE_PATH_DEFAULT.equals(exepath) || CLANGXX_EXECUTABLE_PATH_DEFAULT.equals(exepath)) {
			return getClangVersionString(DEFAULT_VERSION, defaultTarget, DEFAULT_THREADMODEL);
		}
		if (CLANG_EXECUTABLE_PATH_VERSION_DIFF.equals(exepath)) {
			return getClangVersionString(DIFF_VERSION, defaultTarget, DEFAULT_THREADMODEL);
		}
		if (CLANG_EXECUTABLE_PATH_TARGET_DIFF.equals(exepath)) {
			return getClangVersionString(DEFAULT_VERSION, DIFF_TARGET, DEFAULT_THREADMODEL);
		}
		if (CLANG_EXECUTABLE_PATH_THREADMODEL_DIFF.equals(exepath)) {
			return getClangVersionString(DEFAULT_VERSION, defaultTarget, DIFF_THREADMODEL);
		}
		return ClangTestMetric.super.getClangVersionString(clangExe, environment);
	}

	public ConcurrentSkipListMap<SakerPath, String> getCompiledFileClusterNames() {
		return compiledFileClusterNames;
	}

	private static String getClangVersionString(String version, String target, String threadmodel) {
		return "clang version " + version + " (tags/RELEASE_600/final)\n" + "Target: " + target + "\n"
				+ "Thread model: " + threadmodel + "\n" + "InstalledDir: /usr/bin\n" + "";
	}

	private String getEnvironmentDefaultTarget(SakerEnvironment env) throws IOException {
		String clustername = getEnvironmentClusterName(env);
		String result;
		if (clustername == null) {
			result = clusterClangDefaultTargets.get("");
		} else {
			result = clusterClangDefaultTargets.get(clustername);
		}
		if (result == null) {
			throw new IOException("Clang not found on cluster: " + clustername);
		}
		return result;
	}

	public Map<List<String>, Long> getProcessInvocationFrequencies() {
		HashMap<List<String>, Long> result = new HashMap<>();
		for (Entry<List<String>, LongAdder> entry : runCommands.entrySet()) {
			result.put(entry.getKey(), entry.getValue().longValue());
		}
		return result;
	}

	public static String createFileTypeLine(String type, String target) {
		return type + "_" + target;
	}
}
