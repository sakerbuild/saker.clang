package saker.clang.impl.option;

import java.io.Externalizable;

import saker.sdk.support.api.SDKPathCollectionReference;

public final class SDKPathCollectionSimpleParameterOption
		extends SimpleParameterOptionBase<SDKPathCollectionReference> {
	private static final long serialVersionUID = 1L;

	/**
	 * For {@link Externalizable}.
	 */
	public SDKPathCollectionSimpleParameterOption() {
	}

	public SDKPathCollectionSimpleParameterOption(SDKPathCollectionReference value) {
		super(value);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(value);
	}
}