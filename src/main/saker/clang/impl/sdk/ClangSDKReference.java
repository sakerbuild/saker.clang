package saker.clang.impl.sdk;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.file.path.SakerPath;
import saker.clang.impl.util.ClangVersionInformation;
import saker.sdk.support.api.SDKReference;

public class ClangSDKReference implements SDKReference, Externalizable {
	private static final long serialVersionUID = 1L;

	public static final String CLANG_EXECUTABLE = "exe";

	public static final String PROPERTY_VERSION = "version";
	public static final String PROPERTY_TARGET = "target";
	public static final String PROPERTY_THREAD_MODEL = "thread_model";

	private String version;

	private transient String threadModel;
	private transient String defaultTarget;
	private transient String exe;

	/**
	 * For {@link Externalizable}.
	 */
	public ClangSDKReference() {
	}

	public ClangSDKReference(String exe, ClangVersionInformation versioninfo) {
		this.exe = exe;
		this.version = versioninfo.getVersion();
		this.defaultTarget = versioninfo.getTarget();
		this.threadModel = versioninfo.getThreadModel();
	}

	public String getVersion() {
		return version;
	}

	@Override
	public SakerPath getPath(String identifier) throws Exception {
		switch (identifier) {
			case CLANG_EXECUTABLE: {
				return SakerPath.valueOf(exe);
			}
			default: {
				break;
			}
		}
		return null;
	}

	@Override
	public String getProperty(String identifier) throws Exception {
		switch (identifier) {
			case CLANG_EXECUTABLE: {
				return exe;
			}
			case PROPERTY_VERSION: {
				return version;
			}
			case PROPERTY_TARGET: {
				return defaultTarget;
			}
			case PROPERTY_THREAD_MODEL: {
				return threadModel;
			}
			default: {
				break;
			}
		}
		return null;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(version);
		out.writeObject(defaultTarget);
		out.writeObject(threadModel);

		out.writeObject(exe);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		version = (String) in.readObject();
		defaultTarget = (String) in.readObject();
		threadModel = (String) in.readObject();

		exe = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		ClangSDKReference other = (ClangSDKReference) obj;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ClangSDKReference[" + (version != null ? "version=" + version : "") + "]";
	}
}
