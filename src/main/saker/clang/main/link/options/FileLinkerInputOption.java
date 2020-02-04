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

import java.util.Collection;
import java.util.Set;

import saker.build.task.TaskContext;
import saker.std.api.file.location.FileLocation;

final class FileLinkerInputOption implements LinkerInputPassTaskOption, FileLinkerInputPass, LinkerInputPassOption {
	private final Set<FileLocation> fileLocations;

	public FileLinkerInputOption(Set<FileLocation> fileLocations) {
		this.fileLocations = fileLocations;
	}

	@Override
	public Collection<FileLocation> toFileLocations(TaskContext tc) {
		return fileLocations;
	}

	@Override
	public LinkerInputPassTaskOption clone() {
		return this;
	}

	@Override
	public LinkerInputPassOption toLinkerInputPassOption(TaskContext taskcontext) {
		return this;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
}