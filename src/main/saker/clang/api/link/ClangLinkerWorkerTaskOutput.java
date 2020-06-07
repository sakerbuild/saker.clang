package saker.clang.api.link;

import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.sdk.support.api.SDKDescription;

/**
 * Provides access to the output of a linking task that was performed using clang.
 */
public interface ClangLinkerWorkerTaskOutput {
	/**
	 * Gets the result path of the linking operation.
	 * <p>
	 * The path usually points to an executable or library based on the operation configuration.
	 * 
	 * @return The absolute execution path of the linking result.
	 */
	public SakerPath getOutputPath();

	/**
	 * Gets the identifier for the linker task.
	 * 
	 * @return The identifier.
	 */
	public CompilationIdentifier getIdentifier();

	/**
	 * Gets the SDKs that were used during the linking.
	 * <p>
	 * The result contains the resolved SDK descriptions with their configuration pinned to the ones that were used
	 * during linking.
	 * 
	 * @return The SDKs.
	 */
	public Map<String, SDKDescription> getSDKs();
}
