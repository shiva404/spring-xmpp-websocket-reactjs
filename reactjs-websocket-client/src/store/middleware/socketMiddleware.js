import * as actions from '../actions/websocketActions';
import { messageReceived } from '../actions/messagesListActions';
import storage from '../../utils/storage';

const socketMiddleware = () => {
  let socket = null;

  const onOpen = store => event => {
    // store.dispatch(actions.wsConnected(event.target.url));
  };

  const onClose = store => () => {
    store.dispatch(actions.wsDisconnected());
  };

  const onMessage = store => event => {
    const payload = JSON.parse(event.data);
    switch (payload.type) {
      case 'AUTHENTICATED':
        console.log('Connected to XMPP server!');
        store.dispatch({ type: 'LoginSuccess' });
        storage.set('user', payload.to);
        break;
      case 'CHAT':
        store.dispatch(messageReceived(payload.content));
        break;
      case 'GROUP_CHAT':
        // store.dispatch(messageReceived(payload.content));
        break;
      case 'ERROR':
        console.log('Login failed!!!');
        // store.dispatch({ type: 'LoginFail' });
        break;
      default:
        console.log(payload);
        break;
    }
  };

  return store => next => action => {
    switch (action.type) {
      case 'WS_CONNECT':
        if (socket !== null) {
          socket.close();
        }

        // https://www.npmjs.com/package/sockjs-client
        socket = new WebSocket('ws://localhost:8080/chat/' + action.username);

        // websocket handlers
        socket.onmessage = onMessage(store);
        socket.onclose = onClose(store);
        socket.onopen = onOpen(store);

        break;
      case 'WS_DISCONNECT':
        if (socket !== null) {
          socket.close();
        }
        socket = null;
        break;
      case 'CHAT':
        socket.send(JSON.stringify(action.msg));
        break;
      default:
        return next(action);
    }
  };
};

export default socketMiddleware();
