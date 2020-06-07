package saker.clang.api.compile;

import java.util.Collection;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.sdk.support.api.SDKDescription;

/**
 * Provides access to the output of a clang compilation task.
 * <p>
 * The interface provides access to the results of a C/C++ compilation that was done using clang.
 */
public interface ClangCompilerWorkerTaskOutput {
	/**
	 * Gets the collection of execution paths that point to the resulting object files.
	 * <p>
	 * Each path is a result of a single source file compilation.
	 * 
	 * @return An immutable collection of object file paths.
	 */
	public Collection<SakerPath> getObjectFilePaths();

	/**
	 * Gets the compilation identifier of the compilation task.
	 * 
	 * @return The identifier.
	 */
	public CompilationIdentifier getIdentifier();

	/**
	 * Gets the SDKs that were used during the compilation.
	 * <p>
	 * The result contains the resolved SDK descriptions with their configuration pinned to the ones that were used
	 * during compilation.
	 * 
	 * @return The SDKs.
	 */
	public Map<String, SDKDescription> getSDKs();
}
