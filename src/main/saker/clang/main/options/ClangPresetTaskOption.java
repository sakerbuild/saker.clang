package saker.clang.main.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.clang.main.compile.options.ClangCompilerOptions;
import saker.clang.main.link.options.ClangLinkerOptions;

//this is an API class for the main bundle
/**
 * Interface for passing multiple compiler option presets to the clang build tasks.
 * <p>
 * Use {@link #create(Collection)} to create a new instance.
 */
public interface ClangPresetTaskOption {
	/**
	 * Gets the collection of presets.
	 * <p>
	 * The elements should be convertable to {@link ClangCompilerOptions} and {@link ClangLinkerOptions} using the
	 * conversion mechanisms of the build system.
	 * <p>
	 * It is generally acceptable if the elements have the type of {@link Map Map&lt;String, ?&gt;}.
	 * <p>
	 * The elements should implement {@link Object#equals(Object)} and {@link Object#hashCode()}.
	 * 
	 * @return The presets.
	 */
	public List<?> getPresets();

	public static ClangPresetTaskOption create(Collection<?> presets) {
		return new SimpleClangPresetTaskOption(presets);
	}
}
