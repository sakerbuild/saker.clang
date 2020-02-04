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

import java.util.NavigableSet;

import saker.build.file.DirectoryVisitPredicate;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;

class FileNameFilesDirectoryVisitPredicate implements DirectoryVisitPredicate {
	private NavigableSet<String> fileNames;

	public FileNameFilesDirectoryVisitPredicate(NavigableSet<String> fileNames) {
		this.fileNames = fileNames;
	}

	@Override
	public boolean visitFile(String name, SakerFile file) {
		return fileNames.contains(name);
	}

	@Override
	public boolean visitDirectory(String name, SakerDirectory directory) {
		return false;
	}
}