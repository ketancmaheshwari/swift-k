//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Jul 5, 2014
 */
package org.griphyn.vdl.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.globus.cog.abstraction.impl.common.AbstractionFactory;
import org.globus.cog.abstraction.impl.common.ProviderMethodException;
import org.globus.cog.abstraction.impl.common.task.ExecutionServiceImpl;
import org.globus.cog.abstraction.impl.common.task.InvalidProviderException;
import org.globus.cog.abstraction.impl.common.task.ServiceContactImpl;
import org.globus.cog.abstraction.impl.common.task.ServiceImpl;
import org.globus.cog.abstraction.interfaces.ExecutionService;
import org.globus.cog.abstraction.interfaces.Service;
import org.globus.cog.abstraction.interfaces.ServiceContact;
import org.globus.cog.karajan.util.BoundContact;
import org.globus.swift.catalog.site.Application;
import org.globus.swift.catalog.site.SwiftContact;
import org.globus.swift.catalog.site.SwiftContactSet;
import org.griphyn.vdl.util.ConfigTree.Node;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigIncludeContext;
import com.typesafe.config.ConfigIncluder;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

public class SwiftConfig implements Cloneable {
    public static final Logger logger = Logger.getLogger(SwiftConfig.class);
    
    public static final boolean CHECK_DYNAMIC_NAMES = true;
    public static final List<String> DEFAULT_LOCATIONS;
    
    public enum Key {
        DM_CHECKER("mappingCheckerEnabled"),
        PROVENANCE_LOG("logProvenance"),
        FILE_GC_ENABLED("fileGCEnabled"),
        TICKER_ENABLED("tickerEnabled"),
        TICKER_DATE_FORMAT("tickerDateFormat"),
        TICKER_PREFIX("tickerPrefix"),
        TRACING_ENABLED("tracingEnabled"),
        FOREACH_MAX_THREADS("maxForeachThreads"),
        CACHING_ALGORITHM("cachingAlgorithm"), 
        REPLICATION_ENABLED("replicationEnabled"), 
        WRAPPER_STAGING_LOCAL_SERVER("wrapperStagingLocalServer"), 
        REPLICATION_MIN_QUEUE_TIME("replicationMinQueueTime"), 
        REPLICATION_LIMIT("replicationLimit"), 
        WRAPPER_INVOCATION_MODE("wrapperInvocationMode");
        
        public String propName;
        private Key(String propName) {
            this.propName = propName;
        }
    }
    
    public static SwiftConfigSchema SCHEMA;
    
    static {
        SCHEMA = new SwiftConfigSchema();
        DEFAULT_LOCATIONS = new ArrayList<String>();
        if (System.getenv("SWIFT_SITE_CONF") != null) {
            DEFAULT_LOCATIONS.add(System.getenv("SWIFT_SITE_CONF"));
        }
        String swiftHome = System.getProperty("swift.home");
        if (swiftHome == null) {
            swiftHome = System.getenv("SWIFT_HOME");
            if (swiftHome == null) {
                throw new IllegalStateException("SWIFT_HOME is not set");
            }
        }
        
        DEFAULT_LOCATIONS.add(swiftHome + File.separator + 
            "etc" + File.separator + "swift.conf");
        
        // check keys against schema
        
        for (Key k : Key.values()) {
            if (!SCHEMA.isNameValid(k.propName)) {
                throw new IllegalArgumentException("Invalid property name for config key '" + k + "': " + k.propName);
            }
        }
    }
    
    private static class KVPair {
        public final String key;
        public final String value;
        
        public KVPair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private static class IncluderWrapper implements ConfigIncluder {
        private final ConfigIncluder d;
        
        public IncluderWrapper(ConfigIncluder d) {
            this.d = d;
        }

        @Override
        public ConfigIncluder withFallback(ConfigIncluder fallback) {
            return this;
        }

        @Override
        public ConfigObject include(ConfigIncludeContext context, String what) {
            int b = what.indexOf("${");
            while (b != -1) {
                int e = what.indexOf("}", b);
                String var = what.substring(b + 2, e);
                what = what.substring(0, b) + resolve(var) + what.substring(e + 1);
                b = what.indexOf("${");
            }
            return ConfigFactory.parseFile(new File(what)).root();
        }

        private String resolve(String var) {
            String v = null;
            if (var.startsWith("env.")) {
                v = System.getenv(var.substring(4));
            }
            else {
                v = System.getProperty(var);
            }
            if (v == null) {
                throw new IllegalArgumentException("No such system property or environment variable: '" + var + "'");
            }
            return v;
        }
    }
    
