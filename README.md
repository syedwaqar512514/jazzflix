# JazzFlix - Streaming Demo Project

A modern, responsive web-based streaming platform demonstration built with HTML5, CSS3, and JavaScript.

## 🎬 Features

- **HTML5 Video Player**: Native video playback with full controls
- **Responsive Design**: Works seamlessly on desktop, tablet, and mobile devices
- **Video Library**: Browse and select from a collection of videos
- **Keyboard Shortcuts**: Enhanced viewing experience with keyboard controls
- **Modern UI**: Clean, Netflix-inspired interface with smooth animations
- **Dynamic Content**: Click-to-play functionality with smooth scrolling

## 🚀 Getting Started

### Prerequisites

- A modern web browser (Chrome, Firefox, Safari, or Edge)
- A local web server (optional, but recommended for best experience)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/syedwaqar512514/jazzflix.git
cd jazzflix
```

2. Add video files to the `assets` directory (optional):
   - Place your MP4 video files in `assets/demo-video.mp4`
   - Add thumbnail images: `thumb1.jpg`, `thumb2.jpg`, etc.
   - See `assets/README.md` for more details

### Running the Application

#### Option 1: Using Python (Recommended)
```bash
# Python 3
python -m http.server 8000

# Python 2
python -m SimpleHTTPServer 8000
```

Then open your browser and navigate to: `http://localhost:8000`

#### Option 2: Using Node.js
```bash
npx http-server
```

#### Option 3: Direct File Access
Simply open `index.html` in your web browser. Note that some features may require a local server.

## ⌨️ Keyboard Shortcuts

- **Space**: Play/Pause
- **Arrow Left**: Rewind 10 seconds
- **Arrow Right**: Forward 10 seconds
- **Arrow Up**: Increase volume
- **Arrow Down**: Decrease volume
- **F**: Toggle fullscreen
- **M**: Mute/Unmute

## 📁 Project Structure

```
jazzflix/
├── index.html          # Main HTML file
├── styles.css          # Stylesheet
├── script.js           # JavaScript functionality
├── assets/             # Media assets directory
│   ├── README.md       # Assets documentation
│   └── .gitkeep        # Keep assets directory in git
└── README.md           # This file
```

## 🎨 Features Details

### Video Player
- HTML5 native video controls
- Support for multiple video formats (MP4, WebM)
- Poster image support
- Error handling with user-friendly messages

### Video Library
- Grid layout with responsive cards
- Hover effects and play overlays
- Click to load and play videos
- Smooth scroll to player

### Responsive Design
- Mobile-first approach
- Breakpoints for tablet and desktop
- Flexible grid system
- Touch-friendly interface

## 🛠️ Customization

### Adding Your Own Videos

1. Place video files in the `assets` directory
2. Update the video sources in `index.html`:
```html
<source src="assets/your-video.mp4" type="video/mp4">
```

### Changing Colors

Modify the CSS variables or color values in `styles.css`:
- Primary gradient: `#667eea` to `#764ba2`
- Background: `#141414`
- Card background: `#1a1a1a`

## 🌐 Browser Support

- Chrome 60+
- Firefox 55+
- Safari 11+
- Edge 79+
- Mobile browsers (iOS Safari, Chrome Mobile)

## 📝 License

This is a demo project for educational and demonstration purposes.

## 🤝 Contributing

This is a demo project. Feel free to fork and modify for your own use.

## 📧 Contact

For questions or feedback, please open an issue on GitHub.

---

**Built with ❤️ for demonstrating modern web streaming capabilities**