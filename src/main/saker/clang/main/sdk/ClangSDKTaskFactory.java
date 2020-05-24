package saker.clang.main.sdk;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.annot.SakerInput;
import saker.build.trace.BuildTrace;
import saker.clang.impl.sdk.VersionInfoClangSDKDescription;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;

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

				if (executableOption != null) {
					return new VersionInfoClangSDKDescription(null, executableOption);
				}
				return VersionInfoClangSDKDescription.INSTANCE_ANY_VERSION;
			}
		};
	}

}
