---
priority: P2
---
# WebSockets and Real-Time Protocols

A web page that must show new data without a reload needs a way to receive data that the server sends first. HTTP lets only the client start an exchange, so three techniques work around that rule: long polling (a request that the server holds open), Server-Sent Events (one response that never ends), and WebSocket (a different protocol on the same TCP connection after an HTTP upgrade). This page explains each technique at the byte level: the WebSocket handshake and frame format, the Server-Sent Events text format and its reconnection rules, the protocol view of long polling, and what proxies and load balancers do to a connection that stays open for hours. The design choice between the techniques has its home in the [communication patterns](../hld/fundamentals/09-communication-patterns.md) page. This page does not repeat that choice.

!!! info "Where the standards stand (checked on 2026-11-05)"
    - RFC 6455 (December 2011, Proposed Standard) defines WebSocket. It has no replacement. RFC 8441 (September 2018, Proposed Standard) updates it with WebSocket over HTTP/2. RFC 9220 (Proposed Standard, June 2022) does the same for HTTP/3. RFC 7692 (December 2015, Proposed Standard) defines the `permessage-deflate` extension.
    - The WHATWG HTML Standard (a living document) defines Server-Sent Events. No RFC covers the `text/event-stream` format.
    - RFC 6202 (Informational, 2011) records the known problems of long polling and streaming. It is not a protocol specification.

## Why It Matters

Chat, live dashboards, notifications, collaborative editing, and token streaming from a language model all push data to a client. Interviewers ask "how does the server push to the browser?" and then probe the protocol: What bytes does the upgrade send? Why does the client mask frames? How does a client find out that the connection is dead? What does the browser send after a reconnect? On the job, the same details explain incidents: a WebSocket that drops every 60 seconds behind a proxy, a Server-Sent Events stream that shows nothing because a proxy buffers it, and a deploy that sends all clients to one server. This page gives the facts that you need to debug those cases and to answer those questions. The HLD pages for [communication patterns](../hld/fundamentals/09-communication-patterns.md) and the [chat system](../hld/case-studies/06-chat-system.md) use these protocols in a design.

## Core Concepts

**Upgrade**: the HTTP mechanism that asks a server to switch the protocol on the same connection. The client sends `Upgrade: websocket` and `Connection: Upgrade`. If the server agrees, it answers `101 Switching Protocols`, and the bytes after that response use the new protocol. RFC 9110 (section 7.8) defines the `Upgrade` header. See [HTTP](04-http.md) for the HTTP side.

**WebSocket**: a protocol (RFC 6455) that gives a client and a server a two-way channel of messages over one TCP connection. It starts as an HTTP/1.1 request, so it passes through the ports and the proxies that carry HTTP. The URI schemes are `ws` (plain) and `wss` (inside TLS, see [TLS and HTTPS](05-tls-and-https.md)).

**Frame**: the unit of data on a WebSocket connection. A frame has a header (flags, an opcode, a length, and sometimes a masking key) and a payload. A **message** is one frame, or several frames (fragments) that the receiver joins.

**Opcode**: the 4-bit field that tells the receiver what a frame holds: a text message, a binary message, a continuation of a message, or a control frame.

**Control frame**: a frame that manages the connection and carries no application message: close, ping, or pong. A control frame has a payload of at most 125 bytes and is never fragmented.

