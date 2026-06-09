# LanIM V2 实现清单

> 简洁重构版：client / server / ui / data / model / net / util 分包。  
> 远程服务端 **`10.129.245.252:9090`**；进房拉 **10** 条历史；服务端每房最多 **100** 条。  
> 本文档列出 **每个类要实现的功能** 与 **方法签名**，供手动实现时对照。

---

## 1. 全局常量 `util.Constants`

| 常量 | 值 | 说明 |
|------|-----|------|
| `DEFAULT_SERVER_HOST` | `"10.129.245.252"` | 客户端默认服务器 |
| `DEFAULT_SERVER_PORT` | `9090` | TCP 端口 |
| `JOIN_HISTORY_LIMIT` | `10` | JOIN_ACK 下发条数 |
| `ROOM_MESSAGE_CAP` | `100` | 服务端每 room 消息上限 |
| `FILE_CHUNK_SIZE` | `65536` | 文件分片字节数 |
| `DEFAULT_DB_PATH` | `"data/lanim.db"` | SQLite 路径 |
| `DEFAULT_FILES_PATH` | `"data/files"` | 接收文件目录 |
| `DEFAULT_KEYSTORE_PATH` | `"config/keystore.jks"` | TLS 密钥库 |
| `TRANSPORT_MODE_TLS` / `PLAIN` | `"tls"` / `"plain"` | 传输模式字符串 |
| `DEFAULT_TRANSPORT_MODE` | `"tls"` | 默认 TLS |
| `MAX_NICKNAME_LENGTH` | `32` | 昵称上限 |
| `MAX_TEXT_LENGTH` | `4096` | 单条文本上限 |
| `JOIN_TIMEOUT_MS` | `15000` | `connectAndJoin` 等待 ACK 超时（建议） |

---

## 2. 入口

### `Launcher`（`com.alpha.lanim.Launcher`）

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| JavaFX 入口 | `public void start(Stage stage)` | 初始化 `Database`、`TlsContext`；显示 `LoginView` |
| 连接 | `private void doConnect(String nickname, String roomSecret, boolean useTls, String serverAddress)` | 后台线程：`ImClient.connectAndJoin(...)` **成功后再** `Platform.runLater` 打开 `ChatView`；失败弹窗回登录 |
| 主界面 | `private void showChatView(JoinAckPayload ack, ImClient client, ...)` | 创建 `ChatCoordinator`、三 Pane、`ChatView.create(...)` |
| 退出 | `private void shutdown()` | `client.close()` |
| main | `public static void main(String[] args)` | `launch(args)` |

**流程约束：** 不得在收到 `JOIN_ACK` 之前进入 `ChatView`（避免成员/历史竞态）。

---

## 3. UI 层 `ui.*`

### `LoginView`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 建 Scene | `static Scene create(Stage stage, ConnectCallback callback)` | 字段：Server Address（默认 `10.129.245.252:9090`）、Nickname、Room Secret、TLS 勾选、Connect 按钮 |
| 回调 | `interface ConnectCallback { void onConnect(String nickname, String roomSecret, boolean useTls, String serverAddress); }` | 校验交给 `Validator`，通过后禁用按钮并回调 |

### `ChatView`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 建 Scene | `static Scene create(Stage stage, JoinAckPayload ack, ChatCoordinator coordinator, String localNickname)` | `BorderPane`：左 `MemberPane`、中 `ChatPane`、右 `PreviewPane`；底栏 Send/File 委托 `coordinator` |
| 初始化 | （内部） | 用 `ack` 调用 `coordinator.onJoined(ack)` 或等价逻辑，刷成员与历史 |

### `MemberPane`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 节点 | `Node getNode()` | 返回含 `Label`（Members (n)）+ `ListView<String>` 的 `VBox` |
| 全量刷新 | `void setMembers(String localNickname, List<MemberRow> remotes)` | 第一项 `(You)`，其余 `[online]` |
| 增 | `void addMember(String peerId, String nickname)` | 去重后追加 |
| 删 | `void removeMember(String peerId)` | 按 peerId 移除 |
| 计数 | `int size()` | 含自己在内人数 |

