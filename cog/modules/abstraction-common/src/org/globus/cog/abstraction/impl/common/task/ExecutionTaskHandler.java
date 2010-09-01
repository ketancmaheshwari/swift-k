// ----------------------------------------------------------------------
// This code is developed as part of the Java CoG Kit project
// The terms of the license can be found at http://www.cogkit.org/license
// This message may not be removed or altered.
// ----------------------------------------------------------------------

package org.globus.cog.abstraction.impl.common.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.globus.cog.abstraction.impl.common.AbstractionFactory;
import org.globus.cog.abstraction.impl.common.ProviderMethodException;
import org.globus.cog.abstraction.impl.common.StatusImpl;
import org.globus.cog.abstraction.impl.common.TaskHandlerSkeleton;
import org.globus.cog.abstraction.interfaces.Service;
import org.globus.cog.abstraction.interfaces.Status;
import org.globus.cog.abstraction.interfaces.Task;
import org.globus.cog.abstraction.interfaces.TaskHandler;

public class ExecutionTaskHandler extends TaskHandlerSkeleton {
    Logger logger = Logger.getLogger(ExecutionTaskHandler.class);
    private Hashtable mapping;
    public ExecutionTaskHandler() {
        this.mapping = new Hashtable();
        setType(TaskHandler.EXECUTION);
    }

    public void submit(Task task)
        throws
            IllegalSpecException,
            InvalidSecurityContextException,
            InvalidServiceContactException,
            TaskSubmissionException {
        if (task.getType() != Task.JOB_SUBMISSION) {
            throw new TaskSubmissionException
                ("Execution handler can only handle job submission tasks");
        }
        String provider = task.getService(0).getProvider().toLowerCase();
        logger.info("provider="+provider); 
        TaskHandler taskHandler = (TaskHandler) this.mapping.get(provider);

        if (taskHandler == null) {
            try {
                taskHandler = createTaskHandler(provider);
            } catch (InvalidProviderException ipe) {
                throw new TaskSubmissionException("Cannot submit task", ipe);
            }
        }

        logger.debug("taskHandler="+taskHandler);
        taskHandler.submit(task);
    }

    public void suspend(Task task)
        throws InvalidSecurityContextException, TaskSubmissionException {
        if (task.getType() != Task.JOB_SUBMISSION) {
            throw new TaskSubmissionException("Execution handler can only handle job submission tasks");
        }
        String provider = task.getService(Service.DEFAULT_SERVICE).getProvider().toLowerCase();
        TaskHandler taskHandler = (TaskHandler) this.mapping.get(provider);
        if (taskHandler != null) {
            taskHandler.suspend(task);
        } else {
            throw new TaskSubmissionException(
                "Provider " + provider + " unknown");
        }
    }

    public void resume(Task task)
        throws InvalidSecurityContextException, TaskSubmissionException {
        if (task.getType() != Task.JOB_SUBMISSION) {
            throw new TaskSubmissionException("Execution handler can only handle job submission tasks");
        }
        String provider = task.getService(Service.DEFAULT_SERVICE).getProvider().toLowerCase();
        TaskHandler taskHandler = (TaskHandler) this.mapping.get(provider);
        if (taskHandler != null) {
            taskHandler.resume(task);
        } else {
            throw new TaskSubmissionException(
                "Provider " + provider + " unknown");
        }
    }
    
    public void cancel(Task task) throws InvalidSecurityContextException, TaskSubmissionException {
        cancel(task, null);
    }

    public void cancel(Task task, String message)
        throws InvalidSecurityContextException, TaskSubmissionException {
        if (task.getType() != Task.JOB_SUBMISSION) {
            throw new TaskSubmissionException("Execution handler can only handle job submission tasks");
        }
        String provider = task.getService(Service.DEFAULT_SERVICE).getProvider().toLowerCase();
        TaskHandler taskHandler = (TaskHandler) this.mapping.get(provider);
        if (taskHandler != null) {
            taskHandler.cancel(task, message);
        } else {
            task.setStatus(new StatusImpl(Status.CANCELED, message, null));
        }
    }

    public void remove(Task task) throws ActiveTaskException {
        String provider = task.getService(Service.DEFAULT_SERVICE).getProvider().toLowerCase();
        TaskHandler taskHandler = (TaskHandler) this.mapping.get(provider);
        if (taskHandler != null) {
            taskHandler.remove(task);
        }
    }

    public Collection getAllTasks() {
        // extract all the tasks from various TaskHandlers
        List list = new ArrayList();
        Enumeration e1 = this.mapping.elements();
        TaskHandler handler;
        while (e1.hasMoreElements()) {
            handler = (TaskHandler) e1.nextElement();
            list.addAll(handler.getAllTasks());
        }
        return list;
    }

    public Collection getActiveTasks() {
        // extract all the active tasks from various TaskHandlers
        List list = new ArrayList();
        Enumeration e1 = this.mapping.elements();
        TaskHandler handler;
        while (e1.hasMoreElements()) {
            handler = (TaskHandler) e1.nextElement();
            list.addAll(handler.getActiveTasks());
        }
        return list;
    }

    public Collection getFailedTasks() {
        // extract all the failed tasks from various TaskHandlers
        List list = new ArrayList();
        Enumeration e1 = this.mapping.elements();
        TaskHandler handler;
        while (e1.hasMoreElements()) {
            handler = (TaskHandler) e1.nextElement();
            list.addAll(handler.getFailedTasks());
        }
        return list;
    }

    public Collection getCompletedTasks() {
        // extract all the tasks from various TaskHandlers
        List list = new ArrayList();
        Enumeration e1 = this.mapping.elements();
        TaskHandler handler;
        while (e1.hasMoreElements()) {
            handler = (TaskHandler) e1.nextElement();
            list.addAll(handler.getCompletedTasks());
        }
        return list;
    }

    public Collection getSuspendedTasks() {
        // extract all the tasks from various TaskHandlers
        List list = new ArrayList();
        Enumeration e1 = this.mapping.elements();
        TaskHandler handler;
        while (e1.hasMoreElements()) {
            handler = (TaskHandler) e1.nextElement();
            list.addAll(handler.getSuspendedTasks());
        }
        return list;
    }

    public Collection getResumedTasks() {
        // extract all the tasks from various TaskHandlers
        List list = new ArrayList();
        Enumeration e1 = this.mapping.elements();
        TaskHandler handler;
        while (e1.hasMoreElements()) {
            handler = (TaskHandler) e1.nextElement();
            list.addAll(handler.getResumedTasks());
        }
        return list;
    }

    public Collection getCanceledTasks() {
        // extract all the tasks from various TaskHandlers
        List list = new ArrayList();
        Enumeration e1 = this.mapping.elements();
        TaskHandler handler;
        while (e1.hasMoreElements()) {
            handler = (TaskHandler) e1.nextElement();
            list.addAll(handler.getCanceledTasks());
        }
        return list;
    }

    private TaskHandler createTaskHandler(String provider)
        throws InvalidProviderException {
        TaskHandler taskHandler;
        try {
            taskHandler = AbstractionFactory.newExecutionTaskHandler(provider);
        } catch (ProviderMethodException e) {
            throw new InvalidProviderException(
                "Cannot create new task handler for provider " + provider,
                e);
        }
        this.mapping.put(provider, taskHandler);
        return taskHandler;
    }
    
    public String toString() {
        return "ExecutionTaskHandler"; 
    }
}
