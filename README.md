# PTZ Vision
![MIT](https://badgen.net/github/license/PTZ-Vision/PTZ-Vision-Android)
![PTZ Vision Logo](./app/logo.svg)

## A remote controller for your PTZ cameras

**PTZ Vision** is an Android application developed as a project for the **Mobile Programming** course at the **University of Trento** (Computer Science degree).

> **Note**: This project was developed in a short timeframe, so the application is minimal and only supports the **VISCA** protocol (for PTZ control) and **RTSP** (for video streaming).

---

## Features

- **Full PTZ Control**: Pan, Tilt and Zoom via virtual joystick
- **RTSP Video Streaming**: Real-time camera preview
- **Multi-camera Support**: Manage multiple cameras with quick selection
- **Auto Focus & AI Tracking**: Support for advanced camera features
- **Adaptive Layout**: Interface optimized for Portrait and Landscape orientation
- **Data Persistence**: Local database with Room to save camera configurations
- **Haptic Feedback**: Vibration during joystick usage

---

## Project Architecture

The project follows the **MVVM (Model-View-ViewModel)** architecture and is structured as follows:

### Directory Structure

```
ptzvision/
├── controller/           # Controllers for camera communication
│   ├── PTZController.java          # Base controller interface
│   ├── ViscaPTZController.java     # VISCA protocol implementation
│   ├── HttpCgiPTZController.java   # HTTP/CGI implementation
│   ├── utils/                       # Byte conversion utilities
│   └── viscacommands/               # VISCA commands (PanTilt, Zoom, Focus, etc.)
│
├── data/                 # Data layer
│   ├── PTZDatabase.kt              # Room database
│   └── cam/                         # Entity, DAO, Repository for cameras
│
└── ui/                   # Presentation layer
    ├── PtzVisionApp.kt             # Main navigation
    ├── home/                        # Home screen
    ├── console/                     # PTZ control screen
    │   ├── screen/                  # Portrait/Landscape layouts
    │   └── blocks/                  # Components (Joystick, Slider, etc.)
    ├── settings/                    # Settings and camera management
    └── theme/                       # Material Design theme
```

---

## Application Screens

### Home
Main screen with quick access to the control console and settings.

### Console
The main control screen, which includes:
- **Virtual joystick** for Pan/Tilt control
- **Slider** for Zoom and Focus control
- **RTSP video preview** of the selected camera
- **Quick selection** between configured cameras
- **Scene presets** for saved positions

### Settings
Camera management:
- Add/edit/delete cameras
- Configure IP, ports (Control, Stream, HTTP)
- Enable/disable cameras
- Interface layout settings

---

## Technologies Used

| Technology             | Usage                   |
|------------------------|-------------------------|
| **Kotlin**             | Primary language        |
| **Jetpack Compose**    | Modern declarative UI   |
| **Room**               | Local data persistence  |
| **ExoPlayer (Media3)** | RTSP video streaming    |
| **Material Design 3**  | Design system           |
| **VISCA Protocol**     | PTZ camera control      |
| **Coroutines**         | Asynchronous operations |

---

## Requirements

- Android **7.0 (API 24)** or higher
- PTZ camera with **VISCA over IP** support
- Camera with **RTSP** streaming

---

## Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Build and install the app on your device
4. Add a camera from settings
5. Start controlling!

---

## Author

Developed for the **Mobile Programming** course - University of Trento
by [Isaia Tonini](https://github.com/Isax03) and [Kevin Delugan](https://github.com/BisUmTo)

---

## License

See the [LICENSE](LICENSE) file for details.

