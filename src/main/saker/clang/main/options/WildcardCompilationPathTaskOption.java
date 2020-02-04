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
import java.util.LinkedHashSet;
import java.util.NavigableMap;

import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.task.CommonTaskContentDescriptors;
import saker.build.task.TaskContext;
import saker.build.task.dependencies.FileCollectionStrategy;
import saker.build.task.utils.dependencies.WildcardFileCollectionStrategy;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.util.option.FileCompilationPathOptionImpl;
import saker.std.api.file.location.ExecutionFileLocation;

final class WildcardCompilationPathTaskOption implements CompilationPathTaskOption {
	private final WildcardPath path;

	public WildcardCompilationPathTaskOption(WildcardPath path) {
		this.path = path;
	}

	@Override
	public Collection<CompilationPathOption> toCompilationPaths(TaskContext tc) {
		FileCollectionStrategy collectionstrategy = WildcardFileCollectionStrategy.create(path);
		NavigableMap<SakerPath, SakerFile> files = tc.getTaskUtilities().collectFilesReportAdditionDependency(null,
				collectionstrategy);
		tc.getTaskUtilities().reportInputFileDependency(null,
				ObjectUtils.singleValueMap(files.navigableKeySet(), CommonTaskContentDescriptors.PRESENT));
		LinkedHashSet<CompilationPathOption> result = new LinkedHashSet<>();
		for (SakerPath filepath : files.navigableKeySet()) {
			result.add(new FileCompilationPathOptionImpl(ExecutionFileLocation.create(filepath)));
		}
		return result;
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
		WildcardCompilationPathTaskOption other = (WildcardCompilationPathTaskOption) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	
}