### `ChatPane`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 节点 | `Node getNode()` | `ScrollPane` 消息区 + 底栏 `TextField` + Send + File |
| 历史 | `void loadHistory(List<Envelope> messages)` | 清空后按 sequence 渲染；支持 `CHAT_TEXT`、`FILE_META` |
| 追加 | `void append(Envelope env, String senderLabel)` | 去重 `messageId`；文本/文件行样式区分 |
| 本地乐观 | `void appendLocalText(String text, String nickname)` | 自己发送后立即显示（可选，或由 coordinator 统一 `append`） |
| 文件行 | `void appendFileNotice(String senderLabel, String fileName, long sizeBytes)` | 「xxx sent file: name (KB)」 |
| 绑定发送 | `void setOnSend(Runnable action)` / `void setOnFile(Runnable action)` | 按钮/Enter 触发 |

**线程：** 所有改 UI 必须在 `Platform.runLater` 内（由 `ChatCoordinator` 调用时保证）。

### `PreviewPane`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 节点 | `Node getNode()` | 标题 + 预览区（`WebView` / `ImageView` / 占位 Label） |
| 展示 | `void showFile(Path localPath, String contentType, String fileName)` | 按类型选择渲染；不支持则显示「无法预览」 |
| 清空 | `void clear()` | 重置右侧 |
| 规则 | `static boolean canPreview(String contentType, String fileName)` | 扩展名/MIME 白名单：如 `pdf`、常见图片；docx 若 MVP 不支持则返回 false |

### `MemberRow`（`model` 或 `ui`）

```java
public record MemberRow(String peerId, String nickname) {}
```

---

## 4. 客户端核心 `client.*`

### `ImClient`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 构造 | `ImClient(TlsContext tlsContext)` | 保存 TLS 上下文 |
| 连接并入房 | `JoinAckPayload connectAndJoin(String host, int port, boolean useTls, String peerId, String nickname, String roomSecret) throws Exception` | ① `SocketChannel.connect` ② 启动读线程 ③ 发 `JOIN` ④ **阻塞**至收到 `JOIN_ACK`（或超时抛异常）⑤ 返回 payload |
| 发送 | `void send(Envelope env) throws IOException` | `FrameCodec.encode` → `SocketChannel.write` |
| 监听 | `void setListener(InboundListener listener)` | 读线程 decode 后回调（`JOIN_ACK` 在 connectAndJoin 内特殊处理或也转发 listener） |
| 关闭 | `void close()` | 停读线程、关 socket |
| 回调 | `interface InboundListener { void onEnvelope(Envelope env); }` | |

**读线程逻辑：**

```
loop while open:
  bytes = channel.read()
  env = FrameCodec.decode(bytes)
  if env.type == JOIN_ACK && joinFuture pending → complete future
  else if listener != null → listener.onEnvelope(env)
```

### `ChatCoordinator`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 构造 | `ChatCoordinator(ImClient client, MessageRepo repo, FileTransfer fileTransfer, MemberPane members, ChatPane chat, PreviewPane preview, String localPeerId, String localNickname, String roomId)` | 注册 `client.setListener(this)` |
| 首次进房 | `void onJoined(JoinAckPayload ack)` | `members.setMembers(...)`；`chat.loadHistory(ack.getHistory())`；历史写入 `repo` |
| 入站 | `void onEnvelope(Envelope env)` | 按 `MessageType` 分发（见下表） |
| 发文字 | `void sendText(String text)` | 校验 → 构造 `CHAT_TEXT` → `client.send` → 本地 `chat.append` |
| 发文件 | `void sendFile(Path path) throws IOException` | 委托 `fileTransfer.send(...)` |

**`onEnvelope` 分发表：**

