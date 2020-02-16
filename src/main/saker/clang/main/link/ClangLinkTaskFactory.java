package saker.clang.main.link;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import saker.build.file.path.SakerPath;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.SimpleStructuredObjectTaskResult;
import saker.build.task.utils.annot.SakerInput;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.trace.BuildTrace;
import saker.clang.api.compile.ClangCompilerWorkerTaskOutput;
import saker.clang.impl.link.ClangLinkWorkerTaskFactory;
import saker.clang.impl.link.ClangLinkWorkerTaskIdentifier;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.util.ClangUtils;
import saker.clang.main.compile.ClangCompileTaskFactory;
import saker.clang.main.link.options.ClangLinkerOptions;
import saker.clang.main.link.options.CompilerOutputLinkerInputPass;
import saker.clang.main.link.options.FileLinkerInputPass;
import saker.clang.main.link.options.LinkerInputPassOption;
import saker.clang.main.link.options.LinkerInputPassTaskOption;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.compiler.utils.api.CompilerUtils;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.exc.SDKNameConflictException;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;

public class ClangLinkTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.clang.link";

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<Object>() {
			@SakerInput(value = { "", "Input" }, required = true)
			public Collection<LinkerInputPassTaskOption> inputOption;

			@SakerInput(value = "Identifier")
			public CompilationIdentifierTaskOption identifierOption;

			@SakerInput(value = "LibraryPath")
			public Collection<CompilationPathTaskOption> libraryPathOption;

			@SakerInput(value = { "SDKs" })
			public Map<String, SDKDescriptionTaskOption> sdksOption;

			@SakerInput(value = "SimpleParameters")
			public Collection<String> simpleParametersOption;

			@SakerInput(value = { "LinkerOptions" })
			public Collection<ClangLinkerOptions> linkerOptionsOption;

			@Override
			public Object run(TaskContext taskcontext) throws Exception {
				if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
					BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_FRONTEND);
				}

				Collection<LinkerInputPassTaskOption> inputtaskoptions = new ArrayList<>();
				Collection<ClangLinkerOptions> linkeroptions = new ArrayList<>();
				Collection<CompilationPathTaskOption> libpathoptions = new ArrayList<>();
				Map<String, SDKDescriptionTaskOption> sdkoptions = new TreeMap<>(
						SDKSupportUtils.getSDKNameComparator());
				List<String> simpleparameters = ObjectUtils.newArrayList(this.simpleParametersOption);
				CompilationIdentifierTaskOption identifieropt = ObjectUtils.clone(this.identifierOption,
						CompilationIdentifierTaskOption::clone);

				CompilationIdentifier optionidentifier = CompilationIdentifierTaskOption.getIdentifier(identifieropt);

				if (!ObjectUtils.isNullOrEmpty(this.inputOption)) {
					for (LinkerInputPassTaskOption inputtaskoption : this.inputOption) {
						if (inputtaskoption == null) {
							continue;
						}
						inputtaskoptions.add(inputtaskoption.clone());
					}
				}
				if (ObjectUtils.isNullOrEmpty(inputtaskoptions)) {
					taskcontext.abortExecution(new IllegalArgumentException("No inputs specified for linking."));
					return null;
				}
				if (!ObjectUtils.isNullOrEmpty(this.linkerOptionsOption)) {
					for (ClangLinkerOptions linkeropt : this.linkerOptionsOption) {
						if (linkeropt == null) {
							continue;
						}
						linkeroptions.add(linkeropt.clone());
					}
				}
				if (!ObjectUtils.isNullOrEmpty(this.libraryPathOption)) {
					for (CompilationPathTaskOption libpathtaskopt : this.libraryPathOption) {
						if (libpathtaskopt == null) {
							continue;
						}
						libpathoptions.add(libpathtaskopt.clone());
					}
				}
				if (!ObjectUtils.isNullOrEmpty(this.sdksOption)) {
					for (Entry<String, SDKDescriptionTaskOption> entry : this.sdksOption.entrySet()) {
						SDKDescriptionTaskOption sdktaskopt = entry.getValue();
						String sdkname = entry.getKey();
						if (sdkoptions.containsKey(sdkname)) {
							taskcontext.abortExecution(new SDKNameConflictException(
									"SDK with name " + sdkname + " defined multiple times."));
							return null;
						}
						//allow null SDK descriptions to disable inferring
						sdkoptions.put(sdkname, ObjectUtils.clone(sdktaskopt, SDKDescriptionTaskOption::clone));
					}
				}

				Collection<LinkerInputPassOption> inputoptions = new ArrayList<>();
				for (LinkerInputPassTaskOption intaskopt : inputtaskoptions) {
					inputoptions.add(intaskopt.toLinkerInputPassOption(taskcontext));
				}

				Set<FileLocation> inputfiles = new LinkedHashSet<>();

				Set<CompilationPathOption> librarypath = new LinkedHashSet<>();
				Map<CompilationPathTaskOption, Collection<CompilationPathOption>> calculatedlibpathoptions = new HashMap<>();
				NavigableMap<String, SDKDescription> nullablesdkdescriptions = new TreeMap<>(
						SDKSupportUtils.getSDKNameComparator());

				for (Entry<String, SDKDescriptionTaskOption> entry : sdkoptions.entrySet()) {
					SDKDescriptionTaskOption val = entry.getValue();
					SDKDescription[] desc = { null };
					if (val != null) {
						val.accept(new SDKDescriptionTaskOption.Visitor() {
							@Override
							public void visit(SDKDescription description) {
								desc[0] = description;
							}
						});
					}
					nullablesdkdescriptions.put(entry.getKey(), desc[0]);
				}

				for (LinkerInputPassTaskOption inputoption : inputtaskoptions) {
					addLinkerInputs(inputoption, taskcontext, inputfiles);
				}

				for (CompilationPathTaskOption libpathopt : libpathoptions) {
					Collection<CompilationPathOption> libpaths = calculatedlibpathoptions.computeIfAbsent(libpathopt,
							o -> o.toCompilationPaths(taskcontext));
					ObjectUtils.addAll(librarypath, libpaths);
				}

				for (ClangLinkerOptions options : linkeroptions) {
					options.accept(new ClangLinkerOptions.Visitor() {
						@Override
						public void visit(ClangLinkerOptions options) {
							if (!CompilerUtils.canMergeIdentifiers(optionidentifier,
									options.getIdentifier() == null ? null : options.getIdentifier().getIdentifier())) {
								return;
							}
							Collection<CompilationPathTaskOption> optlibrarypath = options.getLibraryPath();
							if (!ObjectUtils.isNullOrEmpty(optlibrarypath)) {
								for (CompilationPathTaskOption libpathtaskoption : optlibrarypath) {
									Collection<CompilationPathOption> libpaths = calculatedlibpathoptions
											.computeIfAbsent(libpathtaskoption, o -> o.toCompilationPaths(taskcontext));
									ObjectUtils.addAll(librarypath, libpaths);
								}
							}
							ClangCompileTaskFactory.mergeSDKDescriptionOptions(taskcontext, nullablesdkdescriptions,
									options.getSDKs());
							ObjectUtils.addAll(simpleparameters, options.getSimpleLinkerParameters());
							Collection<LinkerInputPassTaskOption> optinput = options.getLinkerInput();
							if (!ObjectUtils.isNullOrEmpty(optinput)) {
								for (LinkerInputPassTaskOption opttaskin : optinput) {
									addLinkerInputs(opttaskin, taskcontext, inputfiles);
								}
							}
						}
					});
				}

				nullablesdkdescriptions.putIfAbsent(ClangUtils.SDK_NAME_CLANG, ClangUtils.DEFAULT_CLANG_SDK);
				nullablesdkdescriptions.values().removeIf(sdk -> sdk == null);
				NavigableMap<String, SDKDescription> sdkdescriptions = ImmutableUtils
						.makeImmutableNavigableMap(nullablesdkdescriptions);

				final CompilationIdentifier identifier;
				if (optionidentifier == null) {
					CompilationIdentifier inferred = inferCompilationIdentifier(inputoptions);
					if (inferred == null) {
						String wdfilename = taskcontext.getTaskWorkingDirectoryPath().getFileName();
						try {
							if (wdfilename != null) {
								inferred = CompilationIdentifier.valueOf(wdfilename);
							}
						} catch (IllegalArgumentException e) {
						}
						if (inferred == null) {
							inferred = CompilationIdentifier.valueOf("default");
						}
					}
					identifier = inferred;
				} else {
					identifier = optionidentifier;
				}

				ClangLinkWorkerTaskIdentifier workertaskid = new ClangLinkWorkerTaskIdentifier(identifier);

				ClangLinkWorkerTaskFactory worker = new ClangLinkWorkerTaskFactory();
				worker.setInputs(inputfiles);
				worker.setSimpleParameters(simpleparameters);
				worker.setLibraryPath(librarypath);
				worker.setSdkDescriptions(sdkdescriptions);
				taskcontext.startTask(workertaskid, worker, null);

				SimpleStructuredObjectTaskResult result = new SimpleStructuredObjectTaskResult(workertaskid);
				taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
				return result;
			}
		};
	}

	private static CompilationIdentifier inferCompilationIdentifier(Collection<LinkerInputPassOption> inputoptions) {
		Set<String> parts = new LinkedHashSet<>();
		for (LinkerInputPassOption option : inputoptions) {
			option.accept(new LinkerInputPassOption.Visitor() {
				@Override
				public void visit(CompilerOutputLinkerInputPass input) {
					CompilationIdentifier outputid = input.getCompilerOutput().getIdentifier();
					parts.addAll(outputid.getParts());
				}

				@Override
				public void visit(FileLinkerInputPass input) {
				}
			});
		}
		if (parts.isEmpty()) {
			return null;
		}
		return CompilationIdentifier.valueOf(StringUtils.toStringJoin("-", parts));
	}

	private static void addLinkerInputs(LinkerInputPassTaskOption inputoption, TaskContext taskcontext,
			Set<FileLocation> inputfiles) {
		inputoption.toLinkerInputPassOption(taskcontext).accept(new LinkerInputPassOption.Visitor() {
			@Override
			public void visit(FileLinkerInputPass input) {
				Collection<FileLocation> filelocations = input.toFileLocations(taskcontext);
				ObjectUtils.addAll(inputfiles, filelocations);
			}

			@Override
			public void visit(CompilerOutputLinkerInputPass input) {
				ClangCompilerWorkerTaskOutput compileroutput = input.getCompilerOutput();

				Collection<SakerPath> filepaths = compileroutput.getObjectFilePaths();
				if (filepaths == null) {
					throw new IllegalArgumentException("null object file paths for compiler putput.");
				}
				Set<FileLocation> filelocations = new LinkedHashSet<>();
				for (SakerPath objfilepath : filepaths) {
					filelocations.add(ExecutionFileLocation.create(objfilepath));
				}
				inputfiles.addAll(filelocations);
			}
		});
	}

}
