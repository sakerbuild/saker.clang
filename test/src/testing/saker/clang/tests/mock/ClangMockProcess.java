package testing.saker.clang.tests.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.file.provider.SakerPathFiles;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import testing.saker.clang.ClangTestMetric.MetricProcessIOConsumer;

public class ClangMockProcess {
	private ClangMockProcess() {
		throw new UnsupportedOperationException();
	}

	public static int run(List<String> commands, boolean mergestderr, MetricProcessIOConsumer stdoutconsumer,
			MetricProcessIOConsumer stderrconsumer, String defaulttarget, String version, String defaultthreadmodel)
			throws IOException {
		try {
			try (UnsyncByteArrayOutputStream stdoutbaos = new UnsyncByteArrayOutputStream();
					UnsyncByteArrayOutputStream stderrbaos = new UnsyncByteArrayOutputStream()) {
				int resultCode;
				try (PrintStream stdout = new PrintStream(stdoutbaos);
						PrintStream stderr = mergestderr ? stdout : new PrintStream(stderrbaos);) {

					SakerPath exepath = SakerPath.valueOf(commands.get(0));

					SakerPath outputpath = SakerPath.valueOf(requireCommandArgument(commands, "-o"));
					List<SakerPath> inputfiles = getInputFiles(commands);

					String target = defaulttarget;
					resultCode = executeCompilation(inputfiles, outputpath, stdout, stderr, commands, target, version);

				}
				if (stdoutconsumer != null) {
					stdoutconsumer.handleOutput(ByteBuffer.wrap(stdoutbaos.getBuffer(), 0, stdoutbaos.size()));
				}
				if (!mergestderr && stderrconsumer != null) {
					stderrconsumer.handleOutput(ByteBuffer.wrap(stderrbaos.getBuffer(), 0, stderrbaos.size()));
				}
				return resultCode;
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static int executeCompilation(List<SakerPath> inputpaths, SakerPath outputpath, PrintStream stdout,
			PrintStream stderr, List<String> commands, String target, String version) throws IOException {

		if (commands.contains("-c")) {
			//compile only
			if (inputpaths.size() > 1) {
				throw new IllegalArgumentException("Too many compilation inputs: " + inputpaths);
			}
			SakerPath depfile = SakerPath.valueOf(requireCommandArgument(commands, "-MF"));
			String language = requireCommandArgument(commands, "-x").toLowerCase(Locale.ENGLISH);

			UnsyncByteArrayOutputStream depfilebaos = new UnsyncByteArrayOutputStream();
			UnsyncByteArrayOutputStream fileoutbuf = new UnsyncByteArrayOutputStream();
			try (PrintStream depfileout = new PrintStream(depfilebaos)) {

				SakerPath inputpath = inputpaths.get(0);

				int langmultiplier = getLanguageMockMultipler(language);
				int targetmultiplier = getTargetMultiplier(target);

				List<SakerPath> includedirs = getIncludeDirectoriesFromCommands(commands);
				LinkedList<SourceLine> pendinglines = new LinkedList<>();
				Set<SakerPath> includedpaths = new TreeSet<>();
				Set<SakerPath> referencedlibs = new TreeSet<>();

				try (BufferedReader reader = Files.newBufferedReader(LocalFileProvider.toRealPath(inputpath));
						PrintStream outps = new PrintStream(fileoutbuf)) {
					outps.println(MockingClangTestMetric.createFileTypeLine(MockingClangTestMetric.TYPE_OBJ, target));
					outps.println("#version " + version);
					for (SourceLine srcline; (srcline = nextLine(reader, pendinglines, inputpath)) != null;) {
						String line = srcline.line;
						if (line.isEmpty()) {
							continue;
						}
						if (line.startsWith("#include ")) {
							throw new UnsupportedOperationException("#include");
						}
						int lineval;
						try {
							lineval = Integer.parseInt(line);
						} catch (NumberFormatException e) {
							String defineval = getDefineValue(commands, line);
							if (defineval == null) {
								throw new IllegalArgumentException("Illegal token: " + line);
							}
							if (defineval.isEmpty()) {
								//skip
								continue;
							}
							lineval = Integer.parseInt(defineval);
						}
						outps.println(lineval * langmultiplier * targetmultiplier);
					}
				}
				depfileout.println(outputpath + ": \\");
				if (includedpaths.isEmpty()) {
					depfileout.println(inputpath);
				} else {
					depfileout.println(inputpath + " \\");
					for (Iterator<SakerPath> it = includedpaths.iterator(); it.hasNext();) {
						SakerPath ip = it.next();
						depfileout.print("  ");
						depfileout.print(ip.toString().replace(" ", "\\ "));
						if (it.hasNext()) {
							depfileout.println(" \\");
						}
					}
				}
			}

			try {
				Files.write(LocalFileProvider.toRealPath(outputpath), fileoutbuf.toByteArray());
			} catch (IOException e) {
				e.printStackTrace();
				return -99;
			}

			Files.write(LocalFileProvider.toRealPath(depfile), depfilebaos.toByteArray());

			return 0;
		}

		List<SakerPath> libpathdirs = getLibPathDirectoriesFromCommands(commands);
		Set<SakerPath> includedlibs = new TreeSet<>();

		UnsyncByteArrayOutputStream fileoutbuf = new UnsyncByteArrayOutputStream();
		try (PrintStream outps = new PrintStream(fileoutbuf)) {
			if (containsIgnoreCase(commands, "-shared")) {
				outps.println(MockingClangTestMetric.createFileTypeLine(MockingClangTestMetric.TYPE_DLL, target));
			} else {
				outps.println(MockingClangTestMetric.createFileTypeLine(MockingClangTestMetric.TYPE_EXE, target));
			}
			outps.println("#version " + version);
			for (SakerPath inputpath : inputpaths) {
				try (BufferedReader reader = Files.newBufferedReader(LocalFileProvider.toRealPath(inputpath))) {
					String firstline = reader.readLine();
					String expectedtype = MockingClangTestMetric.createFileTypeLine(MockingClangTestMetric.TYPE_OBJ,
							target);
					if (!expectedtype.equals(firstline)) {
						throw new IllegalArgumentException(
								"Input file not " + expectedtype + ": " + inputpath + " -> " + firstline);
					}
					handleLinkFileLines(target, libpathdirs, includedlibs, outps, reader);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -99;
		}
		try {
			Files.write(LocalFileProvider.toRealPath(outputpath), fileoutbuf.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			return -99;
		}
		return 0;
	}

	private static void handleLinkFileLines(String target, List<SakerPath> libpathdirs, Set<SakerPath> includedlibs,
			PrintStream outps, BufferedReader reader) throws IOException {
		for (String line; (line = reader.readLine()) != null;) {
			if (line.startsWith("#lib ")) {
				SakerPath libpath = SakerPath.valueOf(line.substring(5));
				handleLib(libpathdirs, includedlibs, libpath, target, outps);
				continue;
			}
			if (line.startsWith("#version ")) {
				//skip
				continue;
			}
			int lineval = Integer.parseInt(line);
			outps.println(lineval);
		}
	}

	private static void handleLib(List<SakerPath> libpathdirs, Set<SakerPath> includedlibs, SakerPath libpath,
			String target, PrintStream outps) {
		List<Exception> causes = new ArrayList<>();
		for (SakerPath libpathdir : libpathdirs) {
			SakerPath libabssakerpath = libpathdir.resolve(libpath);
			if (includedlibs.contains(libabssakerpath)) {
				//already included
				break;
			}
			Path libabspath = LocalFileProvider.toRealPath(libabssakerpath);
			try (BufferedReader reader = Files.newBufferedReader(libabspath)) {
				includedlibs.add(libabssakerpath);
				String firstline = reader.readLine();
				String expectedtype = MockingClangTestMetric.createFileTypeLine(MockingClangTestMetric.TYPE_LIB,
						target);
				if (!expectedtype.equals(firstline)) {
					throw new IllegalArgumentException(
							"Lib input file not " + expectedtype + ": " + libabssakerpath + " -> " + firstline);
				}
				handleLinkFileLines(target, libpathdirs, includedlibs, outps, reader);
				return;
			} catch (IOException e) {
				causes.add(e);
				continue;
			}
		}
		IllegalArgumentException exc = new IllegalArgumentException("Lib not found: " + libpath);
		for (Exception c : causes) {
			exc.addSuppressed(c);
		}
		throw exc;
	}

	public static boolean containsIgnoreCase(List<String> commands, String cmd) {
		for (String c : commands) {
			if (cmd.equalsIgnoreCase(c)) {
				return true;
			}
		}
		return false;
	}

	private static List<SakerPath> getLibPathDirectoriesFromCommands(List<String> commands) {
		List<SakerPath> result = new ArrayList<>();
		for (Iterator<String> it = commands.iterator(); it.hasNext();) {
			String cmd = it.next();
			if ("-L".equals(cmd)) {
				result.add(SakerPath.valueOf(it.next()));
			}
		}
		return result;
	}

	private static List<SakerPath> getIncludeDirectoriesFromCommands(List<String> commands) {
		List<SakerPath> result = new ArrayList<>();
		for (Iterator<String> it = commands.iterator(); it.hasNext();) {
			String cmd = it.next();
			if ("-I".equals(cmd)) {
				result.add(SakerPath.valueOf(it.next()));
			}
		}
		return result;
	}

	private static SourceLine nextLine(BufferedReader reader, Deque<SourceLine> pendinglines, SakerPath inputpath)
			throws IOException {
		if (!pendinglines.isEmpty()) {
			return pendinglines.pollFirst();
		}
		String nl = reader.readLine();
		if (nl == null) {
			return null;
		}
		return new SourceLine(inputpath, nl);
	}

	private static String getDefineValue(List<String> commands, String word) {
		for (Iterator<String> it = commands.iterator(); it.hasNext();) {
			String cmd = it.next();
			if ("-D".equals(cmd)) {
				String arg = it.next();
				if (!arg.startsWith("=")) {
					throw new IllegalArgumentException("Command: " + cmd);
				}
				if (arg.startsWith(word + "=")) {
					return arg.substring(word.length() + 1);
				}
			}
		}
		return null;
	}

	private static List<SakerPath> getInputFiles(List<String> commands) {
		List<SakerPath> result = new ArrayList<>();
		Iterator<String> it = commands.iterator();
		//skip exe name
		it.next();
		for (; it.hasNext();) {
			String cmd = it.next();
			switch (cmd) {
				case "-o":
				case "-D":
				case "-I":
				case "-include":
				case "-include-pch":
				case "-MF":
				case "-x": {
					//skip argument
					it.next();
					break;
				}
				case "-c":
				case "-MMD": {
					//skip
					break;
				}
				default: {
					SakerPath path = SakerPath.valueOf(cmd);
					SakerPathFiles.requireAbsolutePath(path);
					result.add(path);
					break;
				}
			}
		}
		return result;
	}

	public static String requireCommandArgument(List<String> commands, String cmdname) {
		for (Iterator<String> it = commands.iterator(); it.hasNext();) {
			String cmd = it.next();
			if (cmd.equals(cmdname)) {
				if (it.hasNext()) {
					return it.next();
				}
			}
		}
		throw new IllegalArgumentException("No command found with name: " + cmdname);
	}

	public static int getTargetMultiplier(String target) {
		return target.hashCode();
	}

	public static int getLanguageMockMultipler(String language) {
		if ("c".equalsIgnoreCase(language)) {
			return MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_C;
		}
		if ("c++".equalsIgnoreCase(language)) {
			return MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP;
		}
		throw new IllegalArgumentException("Unknown language: " + language);
	}

	private static class SourceLine {
		public SakerPath sourcePath;
		public String line;

		public SourceLine(SakerPath sourcePath, String line) {
			this.sourcePath = sourcePath;
			this.line = line;
		}
	}
}
