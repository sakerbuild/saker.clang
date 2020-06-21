package saker.clang.main;

import java.util.Collection;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.build.scripting.model.info.TypeInformationKind;
import saker.clang.main.compile.ClangCompileTaskFactory;
import saker.clang.main.link.ClangLinkTaskFactory;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.sdk.support.main.TaskDocs.DocSDKDescription;

public class TaskDocs {
	public static final String COMPILE_INCLUDE_DIRECTORIES = "Specifies include directories that are used to resolve #include directives in the source code.\n"
			+ "The values may be simple paths, wildcards, file locations, file collections, or SDK paths.\n"
			+ "The compilation task doesn't specify any include directories by default.\n"
			+ "Corresponds to the -I command line option for clang.";

	public static final String COMPILE_MACRO_DEFINITIONS = "Specifies key-value pairs which should be added as macro definitions for the compiled files.\n"
			+ "Each entry in this map will be defined as a preprocessor macro definition for the compiled files.\n"
			+ "Corresponds to the -D <key>=<value> command line option for clang.\n"
			+ "If value is empty or null, it will be omitted, and -D <key> is used.";

	public static final String COMPILE_SIMPLE_PARAMETERS = "Specifies one or more arguments that should be directly passed to clang.\n"
			+ "Any arguments specified will be appended to the clang invocation for the inputs. Take care when using this option "
			+ "as specifying files here may result in incorrect incremental builds.\n"
			+ "The order of the specified arguments are kept.";

	public static final String OPTION_SDKS = "Specifies the SDKs (Software Development Kits) used by the task.\n"
			+ "SDKs represent development kits that are available in the build environment and to the task. "
			+ "SDKs are used to determine the appropriate build environment to execute the task, as well "
			+ "to resolve paths against them (e.g. IncludeDirectory, LibraryPath).\n"
			+ "The \"Clang\" SDK is used to determine the location of the clang executable. If it is "
			+ "not specified, then the task will attempt to determine it automatically.";

	public static final String COMPILE_PRECOMPILED_HEADER = "Specifies the path or file location of a header file that should be precompiled.\n"
			+ "Precompiled headers files can be preprocessed by the compiler and included in multiple source files. "
			+ "Using them can result in faster builds as the compiler can reuse the result of the precompilation.\n"
			+ "It is recommended that the precompiled headers contains infrequently changing source files.\n"
			+ "The precompiled header will be automatically force included in the compiled source files.";

	public static final String COMPILE_FORCE_INCLUDE = "Specifies the files that should be force included in the compiled source files.\n"
			+ "The option corresponds to the -include argument of clang. The option acts as if the specified file was included with the #include directive "
			+ "at the start of the compiled source file.\n"
			+ "Multiple force included files can be specified. You don't need to force include the precompiled header if you're using one.";

	public static final String LINK_INPUT = "Specifies one or more inputs for the link operation.\n"
			+ "The inputs may be either simple paths, wildcards, file locations, file collections or task output from "
			+ ClangCompileTaskFactory.TASK_NAME + "().";
	public static final String LINK_LIBRARY_PATH = "Specifies the library path that will be searched for libraries.\n"
			+ "The values may be simple paths, wildcards, file locations, file collections, or SDK paths.\n"
			+ "The link task doesn't specify any library paths by default.\n"
			+ "Corresponds to the -L command line option for clang.";
	public static final String LINK_SIMPLE_PARAMETERS = "Specifies one or more arguments that should be directly passed to the clang.\n"
			+ "Any arguments specified will be appended to the clang invocation for the inputs. Take care when using this option "
			+ "as specifying files here may result in incorrect incremental builds.\n"
			+ "The order of the specified arguments are kept.";
	public static final String LINK_BINARY_NAME = "Specifies the file name of the link output.\n"
			+ "The specified string will be used as the name of the generated executable or library. An extension is not "
			+ "appended to the binary name automatically.\n"
			+ "If not specified, the file name will be generated based on the compilation Identifier.";

	public static final String OPTIONS_IDENTIFIER = "Specifies the Identifier to which the options should be merged into.\n"
			+ "The associated options will only be merged into the target configuration if the target Identifier "
			+ "contains all parts as this Identifier. If no Identifier specified for this options, the Identifier "
			+ "is not considered as a qualifier.";
	public static final String OPTIONS_LANGUAGE = "Specifies the Language to which the options should be merged into.\n"
			+ "The associated options will only be merged into the target configuration if the target Language "
			+ "is the same as the Language defined in this options. If no Language is specified for this options, "
			+ "the Language is not considered as a qualifier.";