**Masking**: the XOR of the payload with a 32-bit key. Every frame from a client to a server carries a mask. The mask protects intermediaries, not the data (see [Masking](#masking)).

**Subprotocol**: an application-level protocol that runs on top of WebSocket, such as a chat message format. The client lists the subprotocols that it knows in `Sec-WebSocket-Protocol`, and the server picks one.

**Server-Sent Events (SSE)**: a one-way stream of text events from a server to a client in one HTTP response with the media type `text/event-stream`. The browser API is `EventSource`. The WHATWG HTML Standard defines it.

**Long polling**: a technique in which the client sends an ordinary HTTP request and the server holds it until it has data or a timeout ends. The client then sends the next request. See [Long polling at the protocol level](#long-polling-at-the-protocol-level).

**Idle timeout**: the time after which a proxy, a load balancer, or a NAT device drops a connection that carries no bytes. See [Proxies and NAT](09-proxies-and-nat.md) for NAT.

## How It Works

### The WebSocket opening handshake

The client opens a TCP connection (and a TLS session for `wss`) and sends an HTTP/1.1 `GET` request. The example below is the one in RFC 6455 (section 1.2).

```text
GET /chat HTTP/1.1
Host: server.example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Origin: http://example.com
Sec-WebSocket-Protocol: chat, superchat
Sec-WebSocket-Version: 13
```

The server answers:

```text
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
Sec-WebSocket-Protocol: chat
```

After the blank line that ends the `101` response, the connection carries WebSocket frames and no more HTTP messages.

What the client must send (RFC 6455, section 4.1):

| Header | Value and rule |
|---|---|
| Request line | A `GET` request with HTTP version 1.1 or higher |
| `Host` | The server host name |
| `Upgrade` | `websocket` |
| `Connection` | `Upgrade` |
| `Sec-WebSocket-Key` | A nonce: 16 random bytes, Base64-encoded. The client picks a new one for each connection |
| `Sec-WebSocket-Version` | `13` |
| `Origin` | Sent by browsers. The server uses it to decide whether it accepts the page that opened the connection |
| `Sec-WebSocket-Protocol` | Optional. The subprotocols that the client accepts, in order of preference |
| `Sec-WebSocket-Extensions` | Optional. The extensions that the client accepts, for example per-message compression (RFC 7692) |

**How the server computes `Sec-WebSocket-Accept`** (RFC 6455, section 4.2.2):

1. Take the value of `Sec-WebSocket-Key` as text, without decoding it.
2. Append the fixed GUID `258EAFA5-E914-47DA-95CA-C5AB0DC85B11`.
3. Compute the SHA-1 hash of the result (20 bytes).
4. Base64-encode the hash.

For the sample key, the result is `s3pPLMBiTxaQ9kYGzzhZRbK+xOo=`. The [Java example](#java-example) computes it.

**What `Sec-WebSocket-Accept` proves.** It proves that the server read the handshake and understands WebSocket. It does not authenticate the server and it does not protect the data. RFC 6455 (section 1.3) states the purpose: an ordinary HTTP server, or an attacker who makes a browser send crafted HTTP requests (with a form or `XMLHttpRequest`), must not produce a valid answer by accident. A normal HTTP server does not know the GUID, so it does not send a matching value, and a script cannot set the `Sec-WebSocket-*` request headers in a browser.

**What the server does with the request.** The server checks each field in the table above. If a field is missing or wrong, the server stops and answers with an HTTP error, for example `400 Bad Request` (RFC 6455, section 4.2.1). If the server chooses a subprotocol, the value must be one that the client listed. If the client lists none that the server accepts, the server sends no `Sec-WebSocket-Protocol` header. The server also lists only extensions that the client requested.

**Version negotiation** (RFC 6455, section 4.4). If the server does not support the `Sec-WebSocket-Version` that the client sent, it must answer with a `Sec-WebSocket-Version` header (or several of them) that lists every version it is willing to use. The RFC example answers `400 Bad Request` with `Sec-WebSocket-Version: 13, 8, 7`. The RFC does not require a particular status code here. A server can also use `426 Upgrade Required` (RFC 9110, section 15.5.22), but the RFC 6455 text names `400`. The client then repeats the handshake with a version from the list, if it supports one. Only version 13 is in use today, so the case is rare.

**The `Origin` check.** Browsers send `Origin` on a WebSocket handshake, and the same-origin policy does not block a WebSocket connection to another origin. RFC 6455 (section 10.2) says that a server that expects only some origins "SHOULD verify" the `Origin` field and answer `403 Forbidden` to an origin that it does not expect. A server that authenticates the handshake with a cookie must check `Origin`. If it does not, any web page that the user visits can open a connection with the user's cookie (cross-site WebSocket hijacking). The [communication patterns](../hld/fundamentals/09-communication-patterns.md) page gives the same rule.

**The browser limits.** The browser `WebSocket` API cannot set custom headers on the handshake. A browser client authenticates with a cookie, a short-lived token in the query string, or a first message after the connection opens. A token in a URL can reach access logs, so use a token that expires within seconds.

### The frame format

RFC 6455 (section 5.2) defines this layout. Each row is 32 bits.

```text
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - - +
:                     Payload Data continued ...                :
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
|                     Payload Data continued ...                |
+---------------------------------------------------------------+
```

| Field | Size | Meaning |
|---|---|---|
| FIN | 1 bit | 1 if this frame is the last fragment of a message |
| RSV1, RSV2, RSV3 | 1 bit each | 0, unless a negotiated extension gives them a meaning. A receiver that sees a set bit without such an extension fails the connection |
| Opcode | 4 bits | `0x0` continuation, `0x1` text (UTF-8), `0x2` binary, `0x8` close, `0x9` ping, `0xA` pong. Other values are reserved. A receiver fails the connection on an unknown opcode |
| MASK | 1 bit | 1 if a masking key follows the length |
| Payload length | 7 bits, or 7 + 16, or 7 + 64 | See below |
| Masking key | 0 or 4 bytes | Present only if MASK is 1 |
| Payload | the length above | The data, masked if MASK is 1 |

**The three length forms.** The 7-bit field holds 0 to 125 and is the length. The value 126 means that the next 2 bytes hold the length as an unsigned 16-bit number. The value 127 means that the next 8 bytes hold it as an unsigned 64-bit number whose top bit is 0. The sender must use the shortest form. All multi-byte numbers use network byte order (big endian). The length does not include the 4 bytes of the masking key.

The header of a frame is therefore 2 bytes (payload up to 125 bytes), 4 bytes (up to 65,535 bytes), or 10 bytes (more), plus 4 bytes if the frame is masked. A client frame with a 5-byte payload takes 11 bytes: 2 header bytes, 4 mask bytes, 5 payload bytes. The example output below shows this frame.

### Masking

The client generates a 32-bit masking key for each frame. The key must be unpredictable (RFC 6455, section 5.3). The sender XORs byte `i` of the payload with byte `i mod 4` of the key. The receiver applies the same XOR to get the payload back. The server never masks its frames. A server that receives an unmasked client frame must close the connection, and a client that receives a masked frame must close it too (RFC 6455, section 5.1).

**Why clients mask.** Masking does not encrypt anything, because the key travels in the same frame. RFC 6455 (section 10.3) explains the purpose: an attacker who controls a script in a browser must not control the bytes that appear on the wire. Without masking, a script could write bytes that look like an HTTP request. An old proxy that does not understand WebSocket could take those bytes for a new HTTP request, and it could then store a wrong response for a real URL. Masking with an unpredictable key per frame stops the script from choosing the wire bytes. Servers do not mask because the threat is a script in the client. Confidentiality comes from `wss` (TLS).

### Fragmentation

A sender can split one message into several frames. The first frame has FIN = 0 and the real opcode (text or binary). Each next frame has opcode `0x0` (continuation). The last frame has FIN = 1 and opcode `0x0`. A receiver joins the payloads in order. Fragments of two messages must not interleave unless an extension allows it. Control frames can appear between fragments. Fragmentation lets a sender start a large message before it knows the full size, and it stops a large message from blocking a ping for a long time. Every endpoint must accept both fragmented and unfragmented messages. The JDK `WebSocket.Listener` shows this with its `last` argument, which is true on the final part of a message.

### Control frames: ping, pong, close

**Ping and pong.** Either side can send a ping at any time. The receiver must answer with a pong that carries the same payload, as soon as practical, unless it already received a close frame. If several pings arrive before the receiver answers, it may answer only the latest one. A pong without a ping is allowed and works as a one-way heartbeat. The payload of both is at most 125 bytes. Ping and pong give two things: a check that the peer is alive at the application level, and traffic that resets the idle timers of proxies. The browser `WebSocket` API cannot send a ping, but a browser answers a server ping with a pong by itself. A browser client that needs a heartbeat sends an application message.

**Close.** The close handshake is a WebSocket message exchange that sits on top of the TCP close. Either side sends a close frame. Its payload is optional. If present, it has a 2-byte status code (unsigned, big endian) and then an optional UTF-8 reason. The reason is not guaranteed to be readable, and a client must not show it to the user. After a side sends a close frame, it sends no more data frames. The side that receives a close frame without having sent one must answer with a close frame (usually with the same code). When both close frames have passed, the server closes the TCP connection at once, and the client waits for the server to do so (it may close after a reasonable time). The exchange avoids a case in which an abrupt TCP reset makes the peer lose data that was already sent (RFC 6455, sections 1.4 and 5.5.1).

**Close codes** (RFC 6455, section 7.4):

| Code | Name | Meaning |
|---|---|---|
| 1000 | Normal closure | The purpose of the connection is fulfilled |
| 1001 | Going away | A server shuts down, or a browser leaves the page |
| 1002 | Protocol error | The endpoint received a malformed frame, for example an unmasked client frame |
| 1003 | Unsupported data | The endpoint got a data type that it cannot accept (for example binary on a text-only endpoint) |
| 1004 | Reserved | The RFC reserves it. Its meaning may be defined in the future |
| 1005 | No status received | Reserved. An endpoint must not send it. An API reports it when a close frame had no code |
| 1006 | Abnormal closure | Reserved. An endpoint must not send it. An API reports it when the connection ended with no close frame (a TCP reset, a timeout, a dropped network) |
| 1007 | Invalid payload data | The data does not match the message type, for example text that is not UTF-8 |
| 1008 | Policy violation | A generic code for a message that breaks a rule of the endpoint |
| 1009 | Message too big | The message is too large to process |
| 1010 | Mandatory extension | A client closes because the server did not negotiate an extension that the client needs |
| 1011 | Internal error | A server closes because of an unexpected condition |
| 1015 | TLS handshake failure | Reserved. An endpoint must not send it |
| 3000 to 3999 | Registered | For libraries, frameworks, and applications. IANA registers them |
| 4000 to 4999 | Private use | For applications that agree on them. Nobody registers them |

RFC 6455 lists 1005, 1006, and 1015 as values that an API reports to the local application. They never travel in a close frame. The JDK `WebSocket.sendClose` javadoc lists more codes that it rejects (1002, 1003, 1006, 1007, 1009, 1010, 1012, 1013, 1015) and accepts a code from 1000 to 4999 otherwise.

**What a client sees after a drop.** If the network breaks, no close frame arrives, and the API reports 1006. The client cannot tell a crashed server from a dropped network. This is the reason for the reconnection rules in the next paragraph and for ping and pong.

**Reconnection.** RFC 6455 (section 7.2.3) tells a client how to pace reconnects after an abnormal closure. The client should delay the first attempt by a random time (the RFC suggests a value between 0 and 5 seconds) and use a longer delay after each failed attempt, for example truncated binary exponential backoff. WebSocket has no event identifiers and no replay, so the application must build resume logic (for example a "last message ID" in the first message). The communication patterns page describes this design.

### WebSocket over HTTP/2 and HTTP/3

The `Upgrade` mechanism exists only in HTTP/1.1, and HTTP/2 and HTTP/3 carry many requests on one connection. A `101` response would switch the whole connection, so another mechanism is needed.

RFC 8441 defines **Extended CONNECT** for HTTP/2:

- The server sends the setting `SETTINGS_ENABLE_CONNECT_PROTOCOL` with value 1. A client uses Extended CONNECT only after it receives that value.
- The client sends a `CONNECT` request with a `:protocol` pseudo-header whose value is `websocket`, plus `:scheme` (`https` for `wss`), `:path`, and `:authority`. It does not send `Connection` or `Upgrade`, and it does not use `Sec-WebSocket-Key` or `Sec-WebSocket-Accept`.
- The server answers with a `2xx` status (the RFC example shows `200`). The stream then carries WebSocket frames in `DATA` frames, as if the stream were the TCP connection. The WebSocket frame format of RFC 6455 does not change.
- One request stream holds one WebSocket connection. Many WebSocket connections and ordinary requests share one TCP connection. A clean close maps to the `END_STREAM` flag, and an abort maps to `RST_STREAM` with `CANCEL`.
- `:authority` does not name a tunnel target. The server must not open a tunnel to it.
- `Origin`, `Sec-WebSocket-Version`, `Sec-WebSocket-Protocol`, and `Sec-WebSocket-Extensions` stay in the request and the response, as in RFC 6455. The `Origin` check therefore still applies. Header names are lowercase in HTTP/2.

RFC 9220 adapts the same mechanism for HTTP/3. The setting has the same name, with the value 0x08. An orderly close maps to a `FIN` on the QUIC stream, and an abort maps to the `H3_REQUEST_CANCELLED` error. A server that advertises the setting but receives a `:protocol` value that it does not support should answer `501 (Not Implemented)`.

Benefit: a browser opens no extra TCP and TLS connection for each WebSocket. Cost: a proxy must understand Extended CONNECT. Support differs by browser, server, and proxy, so check the product documentation before you rely on it. See [HTTP](04-http.md) for HTTP/2 and HTTP/3 streams.

### Server-Sent Events: the format

The client sends an ordinary `GET` request. The server answers with status `200` and `Content-Type: text/event-stream` and keeps the response open. The body is UTF-8 text. The WHATWG HTML Standard (section "Server-sent events") defines the parsing.

```text
retry: 5000

: keep-alive comment

id: 17
event: order
data: {"id":42}

id: 18
data: line one
data: line two

```

Rules of the format:

- The stream is a series of **events**. A blank line ends an event.
- A line ends with LF, CR, or CRLF.
- A line that starts with a colon is a comment. The client ignores it. A comment is the usual keep-alive.
- Any other line is `name: value`. The parser removes one space after the colon, if there is one. A line with no colon is a field name with an empty value.
- `data`: the parser adds the value and an LF to a buffer. Several `data` lines make one multi-line payload. When the event ends, the parser removes the last LF.
- `event`: the event type. If it is absent, the browser fires a `message` event. If it is present, the browser fires an event of that type, and the page listens with `addEventListener("order", ...)`.
- `id`: the event identifier. The parser ignores a value that contains a NUL character. An `id` line with an empty value sets the identifier to the empty string, so the browser sends no `Last-Event-ID` after that.
- `retry`: the reconnection delay in milliseconds. The parser ignores the field unless the value is only ASCII digits.
- Other field names are ignored.
- If the stream ends in the middle of an event, the client discards the incomplete event.

The browser rejects the response (it fails the connection and does not reconnect) if the status is not `200` or the `Content-Type` is not `text/event-stream`. The standard follows redirects `301` and `307` as for other requests. Any other status (for example `500`) also fails the connection, and `EventSource` does not retry it. A server stops a client from reconnecting with `204 No Content`. The standard decodes the stream as UTF-8 and removes one leading byte order mark.

### Server-Sent Events: reconnection and `Last-Event-ID`

If the connection closes, `EventSource` reconnects by itself (unless the application called `close()`). The delay is the reconnection time. Its start value depends on the browser ("probably in the region of a few seconds", says the standard), and a `retry` field changes it. The browser may add extra delay for backoff.

The browser keeps a **last event ID string**. The parser stores an `id` value in a buffer while it reads the event, and it copies the buffer into the last event ID string when the blank line arrives (the dispatch step). The browser sends the last event ID string in the `Last-Event-ID` request header when it reconnects, but only if the string is not empty. So an event that the connection cut off is not counted, and the server resends from the last complete event. The standard does not say how a server finds the missed events. The server must keep recent events (for example in a log or a stream with identifiers) to replay them. If the server cannot replay, the client must read the current state.

The [Java example](#java-example) shows this: the parser reads an event with `id: 19` that the connection cuts off, and the `Last-Event-ID` stays `18`.

Keep-alive: the standard advises that a server send a comment line "every 15 seconds or so", because legacy proxies close idle connections. Text only: binary data needs an encoding such as Base64.

### Long polling at the protocol level

Long polling uses an ordinary HTTP exchange, and no new protocol.

1. The client sends `GET /updates?since=412` (or `POST`). The `since` value is a cursor that names the last event that the client has.
2. The server checks for events after the cursor. If there are some, it answers at once. If there are none, it parks the request: it holds the connection and does not write a response.
3. The server answers when an event arrives (`200` with the events and a new cursor) or when its hold timeout ends (`200` with an empty list, or `204 No Content`).
4. The client sends the next request with the new cursor, at once.

Protocol details that matter:

- **Timeouts nest.** The hold time of the server must be shorter than the idle timeout of every proxy between the client and the server, and shorter than the client read timeout. RFC 6202 discusses the timeouts of intermediaries as a known problem of long polling. Recommendation: set the hold time to half of the shortest idle timeout on the path (for a 60-second timeout, 30 seconds).
- **Each event costs a full request.** The request carries headers (and cookies) each time. With HTTP/2, header compression reduces that cost ([HTTP](04-http.md)).
- **No loss between polls.** An event that arrives between the response and the next request must wait for the next request, so the server keeps events by cursor.
- **HTTP/1.1 connection limit.** A browser opens about 6 connections to one host on HTTP/1.1. A held request uses one of them.

### Long-lived connections behind proxies and load balancers

A WebSocket connection or an SSE stream lives for minutes or hours. Every device between the client and the server (a NAT, a firewall, a CDN, a load balancer, a reverse proxy) keeps state for the connection and drops it after an idle timeout. The page [Proxies and NAT](09-proxies-and-nat.md) explains NAT and proxies. The design view of load balancers is in [load balancing, DNS, and CDN](../hld/fundamentals/02-load-balancing-dns-cdn.md).

**Idle timeouts.** The values below come from product documentation (checked on 2026-11-05). Configuration can change them.

| Device | Idle timeout | Source |
|---|---|---|
| Nginx reverse proxy | Closes the proxied connection if the backend sends no data for 60 seconds (`proxy_read_timeout`) | Nginx, "WebSocket proxying" |
| AWS Application Load Balancer | 60 seconds by default, range 1 to 4000 seconds. Clients or targets must send data before it ends. HTTP/2 `PING` frames do not reset it | AWS ELB documentation |
| AWS Network Load Balancer, TCP listener | 350 seconds by default, range 60 to 6000 seconds. A TCP keep-alive packet resets it. After the timeout, the next packet gets a TCP RST. A TLS listener has a fixed 350 seconds, and the balancer then sends TCP keep-alive packets itself every 20 seconds on both connections after it receives one | AWS ELB documentation |
| Linux connection tracking (`nf_conntrack`) | `nf_conntrack_tcp_timeout_established` is 432000 seconds (5 days) by default | Linux kernel documentation |

**Heartbeat rule.** Send a heartbeat at an interval of at most half the shortest idle timeout on the path. For a 60-second timeout, that means a ping every 30 seconds or less. The heartbeat has a second job: if the peer does not answer within a deadline, the sender closes the connection and reconnects (see the TCP keep-alive discussion in [TCP and UDP](03-tcp-and-udp.md)). A TCP keep-alive probe does not replace a WebSocket ping. The probe starts after 2 hours by default on Linux, and it covers only one TCP hop: a proxy that terminates the connection answers it for itself, so the probe never reaches the application on the other side. A WebSocket ping reaches the application.

**Upgrade headers at a proxy.** `Upgrade` and `Connection` are hop-by-hop headers (RFC 9110, section 7.6.1): a proxy removes them and does not forward them. A proxy must send them again for WebSocket. In Nginx that is `proxy_set_header Upgrade $http_upgrade;` and `proxy_set_header Connection "upgrade";`, and before Nginx 1.29.7, also `proxy_http_version 1.1;`. A WebSocket proxy also needs to handle a long-lived connection without buffering it. An SSE stream needs `proxy_buffering off` (or the `X-Accel-Buffering: no` response header). See the communication patterns page for the checklist.

**Sticky routing.** After the upgrade, the TCP connection stays on one backend, so a WebSocket needs no stickiness for its frames. Stickiness matters in two cases. First, a client library that falls back to HTTP long polling sends several requests for one session, and all of them must reach the server that holds the session state. Second, a reconnecting client may need the server that holds its state (for example, queued messages). Recommendation: keep the session state in a shared store, so that any gateway can serve a reconnect, and avoid stickiness.

**The connection-balancing problem.** A load balancer assigns a connection to a server when the connection opens. It does not move the connection later. Long-lived connections turn this into a problem:

1. After a deploy that restarts servers one by one, the first server that restarts drops its connections. These clients reconnect, and the load balancer sends them to the servers that still run. The servers that restart last get the connections of all clients that reconnected earlier, and the servers that restart first stay nearly empty. Clients do not move back.
2. After a gateway crashes, all of its clients reconnect at the same moment (a reconnect storm). The same happens after a network outage on the whole fleet.
3. A server that scales out receives no existing connections. Only new connections reach it.

Protocol-level mitigations:

- **Jitter on the client.** The client waits a random time before it reconnects, with exponential backoff, as RFC 6455 (section 7.2.3) advises.
- **Spread the close.** When the server drains, it closes connections at a controlled rate, each with close code 1001 (going away) and not all at once. A client that receives 1001 reconnects with jitter. The HLD [load balancing](../hld/fundamentals/02-load-balancing-dns-cdn.md) page describes draining and the close message.
- **Maximum connection age.** The server closes each connection after a random lifetime (for example, 1 hour plus or minus 10 percent). Clients reconnect and spread over the fleet again. For HTTP connections, AWS ALB does this with the "HTTP client keepalive duration" (default 3600 seconds, range 60 to 604800 seconds): it ends the connection with `Connection: close` or an HTTP/2 `GOAWAY` frame. Assumption: a WebSocket gateway implements the same idea itself with a close frame.
- **Balance by connection count.** Use a least-connections algorithm for new connections. It sends reconnecting clients to the emptiest server. It does not fix a burst that arrives at the same moment.
- **Rate-limit accepts.** A gateway accepts a limited number of new connections per second, because TLS handshakes cost CPU, and the client retries elsewhere.

## Trade-offs and When to Use

| Aspect | Long polling | SSE | WebSocket |
|---|---|---|---|
| Direction | Server to client (client to server with new requests) | Server to client | Both ways on one connection |
| Protocol on the wire | HTTP request and response | One HTTP response that does not end | HTTP/1.1 upgrade, then WebSocket frames |
| Message format | Any HTTP body | UTF-8 text events | Text (UTF-8) or binary frames |
| Per-message overhead | A full HTTP request and response | A few bytes of field names | 2 to 14 bytes of frame header |
| Reconnection | Next request, with a cursor in the application | Built in, with `Last-Event-ID` | None. The application builds it |
| Liveness check | The request timeout | Comment lines. The client sees a closed stream | Ping and pong |
| Works through proxies | Yes | Yes, if the proxy does not buffer the response | Only if the proxy forwards the upgrade |
| Browser API | `fetch` or `XMLHttpRequest` | `EventSource` | `WebSocket` |
| Headers on the request | Any | `EventSource` cannot set headers. A `fetch` stream can | Browser API cannot set headers |
| Load balancer fit | Normal request balancing | Long-lived connection | Long-lived connection, connection-balancing problem |

The frame header range of 2 to 14 bytes is the 2-byte base header, plus up to 8 bytes of extended length, plus 4 bytes of masking key (RFC 6455, section 5.2).

The decision rules (which technique for which requirement) are in the [communication patterns](../hld/fundamentals/09-communication-patterns.md) page. In short: choose SSE for one-way pushes, WebSocket for frequent two-way messages, and long polling when the network blocks both.

## Java Example

The first program shows the WebSocket mechanism over bytes. It computes `Sec-WebSocket-Accept` for the RFC 6455 sample key with `MessageDigest` (SHA-1) and `Base64`, and it prints whether the result matches the value in the RFC. Then it encodes a masked client frame and an unmasked server frame, decodes the client frame, shows the three length forms, and encodes a ping frame and a close frame. The masking key and the payload `Hello` come from RFC 6455 (section 5.7), so the client frame must equal the RFC bytes `81 85 37 fa 21 3d 7f 9f 4d 51 58`. The program opens no socket.

```java
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class WsFrames {
    static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    static final int TEXT = 0x1;
    static final int CLOSE = 0x8;
    static final int PING = 0x9;

    // Sec-WebSocket-Accept = Base64(SHA-1(key + GUID)), RFC 6455 section 4.2.2
    static String accept(String key) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha1.digest((key + GUID).getBytes(StandardCharsets.US_ASCII));
        return Base64.getEncoder().encodeToString(digest);
    }

    // Encodes one unfragmented frame. A null mask key means "server to client": no mask.
    static byte[] encode(int opcode, byte[] payload, byte[] maskKey) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x80 | opcode); // FIN = 1, RSV1-3 = 0
        int maskBit = maskKey == null ? 0 : 0x80;
        int len = payload.length;
        if (len <= 125) {
            out.write(maskBit | len);
        } else if (len <= 0xFFFF) {
            out.write(maskBit | 126);
            out.write(len >>> 8);
            out.write(len & 0xFF);
        } else {
            out.write(maskBit | 127);
            for (int shift = 56; shift >= 0; shift -= 8) {
                out.write((int) (((long) len >>> shift) & 0xFF));
            }
        }
        if (maskKey != null) {
            out.write(maskKey, 0, 4);
            for (int i = 0; i < len; i++) {
                out.write(payload[i] ^ maskKey[i % 4]); // same XOR masks and unmasks
            }
        } else {
            out.write(payload, 0, len);
        }
        return out.toByteArray();
    }

    record Frame(boolean fin, int opcode, boolean masked, byte[] payload) {}

    static Frame decode(byte[] b) {
        int pos = 0;
        int b0 = b[pos++] & 0xFF;
        int b1 = b[pos++] & 0xFF;
        boolean masked = (b1 & 0x80) != 0;
        long len = b1 & 0x7F;
        if (len == 126) {
            len = ((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF);
            pos += 2;
        } else if (len == 127) {
            len = 0;
            for (int i = 0; i < 8; i++) {
                len = (len << 8) | (b[pos + i] & 0xFF);
            }
            pos += 8;
        }
        byte[] key = new byte[4];
        if (masked) {
            System.arraycopy(b, pos, key, 0, 4);
            pos += 4;
        }
        byte[] payload = new byte[(int) len];
        for (int i = 0; i < len; i++) {
            payload[i] = masked ? (byte) (b[pos + i] ^ key[i % 4]) : b[pos + i];
        }
        return new Frame((b0 & 0x80) != 0, b0 & 0x0F, masked, payload);
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x ", x & 0xFF));
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) throws Exception {
        String key = "dGhlIHNhbXBsZSBub25jZQ==";
        String expected = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";
        String computed = accept(key);
        System.out.println("Sec-WebSocket-Accept = " + computed);
        System.out.println("matches RFC 6455 value: " + computed.equals(expected));

        byte[] mask = {0x37, (byte) 0xfa, 0x21, 0x3d}; // masking key from RFC 6455 section 5.7
        byte[] hello = "Hello".getBytes(StandardCharsets.UTF_8);
        byte[] masked = encode(TEXT, hello, mask);
        byte[] plain = encode(TEXT, hello, null);
        System.out.println("client frame: " + hex(masked));
        System.out.println("server frame: " + hex(plain));

        Frame f = decode(masked);
        String text = new String(f.payload(), StandardCharsets.UTF_8);
        System.out.println("decoded: fin=" + f.fin() + " opcode=" + f.opcode()
                + " masked=" + f.masked() + " text=" + text);

        byte[] medium = new byte[300]; // 126 form: 16-bit length
        Arrays.fill(medium, (byte) 'a');
        byte[] mediumFrame = encode(TEXT, medium, mask);
        byte[] mediumHead = Arrays.copyOf(mediumFrame, 8);
        System.out.println("300-byte payload: header=" + hex(mediumHead)
                + " total=" + mediumFrame.length
                + " decoded=" + decode(mediumFrame).payload().length);

        byte[] big = new byte[70000]; // 127 form: 64-bit length
        byte[] bigFrame = encode(TEXT, big, null);
        byte[] bigHead = Arrays.copyOf(bigFrame, 10);
        System.out.println("70000-byte payload: header=" + hex(bigHead)
                + " total=" + bigFrame.length);

        byte[] ping = encode(PING, "hb".getBytes(StandardCharsets.UTF_8), mask);
        System.out.println("ping frame: " + hex(ping));

        byte[] reason = "bye".getBytes(StandardCharsets.UTF_8);
        byte[] closeBody = new byte[2 + reason.length];
        closeBody[0] = (byte) (1000 >> 8);
        closeBody[1] = (byte) (1000 & 0xFF);
        System.arraycopy(reason, 0, closeBody, 2, reason.length);
        byte[] close = encode(CLOSE, closeBody, null);
        Frame cf = decode(close);
        int code = ((cf.payload()[0] & 0xFF) << 8) | (cf.payload()[1] & 0xFF);
        System.out.println("close frame: " + hex(close));
        System.out.println("close code=" + code + " reason="
                + new String(cf.payload(), 2, cf.payload().length - 2, StandardCharsets.UTF_8));
    }
}
```

Expected output:

<!-- expected-output -->
```text
Sec-WebSocket-Accept = s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
matches RFC 6455 value: true
client frame: 81 85 37 fa 21 3d 7f 9f 4d 51 58
server frame: 81 05 48 65 6c 6c 6f
decoded: fin=true opcode=1 masked=true text=Hello
300-byte payload: header=81 fe 01 2c 37 fa 21 3d total=308 decoded=300
70000-byte payload: header=81 7f 00 00 00 00 00 01 11 70 total=70010
ping frame: 89 82 37 fa 21 3d 5f 98
close frame: 88 05 03 e8 62 79 65
close code=1000 reason=bye
```

Read the output in this order:

- The computed accept value equals `s3pPLMBiTxaQ9kYGzzhZRbK+xOo=`, the value in the RFC.
- The client frame has `81` (FIN = 1, opcode text) and `85` (MASK = 1, length 5), then the 4-byte key, then the 5 masked bytes. The server frame has `05` as the second byte (MASK = 0) and the plain text bytes `48 65 6c 6c 6f`.
- A payload of 300 bytes uses the 16-bit form: the length byte is `fe` (MASK = 1 and the value 126), followed by `01 2c` (300). A payload of 70,000 bytes uses the 64-bit form: `7f` (127), followed by 8 bytes (`00 00 00 00 00 01 11 70`).
- The ping frame has opcode `9` (`89`). The close frame has opcode `8` (`88`), the code `03 e8` (1000), and the reason `bye`.

The second program is a Server-Sent Events parser. It follows the dispatch rules of the HTML Standard: the `id` buffer is copied when the blank line arrives, an incomplete event at the end of the stream is dropped, and `retry` accepts only digits. The first call feeds a stream that a dropped connection cuts in the middle of event 19. The second call feeds what the server sends after the reconnect. The program uses strings only and opens no socket.

```java
import java.util.ArrayList;
import java.util.List;

public class SseParse {
    record Event(String type, String data, String lastEventId) {}

    // State that survives a dropped connection: the last event ID and the reconnection time.
    // The ID in an event that was cut off is not committed, so it is not sent on reconnect.
    static class Parser {
        String lastEventId = "";   // the event source's last event ID string
        private String idBuffer = ""; // the last event ID buffer
        long retryMillis = 3000; // implementation-defined start value in this model
        private StringBuilder data = new StringBuilder();
        private String type = "";

        List<Event> feed(String stream) {
            List<Event> events = new ArrayList<>();
            // Lines end with LF, CR, or CRLF. This model normalizes CRLF and CR to LF first.
            String text = stream.replace("\r\n", "\n").replace('\r', '\n');
            int start = 0;
            while (true) {
                int nl = text.indexOf('\n', start);
                if (nl < 0) {
                    break; // an incomplete last line is dropped, like an incomplete event
                }
                String line = text.substring(start, nl);
                start = nl + 1;
                if (line.isEmpty()) {
                    lastEventId = idBuffer; // dispatch step 1: commit the ID, even if data is empty
                    if (data.length() > 0) { // dispatch step 2: an empty data buffer sends nothing
                        String d = data.substring(0, data.length() - 1); // drop the final LF
                        events.add(new Event(type.isEmpty() ? "message" : type, d, lastEventId));
                    }
                    data = new StringBuilder();
                    type = "";
                } else if (line.charAt(0) != ':') { // a line that starts with ':' is a comment
                    int colon = line.indexOf(':');
                    String name = colon < 0 ? line : line.substring(0, colon);
                    String value = colon < 0 ? "" : line.substring(colon + 1);
                    if (value.startsWith(" ")) {
                        value = value.substring(1); // remove one leading space only
                    }
                    switch (name) {
                        case "data" -> data.append(value).append('\n');
                        case "event" -> type = value;
                        case "id" -> {
                            if (value.indexOf('\0') < 0) {
                                idBuffer = value; // not committed until the blank line
                            }
                        }
                        case "retry" -> {
                            boolean digits = !value.isEmpty()
                                    && value.chars().allMatch(c -> c >= '0' && c <= '9');
                            if (digits) {
                                retryMillis = Long.parseLong(value);
                            }
                        }
                        default -> { } // unknown fields are ignored
                    }
                }
            }
            return events;
        }
    }

    public static void main(String[] args) {
        Parser p = new Parser();
        String first = "retry: 5000\n\n"
                + ": keep-alive comment\n\n"
                + "id: 17\nevent: order\ndata: {\"id\":42}\n\n"
                + "id: 18\ndata: line one\ndata: line two\n\n"
                + "id: 19\ndata: cut off by a dropped conne";
        for (Event e : p.feed(first)) {
            System.out.println("event type=" + e.type() + " id=" + e.lastEventId()
                    + " data=" + e.data().replace("\n", "\\n"));
        }
        System.out.println("after drop: Last-Event-ID=" + p.lastEventId
                + " retry=" + p.retryMillis + " ms");
        System.out.println("reconnect request header: Last-Event-ID: " + p.lastEventId);

        // The server replays what follows ID 18. Event 19 was cut off, so it arrives again in full.
        String second = "id: 19\ndata: replayed in full\n\nid: 20\r\ndata: after reconnect\r\n\r\n";
        for (Event e : p.feed(second)) {
            System.out.println("event type=" + e.type() + " id=" + e.lastEventId()
                    + " data=" + e.data());
        }
        System.out.println("final Last-Event-ID=" + p.lastEventId);
    }
}
```

Expected output:

<!-- expected-output -->
```text
event type=order id=17 data={"id":42}
event type=message id=18 data=line one\nline two
after drop: Last-Event-ID=18 retry=5000 ms
reconnect request header: Last-Event-ID: 18
event type=message id=19 data=replayed in full
event type=message id=20 data=after reconnect
final Last-Event-ID=20
```

The `Last-Event-ID` after the drop is `18`, because event 19 never reached its blank line. The server replays event 19 in full after the reconnect.

The class below uses the JDK 21 `java.net.http.WebSocket` API. **It has no `main` method and the runner does not run it**, because it needs a server. Facts from the javadoc: the listener receives data only after it calls `request(n)` (n counts calls of receive methods such as `onText`, not whole messages), so a slow consumer applies back pressure to the connection. The JDK answers a ping with a pong and answers a close with a close by itself. `sendPing` and `sendPong` take at most 125 bytes. `sendClose` takes a reason of at most 123 bytes in UTF-8. The client does not reconnect by itself, so the application code must do that, with backoff and jitter.

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

// This class has no main method. The runner compiles it and does not run it.
public class WsClient {
    private final AtomicInteger received = new AtomicInteger();
    private final StringBuilder partial = new StringBuilder();

    CompletableFuture<WebSocket> connect(String url) {
        HttpClient client = HttpClient.newHttpClient();
        return client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .subprotocols("chat.v1")
                .buildAsync(URI.create(url), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket ws) {
                        ws.request(1); // demand: allow one more receive call (onText, onPing, ...)
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data,
                                                     boolean last) {
                        partial.append(data); // a message can arrive in several parts
                        if (last) {
                            received.incrementAndGet();
                            partial.setLength(0);
                        }
                        ws.request(1); // ask for the next part or message
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
                        ws.request(1); // the JDK answers the ping with a pong by itself
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                        // The JDK sends the reply Close frame. Reconnect here, with jitter.
                        return null;
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        // 1006-style failure: no Close frame arrived. Reconnect with backoff.
                    }
                });
    }

    CompletableFuture<WebSocket> heartbeat(WebSocket ws) {
        return ws.sendPing(ByteBuffer.wrap(new byte[] {'h', 'b'})); // payload is at most 125 bytes
    }

    CompletableFuture<WebSocket> close(WebSocket ws) {
        return ws.sendClose(WebSocket.NORMAL_CLOSURE, "done"); // reason is at most 123 UTF-8 bytes
    }

    int received() {
        return received.get();
    }
}
```

## Interview Angle

### Questions

??? question "How does the WebSocket handshake work? What does `Sec-WebSocket-Accept` prove?"

    The client sends an HTTP/1.1 `GET` request with `Upgrade: websocket`, `Connection: Upgrade`, `Sec-WebSocket-Version: 13`, and a `Sec-WebSocket-Key` that holds 16 random bytes in Base64. The server answers `101 Switching Protocols` with `Upgrade`, `Connection`, and `Sec-WebSocket-Accept`. After the blank line, the same TCP connection carries WebSocket frames.

    The server computes the accept value as `Base64(SHA-1(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"))`. It proves that the server read this handshake and speaks WebSocket. A plain HTTP server does not know the GUID, so it does not return a matching value by accident. A page script cannot set `Sec-WebSocket-*` headers, so it cannot fake the handshake. It is not authentication and it does not give confidentiality. The security of the data comes from `wss` (TLS). The security of who connects comes from checking `Origin` and the credentials in the handshake.

??? question "Why does a WebSocket client mask its frames? Is the data encrypted?"

    No. The masking key is in the frame header, so anyone who sees the frame can unmask it. Masking is a defense for intermediaries. Without it, a script in a browser could choose the bytes that go on the wire, for example bytes that look like an HTTP request. An old proxy that treats the connection as a stream of HTTP messages could take those bytes for a request and cache a wrong response for a real URL. A fresh unpredictable 32-bit key for each frame stops the script from controlling the wire bytes. Servers do not mask, because the attack comes from client code. A server must close the connection on an unmasked client frame. To protect the data from eavesdroppers, use `wss`.

??? question "Describe the WebSocket frame format. How is the payload length encoded?"

    A frame starts with 2 bytes: FIN (1 bit), RSV1 to RSV3 (3 bits, zero unless an extension uses them), the opcode (4 bits), the MASK bit, and a 7-bit length. Opcodes: `0` continuation, `1` text, `2` binary, `8` close, `9` ping, `10` pong. If the 7-bit value is 0 to 125, it is the length. If it is 126, the next 2 bytes hold the length. If it is 127, the next 8 bytes hold it. The shortest form is required. If MASK is 1, a 4-byte masking key follows, and the receiver XORs payload byte `i` with key byte `i mod 4`. So the overhead is 2 to 14 bytes for each frame, and a short server frame costs 2 bytes. Control frames have at most 125 bytes and cannot be fragmented. A fragmented message uses FIN = 0 on all frames but the last, with opcode 0 on all frames after the first.

??? question "A WebSocket connection drops every 60 seconds behind a proxy. Why, and how do you fix it?"

    An idle timeout in a device on the path closes the connection after 60 seconds without bytes. Nginx closes a proxied connection after 60 seconds without data from the backend (`proxy_read_timeout`), and an AWS Application Load Balancer has a default idle timeout of 60 seconds. If the application sends nothing for that time, the device drops the connection, and the client sees close code 1006.

    The fix has two parts. First, send a heartbeat, a ping frame from the server or an application message from the client, at half of the shortest timeout or less. For a 60-second timeout, send every 30 seconds. Second, if you control the proxy, raise the timeout for the WebSocket route. Also check that the proxy forwards `Upgrade` and `Connection`, because the failure looks different without them (the upgrade does not complete). The server closes a connection when the peer does not answer a ping within a deadline, and the client reconnects with backoff and jitter.

??? question "How does an SSE client resume after a disconnect? What must the server do?"

    The browser `EventSource` reconnects by itself after the delay set by `retry` (its start value depends on the browser). The server marks events with `id:` fields. The browser stores the last ID that it completed, which means the ID of an event that ended with a blank line, and sends it in the `Last-Event-ID` request header on the reconnect. The server reads the header and replays the events after that ID. For that, the server must keep recent events in a store with identifiers that follow the order of events, for example a stream or a log with offsets. If the server cannot replay, it sends the current state first. To stop clients from reconnecting, the server answers `204 No Content`. The server also sends a comment line about every 15 seconds so that proxies do not drop an idle stream, and it turns off proxy buffering for the route.

??? question "You deploy a new version of a WebSocket gateway fleet with a rolling restart. Why do the servers end with unequal load, and what do you do?"

    A connection stays on the server that accepted it. When a server restarts, its clients reconnect to the servers that still run, so the load balancer sends them to servers that do not restart yet. The servers that restart last receive the clients of all earlier servers, and the servers that restarted first have few connections. New connections go to whichever server the algorithm picks, and old connections never move.

    Mitigations: (1) The server drains: it stops accepting new connections and closes the existing ones at a controlled rate with code 1001, and clients reconnect with jitter. (2) Clients use exponential backoff with random jitter, so the reconnects do not arrive at the same moment (RFC 6455, section 7.2.3). (3) The balancer sends new connections to the server with the fewest connections. (4) The server sets a maximum connection age with random spread, so the fleet rebalances itself over time. (5) After the deploy finishes, the operator can close a share of connections on the busiest servers.

??? question "How does WebSocket work over HTTP/2?"

    A `101` response cannot switch one stream of a multiplexed connection, so RFC 8441 uses Extended CONNECT. The server sends the setting `SETTINGS_ENABLE_CONNECT_PROTOCOL = 1`. The client sends a `CONNECT` request with `:protocol = websocket`, plus `:scheme`, `:path`, and `:authority`. It sends no `Upgrade` or `Sec-WebSocket-Key`. The server answers with a `2xx` status, and the stream then carries WebSocket frames in `DATA` frames, as if it were a TCP connection. Many WebSocket connections share one TCP and TLS connection with other requests. RFC 9220 does the same on HTTP/3 with QUIC streams. A proxy in the path must support Extended CONNECT.

??? question "What is long polling at the protocol level, and what goes wrong with it?"

    The client sends a normal request with a cursor (`since=412`). If the server has no new data, it holds the request and answers when an event arrives or when a hold timeout ends. The client then sends the next request at once. The problems: the hold time must be shorter than every idle timeout on the path and than the client timeout, each event costs a full request with headers, events that arrive between two requests must wait (so the server keeps them by cursor), and the server must hold many open requests, which needs an asynchronous server stack. It works through any HTTP proxy, which is its one advantage.

### Follow-up Questions

- "How does a server detect that a client vanished without a close frame?" (A ping with a deadline for the pong. TCP keep-alive starts after 2 hours by default on Linux, which is too late. See [TCP and UDP](03-tcp-and-udp.md).)
- "What close code does a browser report when the Wi-Fi drops?" (1006. No close frame arrived, and 1006 never travels on the wire.)
- "Why can a browser not send a ping frame?" (The browser `WebSocket` API has no ping method. A browser still answers a server ping with a pong.)
- "What is cross-site WebSocket hijacking and what stops it?" (A malicious page opens a WebSocket to your server, and the browser attaches the user's cookies. The server must check `Origin`, or use a token that a malicious page cannot read.)
- "How does `permessage-deflate` change the frames?" (A negotiated extension. It sets RSV1 on the first frame of a compressed message. RFC 7692 defines it.)
- "Why does SSE need a keep-alive comment?" (To keep proxies and load balancers from closing an idle stream, and to let the client find a dead connection.)

### Red Flags

- "WebSocket is faster because it is a different protocol." (The main saving is the missing request headers and the open connection. TCP, TLS, and the network are the same.)
- "Masking makes WebSocket secure." (It does not. TLS does.)
- "I use WebSocket for every push." (SSE is simpler for one-way data and reconnects by itself.)
- "A WebSocket needs no heartbeat because TCP keeps the connection open." (Proxies and NAT devices drop idle connections, and TCP keep-alive is off by default.)
- "The load balancer balances WebSocket connections evenly over time." (It balances only at connect time.)
- No reconnect plan, or a reconnect without jitter.

## Common Mistakes

| Mistake | Why it hurts | Fix |
|---|---|---|
| No heartbeat on an idle WebSocket | A proxy or load balancer closes the connection after its idle timeout. The client sees 1006 | Send a ping at half of the shortest timeout, and close the connection if the pong does not arrive |
| Proxy drops `Upgrade` and `Connection` | The handshake fails or the backend answers `200` | Pass both headers explicitly. Check the HTTP version that the proxy uses to the backend |
| Reconnect with no delay | After a server restart, every client reconnects at once and overloads the fleet | Exponential backoff with random jitter |
| No `Origin` check on a cookie-authenticated WebSocket | Cross-site WebSocket hijacking | Check `Origin` against a list. Answer `403` for other values |
| Proxy buffering of an SSE stream | The client sees no events until the buffer fills | Turn buffering off for the route, or send `X-Accel-Buffering: no` |
| An SSE server that cannot replay after `Last-Event-ID` | The client loses events after each reconnect | Keep recent events with ordered IDs. Replay from the ID |
| A token in the WebSocket URL with a long life | The URL appears in access logs and proxy logs | A token that expires in seconds, or authenticate with the first message |
| An SSE or long-polling response compressed by a proxy | The proxy waits for more bytes before it sends | Exclude `text/event-stream` from compression |
| Using `1006` in `sendClose` | The API does not allow it (JDK `IllegalArgumentException`), and the RFC reserves it | Use `1000`, `1001`, `1008`, `1011`, or a code in 4000 to 4999 |
| Counting on ping from a browser client | The browser API has no ping | Send an application heartbeat message |

## Commonly Confused With

| Looks like | Difference | Deciding question |
|---|---|---|
| WebSocket and raw TCP | WebSocket adds an HTTP handshake, message framing, ping and pong, and a close handshake on top of TCP | Do I need messages with boundaries and a browser client? |
| WebSocket ping and TCP keep-alive | A ping is an application-level frame that reaches the peer application. TCP keep-alive is a kernel probe, off by default, and proxies can hide it | Who answers: the application or the kernel? |
| SSE and long polling | SSE keeps one response open and sends many events. Long polling answers and closes the request for each batch | Does the server close the response after each event? |
| `Last-Event-ID` and a WebSocket "last message ID" | `Last-Event-ID` is part of the SSE standard, and the browser sends it. The WebSocket last message ID is an application convention | Does the protocol or the application define the resume? |
| Close code 1001 and 1006 | 1001 is sent in a close frame (the endpoint leaves). 1006 is never sent: an API reports it when the connection ends with no close frame | Did a close frame arrive? |
| WebSocket masking and encryption | Masking uses a key that travels in the frame. Encryption (TLS) uses a secret key | Can an eavesdropper read the key? |
| HTTP/1.1 `Upgrade` and HTTP/2 Extended CONNECT | `Upgrade` switches the whole connection with a `101`. Extended CONNECT opens one stream as a WebSocket and leaves the connection in HTTP/2 | Does the connection carry other requests? |
| The `101` response and `CONNECT` tunnel | `101` switches the protocol with the origin server. A `2xx` to `CONNECT` opens a blind tunnel through a proxy ([Proxies and NAT](09-proxies-and-nat.md)) | Is the other end a proxy or the origin server? |

## Quick Reference Card

- **Handshake:** `GET` with `Upgrade: websocket`, `Connection: Upgrade`, `Sec-WebSocket-Key`, `Sec-WebSocket-Version: 13`. Answer `101` with `Sec-WebSocket-Accept = Base64(SHA-1(key + GUID))`. GUID: `258EAFA5-E914-47DA-95CA-C5AB0DC85B11`.
- **Frame:** FIN, RSV1-3, opcode (4 bits), MASK, length (7 bits, 126 means 16 bits, 127 means 64 bits), optional 4-byte key, payload.
- **Opcodes:** `0` continuation, `1` text, `2` binary, `8` close, `9` ping, `A` pong. Control frames have at most 125 bytes.
- **Masking:** client to server only, a new 32-bit key for each frame, XOR with `key[i mod 4]`. It protects intermediaries, not data.
- **Close codes:** 1000 normal, 1001 going away, 1002 protocol error, 1008 policy, 1009 too big, 1011 internal error. 1005, 1006, and 1015 never go on the wire.
- **SSE:** `text/event-stream`. Fields `data`, `event`, `id`, `retry`. A blank line ends an event. A comment starts with `:`. `Last-Event-ID` on reconnect. `204` stops the reconnect.
- **Proxies:** idle timeouts of 60 seconds (Nginx, ALB) and 350 seconds (NLB TCP). Heartbeat at half of the timeout. Pass `Upgrade` and `Connection`. Turn off buffering for SSE.
- **Balancing:** connections do not move. Use drain with 1001, jitter, a maximum age, and shared session state.
- **HTTP/2 and HTTP/3:** Extended CONNECT with `:protocol = websocket` (RFC 8441, RFC 9220).
- **Java:** `java.net.http.WebSocket`: `request(n)` for demand, `sendPing` up to 125 bytes, `sendClose` reason up to 123 bytes.

## Further Reading

- [RFC 6455, The WebSocket Protocol](https://www.rfc-editor.org/rfc/rfc6455): the handshake (sections 1.3, 4), frames (section 5), close codes (section 7.4), and the security considerations (section 10).
- [RFC 8441, Bootstrapping WebSockets with HTTP/2](https://www.rfc-editor.org/rfc/rfc8441): Extended CONNECT and `SETTINGS_ENABLE_CONNECT_PROTOCOL`.
- [RFC 9220, Bootstrapping WebSockets with HTTP/3](https://www.rfc-editor.org/rfc/rfc9220): the same mechanism for HTTP/3.
- [RFC 7692, Compression Extensions for WebSocket](https://www.rfc-editor.org/rfc/rfc7692): `permessage-deflate`.
- [RFC 6202, Known Issues and Best Practices for the Use of Long Polling and Streaming in Bidirectional HTTP](https://www.rfc-editor.org/rfc/rfc6202): the problems of long polling, from the time before WebSocket.
- [WHATWG HTML Standard, Server-sent events](https://html.spec.whatwg.org/multipage/server-sent-events.html): the event stream grammar, the parsing and dispatch steps, reconnection, and `Last-Event-ID`.
- [RFC 9110, HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110): the `Upgrade` header (section 7.8) and the `Connection` header (section 7.6.1).
- [JDK 21 `java.net.http.WebSocket`](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/WebSocket.html): the client API, with its demand model and size limits.
- [Nginx, WebSocket proxying](https://nginx.org/en/docs/http/websocket.html): the headers, the HTTP version, and the 60-second read timeout.
- [AWS, Application Load Balancer attributes](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/edit-load-balancer-attributes.html) and [Network Load Balancer idle timeout](https://docs.aws.amazon.com/elasticloadbalancing/latest/network/network-load-balancers.html#connection-idle-timeout): the idle timeout defaults and ranges.
- [Linux kernel, nf_conntrack sysctl](https://docs.kernel.org/networking/nf_conntrack-sysctl.html): the default timeout of an established connection in the connection tracker.
- Related pages: [communication patterns](../hld/fundamentals/09-communication-patterns.md), [load balancing, DNS, and CDN](../hld/fundamentals/02-load-balancing-dns-cdn.md), [chat system](../hld/case-studies/06-chat-system.md), [HTTP](04-http.md), [TCP and UDP](03-tcp-and-udp.md), [TLS and HTTPS](05-tls-and-https.md), [Proxies and NAT](09-proxies-and-nat.md).
