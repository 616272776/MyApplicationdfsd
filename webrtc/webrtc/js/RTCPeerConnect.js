import { sendMessage, socket } from "./signaling.js";
import { addVideoTag, getVideoTag, removeVideoTag } from "./tagTool.js";

// let toSocketId;
let serverConfig = {
  "iceServers": [
    {
      "urls": [
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302",
        "stun:stun3.l.google.com:19302",
        "stun:stun4.l.google.com:19302",
        "stun:23.21.150.121",
        "stun:stun01.sipphone.com",
        "stun:stun.ekiga.net",
        "stun:stun.fwdnet.net",
        "stun:stun.ideasip.com",
        "stun:stun.iptel.org",
        "stun:stun.rixtelecom.se",
        "stun:stun.schlund.de",
        "stun:stunserver.org",
        "stun:stun.softjoys.com",
        "stun:stun.voiparound.com",
        "stun:stun.voipbuster.com",
        "stun:stun.voipstunt.com",
        "stun:stun.voxgratia.org",
        "stun:stun.xten.com"
      ]
    }
  ]
};
function createPeerConnection(socketId) {
  // toSocketId = socketId;
  try {
    let pc = new RTCPeerConnection(serverConfig);
    addVideoTag(socketId);
    pc.onicecandidate = function (event) { handleIceCandidate(event, socketId) };
    pc.onaddstream = function (event) { handleRemoteStreamAdded(event, socketId) };
    pc.onremovestream = function (event) { handleRemoteStreamRemoved(event, socketId) };
    pc.oniceconnectionstatechange = function (event) { handleiceconnectionstatechange(event, socketId) };
    console.log('Created RTCPeerConnnection');
    return pc;
  } catch (e) {
    console.log('Failed to create PeerConnection, exception: ' + e.message);
    return;
  }
}
function handleIceCandidate(event, from) {
  console.log('icecandidate event: ', event);
  if (event.candidate) {
    sendMessage({
      type: 'candidate',
      label: event.candidate.sdpMLineIndex,
      id: event.candidate.sdpMid,
      candidate: event.candidate.candidate,
      from: socket.id,
      to: from
    });
  } else {
    console.log('End of candidates.');
  }
}
function handleRemoteStreamAdded(event, form) {
  console.log('Remote stream added.');
  // remoteStream = event.stream;
  let videoTag = getVideoTag(form)
  // remoteVideo.srcObject = event.stream;
  videoTag.srcObject = event.stream;
  console.log(event.stream.getVideoTracks());
}
function handleRemoteStreamRemoved(event, from) {
  console.log('Remote stream removed. Event: ', event);
  removeVideoTag(from);
}
function handleiceconnectionstatechange(event, from) {
  if (event.currentTarget.iceConnectionState === "disconnected") {
    removeVideoTag(from);
  }
}

export { createPeerConnection }