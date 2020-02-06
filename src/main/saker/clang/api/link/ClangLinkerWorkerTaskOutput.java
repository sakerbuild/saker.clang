package saker.clang.api.link;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.CompilationIdentifier;

public interface ClangLinkerWorkerTaskOutput {
	public SakerPath getOutputPath();

	public CompilationIdentifier getIdentifier();
}
