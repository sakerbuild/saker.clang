package saker.clang.main.compile.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.build.util.data.DataConverterUtils;
import saker.clang.main.TaskDocs;
import saker.clang.main.TaskDocs.DocCompilationLanguage;
import saker.clang.main.TaskDocs.MacroDefinitionKeyOption;
import saker.clang.main.TaskDocs.MacroDefinitionValueOption;
import saker.clang.main.compile.ClangCompileTaskFactory;
import saker.clang.main.options.ClangPresetTaskOption;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.clang.main.options.SimpleParameterTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.main.file.option.FileLocationTaskOption;

@NestInformation("Options configuration to be used with " + ClangCompileTaskFactory.TASK_NAME + "().\n"
		+ "The described options will be merged with the compilation input configuration based on the option qualifiers. "
		+ "The Identifier and Language fields are considered to be used as qualifiers for the option merging, "
		+ "in which case they are tested for mergeability with the input configuration.")

@NestFieldInformation(value = "Identifier",
		type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
		info = @NestInformation(TaskDocs.OPTIONS_IDENTIFIER))
@NestFieldInformation(value = "Language",
		type = @NestTypeUsage(DocCompilationLanguage.class),
		info = @NestInformation(TaskDocs.OPTIONS_LANGUAGE))
@NestFieldInformation(value = "IncludeDirectories",
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationPathTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_INCLUDE_DIRECTORIES))
@NestFieldInformation(value = "MacroDefinitions",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { MacroDefinitionKeyOption.class, MacroDefinitionValueOption.class }),
		info = @NestInformation(TaskDocs.COMPILE_MACRO_DEFINITIONS + "\n"
				+ "When merging, the macro definitions won't overwrite macro definitions specified previously."))
@NestFieldInformation(value = "SimpleCompilerParameters",
		type = @NestTypeUsage(value = Collection.class, elementTypes = SimpleParameterTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_SIMPLE_PARAMETERS + "\n"
				+ "When merging, duplicate parameters are removed automatically."))
@NestFieldInformation(value = "SDKs",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class,
						SDKDescriptionTaskOption.class }),
		info = @NestInformation(TaskDocs.OPTION_SDKS + "\n"
				+ "When merging, duplicate SDK definitions are not overwritten."))

@NestFieldInformation(value = "PrecompiledHeader",
		type = @NestTypeUsage(FileLocationTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_PRECOMPILED_HEADER + "\n"
				+ "When merging, only a single precompiled header may be used. An exception "
				+ "is thrown in case of conflict."))
@NestFieldInformation(value = "ForceInclude",
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationPathTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_FORCE_INCLUDE))
public interface ClangCompilerOptions {
	public void accept(Visitor visitor);

	public default ClangCompilerOptions clone() {
		return new SimpleClangCompilerOptions(this);
	}

	public default CompilationIdentifierTaskOption getIdentifier() {
		return null;
	}

	public default Collection<String> getLanguage() {
		return null;
	}

	public default List<CompilationPathTaskOption> getIncludeDirectories() {
		return null;
	}

	public default Map<String, String> getMacroDefinitions() {
		return null;
	}

	public default List<SimpleParameterTaskOption> getSimpleCompilerParameters() {
		return null;
	}

	public default Map<String, SDKDescriptionTaskOption> getSDKs() {
		return null;
	}

	public default FileLocationTaskOption getPrecompiledHeader() {
		return null;
	}

	public default List<CompilationPathTaskOption> getForceInclude() {
		return null;
	}

	public static ClangCompilerOptions valueOf(ClangPresetTaskOption preset) {
		return new ClangCompilerOptions() {
			@Override
			public ClangCompilerOptions clone() {
				return this;
			}

			@Override
			public void accept(Visitor visitor) {
				visitor.visit(preset);
			}
		};
	}

	public interface Visitor {
		public default void visit(ClangCompilerOptions options) {
			throw new UnsupportedOperationException("Unsupported compiler options: " + options);
		}

		public default void visit(ClangPresetTaskOption options) {
			List<?> presets = options.getPresets();
			if (presets == null) {
				return;
			}
			for (Object p : presets) {
				visit(DataConverterUtils.convert(p, ClangCompilerOptions.class));
			}
		}
	}
}
