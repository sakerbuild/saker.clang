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
package saker.clang.impl.compile;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import saker.build.task.identifier.TaskIdentifier;
import saker.compiler.utils.api.CompilationIdentifier;

public class ClangCompileWorkerTaskIdentifier implements TaskIdentifier, Externalizable {
	private static final long serialVersionUID = 1L;

	private CompilationIdentifier passIdentifier;

	/**
	 * For {@link Externalizable}.
	 */
	public ClangCompileWorkerTaskIdentifier() {
	}

	public ClangCompileWorkerTaskIdentifier(CompilationIdentifier passIdentifier) {
		Objects.requireNonNull(passIdentifier, "pass identifier");
		this.passIdentifier = passIdentifier;
	}

	public CompilationIdentifier getPassIdentifier() {
		return passIdentifier;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(passIdentifier);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		passIdentifier = (CompilationIdentifier) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((passIdentifier == null) ? 0 : passIdentifier.hashCode());
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
		ClangCompileWorkerTaskIdentifier other = (ClangCompileWorkerTaskIdentifier) obj;
		if (passIdentifier == null) {
			if (other.passIdentifier != null)
				return false;
		} else if (!passIdentifier.equals(other.passIdentifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + passIdentifier + "]";
	}

}
