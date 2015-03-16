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
package org.atmosphere.loadtest.service;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Nettosphere;
import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class PerfTest {

    private Nettosphere nettosphere;
    private int port;
    private CountDownLatch messages;

    protected int findFreePort() throws IOException {
        ServerSocket socket = null;

        try {
            socket = new ServerSocket(0);

            return socket.getLocalPort();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    @BeforeMethod
    public void start() throws IOException {
        port = findFreePort();
        Config.Builder b = new Config.Builder();
        b.resource(Echo.class)
                .port(port)
                .host("127.0.0.1")
                .broadcasterCache(UUIDBroadcasterCache.class)
                .build();
        nettosphere = new Nettosphere.Builder().config(b.build()).build();
        nettosphere.start();
    }

    @AfterMethod
    public void stop() {
        nettosphere.stop();
    }

    @Test
    public void defaultBroadcasterLoadTest() throws IOException, InterruptedException {
        boolean completed = load("http://127.0.0.1:" + port + "/default/test", 100, 100);
        assertEquals(messages.getCount(), 0);
        assertTrue(completed);
    }

    @Test
    public void simpleBroadcasterLoadTest() throws IOException, InterruptedException {
        boolean completed = load("http://127.0.0.1:" + port + "/simple/test", 100, 100);
        assertEquals(messages.getCount(), 0);
        assertTrue(completed);
    }

    private boolean load(String url, final int clientNum, final int messageNum) throws IOException, InterruptedException {
        AsyncHttpClientConfig.Builder b = new AsyncHttpClientConfig.Builder();
        b.setFollowRedirect(true).setConnectTimeout(-1).setReadTimeout(-1);
        final AsyncHttpClient ahc = new AsyncHttpClient(b.build());

        final CountDownLatch l = new CountDownLatch(clientNum);

        messages = new CountDownLatch(messageNum * clientNum);

        Client client = ClientFactory.getDefault().newClient();
        RequestBuilder request = client.newRequestBuilder();
        request.method(Request.METHOD.GET).uri(url);
        request.transport(Request.TRANSPORT.WEBSOCKET);

        long clientCount = l.getCount();
        final AtomicLong total = new AtomicLong(0);

        Socket[] sockets = new Socket[clientNum];
        for (int i = 0; i < clientCount; i++) {
            final AtomicLong start = new AtomicLong(0);
            sockets[i] = client.create(client.newOptionsBuilder().runtimeShared(true).runtime(ahc).reconnect(false).build())
                    .on(new Function<Integer>() {
                        @Override
                        public void on(Integer statusCode) {
                            start.set(System.currentTimeMillis());
                            l.countDown();
                        }
                    }).on(new Function<String>() {

                        int mCount = 0;

                        @Override
                        public void on(String s) {
                            System.out.println("Message left receive " + messages.getCount() + " message " + s);
                            if (s.startsWith("message")) {
                                String[] m = s.split("\n\r");
                                mCount += m.length;
                                messages.countDown();
                                if (mCount == messageNum) {
                                    // System.out.println("All messages received " + mCount);
                                    total.addAndGet(System.currentTimeMillis() - start.get());
                                }
                            }
                        }
                    }).on(new Function<Throwable>() {
                        @Override
                        public void on(Throwable t) {
                            t.printStackTrace();
                        }
                    });

        }

        for (int i = 0; i < clientCount; i++) {
            sockets[i].open(request.build());
        }
        l.await(30, TimeUnit.SECONDS);
        System.out.println("OK, all Connected: " + clientNum);

        // Let NettoSphere complete the handshake process
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Socket loader = client.create(client.newOptionsBuilder().runtime(ahc).build());
        loader.open(request.build());

        for (int i = 0; i < messageNum; i++ ) {
            loader.fire("message" + i);
        }
        boolean completed = messages.await(5, TimeUnit.MINUTES);
        loader.close();
        for (int i = 0; i < clientCount; i++) {
            sockets[i].close();
        }
        ahc.close();
        return completed;
    }

}
