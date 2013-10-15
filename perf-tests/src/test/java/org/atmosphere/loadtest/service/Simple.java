package org.atmosphere.loadtest.service;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.Singleton;
import org.atmosphere.config.service.WebSocketHandlerService;
import org.atmosphere.util.SimpleBroadcaster;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketHandlerAdapter;

import java.io.IOException;

@Singleton
@WebSocketHandlerService(path = "/simple/{id}", broadcaster = SimpleBroadcaster.class, broadcasterCache = UUIDBroadcasterCache.class)
public class Simple extends WebSocketHandlerAdapter {

    @Override
    public void onTextMessage(WebSocket webSocket, String data) throws IOException {
        webSocket.resource().getBroadcaster().broadcast(data);
    }
}
