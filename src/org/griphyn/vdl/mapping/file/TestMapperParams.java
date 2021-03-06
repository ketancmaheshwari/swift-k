/*
 * Swift Parallel Scripting Language (http://swift-lang.org)
 *
 * Copyright 2013-2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.griphyn.vdl.mapping.file;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.griphyn.vdl.mapping.nodes.AbstractDataNode;
import org.griphyn.vdl.mapping.MappingParamSet;


public class TestMapperParams extends MappingParamSet {

	public static final List<String> NAMES = Arrays.asList("file", "temp", "remappable", "static_");

	private Object file;
	private Object temp = false;
	private Object remappable = false;
	private Object static_ = true;

	@Override
	public Collection<String> getNames() {
		return NAMES;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getFile() {
		return (String) file;
	}

	public void setTemp(Boolean temp) {
		this.temp = temp;
	}

	public Boolean getTemp() {
		return (Boolean) temp;
	}

	public void setRemappable(Boolean remappable) {
		this.remappable = remappable;
	}

	public Boolean getRemappable() {
		return (Boolean) remappable;
	}

	public void setStatic_(Boolean static_) {
		this.static_ = static_;
	}

	public Boolean getStatic_() {
		return (Boolean) static_;
	}

	@Override
	protected boolean set0(String name, Object value) {
		if (name.equals("file")) {
			this.file = value;
		}
		else if (name.equals("temp")) {
			this.temp = value;
		}
		else if (name.equals("remappable")) {
			this.remappable = value;
		}
		else if (name.equals("static_")) {
			this.static_ = value;
		}
		else {
			return super.set0(name, value);
		}
		return true;
	}

	@Override
	public AbstractDataNode getFirstOpen() {
		if (checkOpen(file)) {
			return (AbstractDataNode) file;
		}
		else if (checkOpen(temp)) {
			return (AbstractDataNode) temp;
		}
		else if (checkOpen(remappable)) {
			return (AbstractDataNode) remappable;
		}
		else if (checkOpen(static_)) {
			return (AbstractDataNode) static_;
		}
		else {
			return super.getFirstOpen();
		}
	}

	@Override
	public void toString(StringBuilder sb) {
		addParam(sb, "file", file);
		addParam(sb, "temp", temp);
		addParam(sb, "remappable", remappable);
		addParam(sb, "static_", static_);
		super.toString(sb);
	}

	@Override
	public void unwrapPrimitives() {
		if (file == null) {
			throw new IllegalArgumentException("Missing required argument 'file'");
		}
		file = unwrap(file, String.class);
		temp = unwrap(temp, Boolean.class);
		remappable = unwrap(remappable, Boolean.class);
		static_ = unwrap(static_, Boolean.class);
		super.unwrapPrimitives();
	}


}
