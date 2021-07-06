import { sendMessage, socket } from "../signaling.js";

const offerOptions = {
  offerToReceiveAudio: 1,
  offerToReceiveVideo: 1
};
export async function createOfferSDP(pc,toSocketid) {
  try {
    console.log('pc1 createOffer start');
    const offer = await pc.createOffer(offerOptions);
    await onCreateOfferSuccess(pc,offer,toSocketid);
  } catch (e) {
    onCreateSessionDescriptionError(e);
  }
}
async function onCreateOfferSuccess(pc,desc,toSocketid) {

  console.log(`Offer from pc1\n${desc.sdp}`);
  console.log('pc1 setLocalDescription start');
  try {
    await pc.setLocalDescription(new RTCSessionDescription(desc));
    sendMessage({
      type: desc.type,
      sdp: desc.sdp,
      from: socket.id,
      to: toSocketid
    });
    onSetLocalSuccess(pc);
  } catch (e) {
    onSetSessionDescriptionError(e);
  }

}
function onCreateSessionDescriptionError(error) {
  console.log(`Failed to create session description: ${error.toString()}`);
}
function onSetLocalSuccess(pc) {
  console.log(`${pc} setLocalDescription complete`);
}
function onSetSessionDescriptionError(error) {
  console.log(`Failed to set session description: ${error.toString()}`);
}
