package saker.clang.impl.sdk;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import saker.build.runtime.environment.SakerEnvironment;
import saker.build.thirdparty.saker.util.ArrayUtils;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.clang.impl.util.ClangVersionInformation;
import saker.clang.impl.util.ClangVersionInformationEnvironmentProperty;
import saker.sdk.support.api.EnvironmentSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.exc.SDKNotFoundException;

public class VersionInfoClangSDKDescription implements EnvironmentSDKDescription, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final Pattern PATTERN_SEMICOLON_SPLIT = Pattern.compile(";+");

	private static final Set<String> ALLOWED_EXECUTABLE_NAMES = ImmutableUtils
			.makeImmutableNavigableSet(new String[] { "clang", "clang++" });

	public static final SDKDescription INSTANCE_ANY_VERSION = new VersionInfoClangSDKDescription(null);

	private ClangVersionInformation expectVersionInfo;
	/**
	 * <code>clang</code> or <code>clang++</code>
	 */
	private String executable;

	/**
	 * For {@link Externalizable}.
	 */
	public VersionInfoClangSDKDescription() {
	}

	public VersionInfoClangSDKDescription(ClangVersionInformation expectVersionInfo) {
		this(expectVersionInfo, "clang");
	}

	public VersionInfoClangSDKDescription(ClangVersionInformation expectVersionInfo, String executable) {
		Objects.requireNonNull(executable, "executable");
		if (!ALLOWED_EXECUTABLE_NAMES.contains(executable)) {
			throw new IllegalArgumentException("Invalid clang executable name: " + executable + " expected: "
					+ StringUtils.toStringJoin(", ", ALLOWED_EXECUTABLE_NAMES));
		}
		this.expectVersionInfo = expectVersionInfo;
		this.executable = executable;
	}

	@Override
	public SDKReference getSDK(SakerEnvironment environment) throws Exception {
		Throwable[] causes = ObjectUtils.EMPTY_THROWABLE_ARRAY;
		for (String exe : PATTERN_SEMICOLON_SPLIT.split(
				environment.getUserParameters().getOrDefault("saker." + executable + ".executables", executable))) {
			if (exe.isEmpty()) {
				continue;
			}
			try {
				ClangVersionInformation versioninfo = getExecutableClangVersionInformation(environment, exe);
				if (versioninfo == null) {
					throw new SDKNotFoundException("Failed to determine clang version info: " + exe);
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

	private static ClangVersionInformation getExecutableClangVersionInformation(SakerEnvironment environment,
			String exe) {
		return environment.getEnvironmentPropertyCurrentValue(new ClangVersionInformationEnvironmentProperty(exe));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(expectVersionInfo);
		out.writeUTF(executable);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		expectVersionInfo = (ClangVersionInformation) in.readObject();
		executable = in.readUTF();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((executable == null) ? 0 : executable.hashCode());
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
		if (executable == null) {
			if (other.executable != null)
				return false;
		} else if (!executable.equals(other.executable))
			return false;
		if (expectVersionInfo == null) {
			if (other.expectVersionInfo != null)
				return false;
		} else if (!expectVersionInfo.equals(other.expectVersionInfo))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + executable + ": "
				+ (expectVersionInfo == null ? "any" : expectVersionInfo) + "]";
	}

}
