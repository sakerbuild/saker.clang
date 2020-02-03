package saker.clang.impl;

import java.util.Map;

public class ClangUtils {
	private ClangUtils() {
		throw new UnsupportedOperationException();
	}

	public static void removeClangEnvironmentVariables(Map<String, String> env) {
		//based on https://clang.llvm.org/docs/CommandGuide/clang.html#environment
		env.remove("CPATH");
		env.remove("C_INCLUDE_PATH");
		env.remove("OBJC_INCLUDE_PATH");
		env.remove("CPLUS_INCLUDE_PATH");
		env.remove("OBJCPLUS_INCLUDE_PATH");
		env.remove("MACOSX_DEPLOYMENT_TARGET");

		//XXX remove this environment when linking
//		//based on http://releases.llvm.org/2.5/docs/CommandGuide/html/llvm-ld.html
//		env.remove("LLVM_LIB_SEARCH_PATH");
	}
}
