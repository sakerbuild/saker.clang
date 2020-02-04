package saker.clang.main.compile.options;

import java.util.Collection;

import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.std.main.file.option.MultiFileLocationTaskOption;

public class OptionCompilationInputPassOption
		implements OptionCompilationInputPass, CompilationInputPassOption, CompilationInputPassTaskOption {
	private Collection<MultiFileLocationTaskOption> files;

	public OptionCompilationInputPassOption(CompilationInputPassTaskOption copy) {
		this.files = ObjectUtils.cloneArrayList(copy.getFiles(), MultiFileLocationTaskOption::clone);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public CompilationInputPassTaskOption clone() {
		return this;
	}

	@Override
	public Collection<MultiFileLocationTaskOption> getFiles() {
		return files;
	}

	@Override
	public CompilationInputPassOption toCompilationInputPassOption(TaskContext taskcontext) {
		return this;
	}

}
