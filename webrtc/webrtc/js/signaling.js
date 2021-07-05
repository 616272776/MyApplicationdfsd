
import { createOfferSDP } from "./RTCConnect/offerSDP.js";
import { createPeerConnection} from "./RTCPeerConnect.js";
/////////////////////////////////////////////
var room = 'OldPlace';
var localStream;
var socket;
var other;
let PeerConnectionList = new Map();

// Could prompt for room name:
// room = prompt('Enter room name:');
export function connectSignaling(stream) {
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

    let peerConnector = createPeerConnection(socketId,socket);
    peerConnector.addStream(localStream);
    createOfferSDP(peerConnector,socket.id,socketId);
    PeerConnectionList.set(socket.id,peerConnector);
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
      other = message.from;
      let peerConnector  = createPeerConnection(message.from,socket)
      peerConnector.addStream(localStream);
      peerConnector.setRemoteDescription(new RTCSessionDescription(message));
      PeerConnectionList.set(socket.id,peerConnector);
      doAnswer();
    } else if (message.type === 'answer') {
      PeerConnectionList.get(socket.id).setRemoteDescription(new RTCSessionDescription(message));
    } else if (message.type === 'candidate') {
      var candidate = new RTCIceCandidate({
        sdpMLineIndex: message.label,
        candidate: message.candidate
      });
      PeerConnectionList.get(socket.id).addIceCandidate(candidate);
    } else if (message === 'bye') {
      handleRemoteHangup();
    }
  });
}
export function sendMessage(message) {
  console.log('Client sending message: ', message);
  socket.emit('message', message);
}


function doAnswer() {
  console.log('Sending answer to peer.');
  
  PeerConnectionList.get(socket.id).createAnswer().then(
    setLocalAndSendMessage,
    onCreateSessionDescriptionError
  );
}
function setLocalAndSendMessage(sessionDescription) {
  PeerConnectionList.get(socket.id).setLocalDescription(sessionDescription);
  console.log('setLocalAndSendMessage sending message', sessionDescription);
  sessionDescription.to = other;
  sendMessage({
    type: sessionDescription.type,
    sdp: sessionDescription.sdp,
    from: socket.id,
    to: other
  });
}
function onCreateSessionDescriptionError(error) {
  trace('Failed to create session description: ' + error.toString());
}