/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.clang.impl.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.runtime.execution.ExecutionProperty;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.task.TaskExecutionEnvironmentSelector;

@Deprecated
//TODO use saker.standard
public class EnvironmentSelectionTestExecutionProperty
		implements ExecutionProperty<EnvironmentSelectionResult>, Externalizable {
	private static final long serialVersionUID = 1L;

	private TaskExecutionEnvironmentSelector selector;

	/**
	 * For {@link Externalizable}.
	 */
	public EnvironmentSelectionTestExecutionProperty() {
	}

	public EnvironmentSelectionTestExecutionProperty(TaskExecutionEnvironmentSelector selector) {
		this.selector = selector;
	}

	@Override
	public EnvironmentSelectionResult getCurrentValue(ExecutionContext executioncontext) throws Exception {
		return executioncontext.testEnvironmentSelection(selector, null);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(selector);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		selector = (TaskExecutionEnvironmentSelector) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((selector == null) ? 0 : selector.hashCode());
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
		EnvironmentSelectionTestExecutionProperty other = (EnvironmentSelectionTestExecutionProperty) obj;
		if (selector == null) {
			if (other.selector != null)
				return false;
		} else if (!selector.equals(other.selector))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (selector != null ? "selector=" + selector : "") + "]";
	}

}
