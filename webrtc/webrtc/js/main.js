import { getMedia as getMediaStream } from './media.js'
import { connectSignaling } from './signaling.js'

let stream = getMediaStream();
export let localStream;


stream
    .then(function (value) {
        connectSignaling(value);
    });
