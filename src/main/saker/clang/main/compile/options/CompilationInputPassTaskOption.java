package saker.clang.main.compile.options;

import java.util.Collection;
import java.util.Collections;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.path.WildcardPath.ReducedWildcardPath;
import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileCollection;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.FileLocationTaskOption;
import saker.std.main.file.option.MultiFileLocationTaskOption;

public interface CompilationInputPassTaskOption {
	public default CompilationInputPassTaskOption clone() {
		return new OptionCompilationInputPassOption(this);
	}

	public CompilationInputPassOption toCompilationInputPassOption(TaskContext taskcontext);

	public default Collection<MultiFileLocationTaskOption> getFiles() {
		return null;
	}

	public static CompilationInputPassTaskOption valueOf(FileLocation filelocation) {
		FileLocationTaskOption.validateFileLocation(filelocation);
		return new FileCompilationInputFileOption(Collections.singleton(filelocation));
	}

	public static CompilationInputPassTaskOption valueOf(FileCollection files) {
		return new FileCompilationInputFileOption(ImmutableUtils.makeImmutableLinkedHashSet(files));
	}

	public static CompilationInputPassTaskOption valueOf(SakerPath path) {
		if (!path.isAbsolute()) {
			return new RelativePathFileCompilationInputFileOption(path);
		}
		return new FileCompilationInputFileOption(Collections.singleton(ExecutionFileLocation.create(path)));
	}

	public static CompilationInputPassTaskOption valueOf(WildcardPath path) {
		ReducedWildcardPath reduced = path.reduce();
		if (reduced.getWildcard() == null) {
			return valueOf(reduced.getFile());
		}
		return new WildcardFileCompilationInputFileOption(path);
	}

	public static CompilationInputPassTaskOption valueOf(String path) {
		return valueOf(WildcardPath.valueOf(path));
	}
}
