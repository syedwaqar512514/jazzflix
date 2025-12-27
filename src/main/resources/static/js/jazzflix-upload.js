let eventSource = null;
let currentUploadId = null;
let currentVideoId = null;
let dashPlayer = null;

/* ==========================
   UPLOAD HANDLER
========================== */
document.addEventListener("DOMContentLoaded", () => {

    document.getElementById("uploadBtn").addEventListener("click", uploadVideo);
    document.getElementById("loadDashBtn").addEventListener("click", loadDashStream);

});

/* ==========================
   VIDEO UPLOAD
========================== */
async function uploadVideo() {
    const fileInput = document.getElementById("videoFile");
    const file = fileInput.files[0];

    if (!file) {
        alert("Select a video first");
        return;
    }

    toggleProgress(true);

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch("/jazz/video/api/upload", {
            method: "POST",
            body: formData
        });

        const result = await response.json();

        if (!result.success) {
            showError("Upload failed", result.message);
            return;
        }

        currentUploadId = result.data.uploadId;
        connectSSE();

    } catch (err) {
        showError("Upload error", err.message);
    }
}

/* ==========================
   SSE PROGRESS
========================== */
function connectSSE() {
    eventSource = new EventSource(`/jazz/progress/sse/${currentUploadId}`);

    eventSource.onmessage = e => updateProgress(JSON.parse(e.data));

    eventSource.onerror = () => {
        console.warn("SSE failed, fallback polling");
        eventSource.close();
        pollProgress();
    };
}

async function pollProgress() {
    const interval = setInterval(async () => {
        const res = await fetch(`/jazz/video/api/progress/${currentUploadId}`);
        if (!res.ok) return;

        const progress = await res.json();
        updateProgress(progress);

        if (["COMPLETED", "FAILED"].includes(progress.status)) {
            clearInterval(interval);
        }
    }, 2000);
}

/* ==========================
   UI UPDATE
========================== */
function updateProgress(p) {
    document.getElementById("progressFill").style.width = p.progressPercentage + "%";
    document.getElementById("progressText").textContent = p.progressPercentage + "%";
    document.getElementById("statusInfo").textContent = p.message;

    if (p.status === "COMPLETED") {
        currentVideoId = p.videoId;
        showSuccess(p);
    }

    if (p.status === "FAILED") {
        showError("Upload failed", p.message);
    }
}

/* ==========================
   DASH PLAYER
========================== */
function loadDashStream() {
    if (!currentVideoId) {
        alert("No video available");
        return;
    }

    const quality = "q360p"; // later dynamic

    const manifestUrl =
        `http://localhost:9000/videos-${quality}/videos/${currentVideoId}/dash/manifest.mpd`;

    const video = document.getElementById("videoPlayer");

    if (dashPlayer) dashPlayer.reset();

    dashPlayer = dashjs.MediaPlayer().create();
    dashPlayer.initialize(video, manifestUrl, true);
}

/* ==========================
   HELPERS
========================== */
function toggleProgress(show) {
    document.getElementById("progressSection").style.display = show ? "block" : "none";
}

function showSuccess(p) {
    document.getElementById("result").style.display = "block";
    document.getElementById("videoPlayerSection").style.display = "block";

    document.getElementById("resultTitle").textContent = "Upload Completed";
    document.getElementById("resultMessage").textContent =
        `Video ID: ${p.videoId}`;

    const thumb = document.getElementById("thumbnailImg");
    thumb.src = `/jazz/video/api/thumbnail/${p.videoId}`;
    thumb.style.display = "block";
}

function showError(title, msg) {
    alert(`${title}: ${msg}`);
}
