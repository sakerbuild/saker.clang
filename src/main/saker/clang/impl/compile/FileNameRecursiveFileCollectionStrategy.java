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
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;

import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.execution.ExecutionDirectoryContext;
import saker.build.task.TaskDirectoryContext;
import saker.build.task.dependencies.FileCollectionStrategy;
import saker.build.thirdparty.saker.util.io.SerialUtils;

class FileNameRecursiveFileCollectionStrategy implements FileCollectionStrategy, Externalizable {
	private static final long serialVersionUID = 1L;

	private SakerPath directoryPath;
	private NavigableSet<String> fileNames;

	/**
	 * For {@link Externalizable}.
	 */
	public FileNameRecursiveFileCollectionStrategy() {
	}

	public FileNameRecursiveFileCollectionStrategy(SakerPath directoryPath, NavigableSet<String> fileNames) {
		this.directoryPath = directoryPath;
		this.fileNames = fileNames;
	}

	@Override
	public NavigableMap<SakerPath, SakerFile> collectFiles(ExecutionDirectoryContext executiondirectorycontext,
			TaskDirectoryContext taskdirectorycontext) {
		SakerDirectory dir = SakerPathFiles.resolveDirectoryAtAbsolutePath(executiondirectorycontext,
				directoryPath);
		if (dir == null) {
			return Collections.emptyNavigableMap();
		}
		return dir.getFilesRecursiveByPath(directoryPath, new FileNameFilesDirectoryVisitPredicate(fileNames));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(directoryPath);
		SerialUtils.writeExternalCollection(out, fileNames);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		directoryPath = (SakerPath) in.readObject();
		fileNames = SerialUtils.readExternalSortedImmutableNavigableSet(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((directoryPath == null) ? 0 : directoryPath.hashCode());
		result = prime * result + ((fileNames == null) ? 0 : fileNames.hashCode());
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
		FileNameRecursiveFileCollectionStrategy other = (FileNameRecursiveFileCollectionStrategy) obj;
		if (directoryPath == null) {
			if (other.directoryPath != null)
				return false;
		} else if (!directoryPath.equals(other.directoryPath))
			return false;
		if (fileNames == null) {
			if (other.fileNames != null)
				return false;
		} else if (!fileNames.equals(other.fileNames))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ (directoryPath != null ? "directoryPath=" + directoryPath + ", " : "")
				+ (fileNames != null ? "fileNames=" + fileNames : "") + "]";
	}

}