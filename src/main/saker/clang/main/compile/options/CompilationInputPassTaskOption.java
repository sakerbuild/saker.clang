package saker.clang.main.compile.options;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.path.WildcardPath.ReducedWildcardPath;
import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.clang.main.TaskDocs;
import saker.clang.main.TaskDocs.DocCompilationLanguage;
import saker.clang.main.TaskDocs.MacroDefinitionKeyOption;
import saker.clang.main.TaskDocs.MacroDefinitionValueOption;
import saker.clang.main.compile.ClangCompileTaskFactory;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.clang.main.options.SimpleParameterTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileCollection;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.FileLocationTaskOption;
import saker.std.main.file.option.MultiFileLocationTaskOption;

@NestInformation("Input configuration for the " + ClangCompileTaskFactory.TASK_NAME + "() task.\n"
		+ "The configuration specifies which files should be compiled, in what manner.\n"
		+ "The Files field defines the inputs to the compilation task.")
@NestFieldInformation(value = "Files",
		type = @NestTypeUsage(value = Collection.class, elementTypes = MultiFileLocationTaskOption.class),
		info = @NestInformation("Specifies the files that should be compiled.\n"
				+ "Accepts simple paths, wildcards, file locations, and file collections. "
				+ "The specified files are directly passed to clang."))

@NestFieldInformation(value = "IncludeDirectories",
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationPathTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_INCLUDE_DIRECTORIES))
@NestFieldInformation(value = "CompilerOptions",
		type = @NestTypeUsage(value = Collection.class, elementTypes = ClangCompilerOptions.class),
		info = @NestInformation("Specifies one or more option specifications that are merged with the configuration when applicable.\n"
				+ "Similar to the CompilerOption parameter in " + ClangCompileTaskFactory.TASK_NAME
				+ "(), but only affects this input configuration. Mergeability is determined the same way, but with the "
				+ "SubIdentifier field appended to the task compilation identifier."))
@NestFieldInformation(value = "SubIdentifier",
		type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
		info = @NestInformation("Specifies the sub-identifier that should be used as a qualifier when merging CompilerOptions.\n"
				+ "The sub-identifier specified here will be merged with the Identifier of the compiler task, and "
				+ "the mergeability of a compiler option configuration is determined based on the merged identifier."))
@NestFieldInformation(value = "MacroDefinitions",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { MacroDefinitionKeyOption.class, MacroDefinitionValueOption.class }),
		info = @NestInformation(TaskDocs.COMPILE_MACRO_DEFINITIONS))
@NestFieldInformation(value = "SimpleParameters",
		type = @NestTypeUsage(value = Collection.class, elementTypes = SimpleParameterTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_SIMPLE_PARAMETERS))
@NestFieldInformation(value = "Language",
		type = @NestTypeUsage(DocCompilationLanguage.class),
		info = @NestInformation("Specifies the programming language that should be used for the compilation of the specified files.\n"
				+ "If not specified, the language will be determined based on the extension of each file. "
				+ "If the file extension ends with \"pp\" or \"xx\", C++ is used. "
				+ "Objective-C is used for the \"m\" extension, and Objective-C++ for \"mm\". "
				+ "In any other cases, the file is compiled for the C language.\n"
				+ "The language is also used to determine if the given CompilerOptions configuration should be merged. "
				+ "The languages are treated in an case-insensitive way.\n"
				+ "Corresponds to the -x command line option for clang."))

@NestFieldInformation(value = "PrecompiledHeader",
		type = @NestTypeUsage(FileLocationTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_PRECOMPILED_HEADER))
@NestFieldInformation(value = "ForceInclude",
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationPathTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_FORCE_INCLUDE))
public interface CompilationInputPassTaskOption {
	public default CompilationInputPassTaskOption clone() {
		return new OptionCompilationInputPassOption(this);
	}

	public CompilationInputPassOption toCompilationInputPassOption(TaskContext taskcontext);

	public default Collection<MultiFileLocationTaskOption> getFiles() {
		return null;
	}

	public default List<CompilationPathTaskOption> getIncludeDirectories() {
		return null;
	}

	public default Collection<ClangCompilerOptions> getCompilerOptions() {
		return null;
	}

	public default CompilationIdentifierTaskOption getSubIdentifier() {
		return null;
	}

	public default Map<String, String> getMacroDefinitions() {
		return null;
	}

	public default List<SimpleParameterTaskOption> getSimpleParameters() {
		return null;
	}

	public default String getLanguage() {
		return null;
	}

	public default FileLocationTaskOption getPrecompiledHeader() {
		return null;
	}

	public default List<CompilationPathTaskOption> getForceInclude() {
		return null;
	}

	public static CompilationInputPassTaskOption valueOf(FileLocation filelocation) {
		FileLocationTaskOption.validateFileLocation(filelocation);
		return new FileCompilationInputFileOption(Collections.singleton(filelocation));
	}

	public static CompilationInputPassTaskOption valueOf(FileCollection files) {
		return new FileCompilationInputFileOption(ImmutableUtils.makeImmutableLinkedHashSet(files));
	}

	public static CompilationInputPassTaskOption valueOf(SakerPath path) {
		if (!path.isAbsolute()) {
			return new RelativePathFileCompilationInputFileOption(path);
		}
		return new FileCompilationInputFileOption(Collections.singleton(ExecutionFileLocation.create(path)));
	}

	public static CompilationInputPassTaskOption valueOf(WildcardPath path) {
		ReducedWildcardPath reduced = path.reduce();
		if (reduced.getWildcard() == null) {
			return valueOf(reduced.getFile());
		}
		return new WildcardFileCompilationInputFileOption(path);
	}

	public static CompilationInputPassTaskOption valueOf(String path) {
		return valueOf(WildcardPath.valueOf(path));
	}
}
