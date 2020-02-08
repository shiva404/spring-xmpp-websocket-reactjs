package com.sergiomartinrubio.springxmppwebsocketsecurity.xmpp;

import com.google.gson.Gson;
import com.sergiomartinrubio.springxmppwebsocketsecurity.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jxmpp.jid.EntityBareJid;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static com.sergiomartinrubio.springxmppwebsocketsecurity.model.Message.Type.CHAT;

@Slf4j
@RequiredArgsConstructor
public class XMPPIncomingChatMessageListener implements IncomingChatMessageListener {

    private final WebSocketSession session;

    @Override
    @SneakyThrows
    public void newIncomingMessage(EntityBareJid from, org.jivesoftware.smack.packet.Message message, Chat chat) {
        log.info("New message from {} to {}: {}", message.getFrom(), message.getTo(), message.getBody());
        Message xmppMessage = Message.builder()
                .from(message.getFrom().getLocalpartOrNull().toString())
                .to(message.getTo().getLocalpartOrNull().toString())
                .content(message.getBody())
                .type(CHAT)
                .build();
        Gson gson = new Gson();
        String xmppMessageJson = gson.toJson(xmppMessage);
        session.sendMessage(new TextMessage(xmppMessageJson.getBytes()));
    }
}
