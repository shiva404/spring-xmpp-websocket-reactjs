package com.sergiomartinrubio.springxmppwebsocketsecurity.service;

import com.sergiomartinrubio.springxmppwebsocketsecurity.exception.XMPPGenericException;
import com.sergiomartinrubio.springxmppwebsocketsecurity.model.Account;
import com.sergiomartinrubio.springxmppwebsocketsecurity.model.TextMessage;
import com.sergiomartinrubio.springxmppwebsocketsecurity.xmpp.XMPPClient;
import com.sergiomartinrubio.springxmppwebsocketsecurity.websocket.utils.WebSocketTextMessageTransmitter;
import com.sergiomartinrubio.springxmppwebsocketsecurity.xmpp.utils.XMPPMessageTransmitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.springframework.stereotype.Service;

import javax.websocket.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.sergiomartinrubio.springxmppwebsocketsecurity.model.MessageType.ERROR;
import static com.sergiomartinrubio.springxmppwebsocketsecurity.model.MessageType.FORBIDDEN;
import static com.sergiomartinrubio.springxmppwebsocketsecurity.model.MessageType.JOIN_SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class XMPPService {

    private static final Map<Session, XMPPTCPConnection> CONNECTIONS = new HashMap<>();

    private final AccountService accountService;
    private final WebSocketTextMessageTransmitter webSocketTextMessageTransmitter;
    private final XMPPMessageTransmitter xmppMessageTransmitter;

    public void login(Session session, String username, String password) {

        Optional<Account> account = accountService.getAccount(username);

        if (account.isPresent() && !account.get().getPassword().equals(password)) {
            webSocketTextMessageTransmitter.send(session, TextMessage.builder().messageType(FORBIDDEN).build());
            return;
        }

        XMPPClient xmppClient = new XMPPClient();
        XMPPTCPConnection connection = null;
        try {
            connection = xmppClient.createConnection(username, password);
            connection.connect();

            if (account.isEmpty()) {
                AccountManager accountManager = AccountManager.getInstance(connection);
                accountManager.sensitiveOperationOverInsecureConnection(true);
                accountManager.createAccount(Localpart.from(username), password);
                accountService.saveAccount(new Account(username, password));
            }

            connection.login();

            CONNECTIONS.put(session, connection);
        } catch (SmackException | InterruptedException | XMPPException | IOException e) {
            webSocketTextMessageTransmitter.send(session, TextMessage.builder().messageType(ERROR).build());
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                log.info("XMPP connection was disconnected for user '{}'.", username);
            }
            throw new XMPPGenericException(username, e);
        }

        log.info("Username '{}' connected.", connection.getUser());

        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addIncomingListener((from, message, chat) -> xmppMessageTransmitter.sendResponse(message, session));

        webSocketTextMessageTransmitter.send(session, TextMessage.builder().messageType(JOIN_SUCCESS).build());
    }

    public void sendMessage(String message, String to, Session session) {
        XMPPTCPConnection connection = CONNECTIONS.get(session);
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        try {
            Chat chat = chatManager.chatWith(JidCreate.entityBareFrom(to + "@localhost"));
            chat.send(message);
        } catch (XmppStringprepException | SmackException.NotConnectedException | InterruptedException e) {
            log.error("Unexpected XMPP error.", e);
            webSocketTextMessageTransmitter.send(session, TextMessage.builder().messageType(ERROR).build());
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                log.info("XMPP connection was disconnected for user '{}'.", connection.getUser());
            }
        }
    }

    public void disconnect(Session session) {
        Presence presence = PresenceBuilder.buildPresence()
                .ofType(Presence.Type.unavailable)
                .build();
        XMPPTCPConnection connection = CONNECTIONS.get(session);
        try {
            connection.sendStanza(presence);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            log.error("Unexpected XMPP error.", e);
            webSocketTextMessageTransmitter.send(session, TextMessage.builder().messageType(ERROR).build());
        }
        connection.disconnect();
        log.info("Connection closed for user '{}'.", connection.getUser());
    }
}