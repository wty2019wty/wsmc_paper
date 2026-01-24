# WSMC Paper Plugin

Enable WebSocket support for Minecraft Java Edition (Paper/Spigot/Bukkit).

## Features
* **WebSocket Support**: Allows players to connect via WebSocket (ws:// or wss://), enabling the use of CDNs (like Cloudflare) to hide your server IP and prevent DDoS attacks.
* **Cross-Version Compatibility**: Works on a wide range of Minecraft versions (1.8 - Latest).
* **ProtocolLib Integration**: Uses ProtocolLib for advanced packet handling and IP forwarding.
* **Real IP Forwarding**: Forwards the real client IP (from X-Forwarded-For header) to the server.

## Installation
1. Download `WSMC-Paper-1.0-SNAPSHOT.jar`.
2. Place it in your server's `plugins` folder.
3. Install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) (Required for IP forwarding).
4. Restart your server.

## Configuration
The plugin works out of the box. You can configure it via `config.yml` or System Properties.

### config.yml
```yaml
# Enable debug logging
debug: false

# WebSocket Endpoint (e.g., /mc). If empty, all paths are accepted.
wsmcEndpoint: ""
```

### System Properties (Startup Flags)
You can also use startup flags:
* `-Dwsmc.debug=true`
* `-Dwsmc.wsmcEndpoint=/mc`
* `-Dwsmc.maxFramePayloadLength=65536`

## Usage for Players
Players need a WSMC-enabled client or proxy to connect.
URL format: `ws://your-server-domain.com:port/endpoint`

## Building
```bash
cd paper
./gradlew build
```
The output jar will be in `paper/build/libs/`.