| type | 动作 |
|------|------|
| `USER_JOINED` | `members.addMember` |
| `USER_LEFT` | `members.removeMember` |
| `CHAT_TEXT` | `repo.insert`；`chat.append`（senderId ≠ 自己） |
| `FILE_META` / `FILE_CHUNK` / `FILE_CHUNK_ACK` | `fileTransfer.handle(env)` |
| `JOIN_ACK` | 正常仅 connectAndJoin 内处理；可忽略重复 |

### `FileTransfer`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 构造 | `FileTransfer(ImClient client, FileRepo fileRepo, ChatPane chat, PreviewPane preview, String localPeerId, String roomId)` | 确保 `data/files` 目录存在 |
| 发送 | `void send(Path file, String peerId, String roomId) throws IOException` | 算 SHA-256；发 `FILE_META`；按 `FILE_CHUNK_SIZE` 循环发 `FILE_CHUNK` |
| 入站 | `void handle(Envelope env)` | META：建 `FileRecord` PENDING；CHUNK：Base64 解码写盘；收齐校验 → COMPLETE → `preview.showFile`（若 `canPreview`）+ `chat.appendFileNotice` |
| 缺片 | （内部） | 可选：发 `FILE_CHUNK_ACK` 请求重传 |

---

## 5. 服务端 `server.*`

### `ImServer`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 构造 | `ImServer(int port, boolean useTls, TlsContext tls)` | 创建 `RoomRegistry`、线程池 |
| 启动 | `void start() throws Exception` | `Database.init()`；`ServerSocket` 或 `SSLServerSocket` 绑定 `0.0.0.0:port`；accept 循环 |
| 连接 | `private void handleConnection(Socket raw)` | 包装 `SocketChannel` → `new ClientSession(...).run()` 提交线程池 |
| 停止 | `void shutdown()` | 关 socket、关所有 session、`RoomRegistry.shutdown()` |
| main | `static void main(String[] args)` | 解析 `--port`、`--plain`/`--tls`；挂 shutdown hook |

### `ClientSession`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 构造 | `ClientSession(SocketChannel channel, RoomRegistry registry)` | |
| 运行 | `public void run()` | 读循环 → decode → switch(type)；IO 异常时 `onDisconnect()` |
| 发送 | `void send(Envelope env)` | encode + write（线程安全） |
| 发送 bytes | `void sendRaw(byte[] framed)` | 供 registry 转发 |
| JOIN | `private void onJoin(Envelope env)` | `roomId=SHA512(secret)`；`registry.join`；回 `JOIN_ACK(recent 10, members)`；广播 `USER_JOINED` |
| CHAT | `private void onChat(Envelope env)` | 需已 join；`nextSeq`；`saveAndTrim`；`broadcastExcept`（不含自己） |
| FILE | `private void onFile(Envelope env)` | META 赋 seq 并 saveAndTrim；CHUNK/ACK 仅转发 |
| 断开 | `private void onDisconnect()` | `registry.leave`；广播 `USER_LEFT` |

### `RoomRegistry`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 入房 | `void join(String roomId, String peerId, String nickname, ClientSession session)` | 更新 `roomSessions`、`roomMembers`；初始化序号计数器（从 DB maxSeq+1） |
| 离房 | `void leave(String roomId, String peerId)` | 移除 session；空房可清理 |
| 序号 | `int nextSeq(String roomId)` | 房间内原子递增 |
| 存库 | `void saveAndTrim(Envelope env)` | `MessageRepo.insert` + `trimRoom(roomId, ROOM_MESSAGE_CAP)` |
| 历史 | `List<Envelope> recent(String roomId, int limit)` | 委托 `MessageRepo.recentByRoom` |
| 成员 | `List<JoinAckPayload.MemberInfo> members(String roomId)` | 当前在线 peerId + nickname |
| 转发 | `void broadcastExcept(String roomId, ClientSession exclude, Envelope env)` | 同房其他 session `send` |
| 转发 bytes | `void broadcastExcept(String roomId, ClientSession exclude, byte[] framed)` | 已编码帧直接转发 |
| 关闭 | `void shutdown()` | 关闭所有 session |

