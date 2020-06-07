package saker.clang.impl.link;

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

import saker.build.exception.FileMirroringUnavailableException;
import saker.build.file.DirectoryVisitPredicate;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.ProviderHolderPathKey;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.task.InnerTaskResultHolder;
import saker.build.task.InnerTaskResults;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskExecutionEnvironmentSelector;
import saker.build.task.TaskFactory;
import saker.build.task.exception.TaskEnvironmentSelectionFailedException;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.trace.BuildTrace;
import saker.clang.api.link.ClangLinkerWorkerTaskOutput;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.option.FileCompilationPathOption;
import saker.clang.impl.option.SimpleParameterOption;
import saker.clang.impl.util.ClangUtils;
import saker.clang.impl.util.CollectingProcessIOConsumer;
import saker.clang.main.link.ClangLinkTaskFactory;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKPathCollectionReference;
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

public class ClangLinkWorkerTaskFactory
		implements TaskFactory<ClangLinkerWorkerTaskOutput>, Task<ClangLinkerWorkerTaskOutput>, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final NavigableSet<String> WORKER_TASK_CAPABILITIES = ImmutableUtils
			.makeImmutableNavigableSet(new String[] { CAPABILITY_INNER_TASKS_COMPUTATIONAL });

	private Set<FileLocation> inputs;
	private Set<CompilationPathOption> libraryPath;
	private NavigableMap<String, SDKDescription> sdkDescriptions;
	private List<SimpleParameterOption> simpleParameters;

	private String binaryName;

	public void setInputs(Set<FileLocation> inputs) {
		this.inputs = inputs;
	}

	public void setLibraryPath(Set<CompilationPathOption> libraryPath) {
		this.libraryPath = libraryPath;
	}

	public void setBinaryName(String binaryname) {
		this.binaryName = binaryname;
	}

	public void setSdkDescriptions(NavigableMap<String, SDKDescription> sdkDescriptions) {
		ObjectUtils.requireComparator(sdkDescriptions, SDKSupportUtils.getSDKNameComparator());
		this.sdkDescriptions = sdkDescriptions;
		if (sdkDescriptions.get(ClangUtils.SDK_NAME_CLANG) == null) {
			throw new SDKNotFoundException(ClangUtils.SDK_NAME_CLANG + " SDK unspecified for compilation.");
		}
	}

	public void setSimpleParameters(List<SimpleParameterOption> simpleParameters) {
		if (simpleParameters == null) {
			this.simpleParameters = Collections.emptyList();
		} else {
			this.simpleParameters = ImmutableUtils.makeImmutableList(simpleParameters);
		}
	}

	@Override
	public ClangLinkerWorkerTaskOutput run(TaskContext taskcontext) throws Exception {
		if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
			BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_WORKER);
		}

		TaskIdentifier taskid = taskcontext.getTaskId();
		if (!(taskid instanceof ClangLinkWorkerTaskIdentifier)) {
			taskcontext.abortExecution(
					new IllegalStateException("Invalid task identifier for: " + this.getClass().getName()));
			return null;
		}
		ClangLinkWorkerTaskIdentifier workertaskid = (ClangLinkWorkerTaskIdentifier) taskid;
		CompilationIdentifier passcompilationidentifier = workertaskid.getPassIdentifier();
		String passidstr = passcompilationidentifier.toString();
		String displayid = ClangLinkTaskFactory.TASK_NAME + ":" + passidstr;
		taskcontext.setStandardOutDisplayIdentifier(displayid);

		if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
			BuildTrace.setDisplayInformation("clang.link:" + passidstr, displayid);
		}

		SakerDirectory outdir = SakerPathFiles.requireBuildDirectory(taskcontext)
				.getDirectoryCreate(ClangLinkTaskFactory.TASK_NAME).getDirectoryCreate(passidstr);

		SakerPath outdirpath = outdir.getSakerPath();

		TaskExecutionEnvironmentSelector envselector = SDKSupportUtils
				.getSDKBasedClusterExecutionEnvironmentSelector(sdkDescriptions.values());
		NavigableMap<String, SDKDescription> linkerinnertasksdkdescriptions;
		if (envselector != null) {
			EnvironmentSelectionResult envselectionresult;
			try {
				envselectionresult = taskcontext.getTaskUtilities().getReportExecutionDependency(
						SakerStandardUtils.createEnvironmentSelectionTestExecutionProperty(envselector));
			} catch (Exception e) {
				throw new TaskEnvironmentSelectionFailedException(
						"Failed to select a suitable build environment for linking.", e);
			}
			linkerinnertasksdkdescriptions = SDKSupportUtils.pinSDKSelection(envselectionresult, sdkDescriptions);
			envselector = SDKSupportUtils
					.getSDKBasedClusterExecutionEnvironmentSelector(linkerinnertasksdkdescriptions.values());
		} else {
			NavigableMap<String, SDKReference> resolvedsdks = SDKSupportUtils.resolveSDKReferences(taskcontext,
					sdkDescriptions);
			linkerinnertasksdkdescriptions = SDKSupportUtils.pinSDKSelection(sdkDescriptions, resolvedsdks);
		}

		int inputsize = inputs.size();
		System.out.println("Linking " + inputsize + " file" + (inputsize == 1 ? "" : "s") + ".");

		String binaryname = this.binaryName;
		if (ObjectUtils.isNullOrEmpty(binaryname)) {
			binaryname = passidstr;
		}

		LinkerInnerTaskFactory innertaskfactory = new LinkerInnerTaskFactory(envselector, inputs, libraryPath,
				linkerinnertasksdkdescriptions, simpleParameters, outdirpath, binaryname);
		InnerTaskResults<LinkerInnerTaskFactoryResult> innertaskresults = taskcontext.startInnerTask(innertaskfactory,
				null);
		InnerTaskResultHolder<LinkerInnerTaskFactoryResult> nextres = innertaskresults.getNext();
		if (nextres == null) {
			throw new RuntimeException("Failed to start linker task.");
		}
		LinkerInnerTaskFactoryResult innertaskresult = nextres.getResult();

		return new ClangLinkerWorkerTaskOutputImpl(passcompilationidentifier, innertaskresult.getOutputPath(),
				linkerinnertasksdkdescriptions);
	}

	@Override
	public Set<String> getCapabilities() {
		return WORKER_TASK_CAPABILITIES;
	}

	@Override
	public Task<? extends ClangLinkerWorkerTaskOutput> createTask(ExecutionContext executioncontext) {
		return this;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, inputs);
		SerialUtils.writeExternalCollection(out, libraryPath);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
		SerialUtils.writeExternalCollection(out, simpleParameters);
		out.writeObject(binaryName);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		inputs = SerialUtils.readExternalImmutableLinkedHashSet(in);
		libraryPath = SerialUtils.readExternalImmutableLinkedHashSet(in);
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
				SDKSupportUtils.getSDKNameComparator());
		simpleParameters = SerialUtils.readExternalImmutableList(in);
		binaryName = SerialUtils.readExternalObject(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((binaryName == null) ? 0 : binaryName.hashCode());
		result = prime * result + ((inputs == null) ? 0 : inputs.hashCode());
		result = prime * result + ((libraryPath == null) ? 0 : libraryPath.hashCode());
		result = prime * result + ((sdkDescriptions == null) ? 0 : sdkDescriptions.hashCode());
		result = prime * result + ((simpleParameters == null) ? 0 : simpleParameters.hashCode());
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
		ClangLinkWorkerTaskFactory other = (ClangLinkWorkerTaskFactory) obj;
		if (binaryName == null) {
			if (other.binaryName != null)
				return false;
		} else if (!binaryName.equals(other.binaryName))
			return false;
		if (inputs == null) {
			if (other.inputs != null)
				return false;
		} else if (!inputs.equals(other.inputs))
			return false;
		if (libraryPath == null) {
			if (other.libraryPath != null)
				return false;
		} else if (!libraryPath.equals(other.libraryPath))
			return false;
		if (sdkDescriptions == null) {
			if (other.sdkDescriptions != null)
				return false;
		} else if (!sdkDescriptions.equals(other.sdkDescriptions))
			return false;
		if (simpleParameters == null) {
			if (other.simpleParameters != null)
				return false;
		} else if (!simpleParameters.equals(other.simpleParameters))
			return false;
		return true;
	}

	private static final class ClangLinkerWorkerTaskOutputImpl implements ClangLinkerWorkerTaskOutput, Externalizable {
		private static final long serialVersionUID = 1L;

		private CompilationIdentifier compilationIdentifier;
		private SakerPath outputPath;
		private NavigableMap<String, SDKDescription> sdkDescriptions;

		/**
		 * For {@link Externalizable}.
		 */
		public ClangLinkerWorkerTaskOutputImpl() {
		}

		private ClangLinkerWorkerTaskOutputImpl(CompilationIdentifier passcompilationidentifier, SakerPath outputpath,
				NavigableMap<String, SDKDescription> sdkDescriptions) {
			this.compilationIdentifier = passcompilationidentifier;
			this.outputPath = outputpath;
			this.sdkDescriptions = ImmutableUtils.unmodifiableNavigableMap(sdkDescriptions);
		}

		@Override
		public SakerPath getOutputPath() {
			return outputPath;
		}

		@Override
		public CompilationIdentifier getIdentifier() {
			return compilationIdentifier;
		}

		@Override
		public Map<String, SDKDescription> getSDKs() {
			return sdkDescriptions;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(compilationIdentifier);
			out.writeObject(outputPath);
			SerialUtils.writeExternalMap(out, sdkDescriptions);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			compilationIdentifier = (CompilationIdentifier) in.readObject();
			outputPath = (SakerPath) in.readObject();
			sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
					SDKSupportUtils.getSDKNameComparator());
		}

		@Override
		public int hashCode() {
			return (compilationIdentifier == null) ? 0 : compilationIdentifier.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ClangLinkerWorkerTaskOutputImpl other = (ClangLinkerWorkerTaskOutputImpl) obj;
			if (compilationIdentifier == null) {
				if (other.compilationIdentifier != null)
					return false;
			} else if (!compilationIdentifier.equals(other.compilationIdentifier))
				return false;
			if (outputPath == null) {
				if (other.outputPath != null)
					return false;
			} else if (!outputPath.equals(other.outputPath))
				return false;
			if (sdkDescriptions == null) {
				if (other.sdkDescriptions != null)
					return false;
			} else if (!sdkDescriptions.equals(other.sdkDescriptions))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ (compilationIdentifier != null ? "compilationIdentifier=" + compilationIdentifier + ", " : "")
					+ (outputPath != null ? "outputPath=" + outputPath : "") + "]";
		}
	}

	private static class LinkerInnerTaskFactoryResult implements Externalizable {
		private static final long serialVersionUID = 1L;

		private SakerPath outputPath;

		/**
		 * For {@link Externalizable}.
		 */
		public LinkerInnerTaskFactoryResult() {
		}

		public LinkerInnerTaskFactoryResult(SakerPath outputPath) {
			this.outputPath = outputPath;
		}

		public SakerPath getOutputPath() {
			return outputPath;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(outputPath);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			outputPath = (SakerPath) in.readObject();
		}
	}

	private static class LinkerInnerTaskFactory
			implements TaskFactory<LinkerInnerTaskFactoryResult>, Task<LinkerInnerTaskFactoryResult>, Externalizable {
		private static final long serialVersionUID = 1L;

		private TaskExecutionEnvironmentSelector environmentSelector;
		private Set<FileLocation> inputs;
		private Set<CompilationPathOption> libraryPath;
		private NavigableMap<String, SDKDescription> sdkDescriptions;
		private List<SimpleParameterOption> simpleParameters;
		private SakerPath outDirectoryPath;
		private String binaryName;

		/**
		 * For {@link Externalizable}.
		 */
		public LinkerInnerTaskFactory() {
		}

		public LinkerInnerTaskFactory(TaskExecutionEnvironmentSelector environmentSelector, Set<FileLocation> inputs,
				Set<CompilationPathOption> libraryPath, NavigableMap<String, SDKDescription> sdkDescriptions,
				List<SimpleParameterOption> simpleParameters, SakerPath outdirpath, String binaryname) {
			this.environmentSelector = environmentSelector;
			this.inputs = inputs;
			this.libraryPath = libraryPath;
			this.sdkDescriptions = sdkDescriptions;
			this.simpleParameters = simpleParameters;
			this.outDirectoryPath = outdirpath;
			this.binaryName = binaryname;
		}

		@Override
		public LinkerInnerTaskFactoryResult run(TaskContext taskcontext) throws Exception {
			NavigableMap<SakerPath, ContentDescriptor> inputdescriptors = new TreeMap<>();

			Collection<Path> inputfilepaths = new LinkedHashSet<>();
			Collection<Path> libpaths = new LinkedHashSet<>();
			for (FileLocation inputfilelocation : inputs) {
				inputfilelocation.accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						SakerPath path = loc.getPath();
						SakerFile inputfile = taskcontext.getTaskUtilities().resolveFileAtPath(path);
						if (inputfile == null) {
							throw ObjectUtils
									.sneakyThrow(new FileNotFoundException("Linker input file not found: " + path));
						}
						inputdescriptors.put(path, inputfile.getContentDescriptor());
						try {
							inputfilepaths.add(taskcontext.mirror(inputfile));
						} catch (FileMirroringUnavailableException | NullPointerException | IOException e) {
							throw ObjectUtils.sneakyThrow(e);
						}
					}
				});
			}

			NavigableMap<String, SDKReference> sdks = SDKSupportUtils
					.resolveSDKReferences(taskcontext.getExecutionContext().getEnvironment(), sdkDescriptions);

			SakerEnvironment environment = taskcontext.getExecutionContext().getEnvironment();
			if (!ObjectUtils.isNullOrEmpty(this.libraryPath)) {
				for (CompilationPathOption libpathoption : this.libraryPath) {
					libpathoption.accept(new CompilationPathOption.Visitor() {
						@Override
						public void visit(FileCompilationPathOption libpath) {
							libpath.getFileLocation().accept(new FileLocationVisitor() {
								@Override
								public void visit(ExecutionFileLocation loc) {
									SakerPath path = loc.getPath();
									SakerDirectory dir = taskcontext.getTaskUtilities().resolveDirectoryAtPath(path);
									if (dir == null) {
										throw ObjectUtils.sneakyThrow(
												new FileNotFoundException("Library path directory not found: " + path));
									}

									NavigableMap<SakerPath, ContentDescriptor> dircontents = SakerPathFiles
											.toFileContentMap(dir.getFilesRecursiveByPath(path,
													DirectoryVisitPredicate.subFiles()));
									inputdescriptors.putAll(dircontents);

									try {
										libpaths.add(taskcontext.mirror(dir));
									} catch (FileMirroringUnavailableException | IOException e) {
										throw ObjectUtils.sneakyThrow(e);
									}
								}

								@Override
								public void visit(LocalFileLocation loc) {
									// TODO handle local file location input libpath
									FileLocationVisitor.super.visit(loc);
								}
							});
						}

						@Override
						public void visit(SDKPathCollectionReference libpath) {
							//XXX duplicated code with compiler worker
							try {
								Collection<SakerPath> paths = libpath.getValue(sdks);
								if (paths == null) {
									throw new SDKPathNotFoundException("No SDK library path found for: " + libpath);
								}
								for (SakerPath sdkdirpath : paths) {
									libpaths.add(LocalFileProvider.toRealPath(sdkdirpath));
								}
							} catch (SDKManagementException e) {
								throw e;
							} catch (Exception e) {
								throw new SDKPathNotFoundException(
										"Failed to retrieve library path from SDKs: " + libpath);
							}
						}
					});
				}
			}

			SDKReference clangsdk = SDKSupportUtils.requireSDK(sdks, ClangUtils.SDK_NAME_CLANG);
			String executable = ClangUtils.getClangExecutable(clangsdk);

			List<String> commands = new ArrayList<>();
			commands.add(executable);
			//input file paths added first as the ordering of arguments matter somewhy
			//we've encountered errors when the input files were added last
			for (Path inputpath : inputfilepaths) {
				commands.add(inputpath.toString());
			}
			ClangUtils.evaluateSimpleParameters(commands, simpleParameters, sdks);

			String outputfilename = binaryName;
			SakerPath outputexecpath = outDirectoryPath.resolve(outputfilename);

			if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
				BuildTrace.setDisplayInformation(outputexecpath.getFileName(), null);
			}

			Path outputmirrorpath = taskcontext.getExecutionContext().toMirrorPath(outputexecpath);
			LocalFileProvider.getInstance().createDirectories(outputmirrorpath.getParent());

			for (Path lpath : libpaths) {
				commands.add("-L");
				commands.add(lpath.toString());
			}

			commands.add("-o");
			commands.add(outputmirrorpath.toString());

			//use the output parent path as the working directory
			SakerPath workingdir = SakerPath.valueOf(outputmirrorpath.getParent());
			CollectingProcessIOConsumer stdoutcollector = new CollectingProcessIOConsumer();

			int procresult = ClangUtils.runClangProcess(environment, commands, workingdir, stdoutcollector, null, true);
			taskcontext.getStandardOut().write(stdoutcollector.getOutputBytes());
			if (procresult != 0) {
				throw new IOException("Failed to link: " + procresult + " (0x" + Integer.toHexString(procresult) + ")");
			}

			ProviderHolderPathKey outputpathkey = LocalFileProvider.getInstance().getPathKey(outputmirrorpath);
			taskcontext.invalidate(outputpathkey);

			SakerDirectory outdir = taskcontext.getTaskUtilities().resolveDirectoryAtPath(outDirectoryPath);

			SakerFile outputsakerfile = taskcontext.getTaskUtilities()
					.createProviderPathFile(outputexecpath.getFileName(), outputpathkey);
			outdir.add(outputsakerfile);
			outputsakerfile.synchronize();

			taskcontext.getTaskUtilities().reportInputFileDependency(LinkerFileTags.INPUT_FILE, inputdescriptors);
			taskcontext.reportOutputFileDependency(LinkerFileTags.OUTPUT_FILE, outputexecpath,
					outputsakerfile.getContentDescriptor());

			LinkerInnerTaskFactoryResult result = new LinkerInnerTaskFactoryResult(outputexecpath);

			if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
				BuildTrace.reportOutputArtifact(outputexecpath, BuildTrace.ARTIFACT_EMBED_DEFAULT);
			}

			return result;
		}

		@Override
		public Task<? extends LinkerInnerTaskFactoryResult> createTask(ExecutionContext executioncontext) {
			return this;
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

		@Override
		public int getRequestedComputationTokenCount() {
			return 1;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(environmentSelector);
			SerialUtils.writeExternalCollection(out, inputs);
			SerialUtils.writeExternalCollection(out, libraryPath);
			SerialUtils.writeExternalMap(out, sdkDescriptions);
			SerialUtils.writeExternalCollection(out, simpleParameters);
			out.writeObject(outDirectoryPath);
			out.writeObject(binaryName);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			environmentSelector = SerialUtils.readExternalObject(in);
			inputs = SerialUtils.readExternalImmutableLinkedHashSet(in);
			libraryPath = SerialUtils.readExternalImmutableLinkedHashSet(in);
			sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
					SDKSupportUtils.getSDKNameComparator());
			simpleParameters = SerialUtils.readExternalImmutableList(in);
			outDirectoryPath = SerialUtils.readExternalObject(in);
			binaryName = SerialUtils.readExternalObject(in);
		}
	}

}
