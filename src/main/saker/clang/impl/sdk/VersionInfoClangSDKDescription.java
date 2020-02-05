package saker.clang.impl.sdk;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.regex.Pattern;

import saker.build.runtime.environment.SakerEnvironment;
import saker.build.thirdparty.saker.util.ArrayUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.clang.impl.util.ClangUtils;
import saker.clang.impl.util.ClangVersionInformation;
import saker.clang.impl.util.ClangVersionInformationEnvironmentProperty;
import saker.sdk.support.api.EnvironmentSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.exc.SDKNotFoundException;
import testing.saker.clang.TestFlag;

public class VersionInfoClangSDKDescription implements EnvironmentSDKDescription, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final Pattern PATTERN_SEMICOLON_SPLIT = Pattern.compile(";+");

	public static final SDKDescription INSTANCE_ANY_VERSION = new VersionInfoClangSDKDescription(null);

	private ClangVersionInformation expectVersionInfo;

	/**
	 * For {@link Externalizable}.
	 */
	public VersionInfoClangSDKDescription() {
	}

	public VersionInfoClangSDKDescription(ClangVersionInformation expectVersionInfo) {
		this.expectVersionInfo = expectVersionInfo;
	}

	@Override
	public SDKReference getSDK(SakerEnvironment environment) throws Exception {
		Throwable[] causes = ObjectUtils.EMPTY_THROWABLE_ARRAY;
		for (String exe : PATTERN_SEMICOLON_SPLIT.split(environment.getUserParameters()
				.getOrDefault(ClangUtils.ENVIRONMENT_PARAMETER_CLANG_EXECUTABLES, "clang"))) {
			if (exe.isEmpty()) {
				continue;
			}
			try {
				ClangVersionInformation versioninfo;
				if (TestFlag.ENABLED) {
					versioninfo = new ClangVersionInformation(
							TestFlag.metric().getClangVersionString(exe, environment));
				} else {
					versioninfo = environment
							.getEnvironmentPropertyCurrentValue(new ClangVersionInformationEnvironmentProperty(exe));
				}
				//if expected version info is null, allow any 
				if (expectVersionInfo != null && !versioninfo.equals(expectVersionInfo)) {
					throw new SDKNotFoundException("Clang version information mismatch: " + versioninfo
							+ " with expected: " + expectVersionInfo);
				}
				return new VersionInfoClangSDKReference(exe, versioninfo);
			} catch (Exception e) {
				causes = ArrayUtils.appended(causes, e);
			}
		}
		SDKNotFoundException e = new SDKNotFoundException("Clang not found.");
		for (Throwable c : causes) {
			e.addSuppressed(c);
		}
		throw e;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(expectVersionInfo);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		expectVersionInfo = (ClangVersionInformation) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expectVersionInfo == null) ? 0 : expectVersionInfo.hashCode());
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
		VersionInfoClangSDKDescription other = (VersionInfoClangSDKDescription) obj;
		if (expectVersionInfo == null) {
			if (other.expectVersionInfo != null)
				return false;
		} else if (!expectVersionInfo.equals(other.expectVersionInfo))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (expectVersionInfo == null ? "any" : expectVersionInfo) + "]";
	}

}
