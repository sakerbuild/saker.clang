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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.path.WildcardPath.ReducedWildcardPath;
import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.clang.api.compile.ClangCompilerWorkerTaskOutput;
import saker.clang.main.compile.ClangCompileTaskFactory;
import saker.clang.main.link.ClangLinkTaskFactory;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKPathReference;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileCollection;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.FileLocationTaskOption;

@NestInformation("Input for the " + ClangLinkTaskFactory.TASK_NAME + "() task.\n"
		+ "The configuration specifies which files should be added to the input files for the clang invocation.\n"
		+ "The configuration accepts simple paths, wildcards, file locations, file collections, and outputs of the "
		+ ClangCompileTaskFactory.TASK_NAME + "() task.")
public interface LinkerInputPassTaskOption {
	public LinkerInputPassTaskOption clone();

	public LinkerInputPassOption toLinkerInputPassOption(TaskContext taskcontext);

	public static LinkerInputPassTaskOption valueOf(ClangCompilerWorkerTaskOutput compilationoutput) {
		return new CompilerOutputLinkerInputOption(compilationoutput);
	}

	public static LinkerInputPassTaskOption valueOf(FileLocation filelocation) {
		FileLocationTaskOption.validateFileLocation(filelocation);
		return new FileLinkerInputOption(Collections.singleton(filelocation));
	}

	public static LinkerInputPassTaskOption valueOf(FileCollection files) {
		Set<FileLocation> filelist = ObjectUtils.addAll(new LinkedHashSet<>(), files);
		return new FileLinkerInputOption(ImmutableUtils.unmodifiableSet(filelist));
	}

	public static LinkerInputPassTaskOption valueOf(WildcardPath path) {
		ReducedWildcardPath reduced = path.reduce();
		if (reduced.getWildcard() == null) {
			return valueOf(reduced.getFile());
		}
		return new WildcardLinkerInputTaskOption(path);
	}

	public static LinkerInputPassTaskOption valueOf(SakerPath path) {
		if (!path.isAbsolute()) {
			return new RelativePathLinkerInputTaskOption(path);
		}
		return new FileLinkerInputOption(Collections.singleton(ExecutionFileLocation.create(path)));
	}

	public static LinkerInputPassTaskOption valueOf(String path) {
		return valueOf(WildcardPath.valueOf(path));
	}

	public static LinkerInputPassTaskOption valueOf(SDKPathReference path) {
		return valueOf(SDKPathCollectionReference.valueOf(path));
	}

	public static LinkerInputPassTaskOption valueOf(SDKPathCollectionReference path) {
		return new LinkerInputPassTaskOption() {
			@Override
			public LinkerInputPassTaskOption clone() {
				return this;
			}

			@Override
			public LinkerInputPassOption toLinkerInputPassOption(TaskContext taskcontext) {
				return new LinkerInputPassOption() {
					@Override
					public void accept(Visitor visitor) {
						visitor.visit(path);
					}
				};
			}
		};
	}
}
