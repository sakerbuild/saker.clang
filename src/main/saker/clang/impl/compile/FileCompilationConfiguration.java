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
package saker.clang.impl.compile;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.std.api.file.location.FileLocation;

public class FileCompilationConfiguration implements Externalizable {
	private static final long serialVersionUID = 1L;

	private String outFileName;
	private FileCompilationProperties properties;
	private FileLocation precompiledHeaderFileLocation;
	private String precompiledHeaderOutFileName;

	/**
	 * For {@link Externalizable}.
	 */
	public FileCompilationConfiguration() {
	}

	public FileCompilationConfiguration(String outFileName, FileCompilationProperties properties) {
		this.outFileName = outFileName;
		this.properties = properties;
	}

	/**
	 * Returns the base output file name.
	 */
	public String getOutFileName() {
		return outFileName;
	}

	public FileCompilationProperties getProperties() {
		return properties;
	}

	public String getPrecompiledHeaderOutFileName() {
		return precompiledHeaderOutFileName;
	}

	public FileLocation getPrecompiledHeaderFileLocation() {
		return precompiledHeaderFileLocation;
	}

	public void setPrecompiledHeader(FileLocation filelocation, String precompiledHeaderOutFileName) {
		this.precompiledHeaderFileLocation = filelocation;
		this.precompiledHeaderOutFileName = precompiledHeaderOutFileName;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(outFileName);
		out.writeObject(properties);
		out.writeObject(precompiledHeaderFileLocation);
		out.writeObject(precompiledHeaderOutFileName);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		outFileName = (String) in.readObject();
		properties = (FileCompilationProperties) in.readObject();
		precompiledHeaderFileLocation = (FileLocation) in.readObject();
		precompiledHeaderOutFileName = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((outFileName == null) ? 0 : outFileName.hashCode());
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
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
		FileCompilationConfiguration other = (FileCompilationConfiguration) obj;
		if (outFileName == null) {
			if (other.outFileName != null)
				return false;
		} else if (!outFileName.equals(other.outFileName))
			return false;
		if (precompiledHeaderFileLocation == null) {
			if (other.precompiledHeaderFileLocation != null)
				return false;
		} else if (!precompiledHeaderFileLocation.equals(other.precompiledHeaderFileLocation))
			return false;
		if (precompiledHeaderOutFileName == null) {
			if (other.precompiledHeaderOutFileName != null)
				return false;
		} else if (!precompiledHeaderOutFileName.equals(other.precompiledHeaderOutFileName))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileCompilationConfiguration[" + (outFileName != null ? "outFileName=" + outFileName : "") + "]";
	}

}