package saker.clang.main.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.SimpleStructuredObjectTaskResult;
import saker.build.task.utils.annot.SakerInput;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.thirdparty.saker.util.io.FileUtils;
import saker.build.trace.BuildTrace;
import saker.clang.impl.compile.ClangCompileWorkerTaskFactory;
import saker.clang.impl.compile.ClangCompileWorkerTaskIdentifier;
import saker.clang.impl.compile.FileCompilationConfiguration;
import saker.clang.impl.compile.FileCompilationProperties;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.util.ClangUtils;
import saker.clang.main.compile.options.ClangCompilerOptions;
import saker.clang.main.compile.options.CompilationInputPassOption;
import saker.clang.main.compile.options.CompilationInputPassTaskOption;
import saker.clang.main.compile.options.FileCompilationInputPass;
import saker.clang.main.compile.options.OptionCompilationInputPass;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.compiler.utils.api.CompilerUtils;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.exc.SDKNameConflictException;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.MultiFileLocationTaskOption;
import saker.std.main.file.utils.TaskOptionUtils;

public class ClangCompileTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.clang.compile";

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<Object>() {

			@SakerInput(value = { "", "Input" }, required = true)
			public Collection<CompilationInputPassTaskOption> inputOption;

			@SakerInput(value = { "Identifier" })
			public CompilationIdentifierTaskOption identifierOption;

			@SakerInput(value = { "SDKs" })
			public Map<String, SDKDescriptionTaskOption> sdksOption;

			@SakerInput(value = { "CompilerOptions" })
			public Collection<ClangCompilerOptions> compilerOptionsOption;

			@Override
			public Object run(TaskContext taskcontext) throws Exception {
				if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
					BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_FRONTEND);
				}

				List<CompilationInputPassTaskOption> inputpasses = new ArrayList<>();
				Collection<ClangCompilerOptions> compileroptions = new ArrayList<>();
				Map<String, SDKDescriptionTaskOption> sdkoptions = new TreeMap<>(
						SDKSupportUtils.getSDKNameComparator());
				NavigableMap<String, SDKDescription> sdkdescriptions = new TreeMap<>(
						SDKSupportUtils.getSDKNameComparator());

				CompilationIdentifier optionidentifier = this.identifierOption == null ? null
						: this.identifierOption.clone().getIdentifier();

				Map<CompilationPathTaskOption, Collection<CompilationPathOption>> calculatedincludediroptions = new HashMap<>();
				Map<FileCompilationProperties, String> precompiledheaderoutnamesconfigurations = new HashMap<>();

				//ignore-case comparison of possible output names of the files
				//  as windows has case-insensitive file names, we need to support Main.cpp and main.cpp from different directories
				NavigableSet<String> outnames = new TreeSet<>(String::compareToIgnoreCase);
				Set<FileCompilationConfiguration> files = new LinkedHashSet<>();

				if (!ObjectUtils.isNullOrEmpty(this.sdksOption)) {
					for (Entry<String, SDKDescriptionTaskOption> entry : this.sdksOption.entrySet()) {
						SDKDescriptionTaskOption sdktaskopt = entry.getValue();
						if (sdktaskopt == null) {
							continue;
						}
						SDKDescriptionTaskOption prev = sdkoptions.putIfAbsent(entry.getKey(), sdktaskopt.clone());
						if (prev != null) {
							taskcontext.abortExecution(new SDKNameConflictException(
									"SDK with name " + entry.getKey() + " defined multiple times."));
							return null;
						}
					}
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
						sdkdescriptions.putIfAbsent(entry.getKey(), desc[0]);
					}
				}

				for (CompilationInputPassTaskOption inputopt : inputOption) {
					if (inputopt == null) {
						continue;
					}
					CompilationInputPassTaskOption inputpass = inputopt.clone();
					inputpasses.add(inputpass);
				}
				if (!ObjectUtils.isNullOrEmpty(this.compilerOptionsOption)) {
					for (ClangCompilerOptions options : this.compilerOptionsOption) {
						if (options == null) {
							continue;
						}
						compileroptions.add(options.clone());
					}
				}
				List<ConfigSetupHolder> configbuf = new ArrayList<>();
				for (CompilationInputPassTaskOption inputpass : inputpasses) {
					CompilationIdentifier[] subid = { null };
					OptionCompilationInputPass[] suboptioninputpass = { null };
					inputpass.toCompilationInputPassOption(taskcontext)
							.accept(new CompilationInputPassOption.Visitor() {
								@Override
								public void visit(FileCompilationInputPass input) {
									Collection<FileLocation> filelocations = input.toFileLocations(taskcontext);
									if (ObjectUtils.isNullOrEmpty(filelocations)) {
										return;
									}
									for (FileLocation filelocation : filelocations) {
										FileCompilationConfiguration nconfig = createConfigurationForProperties(
												new FileCompilationProperties(filelocation));
										configbuf.add(new ConfigSetupHolder(nconfig));
									}
								}

								@Override
								public void visit(OptionCompilationInputPass input) {
									Collection<MultiFileLocationTaskOption> files = input.getFiles();
									if (ObjectUtils.isNullOrEmpty(files)) {
										return;
									}

									Map<String, String> macrodefinitions = input.getMacroDefinitions();
									Set<String> simpleparamoption = ImmutableUtils
											.makeImmutableNavigableSet(input.getSimpleParameters());
									String passlang = input.getLanguage();
									FileLocation pchfilelocation = TaskOptionUtils
											.toFileLocation(input.getPrecompiledHeader(), taskcontext);

									Set<CompilationPathOption> inputincludedirs = toIncludePathOptions(taskcontext,
											calculatedincludediroptions, input.getIncludeDirectories());
									Set<CompilationPathOption> inputforceincludes = toIncludePathOptions(taskcontext,
											calculatedincludediroptions, input.getForceInclude());

									CompilationIdentifierTaskOption passsubidopt = input.getSubIdentifier();
									CompilationIdentifier passsubid = CompilationIdentifierTaskOption
											.getIdentifier(passsubidopt);
									subid[0] = passsubid;
									suboptioninputpass[0] = input;

									for (MultiFileLocationTaskOption filesopt : files) {
										Collection<FileLocation> filelocations = TaskOptionUtils
												.toFileLocations(filesopt, taskcontext, null);
										if (ObjectUtils.isNullOrEmpty(filelocations)) {
											continue;
										}
										for (FileLocation filelocation : filelocations) {
											FileCompilationProperties nproperties = new FileCompilationProperties(
													filelocation);
											nproperties.setSimpleParameters(simpleparamoption);
											nproperties.setIncludeDirectories(inputincludedirs);
											nproperties.setMacroDefinitions(macrodefinitions);
											nproperties.setForceInclude(inputforceincludes);

											FileCompilationConfiguration nconfig = createConfigurationForProperties(
													nproperties, passsubid, passlang);
											configbuf.add(new ConfigSetupHolder(nconfig, pchfilelocation));
										}
									}
								}

								private FileCompilationConfiguration createConfigurationForProperties(
										FileCompilationProperties properties, CompilationIdentifier passsubid,
										String optionlanguage) {
									FileLocation filelocation = properties.getFileLocation();
									String pathfilename = ClangUtils.getFileName(filelocation);
									if (pathfilename == null) {
										throw new IllegalArgumentException(
												"Input file doesn't have file name: " + filelocation);
									}
									String outfname = getOutFileName(pathfilename, outnames, passsubid);
									String language = getLanguageBasedOnFileName(optionlanguage, pathfilename);

									FileCompilationConfiguration nconfig = new FileCompilationConfiguration(outfname,
											properties);
									properties.setLanguage(language);
									return nconfig;
								}

								private FileCompilationConfiguration createConfigurationForProperties(
										FileCompilationProperties properties) {
									return createConfigurationForProperties(properties, null, null);
								}
							});

					CompilationIdentifier targetmergeidentifier = CompilationIdentifier.concat(optionidentifier,
							subid[0]);

					for (ConfigSetupHolder configholder : configbuf) {
						FileCompilationConfiguration config = configholder.config;
						FileCompilationProperties configproperties = config.getProperties();
						String configlanguage = configproperties.getLanguage();
						ClangCompilerOptions.Visitor optionsvisitor = new ClangCompilerOptions.Visitor() {
							@Override
							public void visit(ClangCompilerOptions options) {
								if (!CompilerUtils.canMergeIdentifiers(targetmergeidentifier,
										options.getIdentifier() == null ? null
												: options.getIdentifier().getIdentifier())) {
									return;
								}
								if (!CompilerUtils.canMergeLanguages(configlanguage, options.getLanguage())) {
									return;
								}
								mergeCompilerOptions(options, configproperties, taskcontext, sdkdescriptions);
							}

							private void mergeCompilerOptions(ClangCompilerOptions options,
									FileCompilationProperties compilationproperties, TaskContext taskcontext,
									NavigableMap<String, SDKDescription> sdkdescriptions) {
								mergeIncludeDirectories(compilationproperties, toIncludePathOptions(taskcontext,
										calculatedincludediroptions, options.getIncludeDirectories()));
								mergeForceIncludes(compilationproperties, toIncludePathOptions(taskcontext,
										calculatedincludediroptions, options.getForceInclude()));
								mergeSDKDescriptionOptions(taskcontext, sdkdescriptions, options.getSDKs());
								mergeMacroDefinitions(compilationproperties, options.getMacroDefinitions());
								mergeSimpleParameters(compilationproperties, options.getSimpleCompilerParameters());

								mergePrecompiledHeader(configholder,
										TaskOptionUtils.toFileLocation(options.getPrecompiledHeader(), taskcontext));
							}

						};
						if (suboptioninputpass[0] != null) {
							Collection<ClangCompilerOptions> subcompileroptions = suboptioninputpass[0]
									.getCompilerOptions();
							if (!ObjectUtils.isNullOrEmpty(subcompileroptions)) {
								for (ClangCompilerOptions options : subcompileroptions) {
									options.accept(optionsvisitor);
								}
							}
						}
						for (ClangCompilerOptions options : compileroptions) {
							options.accept(optionsvisitor);
						}
					}

					for (ConfigSetupHolder configholder : configbuf) {
						if (configholder.precompiledHeader != null) {
							FileCompilationProperties pchprops = new FileCompilationProperties(
									configholder.precompiledHeader);
							pchprops.copyFrom(configholder.config.getProperties());

							String pchoutfilename = precompiledheaderoutnamesconfigurations.get(pchprops);
							if (pchoutfilename == null) {
								String pchfilename = ClangUtils.getFileName(configholder.precompiledHeader);

								pchoutfilename = getOutFileName(pchfilename, outnames, null);
								precompiledheaderoutnamesconfigurations.put(pchprops, pchoutfilename);
							}
							configholder.config.setPrecompiledHeader(configholder.precompiledHeader, pchoutfilename);

						}
						files.add(configholder.config);
					}
				}

				CompilationIdentifier identifier = optionidentifier;
				if (identifier == null) {
					String wdfilename = taskcontext.getTaskWorkingDirectoryPath().getFileName();
					try {
						if (wdfilename != null) {
							identifier = CompilationIdentifier.valueOf(wdfilename);
						}
					} catch (IllegalArgumentException e) {
					}
				}
				if (identifier == null) {
					identifier = CompilationIdentifier.valueOf("default");
				}

				sdkdescriptions.putIfAbsent(ClangUtils.SDK_NAME_CLANG, ClangUtils.DEFAULT_CLANG_SDK);

				ClangCompileWorkerTaskIdentifier workertaskid = new ClangCompileWorkerTaskIdentifier(identifier);

				ClangCompileWorkerTaskFactory workertask = new ClangCompileWorkerTaskFactory();
				workertask.setSdkDescriptions(sdkdescriptions);
				workertask.setFiles(files);

				taskcontext.startTask(workertaskid, workertask, null);

				SimpleStructuredObjectTaskResult result = new SimpleStructuredObjectTaskResult(workertaskid);
				taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
				return result;
			}
		};
	}

	private static Set<CompilationPathOption> toIncludePathOptions(TaskContext taskcontext,
			Map<CompilationPathTaskOption, Collection<CompilationPathOption>> calculatedincludediroptions,
			Collection<CompilationPathTaskOption> indirtaskopts) {
		Set<CompilationPathOption> inputincludedirs = new LinkedHashSet<>();
		collectIncludeDirectoryOptions(taskcontext, calculatedincludediroptions, indirtaskopts, inputincludedirs);
		return inputincludedirs;
	}

	private static void collectIncludeDirectoryOptions(TaskContext taskcontext,
			Map<CompilationPathTaskOption, Collection<CompilationPathOption>> calculatedincludediroptions,
			Collection<CompilationPathTaskOption> indirtaskopts, Set<CompilationPathOption> inputincludedirs) {
		if (ObjectUtils.isNullOrEmpty(indirtaskopts)) {
			return;
		}
		for (CompilationPathTaskOption indirtaskopt : indirtaskopts) {
			Collection<CompilationPathOption> indiroptions = calculatedincludediroptions.computeIfAbsent(indirtaskopt,
					o -> o.toCompilationPaths(taskcontext));
			ObjectUtils.addAll(inputincludedirs, indiroptions);
		}
	}

	private static void mergeSimpleParameters(FileCompilationProperties config, Collection<String> simpleparams) {
		if (ObjectUtils.isNullOrEmpty(simpleparams)) {
			return;
		}
		TreeSet<String> result = ObjectUtils.newTreeSet(config.getSimpleParameters());
		if (!result.addAll(simpleparams)) {
			//not changed
			return;
		}
		config.setSimpleParameters(result);
	}

	private static void mergeMacroDefinitions(FileCompilationProperties config, Map<String, String> macrodefs) {
		if (ObjectUtils.isNullOrEmpty(macrodefs)) {
			return;
		}
		Map<String, String> configmacros = config.getMacroDefinitions();
		Map<String, String> nmacros;
		if (configmacros == null) {
			nmacros = ImmutableUtils.makeImmutableLinkedHashMap(macrodefs);
		} else {
			nmacros = new LinkedHashMap<>(configmacros);
			for (Entry<String, String> entry : macrodefs.entrySet()) {
				nmacros.putIfAbsent(entry.getKey(), entry.getValue());
			}
		}
		config.setMacroDefinitions(nmacros);
	}

	private static void mergeIncludeDirectories(FileCompilationProperties config,
			Collection<CompilationPathOption> includediroptions) {
		if (ObjectUtils.isNullOrEmpty(includediroptions)) {
			return;
		}
		config.setIncludeDirectories(
				ObjectUtils.addAll(ObjectUtils.newLinkedHashSet(config.getIncludeDirectories()), includediroptions));
	}

	private static void mergeForceIncludes(FileCompilationProperties config,
			Collection<CompilationPathOption> includeoptions) {
		if (ObjectUtils.isNullOrEmpty(includeoptions)) {
			return;
		}
		config.setForceInclude(
				ObjectUtils.addAll(ObjectUtils.newLinkedHashSet(config.getForceInclude()), includeoptions));
	}

	private static void mergePrecompiledHeader(ConfigSetupHolder configholder, FileLocation pch) {
		if (pch == null) {
			return;
		}
		FileLocation presentpch = configholder.precompiledHeader;
		if (presentpch != null && !presentpch.equals(pch)) {
			throw new IllegalArgumentException(
					"Option merge conflict for precompiled header: " + pch + " and " + presentpch);
		}
		configholder.precompiledHeader = pch;
	}

	public static void mergeSDKDescriptionOptions(TaskContext taskcontext,
			NavigableMap<String, SDKDescription> sdkdescriptions, Map<String, SDKDescriptionTaskOption> sdks) {
		if (ObjectUtils.isNullOrEmpty(sdks)) {
			return;
		}
		for (Entry<String, SDKDescriptionTaskOption> entry : sdks.entrySet()) {
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
			sdkdescriptions.putIfAbsent(entry.getKey(), desc[0]);
		}
	}

	private static String getOutFileName(String fname, Set<String> presentfiles, CompilationIdentifier passsubid) {
		Objects.requireNonNull(fname, "file name");
		if (presentfiles.add(fname)) {
			return fname;
		}
		if (passsubid != null) {
			String subidfname = passsubid + "-" + fname;
			if (presentfiles.add(subidfname)) {
				return subidfname;
			}
		}
		int i = 1;
		int dotidx = fname.lastIndexOf('.');
		String base;
		String end;
		if (dotidx < 0) {
			//no extension
			base = fname;
			end = "";
		} else {
			base = fname.substring(0, dotidx);
			end = fname.substring(dotidx);
		}
		while (true) {
			String nfname = base + "_" + i + end;
			if (presentfiles.add(nfname)) {
				return nfname;
			}
			++i;
		}
	}

	private static class ConfigSetupHolder {
		public FileCompilationConfiguration config;
		public FileLocation precompiledHeader;

		public ConfigSetupHolder(FileCompilationConfiguration config) {
			this.config = config;
		}

		public ConfigSetupHolder(FileCompilationConfiguration config, FileLocation precompiledHeader) {
			this.config = config;
			this.precompiledHeader = precompiledHeader;
		}

	}

	private static String getLanguageBasedOnFileName(String language, String filename) {
		if (language != null) {
			if (language.equalsIgnoreCase("c++")) {
				return "c++";
			}
			if (language.equalsIgnoreCase("c")) {
				return "c";
			}
			if (language.equalsIgnoreCase("objc")) {
				return "objc";
			}
			if (language.equalsIgnoreCase("objc++")) {
				return "objc++";
			}
			throw new IllegalArgumentException("Unknown language: " + language);
		}
		String extension = FileUtils.getExtension(filename);
		if (extension != null) {
			if (StringUtils.endsWithIgnoreCase(extension, "xx") || StringUtils.endsWithIgnoreCase(extension, "pp")) {
				return "c++";
			}
			if ("mm".equalsIgnoreCase(extension)) {
				return "objc++";
			}
			if ("m".equalsIgnoreCase(extension)) {
				return "objc";
			}
		}
		return "c";
	}
}
