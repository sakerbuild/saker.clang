package saker.clang.impl.compile;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.option.SimpleParameterOption;
import saker.std.api.file.location.FileLocation;

public final class FileCompilationProperties implements Externalizable {
	private static final long serialVersionUID = 1L;

	protected FileLocation fileLocation;
	protected String language;
	//store include directories as a list, as their order matters
	protected List<CompilationPathOption> includeDirectories;
	protected List<CompilationPathOption> forceInclude;
	protected Map<String, String> macroDefinitions;
	/**
	 * The simple parameters are stored in a list as their order may matter
	 */
	protected List<SimpleParameterOption> simpleParameters = Collections.emptyList();

	/**
	 * For {@link Externalizable}.
	 */
	public FileCompilationProperties() {
	}

	public FileCompilationProperties(FileLocation fileLocation) {
		this.fileLocation = fileLocation;
	}

	public FileCompilationProperties(FileCompilationProperties copy) {
		this.fileLocation = copy.fileLocation;
		copyFrom(copy);
	}

	public FileCompilationProperties withFileLocation(FileLocation fl) {
		FileCompilationProperties result = new FileCompilationProperties(fl);
		result.copyFrom(this);
		return result;
	}

	public void copyFrom(FileCompilationProperties config) {
		this.language = config.language;
		this.includeDirectories = config.includeDirectories;
		this.forceInclude = config.forceInclude;
		this.macroDefinitions = config.macroDefinitions;
		this.simpleParameters = config.simpleParameters;
	}

	public void setIncludeDirectories(Collection<? extends CompilationPathOption> includeDirectories) {
		if (ObjectUtils.isNullOrEmpty(includeDirectories)) {
			this.includeDirectories = null;
		} else {
			this.includeDirectories = ImmutableUtils
					.makeImmutableList(ImmutableUtils.makeImmutableLinkedHashSet(includeDirectories));
		}
	}

	public void setForceInclude(Collection<? extends CompilationPathOption> includeDirectories) {
		if (ObjectUtils.isNullOrEmpty(includeDirectories)) {
			this.forceInclude = null;
		} else {
			this.forceInclude = ImmutableUtils
					.makeImmutableList(ImmutableUtils.makeImmutableLinkedHashSet(includeDirectories));
		}
	}

	public void setSimpleParameters(List<SimpleParameterOption> simpleParameters) {
		if (simpleParameters == null) {
			this.simpleParameters = Collections.emptyList();
		} else {
			this.simpleParameters = ImmutableUtils.makeImmutableList(simpleParameters);
		}
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public FileLocation getFileLocation() {
		return fileLocation;
	}

	public String getLanguage() {
		return language;
	}

	public List<CompilationPathOption> getIncludeDirectories() {
		return includeDirectories;
	}

	public Map<String, String> getMacroDefinitions() {
		return macroDefinitions;
	}

	public List<SimpleParameterOption> getSimpleParameters() {
		return simpleParameters;
	}

	public List<CompilationPathOption> getForceInclude() {
		return forceInclude;
	}

	public void setMacroDefinitions(Map<String, String> macroDefinitions) {
		if (ObjectUtils.isNullOrEmpty(macroDefinitions)) {
			this.macroDefinitions = null;
		} else {
			this.macroDefinitions = macroDefinitions;
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
		out.writeObject(language);
		SerialUtils.writeExternalCollection(out, includeDirectories);
		SerialUtils.writeExternalCollection(out, forceInclude);
		SerialUtils.writeExternalMap(out, macroDefinitions);
		SerialUtils.writeExternalCollection(out, simpleParameters);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		fileLocation = (FileLocation) in.readObject();
		language = (String) in.readObject();
		includeDirectories = SerialUtils.readExternalImmutableList(in);
		forceInclude = SerialUtils.readExternalImmutableList(in);
		macroDefinitions = SerialUtils.readExternalImmutableLinkedHashMap(in);
		simpleParameters = SerialUtils.readExternalImmutableList(in);
	}

	@Override
	public int hashCode() {
		//keep hashcode simple
		return ((fileLocation == null) ? 0 : fileLocation.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileCompilationProperties other = (FileCompilationProperties) obj;
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		if (forceInclude == null) {
			if (other.forceInclude != null)
				return false;
		} else if (!forceInclude.equals(other.forceInclude))
			return false;
		if (includeDirectories == null) {
			if (other.includeDirectories != null)
				return false;
		} else if (!includeDirectories.equals(other.includeDirectories))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (macroDefinitions == null) {
			if (other.macroDefinitions != null)
				return false;
		} else if (!macroDefinitions.equals(other.macroDefinitions))
			return false;
		if (simpleParameters == null) {
			if (other.simpleParameters != null)
				return false;
		} else if (!simpleParameters.equals(other.simpleParameters))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileCompilationProperties[" + (fileLocation != null ? "fileLocation=" + fileLocation + ", " : "")
				+ (language != null ? "language=" + language + ", " : "")
				+ (includeDirectories != null ? "includeDirectories=" + includeDirectories + ", " : "")
				+ (macroDefinitions != null ? "macroDefinitions=" + macroDefinitions + ", " : "")
				+ (simpleParameters != null ? "simpleParameters=" + simpleParameters : "") + "]";
	}

}
