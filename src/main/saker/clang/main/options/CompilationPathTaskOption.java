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
import java.util.LinkedHashSet;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.path.WildcardPath.ReducedWildcardPath;
import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.util.option.FileCompilationPathOptionImpl;
import saker.clang.impl.util.option.SDKPathReferenceCompilationPathOption;
import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKPathReference;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileCollection;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.FileLocationTaskOption;

public interface CompilationPathTaskOption {
	public CompilationPathTaskOption clone();

	public Collection<CompilationPathOption> toCompilationPaths(TaskContext taskcontext);

	public static CompilationPathTaskOption valueOf(FileLocation filelocation) {
		FileLocationTaskOption.validateFileLocation(filelocation);
		return new SimpleCompilationPathTaskOption(Collections.singleton(new FileCompilationPathOptionImpl(filelocation)));
	}

	public static CompilationPathTaskOption valueOf(FileCollection files) {
		Set<CompilationPathOption> filelist = new LinkedHashSet<>();
		for (FileLocation fl : files) {
			filelist.add(new FileCompilationPathOptionImpl(fl));
		}
		return new SimpleCompilationPathTaskOption(ImmutableUtils.unmodifiableSet(filelist));
	}

	public static CompilationPathTaskOption valueOf(SakerPath path) {
		if (!path.isAbsolute()) {
			return new RelativePathCompilationPathTaskOption(path);
		}
		return new SimpleCompilationPathTaskOption(
				Collections.singleton(new FileCompilationPathOptionImpl(ExecutionFileLocation.create(path))));
	}

	public static CompilationPathTaskOption valueOf(WildcardPath path) {
		ReducedWildcardPath reduced = path.reduce();
		if (reduced.getWildcard() == null) {
			return valueOf(reduced.getFile());
		}
		return new WildcardCompilationPathTaskOption(path);
	}

	public static CompilationPathTaskOption valueOf(String path) {
		return valueOf(WildcardPath.valueOf(path));
	}

	public static CompilationPathTaskOption valueOf(CompilationPathOption option) {
		return new SimpleCompilationPathTaskOption(Collections.singleton(option));
	}

	public static CompilationPathTaskOption valueOf(SDKPathReference pathreference) {
		return valueOf(SDKPathCollectionReference.valueOf(pathreference));
	}

	public static CompilationPathTaskOption valueOf(SDKPathCollectionReference pathreference) {
		return valueOf(new SDKPathReferenceCompilationPathOption(pathreference));
	}
}
