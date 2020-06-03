package saker.clang.main.options;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.List;

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;

final class SimpleClangPresetTaskOption implements ClangPresetTaskOption, Externalizable {
	private static final long serialVersionUID = 1L;

	private List<?> presets;

	/**
	 * For {@link Externalizable}.
	 */
	public SimpleClangPresetTaskOption() {
	}

	SimpleClangPresetTaskOption(Collection<?> presets) {
		this.presets = ImmutableUtils.makeImmutableList(presets);
	}

	@Override
	public List<?> getPresets() {
		return presets;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, presets);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		presets = SerialUtils.readExternalImmutableList(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((presets == null) ? 0 : presets.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleClangPresetTaskOption other = (SimpleClangPresetTaskOption) obj;
		if (presets == null) {
			if (other.presets != null)
				return false;
		} else if (!presets.equals(other.presets))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SimpleClangPresetTaskOption[" + (presets != null ? "presets=" + presets : "") + "]";
	}

}