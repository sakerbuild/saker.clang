package saker.clang.impl.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ClangVersionInformation implements Externalizable {
	private static final long serialVersionUID = 1L;

	private String version;
	private String target;
	private String threadModel;

	/**
	 * For {@link Externalizable}.
	 */
	public ClangVersionInformation() {
	}

	public ClangVersionInformation(String versioncmdoutput) {
		int i = 0;
		while (true) {
			int idx = versioncmdoutput.indexOf('\n', i);
			if (i == 0) {
				//first line
				version = versioncmdoutput.substring(0, idx);
			} else if (versioncmdoutput.startsWith("Target: ", i)) {
				this.target = versioncmdoutput.substring(i + 8, idx);
			} else if (versioncmdoutput.startsWith("Thread model: ", i)) {
				this.threadModel = versioncmdoutput.substring(i + 14, idx);
			}
			if (idx < 0) {
				//last line
				break;
			}
			i = idx + 1;
		}
	}

	public String getVersion() {
		return version;
	}

	public String getThreadModel() {
		return threadModel;
	}

	public String getTarget() {
		return target;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(version);
		out.writeObject(target);
		out.writeObject(threadModel);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		version = (String) in.readObject();
		target = (String) in.readObject();
		threadModel = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((threadModel == null) ? 0 : threadModel.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		ClangVersionInformation other = (ClangVersionInformation) obj;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (threadModel == null) {
			if (other.threadModel != null)
				return false;
		} else if (!threadModel.equals(other.threadModel))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (version != null ? "version=" + version + ", " : "")
				+ (target != null ? "target=" + target + ", " : "")
				+ (threadModel != null ? "threadModel=" + threadModel : "") + "]";
	}

}
