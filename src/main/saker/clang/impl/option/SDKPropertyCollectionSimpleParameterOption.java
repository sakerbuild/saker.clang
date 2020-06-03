package saker.clang.impl.option;

import java.io.Externalizable;

import saker.sdk.support.api.SDKPropertyCollectionReference;

public final class SDKPropertyCollectionSimpleParameterOption
		extends SimpleParameterOptionBase<SDKPropertyCollectionReference> {
	private static final long serialVersionUID = 1L;

	/**
	 * For {@link Externalizable}.
	 */
	public SDKPropertyCollectionSimpleParameterOption() {
	}

	public SDKPropertyCollectionSimpleParameterOption(SDKPropertyCollectionReference value) {
		super(value);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(value);
	}
}