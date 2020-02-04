package saker.clang.impl.sdk;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.file.path.SakerPath;
import saker.clang.impl.util.ClangUtils;
import saker.clang.impl.util.ClangVersionInformation;
import saker.sdk.support.api.SDKReference;

public class VersionInfoClangSDKReference implements SDKReference, Externalizable {
	private static final long serialVersionUID = 1L;

	public static final String PROPERTY_VERSION = "version";
	public static final String PROPERTY_TARGET = "target";
	public static final String PROPERTY_THREAD_MODEL = "thread_model";

	private ClangVersionInformation versionInfo;
	private transient String exe;

	/**
	 * For {@link Externalizable}.
	 */
	public VersionInfoClangSDKReference() {
	}

	public VersionInfoClangSDKReference(String exe, ClangVersionInformation versioninfo) {
		this.exe = exe;
		this.versionInfo = versioninfo;
	}

	public ClangVersionInformation getVersionInfo() {
		return versionInfo;
	}

	public String getExecutable() {
		return exe;
	}

	@Override
	public SakerPath getPath(String identifier) throws Exception {
		switch (identifier) {
			case ClangUtils.SDK_PATH_CLANG_EXECUTABLE: {
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
			case ClangUtils.SDK_PATH_CLANG_EXECUTABLE: {
				return exe;
			}
			case PROPERTY_VERSION: {
				return versionInfo.getVersion();
			}
			case PROPERTY_TARGET: {
				return versionInfo.getTarget();
			}
			case PROPERTY_THREAD_MODEL: {
				return versionInfo.getThreadModel();
			}
			default: {
				break;
			}
		}
		return null;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(versionInfo);

		out.writeObject(exe);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		versionInfo = (ClangVersionInformation) in.readObject();

		exe = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((versionInfo == null) ? 0 : versionInfo.hashCode());
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
		VersionInfoClangSDKReference other = (VersionInfoClangSDKReference) obj;
		if (versionInfo == null) {
			if (other.versionInfo != null)
				return false;
		} else if (!versionInfo.equals(other.versionInfo))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ClangSDKReference[" + exe + " : " + versionInfo + "]";
	}
}
