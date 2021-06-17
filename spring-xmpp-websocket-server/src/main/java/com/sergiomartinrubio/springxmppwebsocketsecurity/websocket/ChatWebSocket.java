package com.sergiomartinrubio.springxmppwebsocketsecurity.websocket;

import com.sergiomartinrubio.springxmppwebsocketsecurity.config.SpringContext;
import com.sergiomartinrubio.springxmppwebsocketsecurity.facade.XMPPFacade;
import com.sergiomartinrubio.springxmppwebsocketsecurity.model.WebsocketMessage;
import com.sergiomartinrubio.springxmppwebsocketsecurity.websocket.utils.MessageDecoder;
import com.sergiomartinrubio.springxmppwebsocketsecurity.websocket.utils.MessageEncoder;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@Slf4j
@ServerEndpoint(value = "/chat/{username}/{password}", decoders = MessageDecoder.class, encoders = MessageEncoder.class)
public class ChatWebSocket {

    private final XMPPFacade xmppFacade;

    public ChatWebSocket() {
        this.xmppFacade = (XMPPFacade) SpringContext.getApplicationContext().getBean("XMPPFacade");
    }

    @OnOpen
    public void open(Session session, @PathParam("username") String username, @PathParam("password") String password) {
        xmppFacade.startSession(session, username, password);
    }

    @OnMessage
    public void handleMessage(WebsocketMessage message, Session session) {
        xmppFacade.sendMessage(message, session);
    }

    @OnClose
    public void close(Session session) {
        xmppFacade.disconnect(session);
    }

    @OnError
    public void onError(Throwable e, Session session) {
        log.debug(e.getMessage());
        xmppFacade.disconnect(session);
    }
}
