package saker.clang.main.link.options;

import java.util.List;
import java.util.Map;

import saker.build.util.data.DataConverterUtils;
import saker.clang.main.options.ClangPresetTaskOption;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.clang.main.options.SimpleParameterTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

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
