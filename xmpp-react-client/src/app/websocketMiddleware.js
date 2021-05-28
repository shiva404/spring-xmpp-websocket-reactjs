import { history } from "../browserhistory";
import { disableAlertUser, enableAlertUser } from "../features/user/alertUserSlice";
import { login, logout } from "../features/user/userSlice";

const websocketMiddleware = () => {
  let socket = null;

  const onOpen = (store) => (event) => {
    console.log("hello");
  };

  const onClose = (store) => () => {};

  const onMessage = (store) => (event) => {
    const payload = JSON.parse(event.data);
    switch (payload.messageType) {
      case "JOIN_SUCCESS":
        store.dispatch(
          login({
            username: payload.to,
            loggedIn: true,
          })
        );

        store.dispatch(disableAlertUser());

        history.push("/home");

        console.log("Connected to XMPP server!");
        break;
      case "NEW_MESSAGE":
        console.log(payload.content);
        break;
      case "ERROR":
        console.log("Join failed!!!");
        break;
      case "LEAVE":
        console.log(payload);
        break;
      case "FORBIDDEN":
        store.dispatch(
          enableAlertUser({ message: "Invalid password", enabled: true })
        );
        console.log("Invalid password");
        break;
      default:
        console.log(payload);
        break;
    }
  };

  return (store) => (next) => (action) => {
    switch (action.type) {
      case "WS_CONNECT":
        if (socket !== null) {
          socket.close();
        }

        socket = new WebSocket(
          "ws://localhost:8080/chat/" + action.username + "/" + action.password
        );

        // websocket handlers
        socket.onmessage = onMessage(store);
        socket.onclose = onClose(store);
        socket.onopen = onOpen(store);

        break;
      case "WS_DISCONNECT":
        if (socket !== null) {
          socket.close();
        }
        socket = null;
        store.dispatch(logout());
        history.push("/login");
        break;
      case "NEW_MESSAGE":
        break;
      default:
        return next(action);
    }
  };
};

export default websocketMiddleware();