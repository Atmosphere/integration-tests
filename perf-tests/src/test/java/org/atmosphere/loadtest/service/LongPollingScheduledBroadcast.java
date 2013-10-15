package org.atmosphere.loadtest.service;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.MetaBroadcaster;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@AtmosphereHandlerService(path = "/longpolling/{id}",
        interceptors = TrackMessageSizeInterceptor.class, broadcasterCache = UUIDBroadcasterCache.class)
public class LongPollingScheduledBroadcast extends AbstractReflectorAtmosphereHandler {

    private static final Logger logger = LoggerFactory.getLogger(LongPollingScheduledBroadcast.class);

    final static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    final static AtomicInteger clients = new AtomicInteger(0);

    static {
        scheduler.scheduleAtFixedRate(new Runnable() {

            private int message = 0;
            private boolean waitToStart = true;

            @Override
            public void run() {
                if (clients.get() >= 1) {
                    if (waitToStart) {
                        waitToStart = false;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                    MetaBroadcaster.getDefault().broadcastTo("/longpolling/*", String.valueOf(message++));
                }
            }
        }, 5, 100, TimeUnit.MILLISECONDS);
    }


    @Override
    public void onRequest(final AtmosphereResource resource) throws IOException {
        if (resource.getRequest().getMethod().equalsIgnoreCase("GET")) {
            resource.suspend();
            clients.incrementAndGet();
        }
    }
}
