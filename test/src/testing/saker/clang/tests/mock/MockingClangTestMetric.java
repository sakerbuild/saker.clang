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
	public static final String DEFAULT_VERSION = "6.0.0-1ubuntu2";
	public static final String DEFAULT_TARGET = "x86_64-pc-linux-gnu";
	public static final String DEFAULT_THREADMODEL = "posix";

	public static final int MOCK_MULTIPLIER_LANGUAGE_C = 29;
	public static final int MOCK_MULTIPLIER_LANGUAGE_CPP = 31;

	public static final String TYPE_OBJ = "obj";
	public static final String TYPE_DLL = "dll";
	public static final String TYPE_EXE = "exe";
	public static final String TYPE_LIB = "lib";

	protected ConcurrentSkipListMap<SakerPath, String> compiledFileClusterNames = new ConcurrentSkipListMap<>();
	protected ConcurrentHashMap<List<String>, LongAdder> runCommands = new ConcurrentHashMap<>();

	@Override
	public int runProcess(List<String> command, boolean mergestderr, MetricProcessIOConsumer stdoutconsumer,
			MetricProcessIOConsumer stderrconsumer) throws IOException {
		runCommands.computeIfAbsent(command, x -> new LongAdder()).increment();
		System.out.println("MockingMSVCTestMetric.startProcess() " + command);
		SakerPath exepath = SakerPath.valueOf(command.get(0));
		if (exepath.getFileName().equalsIgnoreCase("clang")) {
			//TODO better handle target
			return ClangMockProcess.run(command, mergestderr, stdoutconsumer, stderrconsumer, DEFAULT_TARGET,
					DEFAULT_VERSION, DEFAULT_THREADMODEL);
		}
		throw new IOException("Exe not found: " + command);
	}

	@Override
	public void compiling(SakerPath path, SakerEnvironment environment) {
		compiledFileClusterNames.put(path, ObjectUtils
				.nullDefault(environment.getUserParameters().get(EnvironmentTestCase.TEST_CLUSTER_NAME_ENV_PARAM), ""));
	}

	@Override
	public String getClangVersionString(String clangExe) {
		if ("clang".equals(clangExe)) {
			return "clang version " + DEFAULT_VERSION + " (tags/RELEASE_600/final)\n" + "Target: " + DEFAULT_TARGET
					+ "\n" + "Thread model: " + DEFAULT_THREADMODEL + "\n" + "InstalledDir: /usr/bin\n" + "";
		}
		return ClangTestMetric.super.getClangVersionString(clangExe);
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
