/*
 * Created on Jun 6, 2006
 */
package org.griphyn.vdl.mapping;

import org.griphyn.vdl.karajan.Loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.griphyn.vdl.type.Field;
import org.griphyn.vdl.type.Type;
import org.griphyn.vdl.type.Types;

import org.griphyn.vdl.karajan.VDL2FutureException;

import org.griphyn.vdl.util.VDL2Config;


public abstract class AbstractDataNode implements DSHandle {

	static final String DATASET_URI_PREFIX = "tag:benc@ci.uchicago.edu,2008:swift:dataset:";

	public static final Logger logger = Logger
			.getLogger(AbstractDataNode.class);

	public static final MappingParam PARAM_PREFIX = new MappingParam("prefix",
			null);

	/**
	 * Datasets are identified within a run by this sequence number and the
	 * partial ID field. The initial value is chosen to aid human recognition of
	 * sequence numbers in the wild. There is no requirement that it start at
	 * this (or any other) particular value. Note that this introduces a maximum
	 * on the number of datasets which can be dealt with in any run to be about
	 * 2^62.
	 */
	private static long datasetIDCounter = 720000000000l;

	/**
	 * This is used to provide a (hopefully) globally unique identifier for each
	 * time the datasetIDCounter is reset (whenever this class is loaded, which
	 * will usually happen once per JVM). No meaning should be inferred from
	 * this value - it exists purely for making unique URIs.
	 */
	private static final String datasetIDPartialID = Loader.getUUID();

	private Field field;
	private Map handles;
	private Object value;
	private boolean closed;
	private List listeners;
	final String identifierURI = makeIdentifierURIString();
	private Path pathFromRoot;

	protected AbstractDataNode(Field field) {
		this.field = field;
		handles = new HashMap();
	}

	public void init(Map params) {

	}

	public Type getType() {
		return field.getType();
	}

	public boolean isPrimitive() {
		return field.getType().isPrimitive();
	}

	public boolean isRestartable() {
		return !isPrimitive();
	}

	protected Field getField() {
		return field;
	}

	/**
	 * create a String representation of this node. If the node has a value,
	 * then uses the String representation of that value. Otherwise, generates a
	 * text description.
	 */
	public String toString() {
		if (this.value != null && !(this.value instanceof Exception)) {
			// special handling for ints...
			if (this.getType().equals(Types.INT)) {
				try {
					Number n = (Number) this.getValue();
					return String.valueOf(n.intValue());
				}
				catch (ClassCastException e) {
					throw new RuntimeException(
							"Internal type error. Value is not a Number for "
									+ getDisplayableName() + getPathFromRoot());
				}
			}
			else {
				return this.value.toString();
			}
		}
		return getIdentifyingString();
	}

