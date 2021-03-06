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

import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.clang.impl.option.CompilationPathOption;
import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKPathReference;

public class SDKPathReferenceCompilationPathOption implements CompilationPathOption, Externalizable {
	private static final long serialVersionUID = 1L;

	private SDKPathCollectionReference pathReference;

	/**
	 * For {@link Externalizable}.
	 */
	public SDKPathReferenceCompilationPathOption() {
	}

	public SDKPathReferenceCompilationPathOption(SDKPathCollectionReference pathReference) {
		this.pathReference = pathReference;
	}

	public SDKPathReferenceCompilationPathOption(String sdkname, String pathidentifier) {
		this(SDKPathCollectionReference.valueOf(SDKPathReference.create(sdkname, pathidentifier)));
	}

	@Override
	public void accept(CompilationPathOption.Visitor visitor) {
		visitor.visit(pathReference);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(pathReference);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		pathReference = SerialUtils.readExternalObject(in);
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
		SDKPathReferenceCompilationPathOption other = (SDKPathReferenceCompilationPathOption) obj;
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
