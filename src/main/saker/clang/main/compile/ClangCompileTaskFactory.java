package saker.clang.main.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import saker.clang.impl.compile.ClangCompileWorkerTaskFactory;
import saker.clang.impl.compile.ClangCompileWorkerTaskIdentifier;
import saker.clang.impl.compile.FileCompilationConfiguration;
import saker.clang.impl.compile.FileCompilationProperties;
import saker.clang.impl.option.CompilationPathOption;
import saker.clang.impl.util.ClangUtils;
import saker.clang.main.compile.options.CompilationInputPassOption;
import saker.clang.main.compile.options.CompilationInputPassTaskOption;
import saker.clang.main.compile.options.FileCompilationInputPass;
import saker.clang.main.compile.options.OptionCompilationInputPass;
import saker.clang.main.options.CompilationPathTaskOption;
import saker.compiler.utils.api.CompilationIdentifier;
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

			@Override
			public Object run(TaskContext taskcontext) throws Exception {
				List<CompilationInputPassTaskOption> inputpasses = new ArrayList<>();
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
				List<ConfigSetupHolder> configbuf = new ArrayList<>();
				for (CompilationInputPassTaskOption inputpass : inputpasses) {
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

									for (MultiFileLocationTaskOption filesopt : files) {
										Collection<FileLocation> filelocations = TaskOptionUtils
												.toFileLocations(filesopt, taskcontext, null);
										if (ObjectUtils.isNullOrEmpty(filelocations)) {
											continue;
										}
										CompilationIdentifier passsubid = CompilationIdentifierTaskOption
												.getIdentifier(passsubidopt);
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

					//TODO handle sub options

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
