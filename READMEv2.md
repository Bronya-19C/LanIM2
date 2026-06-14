# LanIM V2 — Deploy Guide & Architecture

用于局域网内的即时通讯应用。服务端集中转发，客户端 JavaFX 桌面 GUI，支持文字消息与文件分片传输，默认启用 TLS 加密。

---

## 1. 项目架构

### 1.1 整体拓扑

```
┌──────────────────────────────────┐
│   Client Process (gradlew run)   │
│  ┌──────────┐   ┌─────────────┐  │
│  │ Launcher │──▶│  LoginView   │  │
│  └────┬─────┘   └─────────────┘  │
│       │   ┌───────────────────┐  │
│       ├──▶│   ChatView        │  │
│       │   │ ┌──────┬────┬───┐ │  │
│       │   │ │Member│Chat│Prv│ │  │
│       │   │ │Pane  │Pane│Pane│ │  │
│       │   │ └──────┴────┴───┘ │  │
│       │   └───────┬───────────┘  │
│       │           │               │
│  ┌────┴───────────▼───────────┐  │
│  │   ChatCoordinator          │  │
│  │  ┌──────────┬───────────┐  │  │
│  │  │ ImClient │FileTransfer│  │  │
│  │  └────┬─────┴─────┬─────┘  │  │
│  └───────┼───────────┼────────┘  │
│          │           │            │
│  ┌───────┴───────────┴────────┐  │
│  │ DB: MessageRepo / FileRepo │  │
│  └────────────────────────────┘  │
└──────────────────┬───────────────┘
                   │ TCP + TLS
                   │ (FrameCodec 成帧)
┌──────────────────▼───────────────┐
│   Server Process                 │
│   (gradlew runServer)            │
│  ┌──────────────────────────┐   │
│  │       ImServer           │   │
│  │  accept → ThreadPool     │   │
│  └──────────┬───────────────┘   │
│             │                    │
│  ┌──────────▼───────────────┐   │
│  │    ClientSession ×N      │   │
│  │  read loop → dispatch    │   │
│  └──────────┬───────────────┘   │
│             │                    │
│  ┌──────────▼───────────────┐   │
│  │     RoomRegistry         │   │
│  │  members / relay / trim  │   │
│  └──────────┬───────────────┘   │
│             │                    │
│  ┌──────────▼───────────────┐   │
│  │  DB: MessageRepo         │   │
│  └──────────────────────────┘   │
└─────────────────────────────────┘
```

**两个进程、两端各持有独立 SQLite 数据库** —— 客户端保存消息历史与下载文件元数据，服务端保存广-播历史并做容量裁剪。

### 1.2 包职责一览

| 包         | 文件数 | 职责                                                     |
| ---------- | ------ | -------------------------------------------------------- |
| `client`   | 3      | 客户端网络(I-Client)、入站路由(Coordinator)、文件传输     |
| `server`   | 3      | 服务端监听(Server)、会话处理(Session)、房间注册(Registry) |
| `ui`       | 6      | JavaFX 界面：登录、聊天、成员面板、聊天面板、预览面板、样式 |
| `model`    | 11     | 数据模型：Envelope、各类 Payload、枚举、record            |
| `data`     | 3      | SQLite 持久层：数据库初始化、消息仓库、文件仓库            |
| `net`      | 3      | 网络层：帧编解码器、Socket 通道、TLS 上下文               |
| `util`     | 4      | 工具类：常量、哈希、JSON、校验                            |
| (root)     | 1      | Launcher：客户端入口，继承 JavaFX Application             |

### 1.3 核心数据流

