package saker.clang.main.link.options;

import java.util.Collection;
import java.util.Map;

import saker.clang.main.options.CompilationPathTaskOption;
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

	public default Collection<LinkerInputPassTaskOption> getLinkerInput() {
		return null;
	}

	public default Collection<CompilationPathTaskOption> getLibraryPath() {
		return null;
	}

	public default Map<String, SDKDescriptionTaskOption> getSDKs() {
		return null;
	}

	public default Collection<String> getSimpleLinkerParameters() {
		return null;
	}

	public interface Visitor {
		public default void visit(ClangLinkerOptions options) {
			throw new UnsupportedOperationException("Unsupported linker options: " + options);
		}

	}
}
