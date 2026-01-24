# WSMC
Enable Websocket support for Minecraft Java.
Since most CDN providers(at least for their free tier) do not support raw TCP proxy, with the help of this mod, the owner can now hide the server behind a CDN and let the players connect via WebSocket, thus preventing DDoS attacks.

For Minecraft Forge, Neoforge, Fabric and Paper/Spigot:
* 1.21.x (tested up to 1.21.11)
* 1.20.x (tested 1.20.1, 1.20.4, 1.20.6)
* 1.18.2, 1.19.x

This branch is optimized for 1.20 to 1.21.11 compatibility.

This mod runs standalone and does not have any dependency.

## When this plugin is installed on a server:
* The server would allow players to connect via WebSocket.
* Players can still join using vanilla TCP.
* The server accepts and handles TCP and WebSocket connections on the same listening port.
* Without installing this mod on the client side, a player can still join a server that has this mod using vanilla TCP.
* The server can acquire client statistics (e.g., real IP) from the WebSocket handshake..

## How does the client connect:

### Useing [rikka0w0/wsmc](https://github.com/rikka0w0/wsmc/releases)

* The client can join WebSocket-enabled servers using URI like `ws://hostname.com:port/path_to_minecraft_endpoint`.
* The client can join any servers using vanilla TCP using the old syntax, e.g. `hostname_or_ip:port`.



## Note
* This mod does not affect any gameplay.
* This mod does not modify any GUI.
* Vanilla clients can join your server even if you install this mod, note that other mods you have may prevent vanilla clients from joining.
* Installing this mod on your client does not prevent you from joining other vanilla or mod servers.
* The server can still get the real IP of the players who joined via CDN-proxied WebSocket.
* This mod is compatible with other TCP-WebSocket proxies, such as websocat.

## Client Options
Sometimes the DNS returns a slow IP for the HTTP hostname (ws) or the SNI (wss). The client may want to control how to resolve the IP address.

The client can optionally control the HTTP hostname and the SNI used during WebSocket handshake:
```
Insecure WebSocket connection with http hostname specified:
ws://host.com@ip.ip.ip.ip

Specify sni and http hostname to the same value(sni-host.com), resolve server IP from ip.ip.ip.ip:
wss://sni-host.com@ip.ip.ip.ip

Set sni and http hostname differently, resolve server IP from host.com:
wss://sni.com:@host.com[:port]

Set sni and http hostname differently, resolve server IP from sni.com:
wss://:host.com@sni.com[:port]

Set sni, http hostname, and the server address seperately
wss://sni.com:host.com@ip.ip.ip.ip
```

Port and path specification can be appended at the same time.

## Configuration
The configuration of this mod is passed in the "system properties". You can use `-D` in the JVM command line to pass such options.

| Property Key               | Type     | Usage                                                                                                                                                                                        | Side          | Default | Example  |
|----------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|---------|----------|
| wsmc.disableVanillaTCP     | boolean  | Disable vanilla TCP login and server status.                                                                                                                                                 | Server        | false   | true     |
| wsmc.wsmcEndpoint          | string   | Set the WebSocket Endpoint for Minecraft login and server status. If this property does not exist, a client can join the game via ANY WebSocket Endpoint. Must start with /, case-sensitive. | Server        | Not set | /mc      |
| wsmc.debug                 | boolean  | Show debug logs.                                                                                                                                                                             | Server Client | false   | true     |
| wsmc.dumpBytes             | boolean  | Dump raw WebSocket binary frames. Work only if `wsmc.debug` is set to `true`.                                                                                                                | Server Client | false   | true     |
| wsmc.maxFramePayloadLength | integer  | Maximum allowable frame payload length. Setting this value to your modpack's requirement else Netty will throw error "Max frame length of x has been exceeded". (I wonder if any mistakes were made during the changes.)                             | Server Client | 65536   | 65536    |








## Compile Paper artifact
```
git clone https://github.com/wty2019wty/wsmc_paper
cd wsmc/paper
./gradlew build
```


## Paper / Spigot Version
WSMC runs as a standard Bukkit/Spigot/Paper plugin.

### Installation
1. Download the `wsmc-paper-x.x.x.jar`.
2. Place it in your server's `plugins` folder.
3. Restart the server.

### Configuration
WSMC for Paper uses standard Java System Properties for configuration, just like the Forge/Fabric versions. You can set these properties in your server startup script (e.g., `start.bat` or `start.sh`) by adding `-Dkey=value` before `-jar`.

Example:
```bash
java -Dwsmc.wsmcEndpoint=/mc -Dwsmc.debug=true -jar paper.jar
```

### Features

*   **Protocol Coexistence**: Automatically detects and handles both standard Minecraft TCP connections and WebSocket connections on the same port.
*   **Performance**: Uses Netty's low-level API to inject handlers, ensuring minimal overhead.
*   **Real IP Support**: Supports resolving player's real IP address from `X-Forwarded-For` header when behind a reverse proxy.
*   **Compatibility**: Designed to work alongside other plugins. It does not modify the server jar or use Mixins, ~~making it highly compatible with various Paper/Spigot versions.~~（Only Support paper1.21.11）
