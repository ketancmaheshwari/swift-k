/*
 * Swift Parallel Scripting Language (http://swift-lang.org)
 * Code from Java CoG Kit Project (see notice below) with modifications.
 *
 * Copyright 2005-2014 University of Chicago
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

// ----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Apr 7, 2005
 */
package org.globus.cog.karajan.compiled.nodes.grid;

import k.rt.ExecutionException;
import k.rt.Stack;

import org.globus.cog.abstraction.impl.common.StatusEvent;
import org.globus.cog.abstraction.interfaces.FileOperationSpecification;
import org.globus.cog.abstraction.interfaces.GridFile;
import org.globus.cog.abstraction.interfaces.Task;
import org.globus.cog.karajan.analyzer.ArgRef;
import org.globus.cog.karajan.analyzer.ChannelRef;
import org.globus.cog.karajan.analyzer.Signature;
import org.globus.cog.karajan.util.BoundContact;

public class FileInfo extends AbstractFileOperation {
	private ArgRef<String> name;
	private ChannelRef<Object> cr_vargs;
	
	@Override
	protected Signature getSignature() {
		return new Signature(params("name", optional("host", BoundContact.LOCALHOST), 
				optional("provider", "local")), returns(channel("...", 1)));
	}


	protected String getOperation(Stack stack) throws ExecutionException {
		return FileOperationSpecification.FILEINFO;
	}

	protected String[] getArguments(Stack stack) throws ExecutionException {
		return new String[] { name.getValue(stack) };
	}

	@Override
	protected void taskCompleted(StatusEvent e, Stack stack) {
		try {
			Task task = (Task) e.getSource();
			GridFile gf = (GridFile) task.getAttribute("output");
			cr_vargs.append(stack, gf.getFileType());
			StringBuffer sb = new StringBuffer();
			sb.append(gf.getUserPermissions().toDigit());
			sb.append(gf.getGroupPermissions().toDigit());
			sb.append(gf.getWorldPermissions().toDigit());
			cr_vargs.append(stack, sb.toString());
			cr_vargs.append(stack, new Long(gf.getSize()));
			cr_vargs.append(stack, gf.getLastModified());
		}
		catch (Exception ex) {
			throw new ExecutionException(this, "Exception caugh while retrieving output", ex);
		}
	}
}
