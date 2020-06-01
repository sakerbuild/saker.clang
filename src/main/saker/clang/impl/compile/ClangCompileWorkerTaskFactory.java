package saker.clang.impl.compile;

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import saker.build.exception.FileMirroringUnavailableException;
import saker.build.file.DirectoryVisitPredicate;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.PathKey;
import saker.build.file.path.ProviderHolderPathKey;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.file.provider.RootFileProviderKey;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.runtime.execution.SakerLog;
import saker.build.task.CommonTaskContentDescriptors;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.task.InnerTaskExecutionParameters;
import saker.build.task.InnerTaskResultHolder;
import saker.build.task.InnerTaskResults;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskDuplicationPredicate;
import saker.build.task.TaskExecutionEnvironmentSelector;
import saker.build.task.TaskExecutionUtilities;
import saker.build.task.TaskExecutionUtilities.MirroredFileContents;
import saker.build.task.TaskFactory;
import saker.build.task.TaskFileDeltas;
import saker.build.task.delta.DeltaType;
import saker.build.task.delta.FileChangeDelta;
import saker.build.task.exception.TaskEnvironmentSelectionFailedException;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.TaskUtils;
import saker.build.thirdparty.saker.rmi.annot.transfer.RMISerialize;
import saker.build.thirdparty.saker.rmi.annot.transfer.RMIWrap;
import saker.build.thirdparty.saker.rmi.connection.RMIVariables;
import saker.build.thirdparty.saker.rmi.io.RMIObjectInput;
import saker.build.thirdparty.saker.rmi.io.RMIObjectOutput;
import saker.build.thirdparty.saker.rmi.io.wrap.RMIWrapper;
import saker.build.thirdparty.saker.util.ConcurrentPrependAccumulator;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.ReflectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.build.thirdparty.saker.util.function.LazySupplier;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.ByteSink;
import saker.build.thirdparty.saker.util.io.DataInputUnsyncByteArrayInputStream;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import saker.build.trace.BuildTrace;
import saker.clang.impl.compile.CompilerState.CompiledFileState;
import saker.clang.impl.compile.CompilerState.PrecompiledHeaderState;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.option.FileCompilationPathOption;
import saker.clang.impl.util.ClangUtils;
import saker.clang.impl.util.CollectingProcessIOConsumer;
import saker.clang.impl.util.InnerTaskMirrorHandler;
import saker.clang.main.compile.ClangCompileTaskFactory;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.exc.SDKManagementException;
import saker.sdk.support.api.exc.SDKNotFoundException;
import saker.sdk.support.api.exc.SDKPathNotFoundException;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;
import saker.std.api.util.SakerStandardUtils;
import testing.saker.clang.TestFlag;

