package saker.clang.impl.option;

import java.io.Externalizable;

import saker.sdk.support.api.SDKPropertyReference;

public final class SDKPropertyReferenceSimpleParameterOption
		extends SimpleParameterOptionBase<SDKPropertyReference> {
	private static final long serialVersionUID = 1L;

	/**
	 * For {@link Externalizable}.
	 */
	public SDKPropertyReferenceSimpleParameterOption() {
	}

	public SDKPropertyReferenceSimpleParameterOption(SDKPropertyReference value) {
		super(value);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(value);
	}
}