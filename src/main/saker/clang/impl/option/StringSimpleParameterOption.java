package saker.clang.impl.option;

import java.io.Externalizable;

public final class StringSimpleParameterOption extends SimpleParameterOptionBase<String> {
	private static final long serialVersionUID = 1L;

	/**
	 * For {@link Externalizable}.
	 */
	public StringSimpleParameterOption() {
	}

	public StringSimpleParameterOption(String value) {
		super(value);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(value);
	}

}
