package saker.clang.main.compile.options;

import java.util.Collection;
import java.util.Map;

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public class SimpleClangCompilerOptions implements ClangCompilerOptions {
	private CompilationIdentifierTaskOption identifier;
	private String language;
	private Collection<CompilationPathTaskOption> includeDirectories;
	private String architecture;
	private Map<String, SDKDescriptionTaskOption> sdks;
	private Map<String, String> macroDefinitions;
	private Collection<String> simpleParameters;

	public SimpleClangCompilerOptions(ClangCompilerOptions copy) {
		this.identifier = ObjectUtils.clone(copy.getIdentifier(), CompilationIdentifierTaskOption::clone);
		this.language = copy.getLanguage();
		this.includeDirectories = ObjectUtils.cloneArrayList(copy.getIncludeDirectories(),
				CompilationPathTaskOption::clone);
		this.architecture = copy.getArchitecture();
		this.sdks = ObjectUtils.cloneTreeMap(copy.getSDKs(), Functionals.identityFunction(),
				SDKDescriptionTaskOption::clone);
		this.macroDefinitions = ObjectUtils.clone(copy.getMacroDefinitions(),
				ImmutableUtils::makeImmutableLinkedHashMap);
		this.simpleParameters = ImmutableUtils.makeImmutableList(copy.getSimpleCompilerParameters());
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
	public String getLanguage() {
		return language;
	}

	@Override
	public Collection<CompilationPathTaskOption> getIncludeDirectories() {
		return includeDirectories;
	}

	@Override
	public String getArchitecture() {
		return architecture;
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
	public void accept(ClangCompilerOptions.Visitor visitor) {
		visitor.visit(this);
	}
}
