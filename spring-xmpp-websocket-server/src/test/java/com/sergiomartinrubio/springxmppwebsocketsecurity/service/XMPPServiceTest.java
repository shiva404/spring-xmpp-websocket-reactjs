package com.sergiomartinrubio.springxmppwebsocketsecurity.service;

import com.sergiomartinrubio.springxmppwebsocketsecurity.exception.XMPPGenericException;
import com.sergiomartinrubio.springxmppwebsocketsecurity.model.Account;
import com.sergiomartinrubio.springxmppwebsocketsecurity.model.MessageType;
import com.sergiomartinrubio.springxmppwebsocketsecurity.model.TextMessage;
import com.sergiomartinrubio.springxmppwebsocketsecurity.websocket.utils.WebSocketTextMessageTransmitter;
import com.sergiomartinrubio.springxmppwebsocketsecurity.xmpp.XMPPClient;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jxmpp.stringprep.XmppStringprepException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.websocket.Session;
import java.util.Optional;

import static com.sergiomartinrubio.springxmppwebsocketsecurity.model.MessageType.ERROR;
import static com.sergiomartinrubio.springxmppwebsocketsecurity.model.MessageType.FORBIDDEN;
import static com.sergiomartinrubio.springxmppwebsocketsecurity.model.MessageType.JOIN_SUCCESS;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class XMPPServiceTest {

    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String MESSAGE = "hello world";
    private static final String TO = "other-user";

    @Mock
    private Session session;

    @Mock
    private AccountService accountService;

    @Mock
    private WebSocketTextMessageTransmitter webSocketTextMessageTransmitter;

    @Mock
    private XMPPClient xmppClient;

    @InjectMocks
    private XMPPService xmppService;


    @Test
    void startSessionShouldStartSessionWithoutCreatingAccountWhenAccountExistAndCorrectPassword() throws XmppStringprepException {
        // GIVEN
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain("domain")
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
        String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
        given(accountService.getAccount(USERNAME)).willReturn(Optional.of(new Account(USERNAME, hashedPassword)));
        given(xmppClient.connect(USERNAME, PASSWORD)).willReturn(Optional.of(connection));

        // WHEN
        xmppService.startSession(session, USERNAME, PASSWORD);

        // THEN
        then(xmppClient).should().login(connection);
        then(xmppClient).should().addIncomingMessageListener(connection, session);
        then(webSocketTextMessageTransmitter).should().send(session, createTextMessage(JOIN_SUCCESS));
        then(xmppClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void startSessionShouldStartSessionAndCreateAccountWhenAccountDoesNotExist() throws XmppStringprepException {
        // GIVEN
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain("domain")
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
        given(accountService.getAccount(USERNAME)).willReturn(Optional.empty());
        given(xmppClient.connect(USERNAME, PASSWORD)).willReturn(Optional.of(connection));

        // WHEN
        xmppService.startSession(session, USERNAME, PASSWORD);

        // THEN
        then(xmppClient).should().login(connection);
        then(xmppClient).should().addIncomingMessageListener(connection, session);
        then(webSocketTextMessageTransmitter).should().send(session, createTextMessage(JOIN_SUCCESS));
        then(xmppClient).should().createAccount(connection, USERNAME, PASSWORD);
    }

    @Test
    void startSessionShouldSendForbiddenMessageWhenWrongPassword() {
        // GIVEN
        String hashedPassword = BCrypt.hashpw("WRONG", BCrypt.gensalt());
        given(accountService.getAccount(USERNAME)).willReturn(Optional.of(new Account(USERNAME, hashedPassword)));

        // WHEN
        xmppService.startSession(session, USERNAME, PASSWORD);

        // THEN
        then(xmppClient).shouldHaveNoInteractions();
        then(webSocketTextMessageTransmitter).should().send(session, createTextMessage(FORBIDDEN));
    }

    @Test
    void startSessionShouldSendErrorMessageWhenConnectionIsNotPresent() {
        // GIVEN
        String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
        given(accountService.getAccount(USERNAME)).willReturn(Optional.of(new Account(USERNAME, hashedPassword)));
        given(xmppClient.connect(USERNAME, PASSWORD)).willReturn(Optional.empty());

        // WHEN
        xmppService.startSession(session, USERNAME, PASSWORD);

        // THEN
        then(xmppClient).shouldHaveNoMoreInteractions();
        then(webSocketTextMessageTransmitter).should().send(session, createTextMessage(ERROR));
    }

    @Test
    void startSessionShouldSendErrorMessageWhenLoginThrowsXMPPGenericException() throws XmppStringprepException {
        // GIVEN
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain("domain")
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
        String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
        given(accountService.getAccount(USERNAME)).willReturn(Optional.of(new Account(USERNAME, hashedPassword)));
        given(xmppClient.connect(USERNAME, PASSWORD)).willReturn(Optional.of(connection));
        willThrow(XMPPGenericException.class).given(xmppClient).login(connection);

        // WHEN
        xmppService.startSession(session, USERNAME, PASSWORD);

        // THEN
        then(xmppClient).should().disconnect(connection);
        then(webSocketTextMessageTransmitter).should().send(session, createTextMessage(ERROR));
        then(xmppClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void sendMessageShouldSendMessage() throws XmppStringprepException {
        // GIVEN
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain("domain")
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
        String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
        given(accountService.getAccount(USERNAME)).willReturn(Optional.of(new Account(USERNAME, hashedPassword)));
        given(xmppClient.connect(USERNAME, PASSWORD)).willReturn(Optional.of(connection));
        xmppService.startSession(session, USERNAME, PASSWORD);

        // WHEN
        xmppService.sendMessage(MESSAGE, TO, session);

        // THEN
        then(xmppClient).should().sendMessage(connection, MESSAGE, TO);
    }

    @Test
    void sendMessageShouldSendErrorMessageWhenXMPPGenericException() throws XmppStringprepException {
        // GIVEN
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain("domain")
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
        String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
        given(accountService.getAccount(USERNAME)).willReturn(Optional.of(new Account(USERNAME, hashedPassword)));
        given(xmppClient.connect(USERNAME, PASSWORD)).willReturn(Optional.of(connection));
        xmppService.startSession(session, USERNAME, PASSWORD);
        willThrow(XMPPGenericException.class).given(xmppClient).sendMessage(connection, MESSAGE, TO);

        // WHEN
        xmppService.sendMessage(MESSAGE, TO, session);

        // THEN
        then(webSocketTextMessageTransmitter).should().send(session, createTextMessage(ERROR));
    }

    @Test
    void sendMessageShouldDoNothingWhenNotFoundConnection() {
        // WHEN
        xmppService.sendMessage(MESSAGE, TO, session);

        // THEN
        then(xmppClient).shouldHaveNoInteractions();
    }

    @Test
    void disconnectShouldSendStanzaAndDisconnect() throws XmppStringprepException {
        // GIVEN
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain("domain")
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
        String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
        given(accountService.getAccount(USERNAME)).willReturn(Optional.of(new Account(USERNAME, hashedPassword)));
        given(xmppClient.connect(USERNAME, PASSWORD)).willReturn(Optional.of(connection));
        xmppService.startSession(session, USERNAME, PASSWORD);

        // WHEN
        xmppService.disconnect(session);

        // THEN
        then(xmppClient).should().sendStanza(connection, Presence.Type.unavailable);
        then(xmppClient).should().disconnect(connection);
    }

    @Test
    void disconnectShouldSendErrorMessageWhenXMPPGenericException() throws XmppStringprepException {
        // GIVEN
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain("domain")
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);
        String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
        given(accountService.getAccount(USERNAME)).willReturn(Optional.of(new Account(USERNAME, hashedPassword)));
        given(xmppClient.connect(USERNAME, PASSWORD)).willReturn(Optional.of(connection));
        xmppService.startSession(session, USERNAME, PASSWORD);
        willThrow(XMPPGenericException.class).given(xmppClient).sendStanza(connection, Presence.Type.unavailable);


        // WHEN
        xmppService.disconnect(session);

        // THEN
        then(webSocketTextMessageTransmitter).should().send(session, createTextMessage(ERROR));
    }

    @Test
    void disconnectShouldDoNothingWhenNotFoundConnection() {
        // WHEN
        xmppService.disconnect(session);

        // THEN
        then(xmppClient).shouldHaveNoInteractions();
    }

    private TextMessage createTextMessage(MessageType type) {
        return TextMessage.builder()
                .messageType(type)
                .build();
    }
}