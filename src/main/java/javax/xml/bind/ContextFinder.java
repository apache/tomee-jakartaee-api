/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package javax.xml.bind;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * we use it to endorse tomee and we don't want to depend on OSGi as it is done in geronimo
 */
@SuppressWarnings("UnusedDeclaration")
class ContextFinder {

    private static final String PLATFORM_DEFAULT_FACTORY_CLASS = "com.sun.xml.bind.v2.ContextFactory";
    private static final String JAXB_CONTEXT_PROPERTY = JAXBContext.class.getName();
    private static final String JAXB_CONTEXT_FACTORY = JAXBContext.JAXB_CONTEXT_FACTORY;

    private static Class<?> osgiLocator;
    private static Method getServiceClassMethod;
    private static Method loadClassMethod;

    static {
        try {
            osgiLocator = Thread.currentThread().getContextClassLoader().loadClass("org.apache.geronimo.osgi.locator.ProviderLocator");
            getServiceClassMethod = osgiLocator.getMethod("getServiceClass", String.class, Class.class, ClassLoader.class);
            loadClassMethod = osgiLocator.getMethod("loadClass", String.class, Class.class, ClassLoader.class);
        } catch (final Exception e) {
            osgiLocator = null;
        } catch (final NoClassDefFoundError ncdfe) {
            osgiLocator = null;
        }
    }

    public static JAXBContext find(String contextPath, final ClassLoader classLoader, final Map properties) throws JAXBException {
        contextPath = contextPath.trim();
        if (contextPath.length() == 0 || contextPath.equals(":")) {
            throw new JAXBException("Invalid contextPath");
        }
        String className = null;
        final String[] packages = contextPath.split("[:]");
        for (final String pkg : packages) {
            final String url = pkg.replace('.', '/') + "/jaxb.properties";
            className = loadClassNameFromProperties(url, classLoader);
            if (className != null) {
                break;
            }
        }
        if (className == null) {
            className = System.getProperty(JAXB_CONTEXT_PROPERTY);
        }
        Class spi = null;
        // if no specifically specified name, check for META-INF/services, and
        // fall back to the default factory class if that fails
        if (className == null) {
            spi = loadSPIClass(JAXBContext.class, classLoader);
            if (spi == null) {
                spi = loadSpi(PLATFORM_DEFAULT_FACTORY_CLASS, classLoader);
            }
        }
        else {
            spi = loadSpi(className, classLoader);
        }
        try {
            final Method m = spi.getMethod("createContext", new Class[] { String.class, ClassLoader.class, Map.class });
            return (JAXBContext) m.invoke(null, new Object[] { contextPath, classLoader, properties });
        } catch (final NoSuchMethodException e) {
            // will try JAXB 1.0 compatible createContext() method
        } catch (final Throwable t) {
            throw new JAXBException("Unable to create context", t);
        }

        // try old JAXB 1.0 compatible createContext() method
        try {
            final Method m = spi.getMethod("createContext", new Class[] { String.class, ClassLoader.class });
            return (JAXBContext) m.invoke(null, new Object[] { contextPath, classLoader });
        } catch (final Throwable t) {
            throw new JAXBException("Unable to create context", t);
        }
    }


    public static JAXBContext find(final Class[] classes, final Map properties) throws JAXBException {
        String className = null;
        for (final Class cl : classes) {
            final Package pkg = cl.getPackage();
            if (pkg != null) {
                final String url = pkg.getName().replace('.', '/') + "/jaxb.properties";
                className = loadClassNameFromProperties(url, cl.getClassLoader());
                if (className != null) {
                    break;
                }
            }
        }
        if (className == null) {
            className = System.getProperty(JAXB_CONTEXT_PROPERTY);
        }
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Class spi = null;
        // if no specifically specified name, check for META-INF/services, and
        // fall back to the default factory class if that fails
        if (className == null) {
            spi = loadSPIClass(JAXBContext.class, classLoader);
            if (spi == null) {
                spi = loadSpi(PLATFORM_DEFAULT_FACTORY_CLASS, classLoader);
            }
        }
        else {
            spi = loadSpi(className, classLoader);
        }
        try {
            final Method m = spi.getMethod("createContext", new Class[] { Class[].class, Map.class });
            return (JAXBContext) m.invoke(null, new Object[] { classes, properties });
        } catch (final Throwable t) {
            throw new JAXBException("Unable to create context", t);
        }
    }

    private static String loadClassNameFromProperties(final String url, final ClassLoader classLoader) throws JAXBException {
        try {
            final InputStream is;
            if (classLoader != null) {
                is = classLoader.getResourceAsStream(url);
            } else {
                is = ClassLoader.getSystemResourceAsStream(url);
            }
            if (is != null) {
                try {
                    final Properties props = new Properties();
                    props.load(is);
                    final String className = props.getProperty(JAXB_CONTEXT_FACTORY);
                    if (className == null) {
                        throw new JAXBException("jaxb.properties file " + url + " should contain a " + JAXB_CONTEXT_FACTORY + " property");
                    }
                    return className.trim();
                } finally {
                    is.close();
                }
            } else {
                return null;
            }
        } catch (final IOException e) {
            throw new JAXBException(e);
        }
    }

    private static Class<?> loadSPIClass(final Class<?> iface, final ClassLoader classLoader) throws JAXBException {
        if (osgiLocator != null) {
            return loadSPIClassFromOSGi(iface, classLoader);
        }

        try {
            return locateServiceClass(iface.getName(), ContextFinder.class, classLoader);
        } catch (final ClassNotFoundException e) {
            throw new JAXBException("Provider " + iface.getName() + " not found", e);
        }
    }

    static private Class<?> locateServiceClass(final String iface, final Class<?> contextClass, final ClassLoader loader) throws ClassNotFoundException {
        final String className = locateServiceClassName(iface, contextClass, loader);
        if (className == null) {
            return null;
        }

        // we found a name, try loading the class.  This will throw an exception if there is an error
        return loadClass(className, contextClass, loader);
    }

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
            }
        }
        // not found
        return null;
    }

    static public Class<?> loadClass(final String className, final Class<?> contextClass, ClassLoader loader) throws ClassNotFoundException {
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

    static private Collection<String> locateServiceClassNames(final String iface, final Class<?> contextClass, final ClassLoader loader) {
        final Set<String> names = new LinkedHashSet<String>();

        locateServiceClassNames(iface, loader, names);
        if (contextClass != null) {
            locateServiceClassNames(iface, contextClass.getClassLoader(), names);
        }

        return names;
    }

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
                //Ignore
            }
        }
    }

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

    private static Class loadSpi(final String className, final ClassLoader classLoader) throws JAXBException {
        if (osgiLocator != null) {
            return loadSpiFromOSGi(className, classLoader);
        }

        try {
            return loadClass(className, ContextFinder.class, classLoader);
        } catch (final ClassNotFoundException e) {
            throw new JAXBException("Provider " + className + " not found", e);
        }
    }

    private static Class<?> loadSPIClassFromOSGi(final Class<?> iface, final ClassLoader classLoader) throws JAXBException {
        try {
            return (Class<?>) getServiceClassMethod.invoke(null, iface.getName(), ContextFinder.class,classLoader);
        } catch (final Exception e) {
            return null;
        }
    }

    private static Class loadSpiFromOSGi(final String className, final ClassLoader classLoader) throws JAXBException {
        try {
            return (Class<?>) loadClassMethod.invoke(null, className, ContextFinder.class, classLoader);
        } catch (final Exception e) {
            throw new JAXBException("Provider " + className + " not found", e);
        }
    }
}
