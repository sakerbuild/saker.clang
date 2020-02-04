package saker.clang.impl.compile;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.clang.api.compile.ClangCompilerWorkerTaskOutput;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;

final class ClangCompilerWorkerTaskOutputImpl implements ClangCompilerWorkerTaskOutput, Externalizable {
	private static final long serialVersionUID = 1L;

	private CompilationIdentifier compilationIdentifier;
	private NavigableSet<SakerPath> objectFilePaths;
	private Map<String, SDKDescription> sdkDescriptions;

	/**
	 * For {@link Externalizable}.
	 */
	public ClangCompilerWorkerTaskOutputImpl() {
	}

	public ClangCompilerWorkerTaskOutputImpl(CompilationIdentifier passidentifier,
			NavigableSet<SakerPath> objectFilePaths, NavigableMap<String, SDKDescription> sdkDescriptions) {
		this.compilationIdentifier = passidentifier;
		this.objectFilePaths = objectFilePaths;
		this.sdkDescriptions = sdkDescriptions;
	}

	@Override
	public Map<String, SDKDescription> getSDKs() {
		return sdkDescriptions;
	}

	@Override
	public Collection<SakerPath> getObjectFilePaths() {
		return objectFilePaths;
	}

	@Override
	public CompilationIdentifier getIdentifier() {
		return compilationIdentifier;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, objectFilePaths);
		out.writeObject(compilationIdentifier);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		objectFilePaths = SerialUtils.readExternalSortedImmutableNavigableSet(in);
		compilationIdentifier = (CompilationIdentifier) in.readObject();
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
				SDKSupportUtils.getSDKNameComparator());
	}
}