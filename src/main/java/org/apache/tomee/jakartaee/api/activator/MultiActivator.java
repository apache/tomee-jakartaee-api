/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.tomee.jakartaee.api.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This exists only to call the 3 BundleActivators present in the un-amalgamated jars.
 */
public class MultiActivator implements BundleActivator {

    private final org.apache.geronimo.osgi.locator.Activator locator = new org.apache.geronimo.osgi.locator.Activator();

    public void start(final BundleContext bundleContext) throws Exception {
        locator.start(bundleContext);
    }

    public void stop(final BundleContext bundleContext) throws Exception {
        locator.stop(bundleContext);
    }
}
