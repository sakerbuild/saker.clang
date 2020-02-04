package saker.clang.impl.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.process.api.ProcessIOConsumer;
import saker.process.api.SakerProcess;
import saker.process.api.SakerProcessBuilder;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;

public class ClangUtils {
	public static final String SDK_NAME_CLANG = "clang";

	private ClangUtils() {
		throw new UnsupportedOperationException();
	}

	public static void removeClangEnvironmentVariables(Map<String, String> env) {
		//based on https://clang.llvm.org/docs/CommandGuide/clang.html#environment
		env.remove("CPATH");
		env.remove("C_INCLUDE_PATH");
		env.remove("OBJC_INCLUDE_PATH");
		env.remove("CPLUS_INCLUDE_PATH");
		env.remove("OBJCPLUS_INCLUDE_PATH");
		env.remove("MACOSX_DEPLOYMENT_TARGET");

		//XXX remove this environment when linking
//		//based on http://releases.llvm.org/2.5/docs/CommandGuide/html/llvm-ld.html
//		env.remove("LLVM_LIB_SEARCH_PATH");
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

		ClangVersionInformation result = new ClangVersionInformation(output);

		return result;
	}

	public static int runClangProcess(List<String> commands, SakerPath workingdir, ProcessIOConsumer stdoutconsumer,
			ProcessIOConsumer stderrconsumer, boolean mergestderr)
			throws IllegalStateException, IOException, InterruptedException {
		//TODO testflag
//		if (TestFlag.ENABLED) {
//			return TestFlag.metric().runProcess(commands, mergestderr,
//					stdoutconsumer == null ? null : stdoutconsumer::handleOutput,
//					stderrconsumer == null ? null : stderrconsumer::handleOutput);
//		}
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
		//TODO use SakerStandardUtils from saker.standard 0.8.1
		if (fl == null) {
			return null;
		}
		FileLocationFileNameVisitor visitor = new FileLocationFileNameVisitor();
		fl.accept(visitor);
		return visitor.result;
	}

	@Deprecated
	private static class FileLocationFileNameVisitor implements FileLocationVisitor {
		public String result;

		public FileLocationFileNameVisitor() {
		}

		@Override
		public void visit(LocalFileLocation loc) {
			result = loc.getLocalPath().getFileName();
		}

		@Override
		public void visit(ExecutionFileLocation loc) {
			result = loc.getPath().getFileName();
		}

	}
}