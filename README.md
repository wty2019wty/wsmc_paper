# WSMC
##### 注意！这个分支是用AI改的，代码质量没有保障，请勿用于生产环境
##### 注意！这个分支是用AI改的，代码质量没有保障，请勿用于生产环境
##### 注意！这个分支是用AI改的，代码质量没有保障，请勿用于生产环境

为 Minecraft Java 版启用 WebSocket 支持。

由于大多数 CDN 提供商（至少在其免费层级）不支持原始 TCP 代理，借助此模组，服主现在可以将服务器隐藏在 CDN 后面，并让玩家通过 WebSocket 连接，从而防止 DDoS 攻击。

适用于 Minecraft  Paper/Spigot：
* 已测试1.20.4
* 已测试1.21
* 已测试1.21.5
* 已测试1.21.9
* 已测试1.21.11

## 当此插件安装在服务器上时：
* 服务器将允许玩家通过 WebSocket 连接。
* 玩家仍然可以使用原版 TCP 加入。
* 服务器在同一个监听端口上接受并处理 TCP 和 WebSocket 连接。
* 即使客户端未安装此模组，玩家仍然可以通过原版 TCP 加入安装了此模组的服务器。
* 服务器可以从 WebSocket 握手中获取客户端统计信息（例如真实 IP）。

## 客户端如何连接：

### 使用 [rikka0w0/wsmc](https://github.com/rikka0w0/wsmc/releases)

* 客户端可以使用类似 `ws://hostname.com:port/path_to_minecraft_endpoint` 的 URI 加入启用 WebSocket 的服务器。
* 客户端可以使用旧语法（例如 `hostname_or_ip:port`）通过原版 TCP 加入任何服务器。

## 注意
* 此模组不影响任何游戏玩法。
* 此模组不修改任何 GUI。
* 即使您安装了此模组，原版客户端也可以加入您的服务器，请注意您拥有的其他模组可能会阻止原版客户端加入。
* 在您的客户端上安装此模组不会阻止您加入其他原版或模组服务器。
* 服务器仍然可以获取通过 CDN 代理的 WebSocket 加入的玩家的真实 IP。
* 此模组与其他 TCP-WebSocket 代理（如 [websocat](https://github.com/vi/websocat)）兼容。

## 客户端选项
有时 DNS 为 HTTP 主机名 (ws) 或 SNI (wss) 返回较慢的 IP。客户端可能希望控制如何解析 IP 地址。

客户端可以根据需要控制 WebSocket 握手期间使用的 HTTP 主机名和 SNI：
```
指定 http 主机名的不安全 WebSocket 连接(不知道工作正不正常)：
ws://host.com@ip.ip.ip.ip

指定 sni 和 http 主机名为相同值 (sni-host.com)，从 ip.ip.ip.ip 解析服务器 IP：
wss://sni-host.com@ip.ip.ip.ip

分别设置 sni 和 http 主机名，从 host.com 解析服务器 IP：
wss://sni.com:@host.com[:port]

分别设置 sni 和 http 主机名，从 sni.com 解析服务器 IP：
wss://:host.com@sni.com[:port]

分别设置 sni、http 主机名和服务器地址
wss://sni.com:host.com@ip.ip.ip.ip
```

端口和路径规范可以同时附加。

## 配置
此模组的配置通过“系统属性”传递。您可以在 JVM 命令行中使用 `-D` 传递此类选项。

| 属性键                     | 类型    | 用法                                                         | 端            | 默认值 | 示例  |
| -------------------------- | ------- | ------------------------------------------------------------ | ------------- | ------ | ----- |
| wsmc.disableVanillaTCP     | boolean | 禁用原版 TCP 登录和服务器状态。                              | 服务端        | false  | true  |
| wsmc.wsmcEndpoint          | string  | 设置用于 Minecraft 登录和服务器状态的 WebSocket 端点。如果此属性不存在，客户端可以通过任何 WebSocket 端点加入游戏。必须以 / 开头，区分大小写。 | 服务端        | 未设置 | /mc   |
| wsmc.debug                 | boolean | 显示调试日志。                                               | 服务端 客户端 | false  | true  |
| wsmc.dumpBytes             | boolean | 转储原始 WebSocket 二进制帧。仅在 `wsmc.debug` 设置为 `true` 时有效。 | 服务端 客户端 | false  | true  |
| wsmc.maxFramePayloadLength | integer | 最大允许帧负载长度。将其设置为您的模组包要求的值，否则 Netty 将抛出错误 "Max frame length of x has been exceeded"。 (不知道工作正不正常) | 服务端 客户端 | 65536  | 65536 |

## 编译 Paper 制品
```
git clone https://github.com/wty2019wty/wsmc_paper
cd wsmc/paper
./gradlew shadowJar
```

## Paper / Spigot 版本
WSMC 作为标准的 Bukkit/Spigot/Paper 插件运行。

### 安装

1. 下载 `wsmc-paper-all.jar`。
2. 将其放入服务器的 `plugins` 文件夹中。
3. 重启服务器。

### 配置
WSMC for Paper 使用标准的 Java 系统属性进行配置，就像 Forge/Fabric 版本一样。您可以在服务器启动脚本（例如 `start.bat` 或 `start.sh`）中通过在 `-jar` 之前添加 `-Dkey=value` 来设置这些属性。

示例：
```bash
java -Dwsmc.wsmcEndpoint=/mc -Dwsmc.debug=true -jar paper.jar
```

### 特性

*   **协议共存**：自动检测并处理同一端口上的标准 Minecraft TCP 连接和 WebSocket 连接。
*   **性能**：使用 Netty 的底层 API 注入处理程序，确保开销最小。
*   **真实 IP 支持**：支持在反向代理后从 `X-Forwarded-For` 头解析玩家的真实 IP 地址。
*   **兼容性**：旨在与其他插件一起工作。它不修改服务器 jar 或使用 Mixins，使其与各种 Paper/Spigot 版本高度兼容。
