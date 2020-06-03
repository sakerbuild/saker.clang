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
package saker.clang.main.compile.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.clang.main.options.CompilationPathTaskOption;
import saker.clang.main.options.SimpleParameterTaskOption;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.std.main.file.option.FileLocationTaskOption;
import saker.std.main.file.option.MultiFileLocationTaskOption;

public interface OptionCompilationInputPass {
	public Collection<MultiFileLocationTaskOption> getFiles();

	public String getLanguage();

	public List<CompilationPathTaskOption> getIncludeDirectories();
	
	public Collection<ClangCompilerOptions> getCompilerOptions();

	public CompilationIdentifierTaskOption getSubIdentifier();

	public Map<String, String> getMacroDefinitions();

	public List<SimpleParameterTaskOption> getSimpleParameters();

	public FileLocationTaskOption getPrecompiledHeader();

	public List<CompilationPathTaskOption> getForceInclude();
}