```
发送文字:
  ChatPane.onSend → ChatCoordinator.sendText(text)
    → ChatPayload → Envelope(CHAT_TEXT) → ImClient.send(env)
      → FrameCodec.encode → SocketChannel.writeFrame
        → TCP → Server(ClientSession) → RoomRegistry.broadcastExcept
          → 其他 ClientSession → SocketChannel.writeFrame → ...

接收文字:
  ImClient 读线程 → FrameCodec.decode → InboundListener.onEnvelope
    → ChatCoordinator.onEnvelope → switch(type)
      → CHAT_TEXT: MessageRepo.insert + ChatPane.append

发送文件:
  FileTransfer.send(path) → 计算 checksum → FILE_META
    → 分片循环: FILE_CHUNK ×N → 接收方逐 chunk 写盘
      → 收齐 → 校验 checksum → PreviewPane.showFile

用户加入:
  ClientSession.onJoin → RoomRegistry.join → JOIN_ACK(含成员列表+历史)
    → ChatCoordinator.onJoined → members.setMembers + chat.loadHistory
      → USER_JOINED 广播给其他成员
```

### 1.4 类关系图

完整的交互式依赖关系图参见 `class-diagram.html`，在浏览器中打开即可查看所有类与接口之间的实现、继承和组合关系。

---

## 2. 服务端部署

### 2.1 环境要求

| 项       | 最低版本          |
| -------- | ----------------- |
| JDK      | 25                |
| Gradle   | 使用项目自带的 Wrapper，无需额外安装 |
| 操作系统 | 任意支持 JDK 25 的平台 |

### 2.2 快速启动

```bash
# 1. 进入项目根目录
cd LanIM2

# 2. 启动服务端 (Windows)
gradlew.bat runServer

# 或 (Linux / macOS)
./gradlew runServer
```

服务端启动后输出：
```
LanIM server listening on port 9090 (TLS)
```

### 2.3 命令行参数

```
gradlew runServer --args="--port 9090 --tls"
gradlew runServer --args="--port 9090 --plain"
```

| 参数      | 说明                               | 默认值 |
| --------- | ---------------------------------- | ------ |
| `--port`  | 监听端口                           | 9090   |
| `--tls`   | 启用 TLS 加密(默认)               | on     |
| `--plain` | 关闭 TLS，使用明文 TCP(仅测试环境) | —      |

### 2.4 TLS / Keystore 配置

服务端默认从 `config/keystore.jks` 加载密钥库。若文件不存在，**首次启动时自动生成**自签名的 RSA 2048 位证书（别名 `lanim`，有效期 365 天）。

默认口令：`lanim123`

通过 JVM 系统属性可自定义路径：

```bash
# 自定义 keystore
gradlew runServer -Djavax.net.ssl.keyStore=/path/to/keystore.jks \
                  -Djavax.net.ssl.keyStorePassword=yourpassword
```

生成前必须确保 `config/` 目录存在（或让程序自动创建）。

### 2.5 防火墙配置

确保服务端监听端口（默认 `9090`）在防火墙上放行：

```bash
# Windows (管理员)
netsh advfirewall firewall add rule name="LanIM Server" dir=in action=allow protocol=TCP localport=9090

# Linux (firewalld)
firewall-cmd --add-port=9090/tcp --permanent
firewall-cmd --reload
```

### 2.6 进程守护（生产环境）

```bash
# systemd 示例 (Linux)
# /etc/systemd/system/lanim-server.service
[Unit]
Description=LanIM V2 Server
After=network.target

[Service]
Type=simple
User=lanim
WorkingDirectory=/opt/lanim
ExecStart=/opt/lanim/gradlew runServer --args="--port 9090 --tls"
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

---

## 3. 客户端部署

### 3.1 环境要求

| 项       | 最低版本      |
| -------- | ------------- |
| JDK      | 25            |
| JavaFX   | 25 (Gradle 插件自动拉取) |
| 操作系统 | Windows / Linux / macOS |

### 3.2 快速启动

```bash
# 1. 进入项目根目录
cd LanIM2

# 2. 启动客户端 (Windows)
gradlew.bat run