    public static SwiftConfig load(String fileName) {
        return load(fileName, null);
    }

    public static SwiftConfig load(String fileName, Map<String, Object> override) {
        logger.info("Loading swift configuration file: " + fileName);
        ConfigParseOptions opt = ConfigParseOptions.defaults();
        opt = opt.setIncluder(new IncluderWrapper(opt.getIncluder())).
            setSyntax(ConfigSyntax.CONF).setAllowMissing(false);
        Config conf = ConfigFactory.parseFile(new File(fileName), opt);
        Config oconf = ConfigFactory.parseMap(override, "<command line>");
        conf = oconf.withFallback(conf);
        conf = conf.resolveWith(getSubstitutions());
        ConfigTree<Object> out = SCHEMA.validate(conf);
        SwiftConfig sc = new SwiftConfig();
        sc.setFileName(fileName);
        sc.build(out);
        return sc;
    }

    private static Config getSubstitutions() {
        Map<String, Object> m = new HashMap<String, Object>();
        
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            m.put("env." + e.getKey(), e.getValue());
        }
        
        return ConfigFactory.parseMap(m).withFallback(ConfigFactory.parseProperties(System.getProperties()));
    }

    public static SwiftConfig load() {
        for (String loc : DEFAULT_LOCATIONS) {
            if (new File(loc).exists()) {
                return load(loc);
            }
        }
        throw new IllegalStateException("Could not find swift configuration file");
    }
    
    private static SwiftConfig _default;
    
    public synchronized static SwiftConfig getDefault() {
        if (_default == null) {
            _default = load();
        }
        return _default;
    }
    
    public synchronized static void setDefault(SwiftConfig conf) {
        _default = conf;
    }
        
    private SwiftContactSet definedSites;
    private SwiftContactSet sites;
    private ConfigTree<Object> tree;
    private Map<String, Object> flat;
    private String fileName;
    
    public SwiftConfig() {
        definedSites = new SwiftContactSet();
        sites = new SwiftContactSet();
        flat = new HashMap<String, Object>();
    }
    
    public SwiftContactSet getSites() {
        return sites;
    }
    
    public SwiftContactSet getDefinedSites() {
        return definedSites;
    }
    
    public Collection<String> getDefinedSiteNames() {
        Set<String> s = new TreeSet<String>();
        for (BoundContact bc : definedSites.getContacts()) {
            s.add(bc.getName());
        }
        return s;
    }
    