public class ClangCompileWorkerTaskFactory implements TaskFactory<Object>, Task<Object>, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final NavigableSet<String> WORKER_TASK_CAPABILITIES = ImmutableUtils
			.makeImmutableNavigableSet(new String[] { CAPABILITY_INNER_TASKS_COMPUTATIONAL });

	private static final String PRECOMPILED_HEADERS_SUBDIRECTORY_NAME = "pch";

	private static final Pattern CLANG_OUTPUT_DIAGNOSTIC_PATTERN = Pattern
			.compile("((?:[a-zA-Z]:)?[^:]+)(:([0-9]+):([0-9]+): *([^:]+): *(.+))");
	private static final int CLANG_OUTPUT_DIAGNOSTIC_GROUP_FILE = 1;
	private static final int CLANG_OUTPUT_DIAGNOSTIC_GROUP_FILE_REMAINING = 2;
	private static final int CLANG_OUTPUT_DIAGNOSTIC_GROUP_LINE = 3;
	private static final int CLANG_OUTPUT_DIAGNOSTIC_GROUP_COLUMN = 4;
	private static final int CLANG_OUTPUT_DIAGNOSTIC_GROUP_ERRORTYPE = 5;
	private static final int CLANG_OUTPUT_DIAGNOSTIC_GROUP_MESSAGE = 6;

	private Set<FileCompilationConfiguration> files;
	private NavigableMap<String, SDKDescription> sdkDescriptions;

	/**
	 * For {@link Externalizable}.
	 */
	public ClangCompileWorkerTaskFactory() {
	}

	public void setFiles(Set<FileCompilationConfiguration> files) {
		this.files = files;
	}

	public void setSdkDescriptions(NavigableMap<String, SDKDescription> sdkdescriptions) {
		ObjectUtils.requireComparator(sdkdescriptions, SDKSupportUtils.getSDKNameComparator());
		this.sdkDescriptions = sdkdescriptions;
		if (sdkdescriptions.get(ClangUtils.SDK_NAME_CLANG) == null) {
			throw new SDKNotFoundException(ClangUtils.SDK_NAME_CLANG + " SDK unspecified for compilation.");
		}
	}

	@Override
	public Set<String> getCapabilities() {
		return WORKER_TASK_CAPABILITIES;
	}

	@Override
	public Object run(TaskContext taskcontext) throws Exception {
		if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
			BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_WORKER);
		}

		TaskIdentifier taskid = taskcontext.getTaskId();
		if (!(taskid instanceof ClangCompileWorkerTaskIdentifier)) {
			taskcontext.abortExecution(
					new IllegalStateException("Invalid task identifier for: " + this.getClass().getName()));
			return null;
		}
		ClangCompileWorkerTaskIdentifier workertaskid = (ClangCompileWorkerTaskIdentifier) taskid;
		CompilationIdentifier passidentifier = workertaskid.getPassIdentifier();
		String passidstr = passidentifier.toString();
		String displayid = ClangCompileTaskFactory.TASK_NAME + ":" + passidstr;
		taskcontext.setStandardOutDisplayIdentifier(displayid);

		if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
			BuildTrace.setDisplayInformation("clang.compile:" + passidstr, displayid);
		}

		SakerDirectory outdir = SakerPathFiles.requireBuildDirectory(taskcontext)
				.getDirectoryCreate(ClangCompileTaskFactory.TASK_NAME).getDirectoryCreate(passidstr);
		SakerPath outdirpath = outdir.getSakerPath();

		TaskExecutionEnvironmentSelector envselector = SDKSupportUtils
				.getSDKBasedClusterExecutionEnvironmentSelector(sdkDescriptions.values());
		NavigableMap<String, SDKDescription> compilerinnertasksdkdescriptions = sdkDescriptions;
		EnvironmentSelectionResult envselectionresult;
		if (envselector != null) {
			try {
				envselectionresult = taskcontext.getTaskUtilities().getReportExecutionDependency(
						SakerStandardUtils.createEnvironmentSelectionTestExecutionProperty(envselector));
			} catch (Exception e) {
				throw new TaskEnvironmentSelectionFailedException(
						"Failed to select a suitable build environment for compilation.", e);
			}
			compilerinnertasksdkdescriptions = SDKSupportUtils.pinSDKSelection(envselectionresult, sdkDescriptions);
			envselector = SDKSupportUtils
					.getSDKBasedClusterExecutionEnvironmentSelector(compilerinnertasksdkdescriptions.values());
		} else {
			//TODO in this case we probably should report a dependency on the resolved sdks
			envselectionresult = null;
		}

		CompilerState prevoutput = taskcontext.getPreviousTaskOutput(CompilerState.class, CompilerState.class);
		Map<RootFileProviderKey, NavigableMap<SakerPath, PrecompiledHeaderState>> nprecompiledheaders = new ConcurrentHashMap<>();

		CompilerState nstate = new CompilerState();
		nstate.setSdkDescriptions(sdkDescriptions);
		nstate.setEnvironmentSelection(envselectionresult);
		nstate.setPrecompiledHeaders(nprecompiledheaders);

		NavigableMap<String, CompiledFileState> stateexecutioncompiledfiles = new TreeMap<>();
		List<FileCompilationConfiguration> compilationentries = new ArrayList<>(this.files);

		if (prevoutput != null) {
			for (Entry<RootFileProviderKey, NavigableMap<SakerPath, PrecompiledHeaderState>> entry : prevoutput
					.getPrecompiledHeaders().entrySet()) {
				nprecompiledheaders.put(entry.getKey(), new ConcurrentSkipListMap<>(entry.getValue()));
			}
			filterUnchangedPreviousFiles(taskcontext, compilationentries, stateexecutioncompiledfiles, prevoutput,
					nstate);
		}
		if (!compilationentries.isEmpty()) {
			int sccount = compilationentries.size();
			System.out.println("Compiling " + sccount + " source file" + (sccount == 1 ? "" : "s") + ".");
			ConcurrentPrependAccumulator<FileCompilationConfiguration> fileaccumulator = new ConcurrentPrependAccumulator<>(
					compilationentries);
			CompilationDuplicationPredicate duplicationpredicate = new CompilationDuplicationPredicate(fileaccumulator);

			InnerTaskExecutionParameters innertaskparams = new InnerTaskExecutionParameters();
			if (envselector != null) {
				innertaskparams.setClusterDuplicateFactor(compilationentries.size());
			}
			innertaskparams.setDuplicationPredicate(duplicationpredicate);

			WorkerTaskCoordinator coordinator = new WorkerTaskCoordinator() {
				@Override
				public void headerPrecompiled(CompilerInnerTaskResult result, PathKey outputpathkey,
						ContentDescriptor outputcontents) {
					CompilationDependencyInfo depinfo = result.getDependencyInfo();
					try {
						ByteSink stdout = taskcontext.getStandardOut();
						//write file name to signal progress, like MSVC
						//XXX locked print
						stdout.write(ByteArrayRegion
								.wrap((outputpathkey.getPath().getFileName() + "\n").getBytes(StandardCharsets.UTF_8)));
						stdout.write(depinfo.getProcessOutput());
					} catch (IOException e) {
						taskcontext.getTaskUtilities().reportIgnoredException(e);
					}
					SakerPath[] sourcefilepath = { null };
					result.compilationEntry.getProperties().fileLocation.accept(new FileLocationVisitor() {
						@Override
						public void visit(ExecutionFileLocation loc) {
							sourcefilepath[0] = loc.getPath();
						}
						//TODO handle local file location
					});
					nprecompiledheaders
							.computeIfAbsent(outputpathkey.getFileProviderKey(),
									Functionals.concurrentSkipListMapComputer())
							.put(outputpathkey.getPath(),
									new PrecompiledHeaderState(depinfo.getInputContents(), outputcontents,
											result.getCompilationEntry().getProperties(), depinfo.getIncludes(),
											depinfo.getProcessOutput()));
				}

				@Override
				public NavigableMap<SakerPath, PrecompiledHeaderState> getPrecompiledHeaderStates(
						RootFileProviderKey fpk) {
					return nprecompiledheaders.get(fpk);
				}

				@Override
				public FileCompilationConfiguration take() {
					if (duplicationpredicate.isAborted()) {
						return null;
					}
					return fileaccumulator.take();
				}

				@Override
				public void setAborted() {
					duplicationpredicate.setAborted();
				}
			};
			SourceCompilerInnerTaskFactory innertask = new SourceCompilerInnerTaskFactory(coordinator, outdirpath,
					compilerinnertasksdkdescriptions, envselector, outdir);
			InnerTaskResults<CompilerInnerTaskResult> innertaskresults = taskcontext.startInnerTask(innertask,
					innertaskparams);
			InnerTaskResultHolder<CompilerInnerTaskResult> resultholder;

			while ((resultholder = innertaskresults.getNext()) != null) {
				CompilerInnerTaskResult compilationresult = resultholder.getResult();
				if (compilationresult == null) {
					//may be if the inner task doesn't receive a compilation entry as there are no more
					//and returns prematurely
					continue;
				}
				FileCompilationConfiguration compilationentry = compilationresult.getCompilationEntry();
				CompilationDependencyInfo depinfo = compilationresult.getDependencyInfo();
				ByteArrayRegion procout = depinfo.getProcessOutput();
				FileLocation compiledfilelocation = compilationentry.getProperties().getFileLocation();
				//XXX locked print
				taskcontext.getStandardOut().write(ByteArrayRegion
						.wrap((ClangUtils.getFileName(compiledfilelocation) + '\n').getBytes(StandardCharsets.UTF_8)));
				if (!procout.isEmpty()) {
					taskcontext.println(procout.toString());
				}
				if (!compilationresult.isSuccessful()) {
					coordinator.setAborted();
				}
				compiledfilelocation.accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						CompiledFileState compiledfilestate = new CompiledFileState(depinfo.getInputContents(),
								compilationentry);
						compiledfilestate.setCompilerProcessOutput(depinfo.getProcessOutput());
						compiledfilestate.setIncludes(depinfo.getIncludes());
						compiledfilestate.setFailedIncludes(depinfo.getFailedIncludes());
						compiledfilestate.setSuccessful(compilationresult.isSuccessful());
						if (compilationresult.isSuccessful()) {
							String outputobjectfilename = compilationresult.getOutputObjectName();
							if (outputobjectfilename != null) {
								SakerPath outputpath = outdirpath.resolve(outputobjectfilename);
								SakerFile outfile = taskcontext.getTaskUtilities().resolveFileAtPath(outputpath);
								if (outfile == null) {
									throw ObjectUtils.sneakyThrow(new FileNotFoundException(
											"Output object file was not found: " + outdirpath));
								}
								ContentDescriptor outcontentdescriptor = outfile.getContentDescriptor();

								compiledfilestate.setObjectOutputContents(outputpath, outcontentdescriptor);
							}
						}
						stateexecutioncompiledfiles.put(compilationentry.getOutFileName(), compiledfilestate);
					}

					@Override
					public void visit(LocalFileLocation loc) {
						// TODO handle local input file result
						FileLocationVisitor.super.visit(loc);
					}
				});

			}
		}

		nstate.setExecutionCompiledFiles(stateexecutioncompiledfiles);

		NavigableSet<SakerPath> allreferencedincludes = nstate.getAllReferencedIncludes();
		NavigableSet<SakerPath> allfailedincludes = nstate.getAllReferencedFailedIncludes();
		NavigableMap<SakerPath, ContentDescriptor> includecontentdescriptors = new TreeMap<>();
		NavigableSet<String> includedfilenames = new TreeSet<>();
		for (SakerPath includepath : allreferencedincludes) {
			includedfilenames.add(includepath.getFileName());
			//XXX use a more efficient resolveFileAtPath algorithm
			SakerFile includefile = taskcontext.getTaskUtilities().resolveFileAtPath(includepath);
			if (includefile == null) {
				SakerLog.error().verbose().println("Included file no longer found: " + includepath);
				//report an IS_FILE dependency nonetheless, as that will trigger the reinvocation of the
				//compilation the next time.
				//this scenario should not happen at all generally.
				includecontentdescriptors.put(includepath, CommonTaskContentDescriptors.IS_FILE);
				continue;
			}
			includecontentdescriptors.put(includepath, includefile.getContentDescriptor());
		}
		for (SakerPath includepath : allfailedincludes) {
			ContentDescriptor prev = includecontentdescriptors.putIfAbsent(includepath,
					CommonTaskContentDescriptors.IS_NOT_FILE);
			if (prev != null) {
				SakerLog.error().verbose().println("Header referencing concurrency error. Referenced header: "
						+ includepath
						+ " had transient presence for compiled sources. It is recommended to clean the project.");
				if (TestFlag.ENABLED) {
					//this shouldn't happen during testing, so better throw an exception
					throw new AssertionError(
							"header concurrency error: " + includecontentdescriptors + " with " + prev);
				}
			}
		}

		NavigableSet<SakerPath> compiledfileparentdirectorypaths = new TreeSet<>();
		NavigableMap<SakerPath, ContentDescriptor> inputexecutionfilecontents = new TreeMap<>();
		for (CompiledFileState filestate : stateexecutioncompiledfiles.values()) {
			FileCompilationConfiguration compilationconfig = filestate.getCompilationConfiguration();
			compilationconfig.getProperties().getFileLocation().accept(new FileLocationVisitor() {

				@Override
				public void visit(ExecutionFileLocation loc) {
					compiledfileparentdirectorypaths.add(loc.getPath().getParent());
					inputexecutionfilecontents.put(loc.getPath(), filestate.getInputContents());
				}

				@Override
				public void visit(LocalFileLocation loc) {
					// TODO handle local input file dependencies
					FileLocationVisitor.super.visit(loc);
				}
			});

		}
		for (SakerPath includedirpath : compiledfileparentdirectorypaths) {
			reportAdditionDontCareDependenciesForFileNamesIncludeDirectory(taskcontext, includecontentdescriptors,
					includedfilenames, includedirpath);
		}
		for (CompiledFileState filestate : stateexecutioncompiledfiles.values()) {
			FileCompilationConfiguration compilationconfig = filestate.getCompilationConfiguration();
			Collection<CompilationPathOption> includedirs = compilationconfig.getProperties().getIncludeDirectories();
			if (!ObjectUtils.isNullOrEmpty(includedirs)) {
				for (CompilationPathOption includediroption : includedirs) {
					includediroption.accept(new CompilationPathOption.Visitor() {
						@Override
						public void visit(FileCompilationPathOption includedir) {
							includedir.getFileLocation().accept(new FileLocationVisitor() {
								@Override
								public void visit(ExecutionFileLocation loc) {
									SakerPath includedirexecutionpath = loc.getPath();
									if (compiledfileparentdirectorypaths.contains(includedirexecutionpath)) {
										//already reported
										return;
									}
									reportAdditionDontCareDependenciesForFileNamesIncludeDirectory(taskcontext,
											includecontentdescriptors, includedfilenames, includedirexecutionpath);
								}

								@Override
								public void visit(LocalFileLocation loc) {
									// TODO handle local include directory dependencies
									FileLocationVisitor.super.visit(loc);
								}
							});
						}

						@Override
						public void visit(SDKPathReference includedir) {
							//ignore dependency wise
						}
					});
				}
			}
		}
		taskcontext.getTaskUtilities().reportInputFileDependency(CompilationFileTags.INCLUDE_FILE,
				includecontentdescriptors);

		taskcontext.getTaskUtilities().reportInputFileDependency(CompilationFileTags.SOURCE,
				inputexecutionfilecontents);
		taskcontext.getTaskUtilities().reportOutputFileDependency(CompilationFileTags.OBJECT_FILE,
				nstate.getOutputObjectFileContentDescriptors());
		taskcontext.setTaskOutput(CompilerState.class, nstate);

		//remove files which are not part of the output object files
		ObjectUtils.iterateOrderedIterables(outdir.getChildren().entrySet(), nstate.getAllOutputFileNames(),
				(entry, name) -> entry.getKey().compareTo(name), (entry, outf) -> {
					if (outf == null) {
						entry.getValue().remove();
					}
				});
		//use the nothing predicate to only delete the files which were removed
		outdir.synchronize(new NothingKeepKnownDirectoryVisitPredicate());

		if (!nstate.isAllCompilationSucceeded()) {
			taskcontext.abortExecution(new IOException("Compilation failed."));
			return null;
		}

		return new ClangCompilerWorkerTaskOutputImpl(passidentifier, nstate.getOutputObjectFilePaths(),
				sdkDescriptions);
	}

	private static final class CompilationDuplicationPredicate implements TaskDuplicationPredicate {
		private final ConcurrentPrependAccumulator<FileCompilationConfiguration> compilationFiles;
		private boolean aborted;

		private CompilationDuplicationPredicate(
				ConcurrentPrependAccumulator<FileCompilationConfiguration> fileaccumulator) {
			this.compilationFiles = fileaccumulator;
		}

		@Override
		public boolean shouldInvokeOnceMore() throws RuntimeException {
			if (compilationFiles.isEmpty()) {
				return false;
			}
			return !aborted;
		}

		public void setAborted() {
			this.aborted = true;
		}

		public boolean isAborted() {
			return aborted;
		}

	}

	private static final class NothingKeepKnownDirectoryVisitPredicate implements DirectoryVisitPredicate {
		@Override
		public DirectoryVisitPredicate directoryVisitor(String arg0, SakerDirectory arg1) {
			return null;
		}

		@Override
		public boolean visitFile(String name, SakerFile file) {
			return false;
		}

		@Override
		public boolean visitDirectory(String name, SakerDirectory directory) {
			return false;
		}

		@Override
		public NavigableSet<String> getSynchronizeFilesToKeep() {
			//don't remove the pch subdir
			return ImmutableUtils.singletonNavigableSet(PRECOMPILED_HEADERS_SUBDIRECTORY_NAME);
		}
	}

	@Override
	public Task<? extends Object> createTask(ExecutionContext executioncontext) {
		return this;
	}

	private static void reportAdditionDontCareDependenciesForFileNamesIncludeDirectory(TaskContext taskcontext,
			NavigableMap<SakerPath, ContentDescriptor> includecontentdescriptors,
			NavigableSet<String> includedfilenames, SakerPath includedirexecutionpath) {
		if (ObjectUtils.isNullOrEmpty(includedfilenames)) {
			//don't need to report anything
			return;
		}
		FileNameRecursiveFileCollectionStrategy collectionstrategy = new FileNameRecursiveFileCollectionStrategy(
				includedirexecutionpath, includedfilenames);
		NavigableMap<SakerPath, SakerFile> foundsimilarfiles = taskcontext.getTaskUtilities()
				.collectFilesReportAdditionDependency(CompilationFileTags.INCLUDE_FILE, collectionstrategy);
		for (SakerPath similarfilepath : foundsimilarfiles.keySet()) {
			includecontentdescriptors.putIfAbsent(similarfilepath, CommonTaskContentDescriptors.DONT_CARE);
		}
	}

	private static void filterUnchangedPreviousFiles(TaskContext taskcontext,
			List<FileCompilationConfiguration> compilationentries,
			NavigableMap<String, CompiledFileState> stateexecutioncompiledfiles, CompilerState prevoutput,
			CompilerState nstate) throws IOException {
		//XXX sorted iteration for equals?
		if (!Objects.equals(nstate.getSdkDescriptions(), prevoutput.getSdkDescriptions())) {
			//different toolchains used, recompile all
			return;
		}
		if (!Objects.equals(nstate.getEnvironmentSelection(), prevoutput.getEnvironmentSelection())) {
			//different environments are used to compile the sources.
			//recompile all
			return;
		}

		TaskUtils.collectFilesForTags(taskcontext.getFileDeltas(),
				ImmutableUtils.asUnmodifiableArrayList(CompilationFileTags.SOURCE));
		TaskFileDeltas inputfilechanges = taskcontext.getFileDeltas(DeltaType.INPUT_FILE_CHANGE);
		TaskFileDeltas outputfilechanges = taskcontext.getFileDeltas(DeltaType.OUTPUT_FILE_CHANGE);

		TaskFileDeltas inputfileadditions = taskcontext.getFileDeltas(DeltaType.INPUT_FILE_ADDITION);

		NavigableSet<SakerPath> relevantchanges = new TreeSet<>();
		collectFileDeltaPaths(inputfilechanges.getFileDeltasWithTag(CompilationFileTags.SOURCE), relevantchanges);
		collectFileDeltaPaths(outputfilechanges.getFileDeltasWithTag(CompilationFileTags.OBJECT_FILE), relevantchanges);

		NavigableSet<SakerPath> includechanges = new TreeSet<>();
		collectFileDeltaPaths(inputfilechanges.getFileDeltasWithTag(CompilationFileTags.INCLUDE_FILE), includechanges);

		//compare using ignore-case, as if an include file was not found, we should trigger the recompilation
		//if a file with different casing is added
		NavigableSet<String> includeadditionfilenames = new TreeSet<>(String::compareToIgnoreCase);
		for (FileChangeDelta adddelta : inputfileadditions.getFileDeltas()) {
			includeadditionfilenames.add(adddelta.getFilePath().getFileName());
		}

		boolean[] hadfailure = { false };

		NavigableMap<String, CompiledFileState> prevcompiledfiles = new TreeMap<>(
				prevoutput.getExecutionCompiledFiles());
		for (Iterator<FileCompilationConfiguration> it = compilationentries.iterator(); it.hasNext();) {
			FileCompilationConfiguration compilationentry = it.next();
			String outfilename = compilationentry.getOutFileName();
			CompiledFileState prevfilestate = prevcompiledfiles.get(outfilename);
			if (prevfilestate == null) {
				//wasn't compiled previously, compile now
				continue;
			}
			if (!Objects.equals(prevfilestate.getCompilationConfiguration(), compilationentry)) {
				//the configuration for the compiled file changed
				continue;
			}
			SakerPath outobjpath = prevfilestate.getOutputObjectPath();
			if (outobjpath != null) {
				if (relevantchanges.contains(outobjpath)) {
					continue;
				}
			}

			if (isAnyIncludeRelatedChange(includechanges, includeadditionfilenames, prevfilestate.getIncludes())) {
				continue;
			}
			if (isAnyIncludeRelatedChange(includechanges, includeadditionfilenames,
					prevfilestate.getFailedIncludes())) {
				continue;
			}

			compilationentry.getProperties().getFileLocation().accept(new FileLocationVisitor() {
				@Override
				public void visit(ExecutionFileLocation loc) {
					SakerPath sourcefilepath = loc.getPath();
					if (relevantchanges.contains(sourcefilepath)) {
						return;
					}

					//no changes found, remove the file from the to-compile collection

					try {
						printDiagnostics(taskcontext, prevfilestate);
					} catch (NullPointerException | IOException e) {
						throw ObjectUtils.sneakyThrow(e);
					}
					stateexecutioncompiledfiles.put(outfilename, prevfilestate);

					if (!prevfilestate.isSuccessful()) {
						hadfailure[0] = true;
					}

					it.remove();
				}

				@Override
				public void visit(LocalFileLocation loc) {
					// TODO handle local input file changes
					FileLocationVisitor.super.visit(loc);
				}
			});
		}

		//check any deltas for the precompiled headers
		for (NavigableMap<SakerPath, PrecompiledHeaderState> pchs : nstate.getPrecompiledHeaders().values()) {
			for (Iterator<PrecompiledHeaderState> it = pchs.values().iterator(); it.hasNext();) {
				PrecompiledHeaderState pchstate = it.next();
				NavigableSet<SakerPath> pchincludes = pchstate.getIncludes();
				if (isAnyIncludeRelatedChange(includechanges, includeadditionfilenames, pchincludes)) {
					it.remove();
					continue;
				}
			}
		}
		if (hadfailure[0]) {
			//we've had a compilation failure from the previous run
			//for which the source file and any dependencies wasn't changed
			//the build task WILL FAIL
			//however, to improve incremental user experience we should
			//recompile the modified files, but not the ones which had no previous state
			for (Iterator<FileCompilationConfiguration> it = compilationentries.iterator(); it.hasNext();) {
				FileCompilationConfiguration compilationentry = it.next();
				String outfilename = compilationentry.getOutFileName();
				CompiledFileState prevfilestate = prevcompiledfiles.get(outfilename);
				if (prevfilestate == null) {
					//wasn't compiled previously, don't compile it now either
					it.remove();
					continue;
				}
			}
		}
	}

	private static void printDiagnostics(TaskContext taskcontext, CompiledFileState state)
			throws NullPointerException, IOException {
		ByteArrayRegion procout = state.getCompilerProcessOutput();
		if (procout == null || procout.isEmpty()) {
			return;
		}
		taskcontext.println(procout.toString());
	}

	protected static SakerPath getPrecompiledHeaderOutputDirectoryPath(SakerPath outputdirpath) {
		return outputdirpath.resolve(PRECOMPILED_HEADERS_SUBDIRECTORY_NAME);
	}

	private static void collectFileDeltaPaths(Collection<? extends FileChangeDelta> deltas,
			Collection<SakerPath> result) {
		if (ObjectUtils.isNullOrEmpty(deltas)) {
			return;
		}
		for (FileChangeDelta delta : deltas) {
			result.add(delta.getFilePath());
		}
	}

	private static boolean isAnyIncludeRelatedChange(NavigableSet<SakerPath> includechanges,
			NavigableSet<String> includeadditionfilenames, NavigableSet<SakerPath> prevstateincludes) {
		if (ObjectUtils.isNullOrEmpty(prevstateincludes)) {
			return false;
		}
		if (!includechanges.isEmpty()) {
			try {
				NavigableSet<SakerPath> prevstateincludessubset = prevstateincludes.subSet(includechanges.first(), true,
						includechanges.last(), true);
				ObjectUtils.iterateOrderedIterables(prevstateincludessubset, includechanges, (l, r) -> {
					if (l != null && r != null) {
						throw DeltaDetectedException.INSTANCE;
					}
				});
			} catch (DeltaDetectedException e) {
				//changes found in the included files
				return true;
			}
		}
		if (!includeadditionfilenames.isEmpty()) {
			for (SakerPath include : prevstateincludes) {
				if (includeadditionfilenames.contains(include.getFileName())) {
					//possible include resolution change
					return true;
				}
			}
		}
		return false;
	}

	private static class DeltaDetectedException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public static final DeltaDetectedException INSTANCE = new DeltaDetectedException();

		public DeltaDetectedException() {
			super(null, null, false, false);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, files);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		files = SerialUtils.readExternalImmutableLinkedHashSet(in);
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
				SDKSupportUtils.getSDKNameComparator());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((files == null) ? 0 : files.hashCode());
		result = prime * result + ((sdkDescriptions == null) ? 0 : sdkDescriptions.hashCode());
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
		ClangCompileWorkerTaskFactory other = (ClangCompileWorkerTaskFactory) obj;
		if (files == null) {
			if (other.files != null)
				return false;
		} else if (!files.equals(other.files))
			return false;
		if (sdkDescriptions == null) {
			if (other.sdkDescriptions != null)
				return false;
		} else if (!sdkDescriptions.equals(other.sdkDescriptions))
			return false;
		return true;
	}

	private static class CompilationDependencyInfo implements Externalizable {
		private static final long serialVersionUID = 1L;

		protected ContentDescriptor inputContents;
		protected NavigableSet<SakerPath> includes = new TreeSet<>();
		protected NavigableSet<SakerPath> failedIncludes = new TreeSet<>();
		//XXX this should not be here but only for compiled source files. no need for pch
		protected ByteArrayRegion processOutput = ByteArrayRegion.EMPTY;

		/**
		 * For {@link Externalizable}.
		 */
		public CompilationDependencyInfo() {
		}

		public CompilationDependencyInfo(ContentDescriptor inputContents) {
			this.inputContents = inputContents;
		}

		public ContentDescriptor getInputContents() {
			return inputContents;
		}

		public NavigableSet<SakerPath> getIncludes() {
			return includes;
		}

		public NavigableSet<SakerPath> getFailedIncludes() {
			return failedIncludes;
		}

		public ByteArrayRegion getProcessOutput() {
			return processOutput;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(inputContents);
			SerialUtils.writeExternalCollection(out, includes);
			SerialUtils.writeExternalCollection(out, failedIncludes);
			out.writeObject(processOutput);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			inputContents = (ContentDescriptor) in.readObject();
			includes = SerialUtils.readExternalSortedImmutableNavigableSet(in);
			failedIncludes = SerialUtils.readExternalSortedImmutableNavigableSet(in);
			processOutput = (ByteArrayRegion) in.readObject();
		}
	}

	public static class CompilerInnerTaskResult implements Externalizable {
		private static final long serialVersionUID = 1L;

		protected FileCompilationConfiguration compilationEntry;
		protected boolean successful;
		protected String outputObjectName;

		protected CompilationDependencyInfo dependencyInfo;

		/**
		 * For {@link Externalizable}.
		 */
		public CompilerInnerTaskResult() {
		}

		public CompilerInnerTaskResult(FileCompilationConfiguration compilationEntry) {
			this.compilationEntry = compilationEntry;
		}

		public static CompilerInnerTaskResult successful(FileCompilationConfiguration compilationEntry) {
			CompilerInnerTaskResult result = new CompilerInnerTaskResult(compilationEntry);
			result.successful = true;
			return result;
		}

		public static CompilerInnerTaskResult failed(FileCompilationConfiguration compilationEntry) {
			CompilerInnerTaskResult result = new CompilerInnerTaskResult(compilationEntry);
			result.successful = false;
			return result;
		}

		public boolean isSuccessful() {
			return successful;
		}

		public FileCompilationConfiguration getCompilationEntry() {
			return compilationEntry;
		}

		public String getOutputObjectName() {
			return outputObjectName;
		}

		public CompilationDependencyInfo getDependencyInfo() {
			return dependencyInfo;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(compilationEntry);
			out.writeBoolean(successful);
			out.writeObject(outputObjectName);
			out.writeObject(dependencyInfo);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			compilationEntry = (FileCompilationConfiguration) in.readObject();
			successful = in.readBoolean();
			outputObjectName = (String) in.readObject();
			dependencyInfo = (CompilationDependencyInfo) in.readObject();
		}
	}

	public interface WorkerTaskCoordinator {
		public static final Method METHOD_SET_ABORTED = ReflectUtils.getMethodAssert(WorkerTaskCoordinator.class,
				"setAborted");

		public void headerPrecompiled(@RMISerialize CompilerInnerTaskResult result, PathKey outputpathkey,
				@RMISerialize ContentDescriptor outputcontents);

		@RMISerialize
		public NavigableMap<SakerPath, PrecompiledHeaderState> getPrecompiledHeaderStates(
				@RMISerialize RootFileProviderKey fpk);

		public FileCompilationConfiguration take();

		public void setAborted();
	}

	private static class PrecompiledHeaderDependencyInfo {
		protected NavigableSet<SakerPath> includes;

		public PrecompiledHeaderDependencyInfo() {
			this.includes = new TreeSet<>();
		}

		public PrecompiledHeaderDependencyInfo(NavigableSet<SakerPath> includes) {
			this.includes = includes;
		}

		public PrecompiledHeaderDependencyInfo(CompilationDependencyInfo depinfo) {
			this.includes = depinfo.includes;
		}
	}

	@RMIWrap(SourceCompilerRMIWrapper.class)
	private static class SourceCompilerInnerTaskFactory
			implements TaskFactory<CompilerInnerTaskResult>, Task<CompilerInnerTaskResult> {
		protected WorkerTaskCoordinator coordinator;
		protected SakerPath outputDirPath;
		protected NavigableMap<String, SDKDescription> sdkDescriptions;
		protected TaskExecutionEnvironmentSelector environmentSelector;
		protected SakerDirectory outputDir;

		private transient InnerTaskMirrorHandler mirrorHandler = new InnerTaskMirrorHandler();
		private transient NavigableMap<String, Supplier<SDKReference>> referencedSDKCache = new ConcurrentSkipListMap<>(
				SDKSupportUtils.getSDKNameComparator());
		private transient NavigableMap<String, Object> sdkCacheLocks = new ConcurrentSkipListMap<>(
				SDKSupportUtils.getSDKNameComparator());

		private transient ConcurrentHashMap<FileCompilationConfiguration, Object> precompiledHeaderCreationLocks = new ConcurrentHashMap<>();
		private transient ConcurrentHashMap<FileCompilationConfiguration, Optional<PrecompiledHeaderDependencyInfo>> precompiledHeaderCreationResults = new ConcurrentHashMap<>();

		private transient final Supplier<NavigableMap<SakerPath, PrecompiledHeaderState>> precompiledHeaderStatesLazySupplier = LazySupplier
				.of(() -> {
					return coordinator.getPrecompiledHeaderStates(LocalFileProvider.getProviderKeyStatic());
				});

		/**
		 * For RMI transfer.
		 */
		public SourceCompilerInnerTaskFactory() {
		}

		public SourceCompilerInnerTaskFactory(WorkerTaskCoordinator coordinator, SakerPath outputDirPath,
				NavigableMap<String, SDKDescription> sdkDescriptions, TaskExecutionEnvironmentSelector envselector,
				SakerDirectory outputDir) {
			this.coordinator = coordinator;
			this.outputDirPath = outputDirPath;
			this.sdkDescriptions = sdkDescriptions;
			this.environmentSelector = envselector;
			this.outputDir = outputDir;
		}

		@Override
		public Task<? extends CompilerInnerTaskResult> createTask(ExecutionContext executioncontext) {
			return this;
		}

		@Override
		public int getRequestedComputationTokenCount() {
			return 1;
		}

		@Override
		public Set<String> getCapabilities() {
			if (environmentSelector != null) {
				return Collections.singleton(CAPABILITY_REMOTE_DISPATCHABLE);
			}
			return TaskFactory.super.getCapabilities();
		}

		@Override
		public TaskExecutionEnvironmentSelector getExecutionEnvironmentSelector() {
			if (environmentSelector != null) {
				return environmentSelector;
			}
			return TaskFactory.super.getExecutionEnvironmentSelector();
		}

		private static boolean isPrecompiledHeaderUpToDate(TaskContext taskcontext, PrecompiledHeaderState prevstate,
				ContentDescriptor currentcontents, Path outputpath, FileCompilationProperties entrypch) {
			if (prevstate == null) {
				return false;
			}
			if (!prevstate.getInputContents().equals(currentcontents)) {
				//the input header changed
				return false;
			}
			//the output was changed
			if (!prevstate.getOutputContents().equals(taskcontext.getExecutionContext()
					.getContentDescriptor(LocalFileProvider.getInstance().getPathKey(outputpath)))) {
				return false;
			}
			if (!entrypch.equals(prevstate.getCompilationProperties())) {
				return false;
			}
			return true;
		}

		@Override
		public CompilerInnerTaskResult run(TaskContext taskcontext) throws Exception {
			FileCompilationConfiguration compilationentry = coordinator.take();
			if (compilationentry == null) {
				return null;
			}
			FileCompilationProperties compilationentryproperties = compilationentry.getProperties();
			SakerPath outputdirpath = outputDirPath;
			ContentDescriptor[] contents = { null };
			TaskExecutionUtilities taskutilities = taskcontext.getTaskUtilities();
			ExecutionContext executioncontext = taskcontext.getExecutionContext();
			SakerEnvironment environment = executioncontext.getEnvironment();
			Path compilefilepath = getCompileFilePath(compilationentryproperties, environment, taskutilities, contents);

			if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
				BuildTrace.setDisplayInformation(compilefilepath.getFileName().toString(), null);
			}

			List<Path> includedirpaths = getIncludePaths(taskutilities, environment,
					compilationentryproperties.getIncludeDirectories(), true);
			List<Path> forceincludepaths = getIncludePaths(taskutilities, environment,
					compilationentryproperties.getForceInclude(), false);

			FileCompilationConfiguration compilationconfiguration = compilationentry;
			String outputfilenamebase = compilationconfiguration.getOutFileName();
			String outputobjectfilename = outputfilenamebase + ".o";
			String outputdepfilename = outputfilenamebase + ".dep";

			Path objoutpath = executioncontext.toMirrorPath(outputdirpath.resolve(outputobjectfilename));
			Path depfileoutpath = executioncontext.toMirrorPath(outputdirpath.resolve(outputdepfilename));

			//create the parent directory of the output
			LocalFileProvider localfp = LocalFileProvider.getInstance();
			localfp.createDirectories(objoutpath.getParent());

			SDKReference clangsdk = getSDKReferenceForName(environment, ClangUtils.SDK_NAME_CLANG);
			String executable = ClangUtils.getClangExecutable(clangsdk);

			String pchoutfilename = compilationentry.getPrecompiledHeaderOutFileName();
			Path pchoutpath = null;
			String pchname = null;
			PrecompiledHeaderDependencyInfo pchdepinfo = null;
			if (pchoutfilename != null) {
				FileCompilationProperties pchproperties = compilationentryproperties
						.withFileLocation(compilationconfiguration.getPrecompiledHeaderFileLocation());
				SakerPath pchoutdir = getPrecompiledHeaderOutputDirectoryPath(outputdirpath);
				pchname = ClangUtils.getFileName(pchproperties.getFileLocation());
				pchoutpath = executioncontext.toMirrorPath(pchoutdir.resolve(pchoutfilename + ".pch"));

				localfp.createDirectories(pchoutpath.getParent());

				FileCompilationConfiguration entrypch = new FileCompilationConfiguration(pchoutfilename, pchproperties);
				Optional<PrecompiledHeaderDependencyInfo> headerres = precompiledHeaderCreationResults.get(entrypch);
				if (headerres == null) {
					synchronized (precompiledHeaderCreationLocks.computeIfAbsent(entrypch,
							Functionals.objectComputer())) {
						headerres = precompiledHeaderCreationResults.get(entrypch);
						if (headerres == null) {
							ContentDescriptor[] pchcontents = { null };
							Path pchcompilefilepath = getCompileFilePath(pchproperties, environment, taskutilities,
									pchcontents);

							NavigableMap<SakerPath, PrecompiledHeaderState> precompiledheaderstates = precompiledHeaderStatesLazySupplier
									.get();
							SakerPath pchcompilesakerfilepath = SakerPath.valueOf(pchcompilefilepath);
							PrecompiledHeaderState prevheaderstate = ObjectUtils.getMapValue(precompiledheaderstates,
									pchcompilesakerfilepath);
							if (isPrecompiledHeaderUpToDate(taskcontext, prevheaderstate, pchcontents[0],
									pchcompilefilepath, pchproperties)) {
								headerres = Optional
										.of(new PrecompiledHeaderDependencyInfo(prevheaderstate.getIncludes()));
							} else {
								Path pchdepfileoutpath = executioncontext
										.toMirrorPath(outputdirpath.resolve(outputdepfilename));
								List<String> commands = new ArrayList<>();
								commands.add(executable);
								//compile only
								commands.add("-c");
								commands.addAll(pchproperties.getSimpleParameters());
								addLanguageHeaderCommandLineOption(pchproperties.getLanguage(), commands);
								commands.add(pchcompilefilepath.toString());
								commands.add("-o");
								commands.add(pchoutpath.toString());
								addIncludeCommands(commands, includedirpaths);
								addForceIncludeCommands(commands, forceincludepaths);
								addMacroDefinitionCommands(commands, pchproperties.getMacroDefinitions());
								// -MMD			Write a depfile containing user headers
								// -MD			Write a depfile containing user and system headers
								//use -MMD as we're not interested in system headers
								commands.add("-MMD");
								// -MF <file>	Write depfile output from -MMD, -MD, -MM, or -M to <file>
								commands.add("-MF");
								commands.add(pchdepfileoutpath.toString());

								CollectingProcessIOConsumer stdoutcollector = new CollectingProcessIOConsumer();
								//use the output parent path as the working directory
								SakerPath workingdir = SakerPath.valueOf(pchoutpath.getParent());
								int procresult = ClangUtils.runClangProcess(environment, commands, workingdir,
										stdoutcollector, null, true);
								CompilationDependencyInfo depinfo = new CompilationDependencyInfo(pchcontents[0]);
								pchproperties.getFileLocation().accept(new FileLocationVisitor() {
									//add the compiled header file as an include dependency, so it is added to the source files
									@Override
									public void visit(ExecutionFileLocation loc) {
										depinfo.includes.add(loc.getPath());
									}
									//TODO handle local precompiled header
								});
								analyzeClangOutput(taskcontext, includedirpaths, stdoutcollector.getOutputBytes(),
										depinfo, procresult, pchdepfileoutpath, pchoutpath, pchcompilefilepath, null);
								CompilerInnerTaskResult headerprecompileresult;
								if (procresult == 0) {
									headerprecompileresult = CompilerInnerTaskResult.successful(entrypch);

									headerres = Optional.of(new PrecompiledHeaderDependencyInfo(depinfo));
								} else {
									headerprecompileresult = CompilerInnerTaskResult.failed(entrypch);

									headerres = Optional.empty();
								}
								headerprecompileresult.dependencyInfo = depinfo;
								coordinator.headerPrecompiled(headerprecompileresult,
										LocalFileProvider.getPathKeyStatic(pchcompilesakerfilepath),
										taskcontext.getExecutionContext()
												.getContentDescriptor(localfp.getPathKey(pchcompilesakerfilepath)));
							}
							precompiledHeaderCreationResults.put(entrypch, headerres);
							//clear the force include paths as they are part of the precompiled header
							//and they shouldn't be included in the source files
							forceincludepaths = Collections.emptyList();
						}
					}
				}
				if (!headerres.isPresent()) {
					//TODO reify exception
					throw new IOException("Failed to compile required precompiled header. (" + pchname + ")");
				}
				pchdepinfo = headerres.get();
			}

			List<String> commands = new ArrayList<>();
			commands.add(executable);
			//compile only
			commands.add("-c");
			commands.addAll(compilationentryproperties.getSimpleParameters());
			addLanguageCommandLineOption(compilationentryproperties.getLanguage(), commands);
			commands.add(compilefilepath.toString());
			commands.add("-o");
			commands.add(objoutpath.toString());
			addIncludeCommands(commands, includedirpaths);
			addForceIncludeCommands(commands, forceincludepaths);
			addMacroDefinitionCommands(commands, compilationentryproperties.getMacroDefinitions());
			// -MMD			Write a depfile containing user headers
			// -MD			Write a depfile containing user and system headers
			//use -MMD as we're not interested in system headers
			commands.add("-MMD");
			// -MF <file>	Write depfile output from -MMD, -MD, -MM, or -M to <file>
			commands.add("-MF");
			commands.add(depfileoutpath.toString());
			if (pchoutpath != null) {
				commands.add("-include-pch");
				commands.add(pchoutpath.toString());
			}

			CollectingProcessIOConsumer stdoutcollector = new CollectingProcessIOConsumer();
			//use the output parent path as the working directory
			SakerPath workingdir = SakerPath.valueOf(objoutpath.getParent());
			int procresult = ClangUtils.runClangProcess(environment, commands, workingdir, stdoutcollector, null, true);
			CompilationDependencyInfo depinfo = new CompilationDependencyInfo(contents[0]);

			ByteArrayRegion stdoutputbytes = stdoutcollector.getOutputBytes();

			analyzeClangOutput(taskcontext, includedirpaths, stdoutputbytes, depinfo, procresult, depfileoutpath,
					objoutpath, compilefilepath, pchoutpath);

			if (pchdepinfo != null) {
				//no need to add failed includes, as if the pch compilation fails, the source file doesn't get compiled
				depinfo.includes.addAll(pchdepinfo.includes);
			}

			CompilerInnerTaskResult result;
			if (procresult != 0) {
				result = CompilerInnerTaskResult.failed(compilationentry);
				RMIVariables.invokeRemoteMethodAsyncOrLocal(coordinator, WorkerTaskCoordinator.METHOD_SET_ABORTED);
			} else {
				ProviderHolderPathKey objoutpathkey = localfp.getPathKey(objoutpath);
				taskutilities.addSynchronizeInvalidatedProviderPathFileToDirectory(outputDir, objoutpathkey,
						outputobjectfilename);
				result = CompilerInnerTaskResult.successful(compilationentry);
			}

			result.outputObjectName = outputobjectfilename;
			result.dependencyInfo = depinfo;

			return result;
		}

		private static int getPathEndIndexFromInFileIncludedFromLine(String line) {
			//get the path from a line that starts with "In file included from ".
			// e.g. In file included from main.cpp:1:
			int colonidx = line.indexOf(':', 22);
			if (colonidx < 0) {
				return -1;
			}
			if (colonidx == 23) {
				//possibly 
				//In file included from c:/windows/path.cpp:1:
				int ncolonidx = line.indexOf(':', 24);
				if (ncolonidx < 0) {
					return colonidx;
				}
				colonidx = ncolonidx;
			}
			return colonidx;
		}

		private static void analyzeClangOutput(TaskContext taskcontext, List<Path> includedirpaths,
				ByteArrayRegion stdoutputbytes, CompilationDependencyInfo depinfo, int procresult, Path depfileoutpath,
				Path outputpath, Path compilefilepath, Path pchpath) throws IOException {
			NavigableSet<SakerPath> failedincludes = depinfo.failedIncludes;
			NavigableSet<SakerPath> includes = depinfo.includes;
			ExecutionContext executioncontext = taskcontext.getExecutionContext();

			boolean includeerror = false;
			if (stdoutputbytes.isEmpty()) {
				if (procresult != 0) {
					depinfo.processOutput = ByteArrayRegion.wrap(("error: clang exited with error code: " + procresult
							+ " (0x" + Integer.toHexString(procresult) + ")").getBytes(StandardCharsets.UTF_8));
					//failed to execute clang.
					//don't continue analyzing, skip include dependency analysis
					return;
				}
			} else {
				Matcher errmatcher = null;
				try (UnsyncByteArrayOutputStream diagbaos = new UnsyncByteArrayOutputStream(
						stdoutputbytes.getLength())) {
					try (DataInputUnsyncByteArrayInputStream reader = new DataInputUnsyncByteArrayInputStream(
							stdoutputbytes)) {
						for (String line; (line = reader.readLine()) != null;) {
							if (line.isEmpty()) {
								continue;
							}
							if (errmatcher == null) {
								errmatcher = CLANG_OUTPUT_DIAGNOSTIC_PATTERN.matcher(line);
							} else {
								errmatcher.reset(line);
							}
							if (line.startsWith("In file included from ")) {
								int pathendidx = getPathEndIndexFromInFileIncludedFromLine(line);
								if (pathendidx > 22) {
									SakerPath wd = taskcontext.getTaskWorkingDirectoryPath();
									String file = line.substring(22, pathendidx);
									try {
										Path diagpath = Paths.get(file);
										SakerPath execpath = executioncontext.toUnmirrorPath(diagpath);
										if (execpath != null) {
											if (!compilefilepath.equals(diagpath)) {
												includes.add(execpath);
											}
											if (execpath.startsWith(wd)) {
												file = wd.relativize(execpath).toString();
											} else {
												file = execpath.toString();
											}
										}
									} catch (Exception e) {
										SakerLog.error().verbose()
												.println("Failed to parse clang path: " + e + " for " + file);
									}
									diagbaos.write(("In file included from " + file + line.substring(pathendidx) + "\n")
											.getBytes(StandardCharsets.UTF_8));
									continue;
								}
							}
							if (errmatcher.matches()) {
								String file = errmatcher.group(CLANG_OUTPUT_DIAGNOSTIC_GROUP_FILE);
								String msg = errmatcher.group(CLANG_OUTPUT_DIAGNOSTIC_GROUP_MESSAGE);
								SakerPath wd = taskcontext.getTaskWorkingDirectoryPath();
								try {
									Path diagpath = Paths.get(file);
									SakerPath execpath = executioncontext.toUnmirrorPath(diagpath);
									if (execpath != null) {
										if (execpath.startsWith(wd)) {
											file = wd.relativize(execpath).toString();
										} else {
											file = execpath.toString();
										}
									}
								} catch (Exception e) {
									SakerLog.error().verbose()
											.println("Failed to parse clang path: " + e + " for " + file);
								}

								//check not found header files
								//in format
								//     main.cpp:5:10: fatal error: 'second/nonexist.h' file not found
								//     main.cpp:5:10: fatal error: 'second/nonexist.h' file not found [category, id, others...]
								//these errors may be prefixed by one or multiple lines of the following:
								//     In file included from main.cpp:1:
								//     In file included from ./first.h:1:
								//     ./second.h:1:10: fatal error: 'third.h' file not found
								if (!msg.isEmpty() && msg.charAt(0) == '\'') {
									int nfidx = msg.indexOf("' file not found");
									if (nfidx > 0) {
										includeerror = true;
										String notfoundfilepathstr = msg.substring(1, nfidx);
										Path notfoundpath = Paths.get(notfoundfilepathstr);
										if (notfoundpath.isAbsolute()) {
											SakerPath unmirrored = executioncontext.toUnmirrorPath(notfoundpath);
											if (unmirrored != null) {
												if (failedincludes == null) {
													failedincludes = new TreeSet<>();
												}
												failedincludes.add(unmirrored);
											} else {
												//TODO handle local missing include
											}
										} else {
											for (Path includedirpath : includedirpaths) {
												Path notfoundabspath = includedirpath.resolve(notfoundpath);
												SakerPath unmirrored = executioncontext.toUnmirrorPath(notfoundabspath);
												if (unmirrored != null) {
													if (failedincludes == null) {
														failedincludes = new TreeSet<>();
													}
													failedincludes.add(unmirrored);
												} else {
													//TODO handle local missing include
												}
											}
										}
									}
								}

								diagbaos.write(file.getBytes(StandardCharsets.UTF_8));
								diagbaos.write(errmatcher.group(CLANG_OUTPUT_DIAGNOSTIC_GROUP_FILE_REMAINING)
										.getBytes(StandardCharsets.UTF_8));
							} else {
								diagbaos.write(line.getBytes(StandardCharsets.UTF_8));
							}
							diagbaos.write('\n');
						}
					}
					depinfo.processOutput = diagbaos.toByteArrayRegion();
				}
			}
			if (!includeerror) {
				//if there was an include error, clang doesn't create a dependency file
				// in that case don't attempt to read and parse it, as it may be a leftover from previous compilation
				// and could contain incorrect dependency information
				try {
					List<Path> dependencypaths;
					try (Stream<String> depfilelinestream = Files.lines(depfileoutpath)) {
						dependencypaths = getDependencyFileDependencies(depfilelinestream.iterator());
					}
					dependencypaths.remove(outputpath);
					dependencypaths.remove(compilefilepath);
					if (pchpath != null) {
						dependencypaths.remove(pchpath);
					}
					for (Path reallocalpath : dependencypaths) {
						try {
							SakerPath unmirrored = executioncontext.toUnmirrorPath(reallocalpath);
							if (unmirrored != null) {
								includes.add(unmirrored);
							} else {
								//TODO handle non mirrored included path
							}
						} catch (InvalidPathException e) {
							SakerLog.error().verbose().println(
									"Failed to determine included file path for: " + reallocalpath + " (" + e + ")");
							continue;
						}
					}
				} catch (IOException e) {
					//the dependency file may not exist if the preprocessing failed
					//we can ignore this exception
				}
			}
		}

		private static List<Path> getDependencyFileDependencies(Iterator<String> lineit) {
			//expected format:
//			/home/user/temp/build/saker.clang.compile/default/main.cpp.o: \
//			  /home/user/temp/main.cpp /home/user/temp/main.h \
//			  /home/user/temp/second\ header.h

			List<Path> result = new ArrayList<>();
			StringBuilder sb = new StringBuilder();
			outer:
			while (lineit.hasNext()) {
				//trim preceeding whitespace
				String l = lineit.next().trim();
				sb.setLength(0);

				int linelen = l.length();
				for (int i = 0; i < linelen; i++) {
					char c = l.charAt(i);
					if (c == '\\') {
						if (i + 1 == linelen) {
							//go to next line
							//the path string builder should be empty
							continue outer;
						}
						char nc = l.charAt(i + 1);
						if (nc == ' ') {
							sb.append(' ');
							++i;
							continue;
						}
						sb.append('\\');
						sb.append(nc);
						++i;
						//if clang is invoked in windows (e.g. Android NDK)
						//then it can produce backslashes in regular paths
						//e.g.
						//c:\the\path\to\source.cpp: \
						//    etc...

						//so we continue instead of throwing an exception
						continue;
//						throw new IllegalArgumentException("Unrecognized escaped character in dependency file: " + nc
//								+ " as 0x" + Integer.toHexString(nc) + " in line: " + l + " at index: " + i);
					}
					if (c == ' ') {
						//next file is coming up
						if (sb.length() > 0) {
							result.add(Paths.get(sb.toString()));
							sb.setLength(0);
						}
						continue;
					}
					if (c == ':') {
						if (i + 1 == linelen) {
							//: at the end of line. finish the path and continue
							if (sb.length() > 0) {
								result.add(Paths.get(sb.toString()));
								sb.setLength(0);
							}
							//break line char iteration
							break;
						}
						if (l.charAt(i + 1) == ' ') {
							//: in the line, and next is a space
							//finish the path and continue
							if (sb.length() > 0) {
								result.add(Paths.get(sb.toString()));
								sb.setLength(0);
							}
							++i;
							continue;
						}
						//: in the line, and has a valid char next.
						sb.append(c);
						continue;
					}
					sb.append(c);
				}
				//reached end of line without any signal to continue
				//line probably didn't end with \
				break;
			}
			//add the last path of the last line if present
			if (sb.length() > 0) {
				result.add(Paths.get(sb.toString()));
				sb.setLength(0);
			}
			return result;
		}

		private static void addForceIncludeCommands(List<String> commands, List<Path> forceincludepaths) {
			if (ObjectUtils.isNullOrEmpty(forceincludepaths)) {
				return;
			}
			for (Path fipath : forceincludepaths) {
				commands.add("-include");
				commands.add(fipath.toString());
			}
		}

		private static void addIncludeCommands(List<String> commands, List<Path> includedirpaths) {
			if (ObjectUtils.isNullOrEmpty(includedirpaths)) {
				return;
			}
			for (Path incpath : includedirpaths) {
				commands.add("-I");
				commands.add(incpath.toString());
			}
		}

		private static void addMacroDefinitionCommands(List<String> commands, Map<String, String> macrodefs) {
			if (ObjectUtils.isNullOrEmpty(macrodefs)) {
				return;
			}
			for (Entry<String, String> entry : macrodefs.entrySet()) {
				String val = entry.getValue();
				commands.add("-D");
				commands.add(entry.getKey() + (ObjectUtils.isNullOrEmpty(val) ? "" : "=" + val));
			}
		}

		private static void addLanguageCommandLineOption(String language, List<String> commands) {
			if ("c++".equalsIgnoreCase(language)) {
				commands.add("-x");
				commands.add("c++");
				return;
			}
			if ("objc++".equalsIgnoreCase(language)) {
				commands.add("-x");
				commands.add("objective-c++");
				return;
			}
			if ("objc".equalsIgnoreCase(language)) {
				commands.add("-x");
				commands.add("objective-c");
				return;
			}
			if (language == null || "c".equalsIgnoreCase(language)) {
				commands.add("-x");
				commands.add("c");
				return;
			}
			throw new IllegalArgumentException("Unknown language: " + language);
		}

		private static void addLanguageHeaderCommandLineOption(String language, List<String> commands) {
			if ("c++".equalsIgnoreCase(language)) {
				commands.add("-x");
				commands.add("c++-header");
				return;
			}
			if ("objc++".equalsIgnoreCase(language)) {
				commands.add("-x");
				commands.add("objective-c++-header");
				return;
			}
			if ("objc".equalsIgnoreCase(language)) {
				commands.add("-x");
				commands.add("objective-c-header");
				return;
			}
			if (language == null || "c".equalsIgnoreCase(language)) {
				commands.add("-x");
				commands.add("c-header");
				return;
			}
			throw new IllegalArgumentException("Unknown language: " + language);
		}

		private Path getCompileFilePath(FileCompilationProperties compilationentry, SakerEnvironment environment,
				TaskExecutionUtilities taskutilities, ContentDescriptor[] contents) {
			Path[] compilefilepath = { null };
			compilationentry.getFileLocation().accept(new FileLocationVisitor() {
				@Override
				public void visit(ExecutionFileLocation loc) {
					SakerPath path = loc.getPath();
					if (TestFlag.ENABLED) {
						TestFlag.metric().compiling(path, environment);
					}
					try {
						MirroredFileContents mirrorres = mirrorHandler.mirrorFile(taskutilities, path);
						compilefilepath[0] = mirrorres.getPath();
						contents[0] = mirrorres.getContents();
					} catch (FileMirroringUnavailableException | IOException e) {
						throw ObjectUtils.sneakyThrow(e);
					}
				}

				@Override
				public void visit(LocalFileLocation loc) {
					// TODO handle local input file
					FileLocationVisitor.super.visit(loc);
				}
			});
			return compilefilepath[0];
		}

		private List<Path> getIncludePaths(TaskExecutionUtilities taskutilities, SakerEnvironment environment,
				Collection<CompilationPathOption> includeoptions, boolean directories) {
			if (ObjectUtils.isNullOrEmpty(includeoptions)) {
				return Collections.emptyList();
			}
			List<Path> includepaths = new ArrayList<>();
			if (!ObjectUtils.isNullOrEmpty(includeoptions)) {
				for (CompilationPathOption incopt : includeoptions) {
					Path incpath = getIncludePath(taskutilities, environment, incopt, directories);
					includepaths.add(incpath);
				}
			}
			return includepaths;
		}

		private Path getIncludePath(TaskExecutionUtilities taskutilities, SakerEnvironment environment,
				CompilationPathOption includediroption, boolean directories) {
			Path[] includepath = { null };
			includediroption.accept(new CompilationPathOption.Visitor() {
				@Override
				public void visit(FileCompilationPathOption includedir) {
					includedir.getFileLocation().accept(new FileLocationVisitor() {
						@Override
						public void visit(ExecutionFileLocation loc) {
							SakerPath path = loc.getPath();
							try {
								if (directories) {
									includepath[0] = mirrorHandler.mirrorDirectory(taskutilities, path);
								} else {
									//XXX handle mirrored force include contents?
									includepath[0] = mirrorHandler.mirrorFile(taskutilities, path).getPath();
								}
							} catch (FileMirroringUnavailableException | IOException e) {
								throw ObjectUtils.sneakyThrow(e);
							}
						}

						@Override
						public void visit(LocalFileLocation loc) {
							// TODO handle local include directory
							FileLocationVisitor.super.visit(loc);
						}
					});
				}

				@Override
				public void visit(SDKPathReference includedir) {
					//XXX duplicated code with linker worker
					String sdkname = includedir.getSDKName();
					if (ObjectUtils.isNullOrEmpty(sdkname)) {
						throw new SDKPathNotFoundException("Include directory returned empty sdk name: " + includedir);
					}
					SDKReference sdkref = getSDKReferenceForName(environment, sdkname);
					if (sdkref == null) {
						throw new SDKPathNotFoundException("SDK configuration not found for name: " + sdkname
								+ " required by include directory: " + includedir);
					}
					try {
						SakerPath sdkdirpath = includedir.getPath(sdkref);
						if (sdkdirpath == null) {
							throw new SDKPathNotFoundException("No SDK include directory found for: " + includedir
									+ " in SDK: " + sdkname + " as " + sdkref);
						}
						includepath[0] = LocalFileProvider.toRealPath(sdkdirpath);
					} catch (Exception e) {
						throw new SDKPathNotFoundException("Failed to retrieve SDK include directory for: " + includedir
								+ " in SDK: " + sdkname + " as " + sdkref, e);
					}
				}
			});
			return includepath[0];
		}

		//XXX somewhat duplicated with linker worker factory
		private SDKReference getSDKReferenceForName(SakerEnvironment environment, String sdkname) {
			Supplier<SDKReference> sdkref = referencedSDKCache.get(sdkname);
			if (sdkref != null) {
				return sdkref.get();
			}
			synchronized (sdkCacheLocks.computeIfAbsent(sdkname, Functionals.objectComputer())) {
				sdkref = referencedSDKCache.get(sdkname);
				if (sdkref != null) {
					return sdkref.get();
				}
				SDKDescription desc = sdkDescriptions.get(sdkname);
				if (desc == null) {
					sdkref = () -> {
						throw new SDKNotFoundException("SDK not found for name: " + sdkname);
					};
				} else {
					try {
						SDKReference refresult = SDKSupportUtils.resolveSDKReference(environment, desc);
						sdkref = Functionals.valSupplier(refresult);
					} catch (Exception e) {
						sdkref = () -> {
							throw new SDKManagementException("Failed to resolve SDK: " + sdkname + " as " + desc, e);
						};
					}
				}
				referencedSDKCache.put(sdkname, sdkref);
			}
			return sdkref.get();
		}
	}

	protected final static class SourceCompilerRMIWrapper implements RMIWrapper {
		private SourceCompilerInnerTaskFactory task;

		public SourceCompilerRMIWrapper() {
		}

		public SourceCompilerRMIWrapper(SourceCompilerInnerTaskFactory task) {
			this.task = task;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			out.writeRemoteObject(task.coordinator);
			out.writeObject(task.outputDirPath);
			out.writeSerializedObject(task.sdkDescriptions);
			out.writeSerializedObject(task.environmentSelector);
			out.writeRemoteObject(task.outputDir);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			task = new SourceCompilerInnerTaskFactory();
			task.coordinator = (WorkerTaskCoordinator) in.readObject();
			task.outputDirPath = (SakerPath) in.readObject();
			task.sdkDescriptions = (NavigableMap<String, SDKDescription>) in.readObject();
			task.environmentSelector = (TaskExecutionEnvironmentSelector) in.readObject();
			task.outputDir = (SakerDirectory) in.readObject();
		}

		@Override
		public Object resolveWrapped() {
			return task;
		}

		@Override
		public Object getWrappedObject() {
			throw new UnsupportedOperationException();
		}
	}
}
