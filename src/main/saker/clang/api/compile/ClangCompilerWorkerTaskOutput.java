package saker.clang.api.compile;

import java.util.Collection;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.sdk.support.api.SDKDescription;

public interface ClangCompilerWorkerTaskOutput {
	public Collection<SakerPath> getObjectFilePaths();

	public CompilationIdentifier getIdentifier();

	public Map<String, SDKDescription> getSDKs();
}
