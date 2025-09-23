# StorieS Maker

A powerful JavaFX desktop application that combines voice recording with AI-powered transcription and chat capabilities. Record your voice, transcribe it using OpenAI's Whisper API, and interact with the content through an integrated ChatGPT interface.

## Features

- 🎙️ **High-Quality Audio Recording**: Records audio in 16kHz, 16-bit mono format optimized for speech recognition
- 🤖 **AI-Powered Transcription**: Uses OpenAI Whisper API for accurate speech-to-text conversion
- 💬 **ChatGPT Integration**: Built-in chat interface for interacting with transcribed content
- 🔄 **Real-time Processing**: Asynchronous recording and transcription for smooth user experience
- 🌐 **Multi-language Support**: Supports multiple languages (Italian by default)
- 💾 **Save/Load Functionality**: Manage your transcriptions (coming soon)
- 🎨 **Modern UI**: Clean, intuitive interface with visual feedback

## Screenshots

The application features a dual-panel interface:
- **Left Panel**: Audio recording and transcription management
- **Right Panel**: ChatGPT interaction and message handling

## Prerequisites

- **Java 17** or higher
- **JavaFX 19** or compatible version
- **OpenAI API Key** (required for transcription and chat features)
- **Microphone** for audio recording
- **Maven** for building the project

## Installation

### 1. Clone the Repository
```bash
git clone <repository-url>
cd stories-maker
```

### 2. Build with Maven
```bash
mvn clean compile
```

### 3. Run the Application
```bash
mvn javafx:run
```

## Configuration

### OpenAI API Key Setup

1. Obtain an API key from [OpenAI](https://platform.openai.com/api-keys)
2. Launch the application
3. Enter your API key in the "OpenAI API Key" field
4. Click "Test Connection" to validate

**Important**: Keep your API key secure and never commit it to version control.

## Usage

### Recording and Transcription

1. **Setup**: Enter and validate your OpenAI API key
2. **Record**: Click the "🔴 REC" button to start recording
3. **Stop**: Click "⏹️ STOP" to end recording
4. **Transcribe**: The app automatically transcribes your audio using Whisper API
5. **Review**: View the transcription in the text area

### Chat Integration

1. **Insert Transcript**: Use "📝 Insert Transcript" to add transcription to chat
2. **Send Messages**: Type messages and interact with ChatGPT
3. **Clear**: Use "🗑️ Clear" to reset the chat history

### Additional Features

- **📋 SELECT ALL**: Select all transcribed text
- **💾 SAVE**: Save transcription (feature in development)
- **📂 LOAD**: Load previous transcriptions (feature in development)

## Technical Details

### Audio Format Specifications
- **Sample Rate**: 16,000 Hz
- **Bit Depth**: 16-bit
- **Channels**: Mono
- **Format**: PCM signed, little-endian
- **Output**: WAV format for API compatibility

### API Integration
- **Whisper API**: For speech-to-text transcription
- **ChatGPT API**: For conversational AI (integration in progress)
- **Async Processing**: All API calls are handled asynchronously to maintain UI responsiveness

### Dependencies
- **JavaFX**: UI framework
- **Jackson**: JSON processing
- **Java Sound API**: Audio recording
- **HTTP Client**: API communication

## Project Structure

```
src/main/java/com/voiceai/
├── Main.java                    # JavaFX application entry point and UI
├── service/
│   ├── AudioRecordingService.java   # Audio capture and WAV conversion
│   └── OpenAIService.java           # OpenAI API integration
└── ...
```

## Building for Distribution

### Create Executable JAR
```bash
mvn clean package
```

### Run JAR File
```bash
java -jar target/stories-maker-1.0-SNAPSHOT.jar
```

## Troubleshooting

### Common Issues

**"Microphone not available"**
- Check microphone permissions
- Ensure microphone is not in use by other applications
- Verify audio drivers are installed

**"Invalid API key"**
- Verify your OpenAI API key is correct
- Check API key has necessary permissions
- Ensure you have API credits available

**"Audio format not supported"**
- Update Java to version 17 or higher
- Check system audio drivers
- Try running as administrator (Windows)

**Recording produces no audio**
- Check microphone input levels
- Verify default recording device settings
- Test microphone with other applications

### Debug Mode
Run with debug output:
```bash
mvn javafx:run -Djavafx.args="-verbose"
```

## Roadmap

- [ ] Complete ChatGPT integration
- [ ] Implement save/load functionality
- [ ] Add audio playback features
- [ ] Support for multiple audio formats
- [ ] Batch processing capabilities
- [ ] Custom AI model integration
- [ ] Multi-language UI support

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section above
- Verify all prerequisites are met

## Acknowledgments

- OpenAI for Whisper and ChatGPT APIs
- JavaFX community for UI framework
- Contributors and testers

---

**Note**: This application requires an active internet connection and OpenAI API access for transcription and chat features.