	@NestTypeInformation(relatedTypes = @NestTypeUsage(String.class),
			kind = TypeInformationKind.STRING,
			qualifiedName = "ClangExecutableName",
			enumValues = {
					@NestFieldInformation(value = "clang", info = @NestInformation("The executable named: clang")),
					@NestFieldInformation(value = "clang++",
							info = @NestInformation("The executable named: clang++")), })
	@NestInformation("Name of a clang executable.")
	public static class DocClangExecutableName {
	}

	@NestInformation("Output of the " + ClangCompileTaskFactory.TASK_NAME + "() task.\n"
			+ "The object is a reference to the compilation results produced by clang.\n" + "It can be passed to the "
			+ ClangLinkTaskFactory.TASK_NAME + "() linker task to produce the resulting binary, or "
			+ "consume the compilation results in some other way.")
	@NestFieldInformation(value = "ObjectFilePaths",
			type = @NestTypeUsage(value = Collection.class, elementTypes = { SakerPath.class }),
			info = @NestInformation("Contains the paths to the compilation result object files.\n"
					+ "Each path is the result of the compilation of an input file."))
	@NestFieldInformation(value = "Identifier",
			type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
			info = @NestInformation("Contains the compilation Identifier that the compilation task was assigned "
					+ "with when compiling the inputs."))
	@NestFieldInformation(value = "SDKs",
			type = @NestTypeUsage(value = Map.class,
					elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class, DocSDKDescription.class }),
			info = @NestInformation("Contains the SDKs that were used for compiling the inputs.\n"
					+ "The map contains all SDKs (explicit or implicit) that was used during the configuration of the compilation."))
	@NestTypeInformation(qualifiedName = "saker.clang.api.compile.ClangCompilerWorkerTaskOutput")
	public interface DocClangCompilerWorkerTaskOutput {
	}

	@NestInformation("Output of the " + ClangLinkTaskFactory.TASK_NAME + "() task.\n"
			+ "The object is a reference to the link operation results using clang.\n"
			+ "The result can be consumed in any way the developer sees fit. The OutputPath field contains the path to the "
			+ "linker result binary.")
	@NestFieldInformation(value = "OutputPath",
			type = @NestTypeUsage(SakerPath.class),
			info = @NestInformation("Contains the path to the link operation output.\n"
					+ "The type of the file is based on the configuration of the linker task."))
	@NestFieldInformation(value = "Identifier",
			type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
			info = @NestInformation("Contains the Identifier that the linker task was assigned "
					+ "with when linking the inputs."))
	@NestFieldInformation(value = "SDKs",
			type = @NestTypeUsage(value = Map.class,
					elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class, DocSDKDescription.class }),
			info = @NestInformation("Contains the SDKs that were used for linking the inputs.\n"
					+ "The map contains all SDKs (explicit or implicit) that was used during the configuration of the linker."))
	@NestTypeInformation(qualifiedName = "saker.clang.api.link.ClangLinkerWorkerTaskOutput")
	public interface DocCLinkerWorkerTaskOutput {
	}

	@NestInformation("Represents the programming language that should be used for compilation.")
	@NestTypeInformation(kind = TypeInformationKind.ENUM,
			qualifiedName = "CompilationLanguage",
			enumValues = {

					@NestFieldInformation(value = DocCompilationLanguage.C,
							info = @NestInformation("Represents the programming language C.")),
					@NestFieldInformation(value = DocCompilationLanguage.CPP,
							info = @NestInformation("Represents the programming language C++.")),
					@NestFieldInformation(value = DocCompilationLanguage.OBJC,
							info = @NestInformation("Represents the programming language Objective-C.")),
					@NestFieldInformation(value = DocCompilationLanguage.OBJCPP,
							info = @NestInformation("Represents the programming language Objective-C++.")),

			})
	public static class DocCompilationLanguage {
		public static final String C = "C";
		public static final String CPP = "C++";
		public static final String OBJC = "ObjC";
		public static final String OBJCPP = "ObjC++";
	}

	@NestTypeInformation(kind = TypeInformationKind.LITERAL, qualifiedName = "CMacroName")
	@NestInformation("Name of the defined macro for the C/C++ preprocessor.")
	public static class MacroDefinitionKeyOption {
	}

	@NestTypeInformation(kind = TypeInformationKind.LITERAL, qualifiedName = "CMacroValue")
	@NestInformation("Value of the defined macro for the C/C++ preprocessor.")
	public static class MacroDefinitionValueOption {
	}

}