**内存结构建议：**

```
roomSessions: Map<roomId, Map<peerId, ClientSession>>
roomMembers:  Map<roomId, Map<peerId, String>>  // nickname
roomSeq:      Map<roomId, AtomicInteger>
```

---

## 6. 数据层 `data.*`

### `Database`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 初始化 | `static void init()` | 创建 `data/`；执行 `resources/sql/schema.sql` |
| 连接 | `static Connection connect() throws SQLException` | `jdbc:sqlite:data/lanim.db` |

### `MessageRepo`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 插入 | `void insert(Envelope e)` | `INSERT OR IGNORE` by `message_id` |
| 最近 | `List<Envelope> recentByRoom(String roomId, int limit)` | 按 `sequence`（或 timestamp）降序取 limit，再反转为正序展示 |
| 裁剪 | `void trimRoom(String roomId, int maxCount)` | 删除该 room 超出 maxCount 的最旧记录 |
| 最大序号 | `int maxSequence(String roomId)` | 服务端恢复序号用 |

### `FileRepo`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 保存 | `void upsert(FileRecord record)` | 文件传输状态 |
| 查询 | `Optional<FileRecord> findByFileId(String fileId)` | |
| 更新进度 | `void updateProgress(String fileId, int receivedChunks, String status)` | PENDING → COMPLETE |

---

## 7. 模型层 `model.*`

### `MessageType`（enum）

```
JOIN, JOIN_ACK, USER_JOINED, USER_LEFT,
CHAT_TEXT, FILE_META, FILE_CHUNK, FILE_CHUNK_ACK
```

（可选预留 `HEARTBEAT`；MVP 可不实现。）

### `Envelope`

| 字段 | 类型 |
|------|------|
| type | String |
| messageId | String |
| senderId | String |
| roomId | String |
| sequence | int |
| timestamp | long |
| payload | Object / JsonObject |

getter/setter + 全参构造。

### 载荷类（POJO + Gson）

| 类 | 字段 | 用途 |
|----|------|------|
| `JoinPayload` | peerId, nickname, roomSecret | JOIN |
| `JoinAckPayload` | assignedId, roomId, history, members | JOIN_ACK |
| `JoinAckPayload.MemberInfo` | peerId, nickname | 成员列表项 |
| `UserEventPayload` | peerId, nickname | USER_JOINED / USER_LEFT |
| `ChatPayload` | text | CHAT_TEXT |
| `FileMetaPayload` | fileId, fileName, contentType, totalSize, totalChunks, checksum | FILE_META |
| `FileChunkPayload` | fileId, chunkIndex, totalChunks, data(Base64) | FILE_CHUNK |
| `FileChunkAckPayload` | fileId, missingChunks | FILE_CHUNK_ACK |
| `FileRecord` | fileId, messageId, fileName, contentType, totalSize, totalChunks, checksum, localPath, receivedChunks, status | DB 实体 |

---

## 8. 网络层 `net.*`

### `FrameCodec`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 编码 | `static byte[] encode(Envelope env)` | JSON UTF-8 → `[4 byte big-endian len][body]` |
| 解码 | `static Envelope decode(byte[] frame)` | 反序列化 Gson → Envelope |
| 解码流 | `static Envelope decode(DataInputStream in)` | 读 int 长度 + readFully（供 SocketChannel 使用） |

### `SocketChannel`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 连接 | `static SocketChannel connect(String host, int port, boolean useTls, TlsContext tls) throws IOException` | 建立 Plain 或 TLS socket |
| 服务端 | `static SocketChannel fromSocket(Socket socket, boolean useTls)` | accept 后包装 |
| 写帧 | `void writeFrame(byte[] frame) throws IOException` | |
| 读帧 | `byte[] readFrame() throws IOException` | 读长度前缀 + body |
| 关闭 | `void close()` | |
| 状态 | `boolean isOpen()` | |

