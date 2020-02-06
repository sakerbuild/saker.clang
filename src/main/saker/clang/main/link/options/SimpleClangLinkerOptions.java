package saker.clang.main.link.options;

import java.util.Collection;
import java.util.Map;

import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public class SimpleClangLinkerOptions implements ClangLinkerOptions {
	private CompilationIdentifierTaskOption identifier;
	private Collection<LinkerInputPassTaskOption> input;
	private Collection<CompilationPathTaskOption> libraryPath;
	private Map<String, SDKDescriptionTaskOption> sdks;
	private Collection<String> simpleParameters;

	public SimpleClangLinkerOptions() {
	}

	public SimpleClangLinkerOptions(ClangLinkerOptions copy) {
		this.identifier = ObjectUtils.clone(copy.getIdentifier(), CompilationIdentifierTaskOption::clone);
		this.input = ObjectUtils.cloneArrayList(copy.getLinkerInput(), LinkerInputPassTaskOption::clone);
		this.libraryPath = ObjectUtils.cloneArrayList(copy.getLibraryPath(), CompilationPathTaskOption::clone);
		this.sdks = ObjectUtils.cloneTreeMap(copy.getSDKs(), Functionals.identityFunction(),
				SDKDescriptionTaskOption::clone);
		this.simpleParameters = ObjectUtils.cloneArrayList(copy.getSimpleLinkerParameters());
	}

	@Override
	public void accept(ClangLinkerOptions.Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public CompilationIdentifierTaskOption getIdentifier() {
		return identifier;
	}

	@Override
	public Collection<LinkerInputPassTaskOption> getLinkerInput() {
		return input;
	}

	@Override
	public Collection<CompilationPathTaskOption> getLibraryPath() {
		return libraryPath;
	}

	@Override
	public Map<String, SDKDescriptionTaskOption> getSDKs() {
		return sdks;
	}

	@Override
	public Collection<String> getSimpleLinkerParameters() {
		return simpleParameters;
	}

	@Override
	public ClangLinkerOptions clone() {
		return this;
	}
}
