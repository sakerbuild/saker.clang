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
package saker.clang.main.options;

import java.util.Collection;
import java.util.Collections;

import saker.build.file.path.SakerPath;
import saker.build.task.TaskContext;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.util.option.FileCompilationPathOptionImpl;
import saker.std.api.file.location.ExecutionFileLocation;

final class RelativePathCompilationPathTaskOption implements CompilationPathTaskOption {
	private final SakerPath path;

	public RelativePathCompilationPathTaskOption(SakerPath path) {
		this.path = path;
	}

	@Override
	public Collection<CompilationPathOption> toCompilationPaths(TaskContext tc) {
		return Collections.singleton(new FileCompilationPathOptionImpl(
				ExecutionFileLocation.create(tc.getTaskWorkingDirectoryPath().resolve(path))));
	}

	@Override
	public CompilationPathTaskOption clone() {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		RelativePathCompilationPathTaskOption other = (RelativePathCompilationPathTaskOption) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

}