    public void setProperty(String key, Object value) {
        flat.put(key, value);
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @SuppressWarnings("unchecked")
    private void build(ConfigTree<Object> tree) {
        this.tree = tree;
        List<String> sites = null;
        for (Map.Entry<String, ConfigTree.Node<Object>> e : tree.entrySet()) {
            if (e.getKey().equals("site")) {
                for (Map.Entry<String, ConfigTree.Node<Object>> f : e.getValue().entrySet()) {
                    site(definedSites, f.getKey(), f.getValue());
                }
            }
            else if (e.getKey().equals("sites")) {
                sites = (List<String>) getObject(e.getValue());
            }
            else if (e.getKey().equals("app")) {
                SwiftContact dummy = new SwiftContact();
                apps(dummy, e.getValue());
                for (Application app : dummy.getApplications()) {
                    definedSites.addApplication(app);
                }
            }
        }
        if (sites == null || sites.isEmpty()) {
            throw new RuntimeException("No sites enabled");
        }
        for (String siteName : sites) {
            this.sites.addContact((SwiftContact) definedSites.getContact(siteName));
        }
        this.sites.getApplications().putAll(definedSites.getApplications());
        
        for (String leaf : tree.getLeafPaths()) {
            flat.put(leaf, tree.get(leaf));
        }
    }

    private void apps(SwiftContact sc, ConfigTree.Node<Object> n) {
        /*
         * app."*" {
         *   ...
         * }
         */
                
        for (Map.Entry<String, ConfigTree.Node<Object>> e : n.entrySet()) {
            String k = e.getKey();
            ConfigTree.Node<Object> c = e.getValue();
            
            if (e.getKey().equals("ALL")) {
                sc.addApplication(app("*", e.getValue()));
            }
            else {
                sc.addApplication(app(removeQuotes(e.getKey()), e.getValue()));
            }
        }
        
        Application all = sc.getApplication("*");
        if (all != null) {
            mergeEnvsToApps(sc, all.getEnv());
            mergePropsToApps(sc, all.getProperties());
        }
    }
        
    private String removeQuotes(String key) {
        if (key.startsWith("\"") && key.endsWith("\"")) {
            return key.substring(1, key.length() - 2);
        }
        else {
            return key;
        }
    }

    private void mergeEnvsToApps(SwiftContact bc, Map<String, String> envs) {
        for (Application app : bc.getApplications()) {
            for (Map.Entry<String, String> e : envs.entrySet()) {
                if (!app.getEnv().containsKey(e.getKey())) {
                    // only merge if app does not override
                    app.setEnv(e.getKey(), e.getValue());
                }
            }
        }
    }
    
    private void mergePropsToApps(SwiftContact bc, Map<String, Object> props) {
        for (Application app : bc.getApplications()) {
            for (Map.Entry<String, Object> e : props.entrySet()) {
                if (!app.getProperties().containsKey(e.getKey())) {
                    app.addProperty(e.getKey(), e.getValue());
                }
            }
        }
    }
    
    private Application app(String name, ConfigTree.Node<Object> n) {
        /*
         * app."*" {
         *  executable: "?String"
         *
         *  options: "?Object"
         *  jobType: "?String", queue: "?String", project: "?String"
         *  maxWallTime: "?Time"
         *
         *  env."*": "String"
         * }
         */
        
        Application app = new Application();
        app.setName(name);
        
        for (Map.Entry<String, ConfigTree.Node<Object>> e : n.entrySet()) {
            String k = e.getKey();
            ConfigTree.Node<Object> c = e.getValue();
            
            if (k.equals("executable")) {
                app.setExecutable(getString(c));
            }
            else if (k.equals("options")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> opt = (Map<String, Object>) getObject(c, "options");
                for (Map.Entry<String, Object> f : opt.entrySet()) {
                    app.addProperty(f.getKey(), f.getValue());
                }
            }
            else if (k.equals("jobQueue")) {
                app.addProperty("queue", getString(c));
            }
            else if (k.equals("jobProject")) {
                app.addProperty("project", getString(c));
            }
            else if (k.equals("env")) {
                List<KVPair> envs = envs(c);
                for (KVPair env : envs) {
                    app.setEnv(env.key, env.value);
                }
            }
            else {
                app.addProperty(k, getString(c));
            }
        }
                
        return app;
    }


    private List<KVPair> envs(Node<Object> n) {
        List<KVPair> l = new ArrayList<KVPair>();
        for (Map.Entry<String, ConfigTree.Node<Object>> e : n.entrySet()) {
            l.add(new KVPair(e.getKey(), getString(e.getValue())));
        }
        return l;
    }

    private void site(SwiftContactSet sites, String name, ConfigTree.Node<Object> n) {
        try {
            SwiftContact sc = new SwiftContact(name);
    
            if (n.hasKey("OS")) {
                sc.setProperty("sysinfo", getString(n, "OS"));
            }
                    
            
            for (Map.Entry<String, ConfigTree.Node<Object>> e : n.entrySet()) {
                String ctype = e.getKey();
                ConfigTree.Node<Object> c = e.getValue();
                
                if (ctype.equals("execution")) {
                    sc.addService(execution(c));
                }
                else if (ctype.equals("filesystem")) {
                    sc.addService(filesystem(c));
                }
                else if (ctype.equals("workDirectory")) {
                    sc.setProperty("workdir", getString(c));
                }
                else if (ctype.equals("scratch")) {
                    sc.setProperty("scratch", getString(c));
                }
                else if (ctype.equals("app")) {
                    apps(sc, c);
                }
                else if (ctype.equals("options")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> opt = (Map<String, Object>) getObject(c, "options");
                    for (Map.Entry<String, Object> f : opt.entrySet()) {
                        sc.setProperty(f.getKey(), f.getValue());
                    }
                }
                else {
                    sc.setProperty(ctype, getObject(c));
                }
            }
            sites.addContact(sc);
        }
        catch (Exception e) {
            throw new RuntimeException("Invalid site entry '" + name + "': ", e);
        }
    }
    
    private Service filesystem(Node<Object> c) throws InvalidProviderException, ProviderMethodException {        
        Service s = new ServiceImpl();
        s.setType(Service.FILE_OPERATION);
        service(c, s);
        return s;
    }

    private Service execution(ConfigTree.Node<Object> n) throws InvalidProviderException, ProviderMethodException {
        ExecutionService s = new ExecutionServiceImpl();
        service(n, s);                        
        return s;
    }

    private void service(Node<Object> n, Service s) throws InvalidProviderException, ProviderMethodException {
        String provider = null;
        String url = null;
        for (Map.Entry<String, ConfigTree.Node<Object>> e : n.entrySet()) {
            String k = e.getKey();
            ConfigTree.Node<Object> c = e.getValue();
            
            if (k.equals("type")) {
                provider = getString(c);
            }
            else if (k.equals("URL")) {
                url = getString(c);
            }
            else if (k.equals("jobManager")) {
                ((ExecutionService) s).setJobManager(getString(c));
            }
            else if (k.equals("jobProject")) {
                s.setAttribute("project", getObject(c));
            }
            else if (k.equals("maxJobs")) {
                s.setAttribute("slots", getObject(c));
            }
            else if (k.equals("maxJobTime")) {
                s.setAttribute("maxTime", getObject(c));
            }
            else if (k.equals("maxNodesPerJob")) {
                s.setAttribute("maxNodes", getObject(c));
            }
            else if (k.equals("jobQueue")) {
                s.setAttribute("queue", getObject(c));
            }
            else {
                s.setAttribute(k, getObject(c));
            }
        }
        
        s.setProvider(provider);
        if (url != null) {
            ServiceContact contact = new ServiceContactImpl(url);
            s.setServiceContact(contact);
            s.setSecurityContext(AbstractionFactory.newSecurityContext(provider, contact));
        }
    }

    private String getString(Node<Object> c) {
        return (String) c.get();
    }

    private Object getObject(ConfigTree.Node<Object> c) {
        return c.get();
    }
    
    private String getString(ConfigTree.Node<Object> c, String key) {
        return (String) c.get(key);
    }
    
    private Object getObject(ConfigTree.Node<Object> c, String key) {
        return c.get(key);
    }
    
    private void checkType(String name, ConfigValue value, ConfigValueType type) {
        if (!type.equals(value.valueType())) {
            throw new SwiftConfigException(value.origin(), 
                "'" + name + "': wrong type (" + value.valueType() + "). Must be a " + type);
        }
    }
    
    public Object clone() {
        return this;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        SortedSet<String> s = new TreeSet<String>(flat.keySet());
        for (String k : s) {
            sb.append(k);
            sb.append(": ");
            Object o = flat.get(k);
            if (o instanceof String) {
                sb.append('"');
                sb.append(o);
                sb.append('"');
            }
            else {
                sb.append(o);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
    
    private void check(String name) {
        if (CHECK_DYNAMIC_NAMES) {
            if (!SCHEMA.isNameValid(name)) {
                throw new IllegalArgumentException("Unknown property name: '" + name + "'");
            }
        }
    }
    
    private Object get(Key k) {
        return flat.get(k.propName);
    }

    public Object getProperty(String name) {
        check(name);
        return flat.get(name);
    }

    public String getStringProperty(String name) {
        check(name);
        return (String) flat.get(name);
    }

    public boolean isFileGCEnabled() {
        return (Boolean) get(Key.FILE_GC_ENABLED);
    }

    public boolean isTickerEnabled() {
        return (Boolean) get(Key.TICKER_ENABLED);
    }

    public String getTickerDateFormat() {
        return (String) get(Key.TICKER_DATE_FORMAT);
    }

    public String getTickerPrefix() {
        return (String) get(Key.TICKER_PREFIX);
    }

    public boolean isTracingEnabled() {
        return (Boolean) get(Key.TRACING_ENABLED);
    }

    public Object getProperty(String name, Object defVal) {
        check(name);
        Object v = flat.get(name);
        if (v == null) {
            return defVal;
        }
        else {
            return v;
        }
    }
    
    public int getForeachMaxThreads() {
        return (Integer) get(Key.FOREACH_MAX_THREADS);
    }

    public String getCachingAlgorithm() {
        return (String) get(Key.CACHING_ALGORITHM);
    }

    public boolean isReplicationEnabled() {
        return (Boolean) get(Key.REPLICATION_ENABLED);
    }

    public String getWrapperStagingLocalServer() {
        return (String) get(Key.WRAPPER_STAGING_LOCAL_SERVER);
    }
    
    public boolean isProvenanceEnabled() {
        return (Boolean) get(Key.PROVENANCE_LOG);
    }

    public int getReplicationMinQueueTime() {
        return (Integer) get(Key.REPLICATION_MIN_QUEUE_TIME);
    }

    public int getReplicationLimit() {
        return (Integer) get(Key.REPLICATION_LIMIT);
    }

    public String getWrapperInvocationMode() {
        return (String) get(Key.WRAPPER_INVOCATION_MODE);
    }

    public boolean isMappingCheckerEnabled() {
        return (Boolean) get(Key.DM_CHECKER);
    }
}
