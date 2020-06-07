package saker.clang.impl.util;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.thirdparty.saker.util.ArrayUtils;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.clang.impl.option.SimpleParameterOption;
import saker.clang.impl.sdk.DefaultClangSDKDescription;
import saker.process.api.ProcessIOConsumer;
import saker.process.api.SakerProcess;
import saker.process.api.SakerProcessBuilder;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKPropertyCollectionReference;
import saker.sdk.support.api.SDKPropertyReference;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.exc.SDKPathNotFoundException;
import saker.std.api.file.location.FileLocation;
import saker.std.api.util.SakerStandardUtils;
import testing.saker.clang.TestFlag;

public class ClangUtils {
	public static final String SDK_NAME_CLANG = "Clang";

	public static final SDKDescription DEFAULT_CLANG_SDK = new DefaultClangSDKDescription();

	public static final String ENVIRONMENT_PARAMETER_CLANG_EXECUTABLES = "saker.clang.executables";
	public static final String ENVIRONMENT_PARAMETER_CLANGXX_EXECUTABLES = "saker.clang++.executables";

	public static final String SDK_CLANG_PATH_EXECUTABLE = "exe";

	private ClangUtils() {
		throw new UnsupportedOperationException();
	}

	public static String getClangExecutable(SDKReference sdk) {
		Exception[] causes = {};
		try {
			SakerPath path = sdk.getPath(SDK_CLANG_PATH_EXECUTABLE);
			if (path != null) {
				return path.toString();
			}
		} catch (Exception e) {
			causes = ArrayUtils.appended(causes, e);
		}
		try {
			String prop = sdk.getProperty(SDK_CLANG_PATH_EXECUTABLE);
			if (prop != null) {
				return prop;
			}
		} catch (Exception e) {
			causes = ArrayUtils.appended(causes, e);
		}
		SDKPathNotFoundException exc = new SDKPathNotFoundException(
				"Clang executable path not found in SDK for identifier: " + SDK_CLANG_PATH_EXECUTABLE);
		for (Exception ex : causes) {
			exc.addSuppressed(ex);
		}
		throw exc;
	}

	public static void removeClangEnvironmentVariables(Map<String, String> env) {
		//based on https://clang.llvm.org/docs/CommandGuide/clang.html#environment
		env.remove("CPATH");
		env.remove("C_INCLUDE_PATH");
		env.remove("OBJC_INCLUDE_PATH");
		env.remove("CPLUS_INCLUDE_PATH");
		env.remove("OBJCPLUS_INCLUDE_PATH");
		env.remove("MACOSX_DEPLOYMENT_TARGET");

		//based on http://releases.llvm.org/2.5/docs/CommandGuide/html/llvm-ld.html
		env.remove("LLVM_LIB_SEARCH_PATH");
	}

	public static ClangVersionInformation getClangVersionInformation(String executable)
			throws IOException, InterruptedException {
		SakerProcessBuilder pb = SakerProcessBuilder.create()
				.setCommand(ImmutableUtils.asUnmodifiableArrayList(executable, "--version"));
		pb.setStandardErrorMerge(true);
		CollectingProcessIOConsumer stdoutconsumer = new CollectingProcessIOConsumer();
		pb.setStandardOutputConsumer(stdoutconsumer);
		try (SakerProcess proc = pb.start()) {
			proc.processIO();
			int exitcode = proc.waitFor();
			if (exitcode != 0) {
				throw new RuntimeException("Failed to determine clang version, exit code: " + exitcode);
			}
		}

		String output = stdoutconsumer.getOutputString();

		return ClangVersionInformation.createFromVersionOutput(output);
	}

	public static int runClangProcess(SakerEnvironment environment, List<String> commands, SakerPath workingdir,
			ProcessIOConsumer stdoutconsumer, ProcessIOConsumer stderrconsumer, boolean mergestderr)
			throws IllegalStateException, IOException, InterruptedException {
		if (TestFlag.ENABLED) {
			return TestFlag.metric().runProcess(environment, commands, mergestderr,
					stdoutconsumer == null ? null : stdoutconsumer::handleOutput,
					stderrconsumer == null ? null : stderrconsumer::handleOutput);
		}
		SakerProcessBuilder pb = SakerProcessBuilder.create();
		pb.setCommand(commands);
		pb.setStandardOutputConsumer(stdoutconsumer);
		if (mergestderr) {
			pb.setStandardErrorMerge(true);
		} else {
			pb.setStandardErrorConsumer(stderrconsumer);
		}
		pb.setWorkingDirectory(workingdir);
		removeClangEnvironmentVariables(pb.getEnvironment());

		try (SakerProcess proc = pb.start()) {
			proc.processIO();
			return proc.waitFor();
		}
	}

	public static String getFileName(FileLocation fl) {
		return SakerStandardUtils.getFileLocationFileName(fl);
	}

	public static void evaluateSimpleParameters(List<String> result, List<? extends SimpleParameterOption> params,
			Map<String, ? extends SDKReference> sdks) throws Exception {
		if (params == null) {
			return;
		}
		SimpleParameterOption.Visitor visitor = new SimpleParameterOption.Visitor() {
			@Override
			public void visit(String value) {
				result.add(value);
			}

			@Override
			public void visit(SDKPathCollectionReference value) {
				try {
					Collection<SakerPath> paths = value.getValue(sdks);
					if (paths == null) {
						throw new SDKPathNotFoundException("No SDK paths found for: " + value);
					}
					for (SakerPath p : paths) {
						result.add(p.toString());
					}
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			}

			@Override
			public void visit(SDKPropertyCollectionReference value) {
				try {
					Collection<String> props = value.getValue(sdks);
					if (props == null) {
						throw new SDKPathNotFoundException("No SDK paths found for: " + value);
					}
					result.addAll(props);
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			}

			@Override
			public void visit(SDKPathReference value) {
				try {
					SakerPath p = value.getValue(sdks);
					if (p == null) {
						throw new SDKPathNotFoundException("No SDK paths found for: " + value);
					}
					result.add(p.toString());
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			}

			@Override
			public void visit(SDKPropertyReference value) {
				try {
					String prop = value.getValue(sdks);
					if (prop == null) {
						throw new SDKPathNotFoundException("No SDK paths found for: " + value);
					}
					result.add(prop);
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			}

		};
		for (SimpleParameterOption p : params) {
			p.accept(visitor);
		}
	}
}