# 或 (Linux / macOS)
./gradlew run
```

### 3.3 登录界面配置

启动后显示登录窗口，填写：

| 字段           | 说明                                      | 默认值                     |
| -------------- | ----------------------------------------- | -------------------------- |
| Server Address | 格式 `host:port`，只填 host 则使用默认端口 | `10.129.245.252:9090`      |
| Nickname       | 2~32 字符的显示名称                       | 随机 `User-XXXX`           |
| Room Secret    | 房间口令，相同的 secret 进入同一房间      | (必填)                     |
| Enable TLS     | 与服务端保持一致                          | ✓                          |

### 3.4 命令行动态参数

```bash
# 自定义 JavaFX 平台
gradlew run -PjavafxPlatform=win
gradlew run -PjavafxPlatform=linux
gradlew run -PjavafxPlatform=mac
```

### 3.5 打包为独立应用

```bash
# 使用 jpackage 打包 (JDK 25 内置)
# 先构建 fat jar
gradlew jar

# 然后
jpackage --name LanIM2 \
         --input build/libs \
         --main-jar LanIM2-1.0.0.jar \
         --main-class com.alpha.lanim.Launcher \
         --type app-image
```

### 3.6 数据存储

客户端本地数据目录：

```
data/
├── lanim.db           # SQLite 数据库（消息历史 + 文件记录）
└── files/             # 接收的文件
    └── {fileId}_{filename}
```

启动时自动创建，可安全删除（不影响服务端数据）。

---

## 4. 通信协议

### 4.1 帧格式

```
┌────────────┬──────────────────────────┐
│  length    │  JSON payload            │
│  4 bytes   │  (UTF-8)                 │
│  big-endian│                          │
└────────────┴──────────────────────────┘
max payload: 10 MiB
```

`FrameCodec` 负责编解码，对上层透明。

### 4.2 Envelope 结构

```json
{
  "type": "CHAT_TEXT",
  "messageId": "uuid",
  "senderId": "peer-uuid",
  "roomId": "sha512-of-room-secret",
  "sequence": 0,
  "timestamp": 1718000000000,
  "payload": { ... }
}
```

### 4.3 消息类型

| type            | 方向            | payload        | 说明                  |
| --------------- | --------------- | -------------- | --------------------- |
| `JOIN`          | C → S           | JoinPayload    | 加入房间请求           |
| `JOIN_ACK`      | S → C           | JoinAckPayload | 加入确认(含成员+历史) |
| `USER_JOINED`   | S → C(broadcast)| UserEvent      | 新成员加入通知        |
| `USER_LEFT`     | S → C(broadcast)| UserEvent      | 成员离开通知          |
| `CHAT_TEXT`     | C → S → C       | ChatPayload    | 文字消息，服务端序列化 |
| `FILE_META`     | C → S → C       | FileMeta       | 文件元数据            |
| `FILE_CHUNK`    | C → S → C       | FileChunk      | 文件分片(64KB)        |
| `FILE_CHUNK_ACK`| C → S → C       | FileChunkAck   | 分片确认(预留)        |

### 4.4 加入流程 (Handshake)

```
Client                    Server
  │                          │
  │──── JOIN ───────────────▶│
  │    {peerId, nickname,    │── roomId = SHA-512(roomSecret)
  │     roomSecret}          │── RoomRegistry.join(roomId, ...)
  │                          │── 查询历史(最近10条)
  │                          │── 构建成员列表
  │                          │
  │◀─── JOIN_ACK ───────────│
  │    {roomId, history[],   │
  │     members[]}           │
  │                          │
  │◀─── USER_JOINED ────────│ (广播给其他成员)
```

---

## 5. 数据库

### 5.1 SQLite Schema

```sql
-- 消息表 (服务端+客户端各有一份)
CREATE TABLE messages (
    message_id TEXT PRIMARY KEY,
    type       TEXT NOT NULL,
    sender_id  TEXT NOT NULL,
    room_id    TEXT NOT NULL,
    sequence   INTEGER NOT NULL,
    timestamp  INTEGER NOT NULL,
    payload    TEXT NOT NULL
);
CREATE INDEX idx_messages_room_seq ON messages(room_id, sequence);

