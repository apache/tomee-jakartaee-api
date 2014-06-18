/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openejb.javaee.api.activator.org.apache.openejb.javaee.api.locator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class ProviderLocator {
    // our bundle context
    static private Object context;
    // a service tracker for the registry service
    // NB:  This is declared as just Object to avoid classloading issues if we're running
    // outside of an OSGi environment.
    static private Object registryTracker;

    private static Class<?> bundleContextClazz = null;
    private static Class<?> serviceTrackerClazz = null;

    static {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            bundleContextClazz = cl.loadClass("org.osgi.framework.BundleContext");
            serviceTrackerClazz = cl.loadClass("org.osgi.util.tracker.ServiceTracker");
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private ProviderLocator() {
        // private constructor to prevent an instance from getting created.
    }

    /**
     * Utility class for locating a class with OSGi registry
     * support.  Uses the thread context classloader as part of
     * the search order.
     *
     * @param className The name of the target class.
     *
     * @return The loaded class.
     * @exception ClassNotFoundException
     *                   Thrown if the class cannot be located.
     */
    static public Class<?> loadClass(final String className) throws ClassNotFoundException {
        return loadClass(className, null, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Utility class for locating a class with OSGi registry
     * support.  Uses the thread context classloader as part of
     * the search order.
     *
     * @param className The name of the target class.
     *
     * @return The loaded class.
     * @exception ClassNotFoundException
     *                   Thrown if the class cannot be located.
     */
    static public Class<?> loadClass(final String className, final Class<?> contextClass) throws ClassNotFoundException {
        return loadClass(className, contextClass, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Standardized utility method for performing class lookups
     * with support for OSGi registry lookups.
     *
     * @param className The name of the target class.
     * @param loader    An optional class loader.
     *
     * @return The loaded class
     * @exception ClassNotFoundException
     *                   Thrown if the class cannot be loaded.
     */
    static public Class<?> loadClass(final String className, final Class<?>contextClass, ClassLoader loader) throws ClassNotFoundException {
        if (loader != null) {
            try {
                return loader.loadClass(className);
            } catch (final ClassNotFoundException x) {
                //ignore
            }
        }
        if (contextClass != null) {
            loader = contextClass.getClassLoader();
        }
        // try again using the class context loader
        return Class.forName(className, true, loader);
    }


    /**
     * Get a single service instance that matches an interface
     * definition.
     *
     * @param iface  The name of the required interface.
     * @param contextClass
     *               The class requesting the lookup (used for class resolution).
     * @param loader A class loader to use for searching for service definitions
     *               and loading classes.
     *
     * @return The service instance, or null if no matching services
     *         can be found.
     * @exception Exception Thrown for any classloading or exceptions thrown
     *                      trying to instantiate a service instance.
     */
    static public Object getService(final String iface, final Class<?> contextClass, final ClassLoader loader) throws Exception {
        // try for a classpath locatable instance next.  If we find an appropriate class mapping,
        // create an instance and return it.
        final Class<?> cls = locateServiceClass(iface, contextClass, loader);
        if (cls != null) {
            return cls.newInstance();
        }
        // a provider was not found
        return null;
    }


    /**
     * Locate a service class that matches an interface
     * definition.
     *
     * @param iface  The name of the required interface.
     * @param contextClass
     *               The class requesting the lookup (used for class resolution).
     * @param loader A class loader to use for searching for service definitions
     *               and loading classes.
     *
     * @return The located class, or null if no matching services
     *         can be found.
     * @exception Exception Thrown for any classloading exceptions thrown
     *                      trying to load the class.
     */
    static public Class<?> getServiceClass(final String iface, final Class<?> contextClass, final ClassLoader loader) throws ClassNotFoundException {
        return locateServiceClass(iface, contextClass, loader);
    }


    /**
     * Get a list of services that match a given interface
     * name.  This searches both the current class path and
     * the global repository for matches.
     *
     * @param iface  The name of the required interface.
     * @param contextClass
     *               The class requesting the lookup (used for class resolution).
     * @param loader A class loader to use for searching for service definitions
     *               and loading classes.
     *
     * @return A list of matching services.  Returns an empty list if there
     *         are no matches.
     * @exception Exception Thrown for any classloading or exceptions thrown
     *                      trying to instantiate a service instance.
     */
    static public List<Object> getServices(final String iface, final Class<?> contextClass, final ClassLoader loader) throws Exception {
        final List<Object> services = new ArrayList<Object>();

        // try for a classpath locatable instance second.  If we find an appropriate class mapping,
        // create an instance and return it.
        final Collection<Class<?>> classes = locateServiceClasses(iface, contextClass, loader);
        if (classes != null) {
            // create an instance of each of these classes
            for (final Class<?> cls : classes) {
                services.add(cls.newInstance());
            }
        }

        // now return the merged set
        return services;
    }


    /**
     * Get a list of service class implementations that match
     * an interface name.  This searches both the current class path and
     * the global repository for matches.
     *
     * @param iface  The name of the required interface.
     * @param contextClass
     *               The class requesting the lookup (used for class resolution).
     * @param loader A class loader to use for searching for service definitions
     *               and loading classes.
     *
     * @return A list of matching provider classes.  Returns an empty list if there
     *         are no matches.
     * @exception Exception Thrown for any classloading exceptions thrown
     *                      trying to load a provider class.
     */
    static public List<Class<?>> getServiceClasses(final String iface, final Class<?> contextClass, final ClassLoader loader) throws Exception {
        final Set<Class<?>> serviceClasses = new LinkedHashSet<Class<?>>();

        // try for a classpath locatable classes second.  If we find an appropriate class mapping,
        // add this to our return collection.
        final Collection<Class<?>> classes = locateServiceClasses(iface, contextClass, loader);
        if (classes != null) {
            serviceClasses.addAll(classes);
        }
        // now return the merged set
        return new ArrayList(serviceClasses);
    }


    /**
     * Locate the first class name for a META-INF/services definition
     * of a given class.  The first matching provider is
     * returned.
     *
     * @param iface  The interface class name used for the match.
     * @param loader The classloader for locating resources.
     *
     * @return The mapped provider name, if found.  Returns null if
     *         no mapping is located.
     */
    static private String locateServiceClassName(final String iface, final Class<?> contextClass, final ClassLoader loader) {
        // search first with the loader class path
        String name = locateServiceClassName(iface, loader);
        if (name != null) {
            return name;
        }
        // then with the context class, if there is one
        if (contextClass != null) {
            name = locateServiceClassName(iface, contextClass.getClassLoader());
            if (name != null) {
                return name;
            }
        }
        // not found
        return null;
    }


    /**
     * Locate a classpath-define service mapping.
     *
     * @param iface  The required interface name.
     * @param loader The ClassLoader instance to use to locate the service.
     *
     * @return The mapped class name, if one is found.  Returns null if the
     *         mapping is not located.
     */
    static private String locateServiceClassName(final String iface, final ClassLoader loader) {
        if (loader != null) {
            try {
                // we only look at resources that match the file name, using the specified loader
                final String service = "META-INF/services/" + iface;
                final Enumeration<URL> providers = loader.getResources(service);

                while (providers.hasMoreElements()) {
                    final List<String>providerNames = parseServiceDefinition(providers.nextElement());
                    // if there is something defined here, return the first entry
                    if (!providerNames.isEmpty()) {
                        return providerNames.get(0);
                    }
                }
            } catch (final IOException e) {
                //ignore
            }
        }
        // not found
        return null;
    }


    /**
     * Locate the first class for a META-INF/services definition
     * of a given interface class.  The first matching provider is
     * returned.
     *
     * @param iface  The interface class name used for the match.
     * @param loader The classloader for locating resources.
     *
     * @return The mapped provider class, if found.  Returns null if
     *         no mapping is located.
     */
    static private Class<?> locateServiceClass(final String iface, final Class<?> contextClass, final ClassLoader loader) throws ClassNotFoundException {
        final String className = locateServiceClassName(iface, contextClass, loader);
        if (className == null) {
            return null;
        }

        // we found a name, try loading the class.  This will throw an exception if there is an error
        return loadClass(className, contextClass, loader);
    }


    /**
     * Locate all class names name for a META-INF/services definition
     * of a given class.
     *
     * @param iface  The interface class name used for the match.
     * @param loader The classloader for locating resources.
     *
     * @return The mapped provider name, if found.  Returns null if
     *         no mapping is located.
     */
    static private Collection<String> locateServiceClassNames(final String iface, final Class<?> contextClass, final ClassLoader loader) {
        final Set<String> names = new LinkedHashSet<String>();

        locateServiceClassNames(iface, loader, names);
        if (contextClass != null) {
            locateServiceClassNames(iface, contextClass.getClassLoader(), names);
        }

        return names;
    }


    /**
     * Locate all class names name for a META-INF/services definition
     * of a given class.
     *
     * @param iface  The interface class name used for the match.
     * @param loader The classloader for locating resources.
     *
     * @return The mapped provider name, if found.  Returns null if
     *         no mapping is located.
     */
    static void locateServiceClassNames(final String iface, final ClassLoader loader, final Set names) {
        if (loader != null) {

            try {
                // we only look at resources that match the file name, using the specified loader
                final String service = "META-INF/services/" + iface;
                final Enumeration<URL> providers = loader.getResources(service);

                while (providers.hasMoreElements()) {
                    final List<String>providerNames = parseServiceDefinition(providers.nextElement());
                    // just add all of these to the list
                    names.addAll(providerNames);
                }
            } catch (final IOException e) {
            }
        }
    }


    /**
     * Locate all classes that map to a given provider class definition.  This will
     * search both the services directories, as well as the provider classes from the
     * OSGi provider registry.
     *
     * @param iface  The interface class name used for the match.
     * @param loader The classloader for locating resources.
     *
     * @return A list of all mapped classes, if found.  Returns an empty list if
     *         no mappings are found.
     */
    static private Collection<Class<?>> locateServiceClasses(final String iface, final Class<?> contextClass, final ClassLoader loader) throws ClassNotFoundException {
        // get the set of names from services definitions on the classpath
        final Collection<String> classNames = locateServiceClassNames(iface, contextClass, loader);
        final Set<Class<?>> classes = new LinkedHashSet<Class<?>>();

        // load each class and add to our return set
        for (final String name : classNames) {
            classes.add(loadClass(name, contextClass, loader));
        }
        return classes;
    }


    /**
     * Parse a definition file and return the names of all included implementation classes
     * contained within the file.
     *
     * @param u      The URL of the file
     *
     * @return A list of all matching classes.  Returns an empty list
     *         if no matches are found.
     */
    static private List<String> parseServiceDefinition(final URL u) {
        final String url = u.toString();
        final List<String> classes = new ArrayList<String>();
        // ignore directories
        if (url.endsWith("/")) {
            return classes;
        }
        // the identifier used for the provider is the last item in the URL.
        final String providerId = url.substring(url.lastIndexOf("/") + 1);
        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream(), "UTF-8"));
            // the file can be multiple lines long, with comments.  A single file can define multiple providers
            // for a single key, so we might need to create multiple entries.  If the file does not contain any
            // definition lines, then as a default, we use the providerId as an implementation class also.
            String line = br.readLine();
            while (line != null) {
                // we allow comments on these lines, and a line can be all comment
                final int comment = line.indexOf('#');
                if (comment != -1) {
                    line = line.substring(0, comment);
                }
                line = line.trim();
                // if there is nothing left on the line after stripping white space and comments, skip this
                if (line.length() > 0) {
                    // add this to our list
                    classes.add(line);
                }
                // keep reading until the end.
                line = br.readLine();
            }
            br.close();
        } catch (final IOException e) {
            // ignore errors and handle as default
        }
        return classes;
    }

    /**
     * Perform a service class discovery by looking for a
     * property in a target properties file located in the
     * java.home directory.
     *
     * @param path     The relative path to the desired properties file.
     * @param property The name of the required property.
     *
     * @return The value of the named property within the properties file.  Returns
     *         null if the property doesn't exist or the properties file doesn't exist.
     */
    public static String lookupByJREPropertyFile(final String path, final String property) throws IOException {
        final String jreDirectory = System.getProperty("java.home");
        final File configurationFile = new File(jreDirectory + File.separator + path);
        if (configurationFile.exists() && configurationFile.canRead()) {
            final Properties properties = new Properties();
            InputStream in = null;
            try {
                in = new FileInputStream(configurationFile);
                properties.load(in);
                return properties.getProperty(property);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (final Exception e) {
                        //ignore
                    }
                }
            }
        }
        return null;
    }
}
