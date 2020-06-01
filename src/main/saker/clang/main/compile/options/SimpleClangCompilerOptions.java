package saker.clang.main.compile.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.main.file.option.FileLocationTaskOption;

public class SimpleClangCompilerOptions implements ClangCompilerOptions {
	private CompilationIdentifierTaskOption identifier;
	private List<String> language;
	private Collection<CompilationPathTaskOption> includeDirectories;
	private Map<String, SDKDescriptionTaskOption> sdks;
	private Map<String, String> macroDefinitions;
	private Collection<String> simpleParameters;
	private FileLocationTaskOption precompiledHeader;
	private Collection<CompilationPathTaskOption> forceInclude;

	public SimpleClangCompilerOptions(ClangCompilerOptions copy) {
		this.identifier = ObjectUtils.clone(copy.getIdentifier(), CompilationIdentifierTaskOption::clone);
		this.language = ImmutableUtils.makeImmutableList(copy.getLanguage());
		this.includeDirectories = ObjectUtils.cloneArrayList(copy.getIncludeDirectories(),
				CompilationPathTaskOption::clone);
		this.sdks = ObjectUtils.cloneTreeMap(copy.getSDKs(), Functionals.identityFunction(),
				SDKDescriptionTaskOption::clone);
		this.macroDefinitions = ObjectUtils.clone(copy.getMacroDefinitions(),
				ImmutableUtils::makeImmutableLinkedHashMap);
		this.simpleParameters = ImmutableUtils.makeImmutableList(copy.getSimpleCompilerParameters());
		this.precompiledHeader = ObjectUtils.clone(copy.getPrecompiledHeader(), FileLocationTaskOption::clone);
		this.forceInclude = ObjectUtils.cloneArrayList(copy.getForceInclude(), CompilationPathTaskOption::clone);
	}

	@Override
	public ClangCompilerOptions clone() {
		return this;
	}

	@Override
	public CompilationIdentifierTaskOption getIdentifier() {
		return identifier;
	}

	@Override
	public Collection<String> getLanguage() {
		return language;
	}

	@Override
	public Collection<CompilationPathTaskOption> getIncludeDirectories() {
		return includeDirectories;
	}

	@Override
	public Map<String, SDKDescriptionTaskOption> getSDKs() {
		return sdks;
	}

	@Override
	public Map<String, String> getMacroDefinitions() {
		return macroDefinitions;
	}

	@Override
	public Collection<String> getSimpleCompilerParameters() {
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
	public void accept(ClangCompilerOptions.Visitor visitor) {
		visitor.visit(this);
	}
}
