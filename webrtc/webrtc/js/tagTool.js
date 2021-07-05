let videoTagMap = new Map();
let app = document.getElementById("app");
function addVideoTag(socketId){
    
    let videoTag = document.createElement("video");
    
    videoTag.setAttribute("autoplay","autoplay");
    videoTag.setAttribute("playsinline","true");
    videoTag.setAttribute("id",socketId);

    app.appendChild(videoTag);

    videoTagMap.set(socketId,videoTag);
}
function getVideoTag(socketId){
    return videoTagMap.get(socketId);
}
function removeVideoTag(socketId){
    document.getElementById(socketId).remove();
    videoTagMap.delete(socketId);
}

export {addVideoTag,getVideoTag,removeVideoTag}