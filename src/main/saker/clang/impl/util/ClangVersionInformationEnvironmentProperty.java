package saker.clang.impl.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.runtime.environment.EnvironmentProperty;
import saker.build.runtime.environment.SakerEnvironment;
import testing.saker.clang.TestFlag;

public class ClangVersionInformationEnvironmentProperty
		implements EnvironmentProperty<ClangVersionInformation>, Externalizable {
	private static final long serialVersionUID = 1L;

	private String clangExe;

	/**
	 * For {@link Externalizable}.
	 */
	public ClangVersionInformationEnvironmentProperty() {
	}

	public ClangVersionInformationEnvironmentProperty(String clangExe) {
		this.clangExe = clangExe;
	}

	@Override
	public ClangVersionInformation getCurrentValue(SakerEnvironment environment) throws Exception {
		if (TestFlag.ENABLED) {
			return ClangVersionInformation
					.createFromVersionOutput(TestFlag.metric().getClangVersionString(clangExe, environment));
		}
		return ClangUtils.getClangVersionInformation(clangExe);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(clangExe);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		clangExe = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clangExe == null) ? 0 : clangExe.hashCode());
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
		ClangVersionInformationEnvironmentProperty other = (ClangVersionInformationEnvironmentProperty) obj;
		if (clangExe == null) {
			if (other.clangExe != null)
				return false;
		} else if (!clangExe.equals(other.clangExe))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (clangExe != null ? "clangExe=" + clangExe : "") + "]";
	}

}
