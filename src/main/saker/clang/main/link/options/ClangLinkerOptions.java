package saker.clang.main.link.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.build.util.data.DataConverterUtils;
import saker.clang.main.TaskDocs;
import saker.clang.main.link.ClangLinkTaskFactory;
import saker.clang.main.options.ClangPresetTaskOption;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.clang.main.options.SimpleParameterTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

@NestInformation("Options configuration to be used with " + ClangLinkTaskFactory.TASK_NAME + "().\n"
		+ "The described options will be merged with the linker input configuration based on the option qualifiers. "
		+ "The Identifier field is used as qualifier for the option merging, "
		+ "in which case they are tested for mergeability with the input configuration.")

@NestFieldInformation(value = "Identifier",
		type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
		info = @NestInformation(TaskDocs.OPTIONS_IDENTIFIER))

@NestFieldInformation(value = "LinkerInput",
		type = @NestTypeUsage(value = Collection.class, elementTypes = LinkerInputPassTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_INPUT))
@NestFieldInformation(value = "LibraryPath",
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationPathTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_LIBRARY_PATH))
@NestFieldInformation(value = "SDKs",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class,
						SDKDescriptionTaskOption.class }),
		info = @NestInformation(TaskDocs.OPTION_SDKS + "\n"
				+ "When merging, duplicate SDK definitions are not overwritten."))
@NestFieldInformation(value = "SimpleLinkerParameters",
		type = @NestTypeUsage(value = Collection.class, elementTypes = SimpleParameterTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_SIMPLE_PARAMETERS + "\n"
				+ "When merging, duplicate parameters are removed automatically."))
@NestFieldInformation(value = "BinaryName",
		type = @NestTypeUsage(String.class),
		info = @NestInformation(TaskDocs.LINK_BINARY_NAME))
public interface ClangLinkerOptions {
	public void accept(Visitor visitor);

	public default ClangLinkerOptions clone() {
		return new SimpleClangLinkerOptions(this);
	}

	public default CompilationIdentifierTaskOption getIdentifier() {
		return null;
	}

	public default List<LinkerInputPassTaskOption> getLinkerInput() {
		return null;
	}

	public default List<CompilationPathTaskOption> getLibraryPath() {
		return null;
	}

	public default Map<String, SDKDescriptionTaskOption> getSDKs() {
		return null;
	}

	public default List<SimpleParameterTaskOption> getSimpleLinkerParameters() {
		return null;
	}

	public default String getBinaryName() {
		return null;
	}

	public static ClangLinkerOptions valueOf(ClangPresetTaskOption preset) {
		return new ClangLinkerOptions() {
			@Override
			public ClangLinkerOptions clone() {
				return this;
			}

			@Override
			public void accept(Visitor visitor) {
				visitor.visit(preset);
			}
		};
	}

	public interface Visitor {
		public default void visit(ClangLinkerOptions options) {
			throw new UnsupportedOperationException("Unsupported linker options: " + options);
		}

		public default void visit(ClangPresetTaskOption options) {
			List<?> presets = options.getPresets();
			if (presets == null) {
				return;
			}
			for (Object p : presets) {
				visit(DataConverterUtils.convert(p, ClangLinkerOptions.class));
			}
		}

	}
}
