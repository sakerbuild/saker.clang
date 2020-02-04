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
package saker.clang.main.link.options;

import saker.build.task.TaskContext;
import saker.clang.api.compile.ClangCompilerWorkerTaskOutput;

class CompilerOutputLinkerInputOption
		implements CompilerOutputLinkerInputPass, LinkerInputPassTaskOption, LinkerInputPassOption {
	private ClangCompilerWorkerTaskOutput compilerOutput;

	public CompilerOutputLinkerInputOption(ClangCompilerWorkerTaskOutput compilerOutput) {
		this.compilerOutput = compilerOutput;
	}

	@Override
	public ClangCompilerWorkerTaskOutput getCompilerOutput() {
		return compilerOutput;
	}

	@Override
	public LinkerInputPassTaskOption clone() {
		return this;
	}

	@Override
	public LinkerInputPassOption toLinkerInputPassOption(TaskContext taskcontext) {
		return this;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
}
