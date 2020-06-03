package saker.clang.impl.option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.thirdparty.saker.util.io.SerialUtils;

public abstract class SimpleParameterOptionBase<T> implements SimpleParameterOption, Externalizable {
	private static final long serialVersionUID = 1L;

	protected T value;

	/**
	 * For {@link Externalizable}.
	 */
	public SimpleParameterOptionBase() {
	}

	public SimpleParameterOptionBase(T value) {
		this.value = value;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(value);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		value = SerialUtils.readExternalObject(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		SimpleParameterOptionBase<?> other = (SimpleParameterOptionBase<?>) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (value != null ? "value=" + value : "") + "]";
	}

}
