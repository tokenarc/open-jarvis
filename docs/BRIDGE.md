# Open Jarvis CLI Bridge

The Termux CLI bridge allows you to control Open Jarvis from the command line using TCP sockets.

## Quick Start

### Install jarvis.sh

```bash
# Copy to your PATH
cp scripts/jarvis.sh $PREFIX/bin/jarvis
chmod +x $PREFIX/bin/jarvis

# Install network tool (required)
pkg install socat
```

### Usage

```bash
# Execute a command
jarvis "open whatsapp"

# Check service status
jarvis status

# View recent task history
jarvis history

# View provider stats
jarvis providers

# View memory context
jarvis memory

# Get help
jarvis --help
```

### Pipe Mode

```bash
# Pipe commands from file
cat commands.txt | jarvis

# Echo a single command
echo "open chrome" | jarvis
```

## Special Commands

| Command | Description |
|---------|-------------|
| `status` | Returns service state, accessibility permission status, active LLM provider |
| `history` | Returns last 10 tasks as JSON array |
| `providers` | Returns LLM provider statistics |
| `memory` | Returns Graphify memory context summary |

## Protocol

### Request Format

```json
{"cmd": "open whatsapp", "requestId": "abc123"}
```

### Response Format

```json
{"requestId": "abc123", "status": "done", "result": "success"}
```

Status values:
- `progress` - Task is executing
- `done` - Task completed successfully
- `error` - Task failed

## Installation

### Termux

1. Open Termux
2. Copy `jarvis.sh` to `$PREFIX/bin/`:

```bash
cp jarvis.sh $PREFIX/bin/jarvis
chmod +x $PREFIX/bin/jarvis
```

3. Install `socat` (or `nc` as fallback):

```bash
pkg install socat
```

4. Start Open Jarvis Android app (first time setup)

### ADB

To use from a computer via ADB:

```bash
# Forward a local port to the Android device
adb forward tcp:5000 tcp:5000

# Then use netcat
echo '{"cmd":"open chrome","requestId":"1"}' | nc -w 2 127.0.0.1 5000
```

Note: You'll need to find the correct port. Check the app's filesDir.

## Examples

### Open Apps

```bash
jarvis "open whatsapp"
jarvis "open chrome"
jarvis "open settings"
```

### Send Messages

```bash
jarvis "send message to John hey what's up"
```

### Automation Scripts

```bash
#!/bin/bash
# morning-routine.sh

jarvis "open whatsapp"
sleep 2
jarvis "send message to Mom good morning"
```

## Security Model

### Permissions

- **No new Android permissions** — The socket server uses existing permissions
- **Same access level** — As if using the overlay UI
- **Local only** — No network exposure, device-local only

### Considerations

- Anyone with device access can control the agent
- No authentication on the socket (intentional for local use)
- Do not expose via network port forwarding in untrusted environments

## Troubleshooting

### "Open Jarvis service not running"

Start the Open Jarvis app. The socket server starts when the overlay service starts.

### "No network tool (socat/nc) available"

```bash
pkg install socat
```

### Connection timeout

The service might be busy. Wait and retry.

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Error (service not running, command failed, parse error) |