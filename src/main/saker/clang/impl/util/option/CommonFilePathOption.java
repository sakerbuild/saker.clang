package saker.clang.impl.util.option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.clang.impl.compile.option.FileIncludePath;
import saker.clang.impl.compile.option.IncludePathOption;
import saker.clang.impl.link.option.FileLibraryPath;
import saker.clang.impl.link.option.LibraryPathOption;
import saker.std.api.file.location.FileLocation;

public class CommonFilePathOption
		implements FileLibraryPath, FileIncludePath, IncludePathOption, LibraryPathOption, Externalizable {
	private static final long serialVersionUID = 1L;

	private FileLocation fileLocation;

	/**
	 * For {@link Externalizable}.
	 */
	public CommonFilePathOption() {
	}

	public CommonFilePathOption(FileLocation fileLocation) {
		this.fileLocation = fileLocation;
	}

	@Override
	public void accept(LibraryPathOption.Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void accept(IncludePathOption.Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public FileLocation getFileLocation() {
		return fileLocation;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		fileLocation = SerialUtils.readExternalObject(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileLocation == null) ? 0 : fileLocation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommonFilePathOption other = (CommonFilePathOption) obj;
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (fileLocation != null ? "fileLocation=" + fileLocation : "") + "]";
	}
}