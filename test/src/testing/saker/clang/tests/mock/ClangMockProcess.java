package testing.saker.clang.tests.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.file.provider.SakerPathFiles;
import saker.build.thirdparty.saker.util.ArrayUtils;
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
					resultCode = executeClang(inputfiles, outputpath, stdout, stderr, commands, target, version);
				} finally {
					if (stdoutconsumer != null) {
						stdoutconsumer.handleOutput(ByteBuffer.wrap(stdoutbaos.getBuffer(), 0, stdoutbaos.size()));
					}
					if (!mergestderr && stderrconsumer != null) {
						stderrconsumer.handleOutput(ByteBuffer.wrap(stderrbaos.getBuffer(), 0, stderrbaos.size()));
					}
				}
				return resultCode;
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static int executeClang(List<SakerPath> inputpaths, SakerPath outputpath, PrintStream stdout,
			PrintStream stderr, List<String> commands, String target, String version) throws IOException {

		if (commands.contains("-c")) {
			//compile only
			return executeCompilation(inputpaths, outputpath, stdout, stderr, commands, target, version);
		}

		return executeLinking(inputpaths, outputpath, commands, target, version);
	}

	private static int executeLinking(List<SakerPath> inputpaths, SakerPath outputpath, List<String> commands,
			String target, String version) {
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
			Path realpath = LocalFileProvider.toRealPath(outputpath);
			Files.write(realpath, fileoutbuf.toByteArray());
			try {
				//all permissions are set for libs as well
				Files.setPosixFilePermissions(realpath, EnumSet.allOf(PosixFilePermission.class));
			} catch (UnsupportedOperationException e) {
			}
		} catch (IOException e) {
			e.printStackTrace();
			return -99;
		}
		return 0;
	}

	private static int executeCompilation(List<SakerPath> inputpaths, SakerPath outputpath, PrintStream stdout,
			PrintStream stderr, List<String> commands, String target, String version) throws IOException {
		if (inputpaths.size() > 1) {
			throw new IllegalArgumentException("Too many compilation inputs: " + inputpaths);
		}
		SakerPath depfile = SakerPath.valueOf(requireCommandArgument(commands, "-MF"));
		String language = requireCommandArgument(commands, "-x").toLowerCase(Locale.ENGLISH);
		StringBuilder pch = null;
		if (language.endsWith("-header")) {
			pch = new StringBuilder();
		}

		UnsyncByteArrayOutputStream depfilebaos = new UnsyncByteArrayOutputStream();
		UnsyncByteArrayOutputStream fileoutbuf = new UnsyncByteArrayOutputStream();
		try (PrintStream depfileout = new PrintStream(depfilebaos)) {

			SakerPath inputpath = inputpaths.get(0);

			int langmultiplier = getLanguageMockMultipler(language);
			int targetmultiplier = getTargetMultiplier(target);

			List<SakerPath> includedirs = getIncludeDirectoriesFromCommands(commands);
			LinkedList<SourceLine> pendinglines = new LinkedList<>();
			Set<SakerPath> includedpaths = new TreeSet<>();

			for (Iterator<String> it = commands.iterator(); it.hasNext();) {
				String cmd = it.next();
				if (cmd.equals("-include") || cmd.equals("-include-pch")) {
					String fipath = it.next();
					SourceLine incsrcline = new SourceLine(SakerPath.valueOf("<built-in>"),
							"#include \"" + fipath + "\"");
					try {
						SakerPath incpath = SakerPath.valueOf(fipath);

						//TODO this is incorrect, as the transitive includes need to be processed before the next force include is added
						List<SourceLine> lines = getIncludeLines(incsrcline, incpath);
						includedpaths.add(incpath);
						pendinglines.addAll(lines);
					} catch (InvalidPathException | InvalidPathFormatException | IOException e) {
						printIncludeNotFound(incsrcline, fipath, stdout);
						return -2;
					}
					continue;
				}
				if (cmd.equals("-include-pch")) {

				}
			}

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
						String includephrase = line.substring(9).trim();
						if (includephrase.isEmpty()) {
							return -2;
						}
						SakerPath includepath = SakerPath
								.valueOf(includephrase.substring(1, includephrase.length() - 1));
						if (includephrase.charAt(0) == '<' && includephrase.charAt(includephrase.length() - 1) == '>') {
							includeBracketIncludePath(srcline, includedirs, pendinglines, includedpaths, includepath,
									stdout, stderr, commands);
						} else if (includephrase.charAt(0) == '\"'
								&& includephrase.charAt(includephrase.length() - 1) == '\"') {
							throw new UnsupportedOperationException(
									"Quoted inclusion shouldn't be used as it is prone to mirroring errors.");
						} else {
							return -3;
						}
						continue;
					}
					if (pch != null) {
						pch.append(line);
						pch.append('\n');
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
			byte[] outputcontents = pch == null ? fileoutbuf.toByteArray()
					: pch.toString().getBytes(StandardCharsets.UTF_8);
			Files.write(LocalFileProvider.toRealPath(outputpath), outputcontents);
		} catch (IOException e) {
			e.printStackTrace();
			return -99;
		}

		if (depfile != null) {
			Files.write(LocalFileProvider.toRealPath(depfile), depfilebaos.toByteArray());
		}

		return 0;
	}

	private static void includeBracketIncludePath(SourceLine includeline, List<SakerPath> includedirs,
			LinkedList<SourceLine> pendinglines, Set<SakerPath> includedpaths, SakerPath includepath,
			PrintStream stdout, PrintStream stderr, List<String> commands) {
		List<Exception> causes = new ArrayList<>();
		for (SakerPath includedir : includedirs) {
			SakerPath resolvedincludepath = includedir.resolve(includepath);
			if (includedpaths.contains(resolvedincludepath)) {
				return;
			}
			try {
				includeResolvedIncludePath(includeline, pendinglines, resolvedincludepath, stdout, stderr,
						includedpaths, commands);
				return;
			} catch (IOException e) {
				causes.add(e);
				continue;
			}
		}
		printIncludeNotFound(includeline, includepath.toString(), stdout);
		IllegalArgumentException exc = new IllegalArgumentException("Included file not found: " + includepath);
		for (Exception e : causes) {
			exc.addSuppressed(e);
		}
		throw exc;
	}

	private static void printIncludeNotFound(SourceLine includeline, String includepath, PrintStream stdout) {
		for (SakerPath ip : includeline.includeStack) {
			stdout.println("In file included from " + ip + ":1:");
		}
		stdout.println(includeline.sourcePath + ":1:10: fatal error: '" + includepath + "' file not found");
	}

	private static void includeResolvedIncludePath(SourceLine includeline, LinkedList<SourceLine> pendinglines,
			SakerPath includepath, PrintStream stdout, PrintStream stderr, Set<SakerPath> includedpaths,
			List<String> commands) throws IOException {
		List<SourceLine> lines = getIncludeLines(includeline, includepath);
		includedpaths.add(includepath);
		pendinglines.addAll(0, lines);
	}

	private static List<SourceLine> getIncludeLines(SourceLine includeline, SakerPath includepath) throws IOException {
		List<String> alllines = Files.readAllLines(LocalFileProvider.toRealPath(includepath));
		List<SourceLine> lines = new ArrayList<>();
		SakerPath[] nincludestack = ArrayUtils.appended(includeline.includeStack, includeline.sourcePath);
		for (String l : alllines) {
			lines.add(new SourceLine(nincludestack, includepath, l));
		}
		return lines;
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
				int idx = arg.indexOf('=');
				if (idx < 0) {
					if (word.equals(arg)) {
						return "";
					}
					continue;
				}
				if (arg.substring(0, idx).equals(word)) {
					return arg.substring(idx + 1);
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
		if ("c".equalsIgnoreCase(language) || "c-header".equalsIgnoreCase(language)) {
			return MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_C;
		}
		if ("c++".equalsIgnoreCase(language) || "c++-header".equalsIgnoreCase(language)) {
			return MockingClangTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP;
		}
		throw new IllegalArgumentException("Unknown language: " + language);
	}

	private static class SourceLine {
		public static final SakerPath[] EMPTY_SAKERPATH_ARRAY = {};
		public SakerPath[] includeStack = EMPTY_SAKERPATH_ARRAY;
		public SakerPath sourcePath;
		public String line;

		public SourceLine(SakerPath sourcePath, String line) {
			this.sourcePath = sourcePath;
			this.line = line;
		}

		public SourceLine(SakerPath[] includePath, SakerPath sourcePath, String line) {
			this.includeStack = includePath;
			this.sourcePath = sourcePath;
			this.line = line;
		}
	}
}
