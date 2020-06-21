package saker.clang.main.options;

import saker.clang.impl.option.SimpleParameterOption;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;
import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKPropertyCollectionReference;
import saker.sdk.support.api.SDKPropertyReference;

@NestTypeInformation(qualifiedName = "ClangSimpleParameter")
@NestInformation("Simple option that is directly passed to the given tool on the command line.")
public class SimpleParameterTaskOption {
	private SimpleParameterOption parameter;

	public SimpleParameterTaskOption(SimpleParameterOption parameter) {
		this.parameter = parameter;
	}

	public SimpleParameterOption getParameter() {
		return parameter;
	}

	public static SimpleParameterTaskOption valueOf(String input) {
		return new SimpleParameterTaskOption(SimpleParameterOption.create(input));
	}

	public static SimpleParameterTaskOption valueOf(SDKPathCollectionReference input) {
		return new SimpleParameterTaskOption(SimpleParameterOption.create(input));
	}

	public static SimpleParameterTaskOption valueOf(SDKPropertyCollectionReference input) {
		return new SimpleParameterTaskOption(SimpleParameterOption.create(input));
	}

	public static SimpleParameterTaskOption valueOf(SDKPathReference input) {
		return new SimpleParameterTaskOption(SimpleParameterOption.create(input));
	}

	public static SimpleParameterTaskOption valueOf(SDKPropertyReference input) {
		return new SimpleParameterTaskOption(SimpleParameterOption.create(input));
	}
}
