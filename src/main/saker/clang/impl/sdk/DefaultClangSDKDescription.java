package saker.clang.impl.sdk;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.sdk.support.api.IndeterminateSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKReference;

public class DefaultClangSDKDescription implements IndeterminateSDKDescription, Externalizable {
	private static final long serialVersionUID = 1L;

	/**
	 * For {@link Externalizable}.
	 */
	public DefaultClangSDKDescription() {
	}

	@Override
	public SDKDescription getBaseSDKDescription() {
		return VersionInfoClangSDKDescription.INSTANCE_ANY_VERSION;
	}

	@Override
	public SDKDescription pinSDKDescription(SDKReference sdkreference) {
		VersionInfoClangSDKReference sdk = (VersionInfoClangSDKReference) sdkreference;
		return new VersionInfoClangSDKDescription(sdk.getVersionInfo());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ObjectUtils.isSameClass(this, obj);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[]";
	}

}
