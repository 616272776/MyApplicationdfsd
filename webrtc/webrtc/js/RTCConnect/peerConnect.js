import { createOfferSDP } from "./offerOptions.js";
import { sendMessage } from "../signaling.js";

let startTime;
export let pc1;

let remoteVideo = document.getElementById('gum-remote');

export async function call(stream) {

    console.log('Starting call');
    startTime = window.performance.now();

    checkStream(stream);

    const configuration = {};
    console.log('RTCPeerConnection configuration:', configuration);
    pc1 = new RTCPeerConnection(configuration);
    console.log('Created local peer connection object pc1');

    pc1.addEventListener('icecandidate', e => onIceCandidate(pc1, e));
    pc1.addEventListener('iceconnectionstatechange', e => onIceStateChange(pc1, e));
    pc1.addEventListener('track', gotRemoteStream);

    stream.getTracks().forEach(track => pc1.addTrack(track, stream));
    console.log('Added local stream to pc1');

    // let offer = createOfferSDP(pc1);

    // offer.then(function(value){

    // });
    // return pc1;
}
function gotRemoteStream(e) {
  if (remoteVideo.srcObject !== e.streams[0]) {
    remoteVideo.srcObject = e.streams[0];
    console.log('pc2 received remote stream');
  }
}

function checkStream(stream) {
  const videoTracks = stream.getVideoTracks();
  const audioTracks = stream.getAudioTracks();

  if (videoTracks.length > 0) {
    console.log(`Using video device: ${videoTracks[0].label}`);
  }
  if (audioTracks.length > 0) {
    console.log(`Using audio device: ${audioTracks[0].label}`);
  }
}

// async function onIceCandidate(pc, event) {
//   try {
//     await (pc.addIceCandidate(event.candidate));
//     // sendMessage({
//     //   type: 'candidate',
//     //   label: event.candidate.sdpMLineIndex,
//     //   id: event.candidate.sdpMid,
//     //   candidate: event.candidate.candidate
//     // });
//     onAddIceCandidateSuccess(pc);
//   } catch (e) {
//     onAddIceCandidateError(pc, e);
//   }
//   console.log(`${(pc)} ICE candidate:\n${event.candidate ? event.candidate.candidate : '(null)'}`);
// }

function onIceStateChange(pc, event) {
  if (pc) {
    console.log(`${pc} ICE state: ${pc.iceConnectionState}`);
    console.log('ICE state change event: ', event);
  }
}
function onAddIceCandidateError(pc, error) {
  console.log(`${pc} failed to add ICE Candidate: ${error.toString()}`);
}

function onAddIceCandidateSuccess(pc) {
  console.log(`${pc} addIceCandidate success`);
}



