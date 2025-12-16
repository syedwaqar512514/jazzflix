// JazzFlix Demo - JavaScript Functionality

document.addEventListener('DOMContentLoaded', function() {
    // Get main video player
    const mainPlayer = document.getElementById('mainPlayer');
    
    // Get all video cards
    const videoCards = document.querySelectorAll('.video-card');
    
    // Setup image fallbacks for thumbnails
    setupImageFallbacks();
    
    // Add click event to video cards
    videoCards.forEach(card => {
        card.addEventListener('click', function() {
            const videoSrc = this.getAttribute('data-video');
            const videoTitle = this.querySelector('.card-title').textContent;
            const videoDescription = this.querySelector('.card-description').textContent;
            
            // Update main player
            mainPlayer.src = videoSrc;
            mainPlayer.load();
            mainPlayer.play();
            
            // Update video info
            document.querySelector('.video-title').textContent = videoTitle;
            document.querySelector('.video-description').textContent = videoDescription;
            
            // Scroll to video player
            document.querySelector('.hero').scrollIntoView({ 
                behavior: 'smooth',
                block: 'start'
            });
            
            // Add visual feedback
            videoCards.forEach(c => c.style.opacity = '0.6');
            this.style.opacity = '1';
            
            // Reset opacity after animation
            setTimeout(() => {
                videoCards.forEach(c => c.style.opacity = '1');
            }, 1000);
        });
    });
    
    // Add keyboard controls for video player
    document.addEventListener('keydown', function(e) {
        if (mainPlayer && !document.activeElement.matches('input, textarea')) {
            switch(e.key) {
                case ' ':
                    e.preventDefault();
                    if (mainPlayer.paused) {
                        mainPlayer.play();
                    } else {
                        mainPlayer.pause();
                    }
                    break;
                case 'ArrowLeft':
                    e.preventDefault();
                    mainPlayer.currentTime = Math.max(0, mainPlayer.currentTime - 10);
                    break;
                case 'ArrowRight':
                    e.preventDefault();
                    if (!isNaN(mainPlayer.duration)) {
                        mainPlayer.currentTime = Math.min(mainPlayer.duration, mainPlayer.currentTime + 10);
                    }
                    break;
                case 'ArrowUp':
                    e.preventDefault();
                    mainPlayer.volume = Math.min(1, mainPlayer.volume + 0.1);
                    break;
                case 'ArrowDown':
                    e.preventDefault();
                    mainPlayer.volume = Math.max(0, mainPlayer.volume - 0.1);
                    break;
                case 'f':
                case 'F':
                    e.preventDefault();
                    if (mainPlayer.requestFullscreen) {
                        mainPlayer.requestFullscreen();
                    } else if (mainPlayer.webkitRequestFullscreen) {
                        mainPlayer.webkitRequestFullscreen();
                    } else if (mainPlayer.msRequestFullscreen) {
                        mainPlayer.msRequestFullscreen();
                    }
                    break;
                case 'm':
                case 'M':
                    e.preventDefault();
                    mainPlayer.muted = !mainPlayer.muted;
                    break;
            }
        }
    });
    
    // Video player event handlers
    if (mainPlayer) {
        mainPlayer.addEventListener('play', function() {
            console.log('Video started playing');
        });
        
        mainPlayer.addEventListener('pause', function() {
            console.log('Video paused');
        });
        
        mainPlayer.addEventListener('ended', function() {
            console.log('Video ended');
        });
        
        mainPlayer.addEventListener('error', function(e) {
            console.error('Video error:', e);
            // Show user-friendly error message
            const videoInfo = document.querySelector('.video-info');
            const errorMsg = document.createElement('div');
            errorMsg.className = 'error-message';
            errorMsg.textContent = 'Unable to load video. Please check if the video file exists in the assets folder.';
            videoInfo.appendChild(errorMsg);
        });
    }
    
    // Smooth scroll for navigation links
    const navLinks = document.querySelectorAll('.nav a');
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            const targetSection = document.querySelector(targetId);
            if (targetSection) {
                targetSection.scrollIntoView({ 
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
    
    // Console welcome message
    console.log('%c🎬 Welcome to JazzFlix Demo! 🎬', 'font-size: 20px; color: #667eea; font-weight: bold;');
    console.log('%cKeyboard shortcuts:', 'font-size: 14px; color: #999;');
    console.log('Space - Play/Pause');
    console.log('Arrow Left/Right - Seek -/+ 10s');
    console.log('Arrow Up/Down - Volume +/-');
    console.log('F - Fullscreen');
    console.log('M - Mute/Unmute');
});

// Helper function to generate SVG fallback for thumbnails
function generateFallbackSVG(text, bgColor) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="300" height="200">
        <rect fill="${bgColor}" width="300" height="200"/>
        <text fill="#fff" font-family="Arial" font-size="20" x="50%" y="50%" text-anchor="middle" dy=".3em">${text}</text>
    </svg>`;
    return 'data:image/svg+xml,' + encodeURIComponent(svg);
}

// Setup image fallbacks for all thumbnail images
function setupImageFallbacks() {
    const thumbnails = document.querySelectorAll('.video-thumbnail img');
    const colors = ['#333', '#444', '#555', '#666'];
    
    thumbnails.forEach((img, index) => {
        img.addEventListener('error', function() {
            const fallbackText = this.getAttribute('data-fallback-text') || 'Video';
            const bgColor = colors[index % colors.length];
            this.src = generateFallbackSVG(fallbackText, bgColor);
        });
        
        // Set opacity for loaded images
        img.addEventListener('load', function() {
            this.style.opacity = '1';
        });
    });
}
