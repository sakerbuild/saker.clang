package saker.clang.main.compile;

import java.util.Map;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.annot.SakerInput;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public class ClangCompileTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<Object>() {

			@SakerInput(value = "Identifier")
			public CompilationIdentifierTaskOption identifierOption;

			@SakerInput(value = { "SDKs" })
			public Map<String, SDKDescriptionTaskOption> sdksOption;

			@Override
			public Object run(TaskContext taskcontext) throws Exception {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException();
			}
		};
	}

}
