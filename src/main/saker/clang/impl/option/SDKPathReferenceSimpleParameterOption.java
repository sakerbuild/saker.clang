package saker.clang.impl.option;

import java.io.Externalizable;

import saker.sdk.support.api.SDKPathReference;

public final class SDKPathReferenceSimpleParameterOption extends SimpleParameterOptionBase<SDKPathReference> {
	private static final long serialVersionUID = 1L;

	/**
	 * For {@link Externalizable}.
	 */
	public SDKPathReferenceSimpleParameterOption() {
	}

	public SDKPathReferenceSimpleParameterOption(SDKPathReference value) {
		super(value);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(value);
	}
}