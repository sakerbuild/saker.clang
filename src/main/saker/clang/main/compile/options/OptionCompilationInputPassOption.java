package saker.clang.main.compile.options;

import java.util.Collection;
import java.util.Map;

import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.std.main.file.option.FileLocationTaskOption;
import saker.std.main.file.option.MultiFileLocationTaskOption;

public class OptionCompilationInputPassOption
		implements OptionCompilationInputPass, CompilationInputPassOption, CompilationInputPassTaskOption {
	private Collection<MultiFileLocationTaskOption> files;
	private Collection<CompilationPathTaskOption> includeDirectories;
	private CompilationIdentifierTaskOption subIdentifier;
	private Map<String, String> macroDefinitions;
	private Collection<String> simpleParameters;
	private String language;
	private FileLocationTaskOption precompiledHeader;
	private Collection<CompilationPathTaskOption> forceInclude;
	private Collection<ClangCompilerOptions> compilerOptions;

	public OptionCompilationInputPassOption(CompilationInputPassTaskOption copy) {
		this.files = ObjectUtils.cloneArrayList(copy.getFiles(), MultiFileLocationTaskOption::clone);
		this.includeDirectories = ObjectUtils.cloneArrayList(copy.getIncludeDirectories(),
				CompilationPathTaskOption::clone);
		this.subIdentifier = ObjectUtils.clone(copy.getSubIdentifier(), CompilationIdentifierTaskOption::clone);
		this.macroDefinitions = ImmutableUtils.makeImmutableNavigableMap(copy.getMacroDefinitions());
		this.simpleParameters = ImmutableUtils.makeImmutableList(copy.getSimpleParameters());
		this.compilerOptions = ObjectUtils.cloneArrayList(copy.getCompilerOptions(), ClangCompilerOptions::clone);
		this.language = copy.getLanguage();
		this.precompiledHeader = ObjectUtils.clone(copy.getPrecompiledHeader(), FileLocationTaskOption::clone);
		this.forceInclude = ObjectUtils.cloneArrayList(copy.getForceInclude(), CompilationPathTaskOption::clone);
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
	public String getLanguage() {
		return language;
	}

	@Override
	public Collection<CompilationPathTaskOption> getIncludeDirectories() {
		return includeDirectories;
	}

	@Override
	public Collection<ClangCompilerOptions> getCompilerOptions() {
		return compilerOptions;
	}

	@Override
	public CompilationIdentifierTaskOption getSubIdentifier() {
		return subIdentifier;
	}

	@Override
	public Map<String, String> getMacroDefinitions() {
		return macroDefinitions;
	}

	@Override
	public Collection<String> getSimpleParameters() {
		return simpleParameters;
	}

	@Override
	public FileLocationTaskOption getPrecompiledHeader() {
		return precompiledHeader;
	}

	@Override
	public Collection<CompilationPathTaskOption> getForceInclude() {
		return forceInclude;
	}

	@Override
	public CompilationInputPassOption toCompilationInputPassOption(TaskContext taskcontext) {
		return this;
	}

}