### `TlsContext`

| 方法 | 签名 | 要实现的功能 |
|------|------|----------------|
| 初始化 | `void init() throws Exception` | 加载或生成 `keystore.jks` |
| 服务端 | `SSLContext serverContext()` | |
| 客户端 | `SSLContext clientContext()` | TOFU：接受自签服务端证书 |

---

## 9. 工具 `util.*`

### `JsonUtil`

| 方法 | 签名 |
|------|------|
| | `static Gson gson()` |
| | `static String toJson(Object o)` |
| | `static byte[] toJsonBytes(Object o)` |
| | `static <T> T fromJson(byte[] bytes, Class<T> clazz)` |
| | `static <T> T fromPayload(Object payload, Class<T> clazz)` |

### `HashUtil`

| 方法 | 签名 | 功能 |
|------|------|------|
| | `static String sha512Hex(String input)` | roomId |
| | `static String sha256Hex(byte[] data)` | 文件校验 |
| | `static String sha256HexFile(Path path)` | 发送前文件 hash |

### `Validator`

| 方法 | 签名 | 功能 |
|------|------|------|
| | `static String validateNickname(String s)` | null=OK，否则错误信息 |
| | `static String validateRoomSecret(String s)` | |
| | `static String validateChatText(String s)` | |

---

## 10. 协议行为速查

### JOIN 流程

1. Client → `JOIN {peerId, nickname, roomSecret}`
2. Server：`roomId = sha512(roomSecret)`；join registry
3. Server → Client：`JOIN_ACK {roomId, history≤10, members[]}`
4. Server → Others：`USER_JOINED {peerId, nickname}`

### CHAT_TEXT 流程

1. Client → Server：`CHAT_TEXT`（sequence 可先 0）
2. Server：赋 seq；`saveAndTrim`；转发其他人
3. Others：`ChatCoordinator` → DB + UI

### 断开

1. TCP 断 → `ClientSession.onDisconnect`
2. Server → Others：`USER_LEFT`

---

## 11. Gradle 入口（重构后）

```groovy
application {
    mainClass = 'com.alpha.lanim.Launcher'
}
tasks.register('runServer', JavaExec) {
    mainClass = 'com.alpha.lanim.server.ImServer'
}
```

---

## 12. 删除清单（重构时移除）

- 包 `bll/` 全部
- `dal/PeerDao`、`model/Peer.java`（若不再使用）
- `SyncReqPayload`、`SyncRespPayload`
- `ui/MainController`、`ui/LoginController`（由 ChatView/LoginView 替代）
- `server/LanIMServer`、`ClientHandler`、`RoomManager`（由 ImServer、ClientSession、RoomRegistry 替代）
- `build.gradle` 中 `jmdns` 依赖

---

## 13. 建议实现与自测顺序

| 步骤 | 内容 | 自测 |
|------|------|------|
| 1 | `Constants` + `model` + `FrameCodec` + `JsonUtil` | 单元测试 encode/decode |
| 2 | `Database` + `MessageRepo` | recent / trim |
| 3 | `RoomRegistry` + `ClientSession` + `ImServer` | 本机 `runServer`，telnet/临时 client 发 JOIN |
| 4 | `ImClient.connectAndJoin` | 能拿 JOIN_ACK |
| 5 | `ChatCoordinator` + 三 Pane + `ChatView` | 双开客户端成员/聊天 |
| 6 | `FileTransfer` + `PreviewPane` | 传 pdf/图片 |
| 7 | 删旧代码、更新 README | 联调 `10.129.245.252` |

---

*文档版本：V2 简洁重构 · 与 `.cursor/plans/lanim_top-down_design_fe4a1768.plan.md` 一致*
