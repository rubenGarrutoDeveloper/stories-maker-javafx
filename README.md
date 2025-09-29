# StorieS Maker

A powerful JavaFX desktop application that combines voice recording with AI-powered transcription and chat capabilities. Record your voice, transcribe it using OpenAI's Whisper API, and interact with the content through an integrated ChatGPT interface with real-time streaming responses.

## Features

- 🎙️ **High-Quality Audio Recording**: Records audio in 16kHz, 16-bit mono format optimized for speech recognition
- 🤖 **AI-Powered Transcription**: Uses OpenAI Whisper API for accurate speech-to-text conversion
- ⚡ **Real-Time Transcription**: Optional chunked processing for live transcription during recording
- 💬 **ChatGPT Integration**: Fully integrated chat interface with streaming responses
- 🔄 **Asynchronous Processing**: Non-blocking operations for smooth user experience
- 🌐 **Multi-language Support**: Supports multiple languages for transcription (Italian by default)
- 💾 **Save/Load Functionality**: Complete file management for transcriptions
- 📊 **Token Usage Tracking**: Monitor your API token consumption in real-time
- ⚙️ **Persistent Settings**: Automatic saving of API keys, window geometry, and preferences
- 🎨 **Modern UI**: Clean, intuitive dual-panel interface with visual feedback

## Prerequisites

- **Java 17** or higher
- **JavaFX 19** or compatible version
- **OpenAI API Key** (required for transcription and chat features)
- **Microphone** for audio recording
- **Maven** for building the project

## Installation