	public String getIdentifyingString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getClass().getName());

		sb.append(" identifier ");
		sb.append(this.getIdentifier());

		sb.append(" type ");
		sb.append(getType());

		if (value == null) {
			sb.append(" with no value at dataset=");
		}
		else if (value instanceof Throwable) {
			sb.append(" containing throwable ");
			sb.append(value.getClass());
			sb.append(" dataset=");
		}
		else {
			sb.append(" value=");
			sb.append(this.value.toString());
			sb.append(" dataset=");
		}

		sb.append(getDisplayableName());

		if (!Path.EMPTY_PATH.equals(getPathFromRoot())) {
			sb.append(" path=");
			sb.append(getPathFromRoot().toString());
		}

		if (closed) {
			sb.append(" (closed)");
		}
		else {
			sb.append(" (not closed)");
		}

		return sb.toString();
	}

	protected String getDisplayableName() {
		String prefix = getRoot().getParam("dbgname");
		if (prefix == null) {
			prefix = getRoot().getParam("prefix");
		}
		if (prefix == null) {
			prefix = "unnamed SwiftScript value";
		}
		return prefix;
	}

	public DSHandle getField(Path path) throws InvalidPathException {
		if (path.isEmpty()) {
			return this;
		}
		try {
			DSHandle handle = getField(path.getFirst());
			if (path.size() > 1) {
				return handle.getField(path.butFirst());
			}
			else {
				return handle;
			}
		}
		catch (NoSuchFieldException e) {
			throw new InvalidPathException(path, this);
		}
	}

	public Collection getFields(Path path) throws InvalidPathException,
			HandleOpenException {
		List fields = new ArrayList();
		getFields(fields, path);
		return fields;
	}

	private void getFields(List fields, Path path) throws InvalidPathException,
			HandleOpenException {
		if (path.isEmpty()) {
			fields.add(this);
		}
		else {
			Path rest = path.butFirst();
			if (path.isWildcard(0)) {
				if (isArray() && !closed) {
					throw new HandleOpenException(this);
				}
				Iterator i = this.handles.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry e = (Map.Entry) i.next();
					((AbstractDataNode) e.getValue()).getFields(fields, rest);
				}
			}
			else {
				try {
					((AbstractDataNode) getField(path.getFirst())).getFields(
							fields, rest);
				}
				catch (NoSuchFieldException e) {
					throw new InvalidPathException(path, this);
				}
			}
		}
	}

	public void set(DSHandle handle) {
		// TODO check type
		if (closed) {
			throw new IllegalArgumentException(this.getDisplayableName()
					+ " is already assigned");
		}
		if (getParent() == null) {
			/*
			 * AbstractDataNode node = (AbstractDataNode)handle; field =
			 * node.getField(); handles = node.getHandles(); closed =
			 * node.isClosed(); value = node.getValue();
			 */
			throw new RuntimeException("Can't set root data node!");
		}
		else {
			((AbstractDataNode) getParent()).setField(field.getName(), handle);
		}
	}

	protected void setField(String name, DSHandle handle) {
		synchronized (handles) {
			handles.put(name, handle);
		}
	}

	protected synchronized DSHandle getField(String name)
			throws NoSuchFieldException {
		DSHandle handle = getHandle(name);
		if (handle == null) {
			if (closed) {
				throw new NoSuchFieldException(name);
			}
			else {
				handle = createDSHandle(name);
			}

		}
		return handle;
	}

	protected DSHandle getHandle(String name) {
		synchronized (handles) {
			return (DSHandle) handles.get(name);
		}
	}

	protected boolean isHandlesEmpty() {
		synchronized (handles) {
			return handles.isEmpty();
		}
	}

	public DSHandle createDSHandle(String fieldName)
			throws NoSuchFieldException {
		if (closed) {
			throw new RuntimeException("Cannot write to closed handle: " + this
					+ " (" + fieldName + ")");
		}

		AbstractDataNode child;
		Field childField = getChildField(fieldName);
		if (childField.getType().isArray()) {
			child = new ArrayDataNode(getChildField(fieldName), getRoot(), this);
		}
		else {
			child = new DataNode(getChildField(fieldName), getRoot(), this);
		}

		synchronized (handles) {
			Object o = handles.put(fieldName, child);
			if (o != null) {
				throw new RuntimeException(
						"Trying to create a handle that already exists ("
								+ fieldName + ") in " + this);
			}
		}
		return child;
	}

	protected Field getChildField(String fieldName) throws NoSuchFieldException {
		return Field.Factory.createField(fieldName, field.getType().getField(
				fieldName).getType());
	}

	protected void checkDataException() {
		if (value instanceof DependentException) {
			throw (DependentException) value;
		}
	}

	protected void checkMappingException() {
		if (value instanceof MappingDependentException) {
			throw (MappingDependentException) value;
		}
	}

	public Object getValue() {
		checkDataException();
		if (field.getType().isArray()) {
			return handles;
		}
		else {
			return value;
		}
	}

	public Map getArrayValue() {
		checkDataException();
		if (!field.getType().isArray()) {
			throw new RuntimeException("getArrayValue called on a struct: "
					+ this);
		}
		return handles;
	}

	public boolean isArray() {
		return false;
	}

	public void setValue(Object value) {
		if (this.closed) {
			throw new IllegalArgumentException(this.getDisplayableName()
					+ " is closed with a value of " + this.value);
		}
		if (this.value != null) {
			throw new IllegalArgumentException(this.getDisplayableName()
					+ " is already assigned with a value of " + this.value);
		}
		this.value = value;
		closeShallow();
	}

	public Collection getFringePaths() throws HandleOpenException {
		ArrayList list = new ArrayList();
		getFringePaths(list, Path.EMPTY_PATH);
		return list;
	}

	public void getFringePaths(List list, Path parentPath)
			throws HandleOpenException {
		checkMappingException();
		if (getField().getType().getBaseType() != null) {
			list.add(Path.EMPTY_PATH);
		}
		else {
			Iterator i = getField().getType().getFields().iterator();
			while (i.hasNext()) {
				Field field = (Field) i.next();
				AbstractDataNode mapper;
				try {
					mapper = (AbstractDataNode) this.getField(field.getName());
				}
				catch (NoSuchFieldException e) {
					throw new RuntimeException(
							"Inconsistency between type declaration and handle for field '"
									+ field.getName() + "'");
				}
				// [m] Hmm. Should there be a difference between field and
				// mapper.getField()?
				// Path fullPath =
				// parentPath.addLast(mapper.getField().getName());
				Path fullPath = parentPath.addLast(field.getName());
				if (!mapper.field.getType().isPrimitive() && !mapper.isArray() && mapper.field.getType().getFields().size() == 0) {
					list.add(fullPath);
				}
				else {
					mapper.getFringePaths(list, fullPath);
				}
			}
		}
	}

	public synchronized void closeShallow() {
		if (this.closed) {
			return;
		}
		this.closed = true;
		notifyListeners();
		if (logger.isInfoEnabled()) {
			logger.info("closed " + this.getIdentifyingString());
		}
		// so because its closed, we can dump the contents

		try {
			if(VDL2Config.getConfig().getProvenanceLog()) {
				logContent();
			}
		}
		catch (Exception e) {
			logger.warn("Exception whilst logging dataset content for " + this,
				e);
		}
		// TODO record retrospective provenance information for this dataset
		// here
		// we should do it at closing time because that's the point at which we
		// know the dataset has its values (all the way down the tree) assigned.

		// provenance-id for this dataset should have been assigned at creation
		// time,
		// though, so that we can refer to this dataset elsewhere before it is
		// closed.

		// is this method the only one called to set this.closed? or do
		// subclasses
		// or other methods ever change it?
	}

	public synchronized void logContent() {
		String identifier = this.getIdentifier();
		Path pathFromRoot = this.getPathFromRoot();
		if (this.getPathFromRoot() != null) {
			if (logger.isInfoEnabled()) {
				logger.info("ROOTPATH dataset=" + identifier + " path="
						+ pathFromRoot);
				if (this.getType().isPrimitive()) {
					logger.info("VALUE dataset=" + identifier + " VALUE="
							+ this.toString());
				}
			}

			Mapper m;

			try {
				m = this.getMapper();
			}
			catch (VDL2FutureException fe) {
				m = null; // no mapping info if mapper isn't initialised yet
			}
			if (m != null) {
				// TODO proper type here
				// Not sure catching exception here is really the right thing to
				// do here
				// anyway - should perhaps only be trying to map leafnodes?
				// Mapping
				// non-leaf stuff is giving wierd paths anyway

// TODO this is perhaps an unpleasant way of finding if this is a file-backed
// leaf node or not
				boolean filemapped = true;
				Type type = this.getType();
				if(type.getName().equals("external")) {
					filemapped = false;
				}
				if(type.isPrimitive()) {
					filemapped = false;
				}
				if(type.isArray()) {
					filemapped = false;
				}
				if(handles.size()>0) {
					filemapped = false;
				}

				try {
					if(filemapped) {
						Object path = m.map(pathFromRoot);
						if (logger.isInfoEnabled()) {
							logger.info("FILENAME dataset=" + identifier + " filename="
								+ path);
						}
					}
				}
				catch (Exception e) {
					if (logger.isInfoEnabled()) {
						logger.info("NOFILENAME dataset=" + identifier);
					}
				}
			}
		}

		synchronized (handles) {
			Iterator i = handles.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry) i.next();
				AbstractDataNode node = (AbstractDataNode) e.getValue();
				if (logger.isInfoEnabled()) {
					logger.info("CONTAINMENT parent=" + identifier + " child="
							+ node.getIdentifier());
				}
				node.logContent();
			}
		}

	}

	public boolean isClosed() {
		return closed;
	}

	public void closeDeep() {
		if (!this.closed) {
			closeShallow();
		}
		synchronized (handles) {
			Iterator i = handles.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry) i.next();
				AbstractDataNode mapper = (AbstractDataNode) e.getValue();
				mapper.closeDeep();
			}
		}
	}

	/** Recursively closes arrays through a tree of arrays and complex
 	    types. */
	public void closeDeepStructure() {
		if (!this.closed && this.getType().isArray()) {
			closeShallow();
		}
		synchronized (handles) {
			Iterator i = handles.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry e = (Map.Entry) i.next();
				AbstractDataNode child = (AbstractDataNode) e.getValue();
				if(child.getType().isArray()  ||
				   child.getType().getFields().size() > 0 ) {
					child.closeDeepStructure();
				}
			}
		}
	}

	public synchronized Path getPathFromRoot() {
		if (pathFromRoot == null) {
			AbstractDataNode parent = (AbstractDataNode) this.getParent();
			Path myPath;
			if (parent != null) {
				myPath = parent.getPathFromRoot();
				pathFromRoot = myPath.addLast(getField().getName(), parent
						.getField().getType().isArray());
			}
			else {
				pathFromRoot = Path.EMPTY_PATH;
			}
		}
		return pathFromRoot;
	}

	public Mapper getMapper() {
		return ((AbstractDataNode) getRoot()).getMapper();
	}

	protected Map getHandles() {
		return handles;
	}

	public synchronized void addListener(DSHandleListener listener) {
		if (logger.isInfoEnabled()) {
			logger.info("Adding handle listener \"" + listener + "\" to \""
					+ getIdentifyingString() + "\"");
		}
		if (listeners == null) {
			listeners = new LinkedList();
		}
		listeners.add(listener);
		if (closed) {
			notifyListeners();
		}
	}

	protected synchronized void notifyListeners() {
		if (listeners != null) {
			Iterator i = listeners.iterator();
			while (i.hasNext()) {
				DSHandleListener listener = (DSHandleListener) i.next();
				i.remove();
				if (logger.isInfoEnabled()) {
					logger.info("Notifying listener \"" + listener
							+ "\" about \"" + getIdentifyingString() + "\"");
				}
				listener.handleClosed(this);
			}
			listeners = null;
		}
	}

	public String getIdentifier() {
		return identifierURI;
	}

	String makeIdentifierURIString() {
		datasetIDCounter++;
		return DATASET_URI_PREFIX + datasetIDPartialID + ":" + datasetIDCounter;
	}
}