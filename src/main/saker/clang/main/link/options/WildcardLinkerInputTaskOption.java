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
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;

final class WildcardLinkerInputTaskOption
		implements LinkerInputPassTaskOption, LinkerInputPassOption, FileLinkerInputPass {
	private final WildcardPath path;

	public WildcardLinkerInputTaskOption(WildcardPath path) {
		this.path = path;
	}

	@Override
	public Collection<FileLocation> toFileLocations(TaskContext tc) {
		FileCollectionStrategy collectionstrategy = WildcardFileCollectionStrategy.create(path);
		NavigableMap<SakerPath, SakerFile> files = tc.getTaskUtilities().collectFilesReportAdditionDependency(null,
				collectionstrategy);
		tc.getTaskUtilities().reportInputFileDependency(null,
				ObjectUtils.singleValueMap(files.navigableKeySet(), CommonTaskContentDescriptors.PRESENT));
		LinkedHashSet<FileLocation> result = new LinkedHashSet<>();
		for (SakerPath filepath : files.navigableKeySet()) {
			result.add(ExecutionFileLocation.create(filepath));
		}
		return result;
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