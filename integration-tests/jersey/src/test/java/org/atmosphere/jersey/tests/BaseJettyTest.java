/*
 * Copyright 2015 Async-IO.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.jersey.tests;


import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;


public abstract class BaseJettyTest extends BaseTest {

    protected Server server;
    private Context root;

    @Override
    public void configureCometSupport() {
        atmoServlet.framework().setAsyncSupport(new org.atmosphere.container.JettyCometSupport(atmoServlet.framework().getAtmosphereConfig()));
    }
    @Override
    public void startServer() throws Exception {
        server = new Server(port);
        root = new Context(server, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(atmoServlet), ROOT);

        Connector listener = new SelectChannelConnector();

        listener.setHost("127.0.0.1");
        listener.setPort(TestHelper.getEnvVariable("ATMOSPHERE_HTTP_PORT", findFreePort()));
        server.addConnector(listener);

        server.start();
    }

    @Override
    public void stopServer() throws Exception {
        server.stop();
    }
}
