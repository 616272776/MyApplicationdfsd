import { sendMessage } from "./signaling.js";
import { addVideoTag, getVideoTag, removeVideoTag } from "./tagTool.js";

let toSocketId;
let mySocket;
var remoteVideo = document.getElementById("gum-remote");
let serverConfig = {
  "iceServers": [
      { "urls": [
        "stun:stun.l.google.com:19302",
"stun:stun.ideasip.com",
"stun:stun.schlund.de",
"stun:stun.voiparound.com",
"stun:stun.voipbuster.com",
"stun:stun.voipstunt.com",
"stun:stun.xten.com"
      ]
      }
  ]
};
function createPeerConnection(socketId,socket) {
  toSocketId= socketId;
  mySocket = socket;
  try {
    let pc = new RTCPeerConnection(serverConfig);
    addVideoTag(socket.id);
    pc.onicecandidate = handleIceCandidate;
    pc.onaddstream = handleRemoteStreamAdded;
    pc.onremovestream = handleRemoteStreamRemoved;
    pc.oniceconnectionstatechange = handleiceconnectionstatechange;
    console.log('Created RTCPeerConnnection');
    return pc;
  } catch (e) {
    console.log('Failed to create PeerConnection, exception: ' + e.message);
    return;
  }
}
function handleIceCandidate(event) {
  console.log('icecandidate event: ', event);
  if (event.candidate) {
    sendMessage({
      type: 'candidate',
      label: event.candidate.sdpMLineIndex,
      id: event.candidate.sdpMid,
      candidate: event.candidate.candidate,
      from:mySocket.id,
      to: toSocketId
    });
  } else {
    console.log('End of candidates.');
  }
}
function handleRemoteStreamAdded(event) { 
  console.log('Remote stream added.');
  // remoteStream = event.stream;
  let videoTag = getVideoTag(mySocket.id)
  // remoteVideo.srcObject = event.stream;
  videoTag.srcObject = event.stream;
  console.log(event.stream.getVideoTracks());
}
function handleRemoteStreamRemoved(event) {
  console.log('Remote stream removed. Event: ', event);
  removeVideoTag(mySocket.id);
}
function handleiceconnectionstatechange(event){
  if(event.currentTarget.iceConnectionState==="disconnected"){
      removeVideoTag(mySocket.id);
  }
}

export{createPeerConnection}