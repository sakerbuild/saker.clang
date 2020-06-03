package saker.clang.main.compile.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.clang.main.options.CompilationPathTaskOption;
import saker.clang.main.options.SimpleParameterTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.main.file.option.FileLocationTaskOption;

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

	public interface Visitor {
		public default void visit(ClangCompilerOptions options) {
			throw new UnsupportedOperationException("Unsupported compiler options: " + options);
		}
	}
}
