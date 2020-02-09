package com.sergiomartinrubio.springxmppwebsocketsecurity.handler;

import com.google.gson.Gson;
import com.sergiomartinrubio.springxmppwebsocketsecurity.model.Message;
import com.sergiomartinrubio.springxmppwebsocketsecurity.service.XMPPServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@AllArgsConstructor
public class XMPPWebSocketHandler extends TextWebSocketHandler {

    private final XMPPServiceImpl xmppService;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        var xmppMessage = new Gson().fromJson(message.getPayload(), Message.class);
        switch (xmppMessage.getType()) {
            case CHAT:
                xmppService.handleMessage(xmppMessage, session);
                break;
            case ERROR:
                xmppService.closeConnection(session);
                break;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Websocket connection created.");
        String username = (String) session.getAttributes().get("username");
        xmppService.addConnection(session, username);
        xmppService.connect(session, username);
        xmppService.addListener(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
//        xmppService.closeConnection(session);
        log.info("Websocket connection error.");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        xmppService.closeConnection(session);
        log.info("Websocket connection closed with status {}.", status);
    }
}