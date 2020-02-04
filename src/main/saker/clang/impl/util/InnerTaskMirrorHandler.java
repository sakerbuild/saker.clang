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
package saker.clang.impl.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentSkipListMap;

import saker.build.file.SakerFile;
import saker.build.file.content.DirectoryContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.task.TaskExecutionUtilities;
import saker.build.task.TaskExecutionUtilities.MirroredFileContents;
import saker.build.thirdparty.saker.util.function.Functionals;

/**
 * Class for caching mirrored paths, in order to avoid multiple mirroring requests for clustered inner task invocations.
 * <p>
 * Serves as a common cache for inner tasks invoked on a given cluster.
 */
public class InnerTaskMirrorHandler {
	private ConcurrentSkipListMap<SakerPath, Object> mirrorLocks = new ConcurrentSkipListMap<>();
	private ConcurrentSkipListMap<SakerPath, MirroredFileContents> mirrorResults = new ConcurrentSkipListMap<>();

	public MirroredFileContents mirrorFile(TaskExecutionUtilities taskutils, SakerPath path) throws IOException {
		MirroredFileContents result = mirrorResults.get(path);
		if (result != null) {
			return result;
		}
		synchronized (mirrorLocks.computeIfAbsent(path, Functionals.objectComputer())) {
			result = mirrorResults.get(path);
			if (result != null) {
				return result;
			}
			result = taskutils.mirrorFileAtPathContents(path);
			mirrorResults.put(path, result);
			return result;
		}
	}

	public Path mirrorDirectory(TaskExecutionUtilities taskutils, SakerPath path) throws IOException {
		MirroredFileContents result = mirrorResults.get(path);
		if (result != null) {
			return result.getPath();
		}
		synchronized (mirrorLocks.computeIfAbsent(path, Functionals.objectComputer())) {
			result = mirrorResults.get(path);
			if (result != null) {
				return result.getPath();
			}
			Path mirrorpath = taskutils.mirrorDirectoryAtPath(path, null);
			SakerFile file = taskutils.resolveDirectoryAtAbsolutePath(path);
			if (file == null) {
				throw new FileNotFoundException("Directory not found at path: " + path);
			}
			result = new MirroredFileContents(mirrorpath, DirectoryContentDescriptor.INSTANCE);
			mirrorResults.put(path, result);
			return result.getPath();
		}
	}

}
