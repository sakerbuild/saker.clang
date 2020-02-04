/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.clang.impl.util.option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.clang.impl.compile.option.IncludePathOption;
import saker.clang.impl.link.option.LibraryPathOption;
import saker.sdk.support.api.SDKPathReference;

public class CommonSDKPathReferenceOption implements IncludePathOption, LibraryPathOption, Externalizable {
	private static final long serialVersionUID = 1L;

	private SDKPathReference pathReference;

	/**
	 * For {@link Externalizable}.
	 */
	public CommonSDKPathReferenceOption() {
	}

	public CommonSDKPathReferenceOption(SDKPathReference pathReference) {
		this.pathReference = pathReference;
	}

	public CommonSDKPathReferenceOption(String sdkname, String pathidentifier) {
		this.pathReference = SDKPathReference.create(sdkname, pathidentifier);
	}

	@Override
	public void accept(LibraryPathOption.Visitor visitor) {
		visitor.visit(pathReference);
	}

	@Override
	public void accept(IncludePathOption.Visitor visitor) {
		visitor.visit(pathReference);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(pathReference);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		pathReference = (SDKPathReference) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pathReference == null) ? 0 : pathReference.hashCode());
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
		CommonSDKPathReferenceOption other = (CommonSDKPathReferenceOption) obj;
		if (pathReference == null) {
			if (other.pathReference != null)
				return false;
		} else if (!pathReference.equals(other.pathReference))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + pathReference + "]";
	}

}