-- 文件表 (仅客户端)
CREATE TABLE files (
    file_id         TEXT PRIMARY KEY,
    message_id      TEXT NOT NULL,
    file_name       TEXT NOT NULL,
    content_type    TEXT,
    total_size      INTEGER NOT NULL,
    total_chunks    INTEGER NOT NULL,
    checksum        TEXT,
    local_path      TEXT,
    received_chunks INTEGER DEFAULT 0,
    status          TEXT DEFAULT 'PENDING'
);
```

### 5.2 数据容量策略

| 规则            | 值  |
| --------------- | --- |
| JOIN 拉历史条数 | 10  |
| 房间消息上限    | 100 |
| 超限处理        | 服务端 `trimRoom()` 按 sequence 删除最旧记录 |

---

## 6. 目录结构

```
LanIM2/
├── build.gradle                  # Gradle 构建脚本
├── class-diagram.html            # 交互式类关系图 (浏览器打开)
├── README.md                     # 旧版 README
├── READMEv2.md                   # 本文档
├── gradlew / gradlew.bat         # Gradle Wrapper
├── gradle/                       # Gradle 配置
│   └── wrapper/
├── config/                       # TLS 配置 (自动生成)
│   └── keystore.jks
├── data/                         # 运行时数据 (自动生成)
│   ├── lanim.db
│   └── files/
└── src/
    └── main/
        ├── java/
        │   └── com/alpha/lanim/
        │       ├── Launcher.java
        │       ├── client/
        │       │   ├── ImClient.java
        │       │   ├── ChatCoordinator.java
        │       │   └── FileTransfer.java
        │       ├── server/
        │       │   ├── ImServer.java
        │       │   ├── ClientSession.java
        │       │   └── RoomRegistry.java
        │       ├── ui/
        │       │   ├── LoginView.java
        │       │   ├── ChatView.java
        │       │   ├── MemberPane.java
        │       │   ├── ChatPane.java
        │       │   ├── PreviewPane.java
        │       │   └── Styles.java
        │       ├── model/
        │       │   ├── Envelope.java
        │       │   ├── MessageType.java
        │       │   ├── JoinPayload.java
        │       │   ├── JoinAckPayload.java
        │       │   ├── UserEventPayload.java
        │       │   ├── ChatPayload.java
        │       │   ├── FileMetaPayload.java
        │       │   ├── FileChunkPayload.java
        │       │   ├── FileChunkAckPayload.java
        │       │   ├── MemberRow.java
        │       │   └── FileRecord.java
        │       ├── data/
        │       │   ├── Database.java
        │       │   ├── MessageRepo.java
        │       │   └── FileRepo.java
        │       ├── net/
        │       │   ├── FrameCodec.java
        │       │   ├── SocketChannel.java
        │       │   └── TlsContext.java
        │       └── util/
        │           ├── Constants.java
        │           ├── HashUtil.java
        │           ├── JsonUtil.java
        │           └── Validator.java
        └── resources/
            ├── sql/
            │   └── schema.sql
            └── styles/
                └── lanim.css
```

---

## 7. 关键常量

| 常量                     | 值                    | 说明              |
| ------------------------ | --------------------- | ----------------- |
| `DEFAULT_SERVER_HOST`    | `10.129.245.252`      | 默认服务端地址    |
| `DEFAULT_SERVER_PORT`    | `9090`                | 默认服务端端口    |
| `JOIN_TIMEOUT_MS`        | `15000`               | JOIN 超时(毫秒)   |
| `JOIN_HISTORY_LIMIT`     | `10`                  | 加入时拉取历史条数|
| `ROOM_MESSAGE_CAP`       | `100`                 | 房间消息容量      |
| `FILE_CHUNK_SIZE`        | `65536` (64 KiB)      | 文件分片大小      |
| `MAX_NICKNAME_LENGTH`    | `32`                  | 昵称最大字符数    |
| `MAX_TEXT_LENGTH`        | `4096`                | 单条消息最大字节  |

---

## 8. 开发命令

```bash
# 编译
./gradlew compileJava

# 运行客户端
./gradlew run

# 运行服务端
./gradlew runServer

# 运行测试 (如有)
./gradlew test

# 构建分发
./gradlew assemble
```
