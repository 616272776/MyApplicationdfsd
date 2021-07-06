
import { createOfferSDP } from "./RTCConnect/offerSDP.js";
import { createPeerConnection} from "./RTCPeerConnect.js";

var room = 'OldPlace';
var localStream;
let socket;
let PeerConnectionList = new Map();

// Could prompt for room name:
// room = prompt('Enter room name:');
 function connectSignaling(stream) {
  localStream = stream;
  socket = io.connect("http://139.224.12.1:8084");
  if (room !== '') {
    socket.emit('create or join', room);
    console.log('Attempted to create or  join room', room);
  }
  socket.on('created', function (room) {
    console.log('Created room ' + room);
  });
  socket.on('full', function (room) {
    console.log('Room ' + room + ' is full');
  });
  socket.on('join', function (room,socketId) {
    console.log(socketId + ' made a request to join room ' + room);
    console.log('This peer is the initiator of room ' + room + '!');

    let peerConnector = createPeerConnection(socketId);
    peerConnector.addStream(localStream);
    createOfferSDP(peerConnector,socketId);
    PeerConnectionList.set(socketId,peerConnector);
  });
  socket.on('joined', function (room) {
    console.log('joined: ' + room);
  });
  socket.on('log', function (array) {
    console.log.apply(console, array);
  });
  // This client receives a message
  socket.on('message', function (message) {
    console.log('Client received message:', message);
    if (message.type === 'offer') {
      // let other = message.from;
      let peerConnector  = createPeerConnection(message.from)
      peerConnector.addStream(localStream);
      peerConnector.setRemoteDescription(new RTCSessionDescription(message));
      peerConnector.other = message.from;
      PeerConnectionList.set(message.from,peerConnector);
      doAnswer(message.from);
    } else if (message.type === 'answer') {
      PeerConnectionList.get(message.from).setRemoteDescription(new RTCSessionDescription(message));
    } else if (message.type === 'candidate') {
      var candidate = new RTCIceCandidate({
        sdpMLineIndex: message.label,
        candidate: message.candidate
      });
      PeerConnectionList.get(message.from).addIceCandidate(candidate);
    } else if (message === 'bye') {
      handleRemoteHangup();
    }
  });
}
function sendMessage(message) {
  console.log('Client sending message: ', message);
  socket.emit('message', message);
}

function doAnswer(from) {
  console.log('Sending answer to peer.');
  
  PeerConnectionList.get(from).createAnswer().then(
    function (event) {
      setLocalAndSendMessage(event,from)
      }
    ,
    onCreateSessionDescriptionError
  );
}
function setLocalAndSendMessage(sessionDescription,from) {
  PeerConnectionList.get(from).setLocalDescription(sessionDescription);
  console.log('setLocalAndSendMessage sending message', sessionDescription);
  sessionDescription.to = from;
  sendMessage({
    type: sessionDescription.type,
    sdp: sessionDescription.sdp,
    from: socket.id,
    to: from
  });
}
function onCreateSessionDescriptionError(error) {
  trace('Failed to create session description: ' + error.toString());
}

export {sendMessage,connectSignaling,socket}