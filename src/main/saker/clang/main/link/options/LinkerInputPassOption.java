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
package saker.clang.main.link.options;

import saker.sdk.support.api.SDKPathCollectionReference;

public interface LinkerInputPassOption {
	public void accept(Visitor visitor);

	public interface Visitor {
		public default void visit(FileLinkerInputPass input) {
			throw new UnsupportedOperationException("Unsupported input: " + input);
		}

		public default void visit(CompilerOutputLinkerInputPass input) {
			throw new UnsupportedOperationException("Unsupported input: " + input);
		}

		public default void visit(SDKPathCollectionReference input) {
			throw new UnsupportedOperationException("Unsupported input: " + input);
		}
	}

}
