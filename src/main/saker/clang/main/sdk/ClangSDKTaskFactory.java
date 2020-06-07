package saker.clang.main.sdk;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.annot.SakerInput;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.trace.BuildTrace;
import saker.clang.impl.sdk.VersionInfoClangSDKDescription;
import saker.clang.main.TaskDocs.DocClangExecutableName;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.main.TaskDocs.DocSDKDescription;

@NestTaskInformation(returnType = @NestTypeUsage(DocSDKDescription.class))
@NestInformation("Gets an SDK description for clang.\n"
		+ "The task can be used to configure other tasks with an SDK that uses clang.")
@NestParameterInformation(value = "Executable",
		type = @NestTypeUsage(DocClangExecutableName.class),
		info = @NestInformation("Specifies the executable that should be used to get the clang configuration information.\n"
				+ "The executable will be used to get the version information about clang. It can be clang or clang++."))
public class ClangSDKTaskFactory extends FrontendTaskFactory<SDKDescription> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.clang.sdk";

	@Override
	public ParameterizableTask<? extends SDKDescription> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<SDKDescription>() {

			/**
			 * <code>clang</code> or <code>clang++</code>
			 */
			@SakerInput(value = { "Executable" })
			public String executableOption;

			@Override
			public SDKDescription run(TaskContext taskcontext) throws Exception {
				if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
					BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_CONFIGURATION);
				}

				SDKDescription result;
				if (!ObjectUtils.isNullOrEmpty(executableOption)) {
					result = new VersionInfoClangSDKDescription(null, executableOption);
				} else {
					result = VersionInfoClangSDKDescription.INSTANCE_ANY_VERSION;
				}
				taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
				return result;
			}
		};
	}

}
