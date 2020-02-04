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
package testing.saker.clang;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import saker.build.file.path.SakerPath;
import saker.build.runtime.environment.SakerEnvironment;

public interface ClangTestMetric {
	public default int runProcess(List<String> command, boolean mergestderr, MetricProcessIOConsumer stdoutconsumer,
			MetricProcessIOConsumer stderrconsumer) throws IOException {
		throw new UnsupportedOperationException();
	}

	public default void compiling(SakerPath path, SakerEnvironment environment) {
	}

	public default String getClangVersionString(String clangExe) {
		throw new UnsupportedOperationException();
	}

	@FunctionalInterface
	public interface MetricProcessIOConsumer {
		public void handleOutput(ByteBuffer bytes) throws IOException;
	}

}