### 1. Clone the Repository
```bash
git clone https://github.com/rubenGarrutoDeveloper/stories-maker-javafx.git
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

**Security**: API keys are encrypted and stored locally in `~/.stories-maker/stories-maker-config.json`. Your API key is never transmitted except to OpenAI's servers.

### Settings Management

The application automatically saves:
- **API Key**: Encrypted for security
- **Window Geometry**: Position and size preferences
- **Language Setting**: Default transcription language
- **Real-Time Transcription**: Enable/disable preference

Configuration file location:
- **Linux/Mac**: `~/.stories-maker/stories-maker-config.json`
- **Windows**: `%USERPROFILE%\.stories-maker\stories-maker-config.json`

## Usage

### Recording and Transcription

#### Standard Mode
1. **Setup**: Enter and validate your OpenAI API key
2. **Record**: Click the "🔴 REC" button to start recording
3. **Stop**: Click "⏹ STOP" to end recording
4. **Transcribe**: The app automatically transcribes your complete audio using Whisper API
5. **Review**: View the transcription in the text area

#### Real-Time Mode
1. **Enable**: Check the "Real-time" checkbox
2. **Record**: Click "🔴 REC" to start recording
3. **Live Transcription**: Text appears as you speak (processed in 5-second chunks)
4. **Stop**: Click "⏹ STOP" to end recording and process remaining audio

### Transcription Management

- **💾 SAVE**: Save transcription to text, markdown, or JSON format
- **📂 LOAD**: Load previously saved transcriptions
- **📋 SELECT ALL**: Select all transcribed text for copying

### Chat Integration

1. **Insert Transcript**: Click "📝 Insert Transcript" to add transcription to chat input
2. **Send Messages**: Type messages and press Enter or click "➤" to send
3. **Streaming Responses**: Watch as ChatGPT's response appears in real-time
4. **Token Tracking**: Monitor token usage displayed in the header
5. **Clear**: Use "🗑 Clear" to reset the chat history (with confirmation)

### Conversation Features

- **Context Preservation**: Maintains full conversation history
- **System Prompts**: Pre-configured for Italian language assistance
- **Auto-Scrolling**: Chat automatically scrolls to latest messages
- **Token Management**: Automatic conversation trimming to stay within API limits

## Technical Details

### Architecture

The application uses a service-oriented architecture with clear separation of concerns:

- **Services Layer**: Business logic and external API integration
- **State Management**: Centralized application state with listener pattern
- **UI Layer**: JavaFX components with reactive updates
- **Model Layer**: Domain objects for chat messages and conversations

### Audio Format Specifications
- **Sample Rate**: 16,000 Hz (16kHz)
- **Bit Depth**: 16-bit
- **Channels**: Mono
- **Format**: PCM signed, little-endian
- **Output**: WAV format for API compatibility
- **Real-Time Chunks**: 5-second segments with 500ms overlap

### API Integration

#### Whisper API (Transcription)
- **Model**: whisper-1
- **Format**: Multipart form-data upload
- **Language**: Configurable (default: Italian)
- **Response Format**: JSON
- **Max File Size**: 25MB

#### ChatGPT API
- **Model**: gpt-3.5-turbo (configurable)
- **Temperature**: 0.7 (default)
- **Max Tokens**: 2000 per response
- **Streaming**: Server-sent events (SSE) for real-time responses
- **Context Management**: Automatic conversation history trimming

### Dependencies

Core dependencies defined in `pom.xml`:

- **JavaFX** (19.0.2.1): UI framework
  - javafx-controls
  - javafx-fxml
- **Jackson** (2.15.2): JSON processing
  - jackson-core
  - jackson-databind
  - jackson-annotations
- **Tritonus Share** (0.3.7.4): Audio processing utilities
- **Java Sound API**: Native audio recording
- **Java HTTP Client**: Modern async HTTP communication

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
- Check microphone permissions in system settings
- Ensure microphone is not in use by other applications
- Verify audio drivers are installed and up to date
- Try selecting a different default recording device

**"Invalid API key"**
- Verify your OpenAI API key is correct (starts with "sk-")
- Check API key has necessary permissions on OpenAI platform
- Ensure you have API credits available in your OpenAI account
- Test the key directly at platform.openai.com

**"API key cannot be empty"**
- Enter a valid API key before attempting to record
- Click "Test Connection" to validate before recording

**"Audio format not supported"**
- Update Java to version 17 or higher
- Check system audio drivers
- Verify Java Sound API is properly installed

**Recording produces no audio**
- Check microphone input levels in system settings
- Verify default recording device is correctly set
- Test microphone with other applications
- Check if antivirus is blocking microphone access

**Real-time transcription is delayed**
- This is normal - transcription occurs in 5-second chunks
- Check your internet connection speed
- Verify OpenAI API is responding normally

**Chat messages not sending**
- Ensure API key is validated (check connection status indicator)
- Wait for any in-progress messages to complete
- Check internet connectivity
- Verify OpenAI API status

### Debug Mode
Run with verbose output:
```bash
mvn javafx:run -Djavafx.args="-verbose"
```

Check console output for detailed error messages and service logs.

## API Usage and Costs

### Token Usage Tracking
The application displays real-time token usage in the top bar. Token colors indicate usage levels:
- **Green**: Normal usage (< 2000 tokens)
- **Orange**: Moderate usage (2000-5000 tokens)
- **Red**: High usage (> 5000 tokens)

### Cost Estimation
Refer to [OpenAI Pricing](https://openai.com/pricing) for current rates:
- **Whisper API**: Charged per minute of audio
- **GPT-3.5-turbo**: Charged per token (input + output)

The application automatically manages conversation history to stay within token limits.

## Performance Considerations

### Real-Time Transcription
- Processes audio in 5-second chunks with 500ms overlap
- Requires stable internet connection
- Higher API usage compared to standard mode
- Best for long recordings where immediate feedback is valuable

### Standard Transcription
- Processes entire recording after stopping
- More cost-effective for short recordings
- Single API call per recording
- Best for short, focused recordings

## Known Limitations

- Maximum audio file size: 25MB (OpenAI Whisper API limit)
- Maximum conversation context: ~8000 tokens (automatically managed)
- Real-time transcription minimum chunk: 1 second of audio
- Requires active internet connection for all AI features

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

### Development Guidelines

- Follow existing code structure and naming conventions
- Add logging for important operations
- Handle errors gracefully with user-friendly messages
- Update documentation for new features
- Test with various audio inputs and edge cases

## Support

For support and questions:
- Create an issue in the [GitHub repository](https://github.com/rubenGarrutoDeveloper/stories-maker-javafx/issues)
- Check the troubleshooting section above
- Verify all prerequisites are met
- Review console logs for error details

## Acknowledgments

- **OpenAI** for Whisper and ChatGPT APIs
- **JavaFX Community** for the excellent UI framework
- **Apache Maven** for build management
- **Jackson** for JSON processing
- All contributors and testers

## Changelog

### Version 1.0-SNAPSHOT
- ✅ Complete audio recording and transcription
- ✅ Real-time transcription with chunked processing
- ✅ Full ChatGPT integration with streaming responses
- ✅ Save/Load functionality for transcriptions
- ✅ Persistent settings and configuration
- ✅ Token usage tracking
- ✅ Multi-language support
- ✅ Modern UI with visual feedback

---

**Note**: This application requires an active internet connection and OpenAI API access for transcription and chat features. API usage incurs costs according to OpenAI's pricing structure.
