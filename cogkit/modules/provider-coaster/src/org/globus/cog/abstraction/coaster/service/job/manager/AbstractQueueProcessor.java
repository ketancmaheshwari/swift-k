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

//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Feb 13, 2008
 */
package org.globus.cog.abstraction.coaster.service.job.manager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.globus.cog.abstraction.coaster.service.LocalTCPService;
import org.globus.cog.abstraction.interfaces.Task;
import org.globus.cog.coaster.channels.ChannelContext;

public abstract class AbstractQueueProcessor extends Thread implements QueueProcessor {
    private final BlockingQueue<AssociatedTask> q;
    private boolean shutdownFlag;
    private boolean wrap;
    private final LocalTCPService localService;

    public AbstractQueueProcessor(String name, LocalTCPService localService) {
        super(name);
        this.localService = localService;
        q = new LinkedBlockingQueue<AssociatedTask>();
    }

    public LocalTCPService getLocalService() {
        return localService;
    }

    public void enqueue(Task t) {
        if (shutdownFlag) {
            throw new IllegalStateException("Queue is shut down");
        }
        q.offer(new AssociatedTask(t));
    }

    public void shutdown() {
        shutdownFlag = true;
    }

    protected boolean getShutdownFlag() {
        return shutdownFlag;
    }

    protected final AssociatedTask take() throws InterruptedException {
        return q.take();
    }

    protected final boolean hasWrapped() {
        return wrap;
    }

    protected int queuedTaskCount() {
        return q.size();
    }

    @Override
    public void setClientChannelContext(ChannelContext channelContext) {
        // TODO Auto-generated method stub

    }

    @Override
    public synchronized void start() {
        if (this.isAlive()) {
            throw new RuntimeException(this.getName() + " already started");
        }
        super.start();
    }
}
