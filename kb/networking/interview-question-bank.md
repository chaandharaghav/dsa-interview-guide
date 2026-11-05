---
priority: P1
---
# Networking Interview Question Bank

This page collects the questions that interviewers ask most often about the networking pages of this knowledge base. The questions follow the page order: OSI and TCP/IP models, IP addressing and routing, TCP and UDP, HTTP, TLS and HTTPS, DNS, what happens when you load a URL, WebSockets and real-time protocols, and proxies and NAT. The pages on TCP, HTTP, TLS, DNS, and the URL walk have the most questions, because interviews test them most. Use the page in two steps. First, read the question and answer it aloud, as you would in the interview, and say the reason as well as the result. Second, open the block and compare your answer with the model answer. Each model answer is short. It gives the direct answer, then the reason, then the trade-off or the edge case, and it ends with a link to the section that explains the topic in depth. If your answer missed a point, read that section before you move on. Many answers keep the condition that the page states (the operating system, the browser, the JDK version), because a correct answer under one condition is wrong under another.

## Contents

- [OSI and TCP/IP Models](#osi-and-tcpip-models)
- [IP Addressing and Routing](#ip-addressing-and-routing)
- [TCP and UDP](#tcp-and-udp)
- [HTTP](#http)
- [TLS and HTTPS](#tls-and-https)
- [DNS](#dns)
- [What Happens When You Load a URL](#what-happens-when-you-load-a-url)
- [WebSockets and Real-Time Protocols](#websockets-and-real-time-protocols)
- [Proxies and NAT](#proxies-and-nat)

## OSI and TCP/IP Models

??? question "What do L4 and L7 mean, and how does an L4 load balancer differ from an L7 load balancer?"

    The numbers are OSI layers: layer 4 is the transport layer (TCP, UDP, ports) and layer 7 is the application layer (HTTP). An L4 balancer reads the IP addresses, the ports, and the protocol, and it sends a whole connection to one backend without reading HTTP bytes. An L7 balancer terminates the client connection, reads each request, and picks a backend by host, path, or header. L4 works for any TCP or UDP protocol, but a long-lived HTTP/2 or gRPC connection stays on one backend behind it. See [OSI and TCP/IP Models](01-osi-and-tcp-ip-models.md#the-osi-model).

??? question "At which layer do a switch, a router, and a firewall work, and what does each one read?"

    A switch works at layer 2 and forwards frames by the destination MAC address. A router works at layer 3: it reads the destination IP address, picks the next hop by longest prefix match, decrements the TTL, and builds a new Ethernet header for the next link. So the MAC addresses change at every hop, and the IP addresses stay the same unless a NAT device rewrites them. A stateful firewall works at layers 3 and 4, and many real devices do several jobs. See [OSI and TCP/IP Models](01-osi-and-tcp-ip-models.md#where-devices-work).

??? question "What is the MTU, and what happens when a packet is larger than the MTU of a link?"

    The MTU is the largest IP packet that one link carries (1500 bytes for standard Ethernet), and the path MTU is the smallest MTU on the path. In IPv4, a router fragments the packet if the DF bit is 0. If DF is 1, the router drops it and returns an ICMP "fragmentation needed" message (type 3, code 4). In IPv6, routers never fragment: they drop the packet and send a Packet Too Big message, and only the source fragments. Fragmentation costs because one lost fragment loses the whole datagram, and a firewall cannot read the ports in later fragments, so TCP avoids it with the MSS. See [OSI and TCP/IP Models](01-osi-and-tcp-ip-models.md#mtu-fragmentation-and-path-mtu-discovery).

??? question "A service works for small requests but hangs on large responses, and `ping` works. What do you check?"

    I suspect a path MTU black hole. The TCP handshake and small packets pass, but full-size segments are dropped on a link with a smaller MTU, and a firewall blocks the ICMP message that would tell the sender. `ping` works because its default packet is small. I confirm with `ping -M do -s 1472 host` on Linux and lower the size until the reply arrives. The fixes are to allow ICMP type 3 code 4 (IPv4) and Packet Too Big (IPv6), clamp the MSS, lower the MTU before the tunnel, or enable `tcp_mtu_probing`, and an overlay network or VPN is the usual cause in a cluster. See [OSI and TCP/IP Models](01-osi-and-tcp-ip-models.md#mtu-fragmentation-and-path-mtu-discovery).

??? question "How does a host find the MAC address of the machine it sends an IPv4 packet to? What changes if the destination is in another network?"

    The host compares the destination with its own address and subnet mask. If the destination is in the same subnet, the host needs the destination's MAC address. If not, the host sends the frame to its default gateway, so it needs the gateway's MAC address. If the ARP cache has no entry, the host broadcasts an ARP request and the owner answers with a unicast reply. ARP has no authentication, so a host on the same link can send false replies (ARP spoofing), and IPv6 uses Neighbor Discovery instead. See [OSI and TCP/IP Models](01-osi-and-tcp-ip-models.md#ethernet-and-arp-in-brief).

## IP Addressing and Routing

??? question "How many usable hosts does a /26 have? Give the network and broadcast address of 192.168.10.77/26."

    A /26 has 2^(32 - 26) = 64 addresses, and 62 are usable because the network and broadcast addresses are not given to hosts. The mask is 255.255.255.192, so the block size in the last octet is 64 and the blocks start at 0, 64, 128, and 192. The address ends in 77, which lies in the block that starts at 64, so the network address is 192.168.10.64 and the broadcast address is 192.168.10.127. Edge cases: a /31 has two usable addresses on a point-to-point link (RFC 3021), a /32 is one host, and cloud providers reserve extra addresses in each subnet. See [IP Addressing and Routing](02-ip-addressing-and-routing.md#subnets-and-cidr-worked-calculations).

??? question "How does a router choose among several routes that match a destination?"

    It uses longest prefix match: among the entries whose prefix matches, it picks the one with the most matching bits. The default route `0.0.0.0/0` has length 0, so it wins only when nothing else matches. With `10.20.0.0/16` via B and `10.20.30.0/24` via C, a packet to 10.20.30.40 goes to C and a packet to 10.20.99.1 goes to B. The same rule makes a BGP hijack work: an announcement of a more specific prefix attracts traffic even though the legitimate larger block is still announced. See [IP Addressing and Routing](02-ip-addressing-and-routing.md#how-a-packet-is-routed).

??? question "Why does IPv6 not need NAT, and does that remove the need for a firewall?"

    NAT exists because IPv4 has too few addresses, so many hosts share one public address. IPv6 gives each host a globally unique address (a /64 holds 2^64 interface identifiers), so nothing needs sharing and every host is reachable end to end. The firewall is still required, because NAT drops unsolicited inbound packets only as a side effect of the missing mapping, and a stateful firewall at the edge does that job on purpose. The cost is a second address family to test and monitor during dual stack. See [IP Addressing and Routing](02-ip-addressing-and-routing.md#ipv6).

??? question "How does `traceroute` work, and why do some hops show `* * *`?"

    `traceroute` sends probes with TTL 1, 2, 3, and so on. A router that receives a packet with TTL 1 decrements it to 0, drops it, and sends an ICMP Time Exceeded message (type 11), so probe number n finds the router at distance n. The trace ends when the destination answers: with the default UDP probes to a high port, it sends ICMP port unreachable. A `*` means that no answer arrived before the timeout, not that the router is dead: it may drop probes or send no ICMP errors, and the return path can differ from the forward path. See [IP Addressing and Routing](02-ip-addressing-and-routing.md#icmp-ping-and-traceroute).

??? question "What is BGP, and why did the Facebook outage in October 2021 take the whole company offline?"

    BGP is the protocol that autonomous systems use to tell each other which prefixes they can reach, and it chooses routes by policy first and then by the shortest AS path. A network is reachable from the Internet only while someone announces its prefix. A faulty maintenance command disconnected the backbone, and the DNS servers withdrew their BGP announcements because they could not reach the data centers, so the rest of the Internet could not find them. Repair was slow because the internal tools and remote access depended on the same network. The lessons: a health check that withdraws a route can turn a partial failure into a total one, and the repair path must not depend on the system it repairs. See [IP Addressing and Routing](02-ip-addressing-and-routing.md#bgp-and-autonomous-systems).

## TCP and UDP

??? question "Explain the TCP three-way handshake. Why does it need three messages?"

    The client sends a SYN with its initial sequence number (ISN), the server answers with a SYN+ACK that acknowledges the client ISN plus one and carries its own ISN, and the client sends an ACK of the server ISN plus one. Each side must announce its ISN and receive an acknowledgment of it, and the server joins its ACK and its SYN in one segment, so three messages are the minimum. The handshake also stops a delayed duplicate SYN from opening a connection that nobody wants: the client sees an acknowledgment for a SYN that it did not send and answers with an RST. The cost is one RTT before the first request reaches the server (about 0.5 ms in one data center, about 150 ms between California and the Netherlands), which a connection pool pays only once. See [TCP and UDP](03-tcp-and-udp.md#the-three-way-handshake).

??? question "A server shows 30,000 sockets in TIME_WAIT. Is that a problem?"

    First find out which side holds them, because TIME_WAIT lives on the side that closed first. On a server that closes each connection after the response, it costs memory only, since each client has its own 4-tuple. On a client that opens many short connections to one destination, it is a problem: Linux compiles in 60 seconds for TIME_WAIT (the RFC wait is 2 x MSL, which is 4 minutes), and the 28,232 ephemeral ports allow about 470 new connections per second, with the symptom `Cannot assign requested address`. Fix it in this order: reuse connections with keep-alive and a pool, widen the port range, or set `tcp_tw_reuse` for outgoing connections with TCP timestamps on, where the default 2 covers loopback only and `tcp_tw_recycle` is gone since Linux 4.12. See [TCP and UDP](03-tcp-and-udp.md#time_wait).

??? question "What does it mean when a service has thousands of sockets in CLOSE_WAIT?"

    The peer sent a FIN, the kernel acknowledged it, and the local application has not called `close`. No timer ends this state, so a large count is an application leak and not a kernel problem. Typical causes in Java are a missing `close` on an error path, a thread that blocks before its `close`, and a pool that keeps connections that the server closed. The fix is in the code (`try`-with-resources and a pool that validates connections), unlike TIME_WAIT, which the kernel handles. See [TCP and UDP](03-tcp-and-udp.md#close_wait).

??? question "What is the difference between flow control and congestion control?"

    Flow control protects the receiver. The receiver writes its free buffer space into the window field of each ACK, and the sender keeps no more unacknowledged data than that (the receive window). Congestion control protects the network: the sender keeps its own congestion window (cwnd), grows it when ACKs arrive, and cuts it when it detects loss. The sender limits the data in flight to the smaller of the two windows. Nobody tells the sender the capacity of the routers, so loss-based algorithms such as CUBIC infer it from losses and BBR infers it from bandwidth and delay estimates. See [TCP and UDP](03-tcp-and-udp.md#congestion-control).

??? question "How does TCP react to a lost packet?"

    There are two detection paths. If later segments arrive, the receiver sends duplicate ACKs, and after three of them the sender resends the missing segment at once (fast retransmit), sets ssthresh to `max(FlightSize/2, 2 x SMSS)`, and enters fast recovery at about half its previous rate. With SACK the ACKs also list the ranges that arrived, so the sender resends only the gaps. If no ACKs return, the retransmission timer expires: the sender resends, doubles the RTO, and sets cwnd to one segment, which restarts slow start. A timeout costs far more than a fast retransmit, so a short flow with too few segments to produce three duplicate ACKs suffers most. See [TCP and UDP](03-tcp-and-udp.md#congestion-control).

??? question "What are Nagle's algorithm and delayed ACK, and when do you set `TCP_NODELAY`?"

    Nagle's algorithm holds small writes in a buffer while earlier data is unacknowledged, and delayed ACK lets the receiver wait before it acknowledges, so the two together can stall a request. A client that sends a request in two small writes waits for an ACK of the first, and the server waits for the rest of the request before it answers, until the delayed-ACK timer fires. Set `TCP_NODELAY` (in Java, `socket.setTcpNoDelay(true)`) for latency-sensitive request-response traffic such as RPC and database drivers, and write each message with one call. Do not set it for a program that writes one byte at a time in a loop, because it sends many tiny segments. In Java `TCP_NODELAY` is off by default, so Nagle's algorithm is on. See [TCP and UDP](03-tcp-and-udp.md#nagles-algorithm-and-delayed-acks).

??? question "When do you choose UDP over TCP, and what must you build yourself?"

    Choose UDP when the application tolerates loss or handles it better than TCP, or when retransmission and ordering add delay that it does not want. Examples are DNS queries, real-time voice and video, game state updates, and QUIC. UDP keeps message boundaries and has no head-of-line blocking between datagrams, but it gives no delivery guarantee, no ordering, and no flow or congestion control. The application must add loss handling, ordering, message sizes below the path MTU, and congestion control, and if it needs all of them, TCP or QUIC is a better base than a home-made protocol. See [TCP and UDP](03-tcp-and-udp.md#udp).

??? question "A Java service call sometimes hangs for minutes. What do you check?"

    The most likely cause is a missing timeout. `Socket.connect` with timeout 0 waits for the kernel, about 131 seconds with the Linux default of 6 SYN retries, and `SO_TIMEOUT` of 0 waits forever for the next byte. A peer that vanished without a FIN or RST gives no signal: a thread that writes waits about 15 minutes for the kernel retries (`tcp_retries2` is 15), and a thread that only reads waits until keep-alive fires, which is off by default and starts after 2 hours on Linux. Set a connect timeout, a read timeout, and an overall deadline, shorten the keep-alive timers or use application heartbeats, and validate pooled connections. See [TCP and UDP](03-tcp-and-udp.md#a-client-with-explicit-timeouts-and-options).

??? question "A Java client logs `Connection reset by peer`. What happened, and what do you do?"

    The local TCP received an RST, which means that the peer has no such connection or aborted it on purpose. Causes: the server process crashed or restarted, the server closed the socket with unread data in its buffer, a load balancer or firewall dropped its state for an idle connection, or a pool reused a connection that the server had already closed. The last cause is the most common in production. Set the pool's idle time and lifetime below the idle timeout of the server and of every device on the path, validate connections before use, and retry on a fresh connection only if the request is idempotent, because the server may have processed the first attempt. See [TCP and UDP](03-tcp-and-udp.md#a-client-with-explicit-timeouts-and-options).

## HTTP

??? question "What is the difference between safe and idempotent methods, and why does it matter for retries?"

    A safe method requests no state change (`GET`, `HEAD`, `OPTIONS`, `TRACE`), and an idempotent method has the same intended effect on the server whether the client sends it once or several times (`PUT`, `DELETE`, and every safe method). `POST` and `PATCH` are not idempotent by definition. It matters because a dropped connection hides the outcome: a retry of `PUT /orders/7` leaves the same state, but a retry of `POST /payments` can charge twice, so `POST` needs an idempotency key. Idempotent describes the effect and not the response, because the first `DELETE` returns `204` and the second returns `404`. See [HTTP](04-http.md#methods).

??? question "When do you use the redirect codes 301, 302, 303, 307, and 308?"

    Two questions decide it: is the move permanent, and does the client keep the method? `301` is permanent and `302` is temporary, and both let a client change `POST` to `GET`. `303` always makes the follow-up a `GET`, which fits "post, redirect, get" after a `POST` that created something. `307` (temporary) and `308` (permanent) keep the method and the body, so a `POST` stays a `POST`. Browsers can cache the permanent codes, so test with a temporary code first. See [HTTP](04-http.md#status-codes).

??? question "How do you choose between 400, 401, 403, 404, 409, 412, and 422?"

    `400` means the server cannot parse the request. `401` means no valid authentication (unauthenticated, despite the name) and must carry `WWW-Authenticate`, while `403` means the client is known and not allowed. `404` means no resource at the URI, and some APIs send it instead of `403` to hide that a resource exists. `409` is a conflict with the state of the resource, `412` is a failed `If-Match` precondition, and `422` means the request parses but its content breaks a rule. Many APIs use `400` for validation errors, which is acceptable if the API documents the rule and applies it consistently. See [HTTP](04-http.md#status-codes).

??? question "What are the differences between HTTP/1.1, HTTP/2, and HTTP/3?"

    All three carry the same semantics (RFC 9110) and differ in wire format and transport. HTTP/1.1 is text and handles one request at a time per connection, so browsers open about 6 connections per host. HTTP/2 is binary: it multiplexes streams over one TCP connection, compresses headers with HPACK, and has per-stream flow control, but one lost TCP segment stalls all streams. HTTP/3 runs over QUIC on UDP, so a loss stalls only the affected stream. QUIC also merges the transport and TLS 1.3 handshakes (a new connection needs 1 round trip against 2 for TCP plus TLS 1.3), keeps a connection alive across an address change by connection ID, and needs UDP to pass the network, so clients keep a TCP fallback. See [HTTP](04-http.md#http3-and-quic-rfc-9114-rfc-9000).

??? question "HTTP/2 solves head-of-line blocking, right?"

    It solves the blocking at the HTTP level only. In HTTP/1.1 responses return in request order on one connection, and in HTTP/2 they are separate streams whose frames interleave, so a slow response does not delay a fast one. But all streams share one TCP connection and TCP delivers bytes in order, so one lost segment holds back every later byte of every stream (RFC 9113 says that TCP head-of-line blocking is not addressed). On a lossy network, one HTTP/2 connection can be slower than six HTTP/1.1 connections. HTTP/3 fixes this in the transport, but the streams still share one congestion window. See [HTTP](04-http.md#http2-binary-framing-and-multiplexing-rfc-9113).

??? question "What does `Cache-Control: no-cache` mean, and how does it differ from `no-store`?"

    `no-cache` does not mean "do not cache": a cache may store the response but must validate it with the origin before each reuse, and with an `ETag` the origin answers `304` with no body if the copy is current. `no-store` means that a cache must not store any part of the request or the response, and it fits sensitive data such as a token. `private` is a third option: it lets the browser store the response and forbids shared caches such as a CDN. `no-store` costs a full request every time, and it is a request to compliant caches, not a security control by itself. See [HTTP](04-http.md#caching-rfc-9111).

??? question "How do you stop two clients from overwriting each other's changes through a REST API?"

    Use optimistic concurrency with `ETag` and `If-Match`. The `GET` returns an `ETag`, the client sends it back in `If-Match` on the `PUT`, and the server applies the change only if the current `ETag` matches (strong comparison). If another client changed the resource first, the server answers `412 Precondition Failed`, and the client reads again and merges. This needs no lock across requests and works through caches and proxies. It detects the conflict and does not resolve it, and a weak `ETag` never matches strongly. See [HTTP](04-http.md#caching-rfc-9111).

??? question "Explain CORS. What is a preflight, and when does the browser send one?"

    The same-origin policy stops a script from reading responses from another origin, and CORS lets a server allow it for chosen origins with `Access-Control-*` headers. The browser enforces it, so `curl` and server-to-server calls ignore it, and it is neither authentication nor protection for the API. The browser sends a preflight (an `OPTIONS` request without credentials) when the real request is not CORS-safelisted: a method other than `GET`, `HEAD`, or `POST`, a header such as `Authorization`, or a `Content-Type` other than `application/x-www-form-urlencoded`, `multipart/form-data`, or `text/plain`, so a JSON `POST` triggers one. With cookies, the server must send `Access-Control-Allow-Credentials: true` and name one exact origin, because `*` does not work with credentials, and echoing any `Origin` with credentials is a vulnerability. See [HTTP](04-http.md#cors-whatwg-fetch-standard).

??? question "Which attributes do you set on a session cookie, and why?"

    Set `Secure` (HTTPS only), `HttpOnly` (a script cannot read it, which limits XSS damage), `SameSite=Lax` or `Strict` (the browser withholds the cookie on most cross-site requests, which blocks most CSRF), `Max-Age`, and `Path=/`. Leave out `Domain` so the cookie is host-only, and use the `__Host-` prefix so the browser enforces `Secure`, `Path=/`, and no `Domain`. `SameSite` works on the site (the registrable domain) and not on the origin, so a CSRF token remains the stronger defense. Set `SameSite` explicitly: only Chrome and Edge apply the `Lax` default, and Firefox and Safari do not. See [HTTP](04-http.md#cookies-rfc-6265-and-its-revision).

??? question "What are the defaults of the Java `HttpClient`, and what do you change in production?"

    The default protocol preference is HTTP/2, with a fall back to HTTP/1.1, and the default redirect policy is `NEVER`. There is no default connect timeout and no default request timeout, so set `connectTimeout` on the builder and `timeout` on each request, and share one client, because a new client for each request prevents connection reuse. Keep the idle timeout of the pool (`jdk.httpclient.keepalive.timeout`, default 30 seconds) below the idle timeout of the server and of every load balancer on the path. JDK 21 has no HTTP/3 in `HttpClient`: JEP 517 adds it in JDK 26 as an opt-in setting. See [HTTP](04-http.md#what-a-java-developer-sees).

## TLS and HTTPS

??? question "Walk through the TLS 1.3 handshake. Why does it need one round trip?"

    The client sends a `ClientHello` that already contains a key share (a guess for a group that the server supports), plus the SNI name and the ALPN list. The server answers with a `ServerHello` and its key share, and both sides compute the handshake keys. The server then sends `EncryptedExtensions`, `Certificate`, `CertificateVerify`, and `Finished` in the same flight. The client validates the chain, checks `Finished`, and sends its own `Finished` with the first application data, so one round trip is enough. If the server accepts no group that the client sent a share for, it sends `HelloRetryRequest` and the handshake takes 2 round trips. See [TLS and HTTPS](05-tls-and-https.md#the-tls-13-handshake-in-one-round-trip).

??? question "What changed from TLS 1.2 to TLS 1.3, and what is forward secrecy?"

    TLS 1.3 needs 1 round trip against 2, removes static RSA and static DH key exchange, and allows only AEAD ciphers. It encrypts every message after `ServerHello` and drops compression and renegotiation. Forward secrecy means that a recorded connection stays unreadable even if an attacker steals the server's long-term key later. Ephemeral key exchange gives it, and TLS 1.3 requires it for every public-key exchange. The exceptions are PSK-only resumption, which has no new key exchange, and 0-RTT early data, which is encrypted under a key derived from the PSK alone. With static RSA, the client encrypted the secret to the server's long-term key, so a later key theft decrypted all recorded traffic. See [TLS and HTTPS](05-tls-and-https.md#what-changed-from-tls-12).

??? question "How does a client validate a server certificate?"

    The client builds a path from the leaf certificate through the intermediates to a trust anchor in its trust store, and the server must send the intermediates. For each certificate it checks the signature against the issuer's key, the validity period, the CA flag and path length, and revocation. It also checks that the host name matches a subject alternative name (SAN), not the common name. Then the `CertificateVerify` signature proves that the server holds the private key now, and `Finished` confirms that nobody changed the handshake. If any step fails, the handshake ends with a fatal alert. See [TLS and HTTPS](05-tls-and-https.md#certificates).

??? question "A Java client fails with `PKIX path building failed`, but the site works in Chrome. Why?"

    The JDK could not build a path from the server's certificate to a trust anchor. A frequent cause is a server that sends only the leaf certificate: a browser can fetch the missing intermediate from the AIA URL, but a JDK 21 client does not do that by default (it needs `com.sun.security.enableAIAcaIssuers=true`). Other causes are a private or self-signed CA that is missing from the trust store, a corporate proxy that re-signs traffic, and an old trust store. Fix the server so that it sends the full chain, or add the right root to the trust store of that process. Never fix it with a trust manager that accepts every certificate, because that removes authentication. See [TLS and HTTPS](05-tls-and-https.md#certificates).

??? question "What is 0-RTT in TLS 1.3, and why is it dangerous?"

    A client that holds a session ticket sends application data in its first flight, so the request needs no extra round trip. The data is encrypted only with a key derived from the pre-shared key, so it has no forward secrecy guarantee, and it has no replay protection between connections: an attacker who records the first flight can send it again, and the server cannot tell the copy from the original. A payment can then run twice. RFC 9846 requires each server instance to accept a given 0-RTT handshake at most once, which limits a replay to one per instance, but a cluster without shared state still accepts copies. So servers reject early data, delay it until the handshake ends, or answer `425 Too Early` (RFC 8470). Allow it only for requests that are safe to replay, such as a `GET` of static content, and keep it off for a `POST /pay`, and note that JDK TLS 1.3 does not support 0-RTT. See [TLS and HTTPS](05-tls-and-https.md#0-rtt-and-replay).

??? question "How does certificate revocation work, and what is the status of OCSP?"

    A CRL is a signed list of revoked serial numbers, and OCSP lets a client ask a responder about one certificate. OCSP adds a round trip to a third party in the middle of the handshake, and the responder learns which sites the user visits. OCSP stapling lets the server send its own signed response in the handshake. Let's Encrypt turned off its OCSP responders on 2025-08-06 and publishes CRLs, so do not design around OCSP for its certificates. The JDK checks revocation only if you turn it on (`com.sun.net.ssl.checkRevocation` is `false` by default), and short certificate lifetimes reduce the need for revocation. See [TLS and HTTPS](05-tls-and-https.md#certificates).

??? question "Certificate lifetimes are shrinking. What is the schedule, and what should a team do?"

    The CA/Browser Forum maximum validity depends on the issue date: 398 days before 2026-03-15, 200 days from 2026-03-15, 100 days from 2027-03-15, and 47 days from 2029-03-15. At 47 days a certificate renews about 8 times a year, so manual renewal does not scale and misses become outages. Automate issuance and renewal with ACME (RFC 8555), use ACME Renewal Information (RFC 9773) where the CA offers it, and add a separate expiry monitor that alerts weeks ahead. See [TLS and HTTPS](05-tls-and-https.md#certificates).

??? question "What does HSTS do, and what is its weakness?"

    The server sends `Strict-Transport-Security: max-age=...` over HTTPS, and the browser then rewrites `http://` to `https://` for that host before it sends anything. It also refuses to continue after a certificate error, so the user cannot click through the warning. The browser ignores the header if it arrives over plain HTTP. The weakness is the first visit, which has no protection, and the preload list closes that gap for listed hosts but removal takes months. Start with a short `max-age` and test every sub-domain before you use `includeSubDomains` with a long value. See [TLS and HTTPS](05-tls-and-https.md#https-specific-items).

## DNS

??? question "Walk through DNS resolution for `www.example.com`."

    The browser or JVM asks the stub resolver of the operating system, which sends one recursive query to the configured recursive resolver. If the resolver has no cached answer, it sends iterative queries: the root server answers with a referral to the `com` servers, a `com` server answers with a referral to the `example.com` servers (with glue), and the authoritative server answers with the A record and the AA flag. The resolver caches every record for its TTL and replies with the RA flag set. On a busy resolver the NS records of `com` are cached (TTL 172,800 seconds), so the common cost of a miss is one query to the authoritative server. See [DNS](06-dns.md#the-resolution-walk-for-wwwexamplecom).

??? question "You change a DNS record, and half the users still see the old value. Why, and how do you plan a migration?"

    DNS does not push changes: each cache keeps the old record until its TTL ends, so "propagation" is cache expiry. A resolver returns the remaining TTL, and a JVM or browser cache adds its own time on top. A change of name servers is slower, because the parent zone holds the NS records and its TTL applies, not yours. Lower the TTL (for example to 60 seconds) at least one old TTL before the change, make the change, check it, and then raise the TTL again. See [DNS](06-dns.md#dns-in-operations).

??? question "What is negative caching in DNS? Why can a new record stay invisible after you create it?"

    A resolver also caches "no": NXDOMAIN (the name does not exist) and NODATA (the name exists without that type). It keeps the answer for the lower of the TTL of the SOA record and its MINIMUM field (RFC 2308). If someone queries `new.example.com` before you create the record, every resolver that saw the NXDOMAIN keeps it for that negative TTL, even after you add the record. Create the record before anyone tests the name, and keep the SOA MINIMUM low (for example 60 to 300 seconds) on zones where you add records often. See [DNS](06-dns.md#negative-caching).

??? question "How does the JVM cache DNS lookups, and how do you change it?"

    The JVM has its own cache in front of the operating system, and it ignores the TTL of the record. `networkaddress.cache.ttl` is 30 seconds in OpenJDK without a security manager and "forever" with one, `-1` means forever, and `networkaddress.cache.negative.ttl` is 10 seconds. These are security properties and not system properties, so `-Dnetworkaddress.cache.ttl=10` has no effect. Set them in `java.security`, in a file that you give with `-Djava.security.properties`, or with `Security.setProperty` before the first lookup. A pooled connection also keeps the old address until it closes, so set a maximum lifetime for pooled connections. See [DNS](06-dns.md#the-jvm-cache).

??? question "Why can a CNAME not sit at the zone apex?"

    A CNAME must be the only record at its name. The apex must hold the SOA and NS records of the zone, so a CNAME there would conflict with them, and it would also hide an MX record for the domain's mail. There is no standard "CNAME at the apex" record. Providers offer their own alias records (Route 53 alias, ANAME, CNAME flattening) that answer with A and AAAA records, and the standard mechanism for web traffic is an HTTPS record in AliasMode. Recursive resolvers see ordinary A records from such a provider, so the TTL and failover behavior depend on the provider. See [DNS](06-dns.md#record-types).

??? question "Does DNS use UDP or TCP? What is the 512-byte limit?"

    DNS listens on port 53 for both. UDP is the first choice because it needs one packet each way and no connection setup. Without extensions a UDP message is limited to 512 bytes, so if the answer does not fit, the server sets the TC flag and the client repeats the query over TCP, with a two-byte length in front of the message. EDNS(0) lets the client advertise a larger UDP size, and DNS Flag Day 2020 recommends 1232 bytes because large UDP messages are fragmented and often lost. DNSSEC answers are larger, so TCP fallback is more common now. See [DNS](06-dns.md#message-format-and-transport).

??? question "What is DNS cache poisoning, and how do DNSSEC and encrypted DNS relate to it?"

    A resolver accepts the first reply that matches its query, and an attacker who guesses the 16-bit query ID can plant a wrong record. The Kaminsky attack (2008) used replies for random names under the target domain to plant forged NS and glue records. Defenses are a random ID and source port (RFC 5452), accepting only in-domain data, a TCP fallback, and DNSSEC, which signs the data so that a validating resolver rejects forged answers (and returns SERVFAIL for bogus data). DNSSEC gives authenticity and integrity but no confidentiality. DoT, DoH, and DoQ encrypt the path to the resolver but do not authenticate the data. See [DNS](06-dns.md#security).

??? question "Why does a Kubernetes Pod make several DNS queries for one external name?"

    The kubelet writes `options ndots:5` and a search list into `/etc/resolv.conf` of each Pod. A name with fewer than five dots, such as `api.example.com`, is first tried with each search suffix, and every failed attempt is an NXDOMAIN query (two for A and AAAA). Only then does the resolver try the name as it is. Fixes: write the name with a final dot (`api.example.com.`), lower `ndots` in `dnsConfig.options`, or reuse connections. The page marks this as a derived explanation, so measure it in your cluster before you change a setting. See [DNS](06-dns.md#dns-in-operations).

## What Happens When You Load a URL

??? question "What happens when you type a URL into the browser and press Enter?"

    The browser parses the URL and checks its HSTS list, then looks in the service worker and the HTTP cache. On a miss it resolves the name with DNS, opens a TCP connection (or QUIC for HTTP/3), runs the TLS handshake, and sends the HTTP request. The request crosses a CDN edge, a load balancer, and the application server, which reads a cache and then the database on a miss. The browser then builds the DOM, the CSSOM, and the render tree, lays out and paints the page, and sends the follow-up requests on the open connection. I would give each step with its cost in round trips and offer to go deeper on any step. See [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md#how-it-works).

??? question "How many round trips does the first request cost, and how do the HTTP versions differ?"

    A cold request over TCP and TLS 1.3 costs the DNS lookup plus 3 round trips: 1 for TCP, 1 for TLS, and 1 for the request and the first response byte. TLS 1.2 adds one more (4), and HTTP/3 over QUIC needs 2 because QUIC carries the TLS 1.3 handshake inside its own. With 0-RTT, HTTP/3 needs 1 and TCP with TLS 1.3 needs 2, and a warm connection needs 1. A fresh entry in the browser cache needs none. These counts exclude server processing time and body transfer. See [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md#round-trips-cold-against-warm-and-the-three-http-versions).

??? question "Why does HSTS make the first request faster?"

    For a known HSTS host the browser rewrites an `http` URL to `https` locally, before it sends anything, so it costs zero round trips. Without HSTS, a user who types `http://www.example.com` pays a TCP handshake and a request to port 80 to receive a `301` or `308`, which is 2 extra round trips, and then pays for the full HTTPS path. The first visit to a host has no HSTS entry unless the host is on the preload list, so the redirect remains the fallback there. See [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md#step-1-url-parsing-and-the-hsts-check).

??? question "In what order does the browser look for a stored answer before it uses the network?"

    First the service worker: if one is registered for the URL scope, it gets a `fetch` event and can answer from its own storage, from code, or from the network. If it does not call `respondWith()`, the browser continues with the HTTP cache. A fresh entry there goes to the page with no network use, and a stale entry with a validator causes a conditional request, where a `304` renews it. Only then does the request go to the network. Browsers partition the HTTP cache (Chrome keys it on the top-frame site, the frame site, and the resource URL), so check the browser documentation before you state a cache key. See [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md#step-2-the-service-worker-and-the-browser-cache).

??? question "Why does a CDN edge make the first request faster, even on a cache miss?"

    The TCP and TLS handshakes run against the nearby edge and not against the far origin. With an edge 10 ms away, the two handshake round trips cost 20 ms, against 300 ms for an origin 150 ms away (the page's example numbers, not measurements). On a cache hit the edge answers after 3 round trips of 10 ms, which is 30 ms. On a miss with a warm edge-to-origin connection, the user adds one 150 ms trip, and a cold edge-to-origin connection adds 3 trips of 150 ms. See [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md#step-5-the-tls-handshake).

??? question "What does the server side of a request look like, and where can a stale copy hide?"

    The request reaches the CDN edge, which terminates TLS and answers from its cache on a hit. On a miss it goes to the load balancer, then to a reverse proxy or API gateway, then to the application server, which reads a cache such as Redis and queries the database on a miss. Each hop inside one data center costs about 0.5 ms (approximate), so four sequential hops cost about 2 ms of network time plus processing time. A stale copy can hide in every cache layer: the browser, the CDN, the proxy, and the application cache, so list the layers in order and check each one. See [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md#step-7-the-server-side).

??? question "What blocks rendering, and what do `async`, `defer`, `preload`, and `preconnect` change?"

    CSS blocks rendering, because later rules can override earlier ones. A classic `<script src>` with neither `async` nor `defer` blocks parsing until it is fetched and evaluated. `async` runs the script as soon as it arrives, possibly before parsing ends, and `defer` runs it after parsing ends and keeps the order. `preload` fetches one known resource earlier, and `preconnect` opens the DNS, TCP, and TLS connection to another origin ahead of time but fetches nothing. A request to a new origin pays DNS, TCP, TLS, and the request again, and web.dev says that the browser closes an unused preconnected connection after 10 seconds. See [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md#step-8-the-response-and-rendering).

## WebSockets and Real-Time Protocols

??? question "How do WebSocket, Server-Sent Events, and long polling differ, and when do you pick each?"

    WebSocket is a two-way channel of text or binary frames on one TCP connection after an HTTP upgrade, SSE is a one-way stream of UTF-8 text events in one HTTP response, and long polling is an ordinary request that the server holds until it has data. SSE reconnects by itself with `Last-Event-ID`, a WebSocket has no built-in reconnection or replay, and long polling pays a full HTTP request for each batch. Choose SSE for one-way pushes, WebSocket for frequent two-way messages, and long polling when the network blocks both. SSE also needs a proxy that does not buffer the response, and a WebSocket needs a proxy that forwards the upgrade. See [WebSockets and Real-Time Protocols](08-websockets-and-realtime.md#trade-offs-and-when-to-use).

??? question "How does the WebSocket opening handshake work, and what does `Sec-WebSocket-Accept` prove?"

    The client sends an HTTP/1.1 `GET` with `Upgrade: websocket`, `Connection: Upgrade`, a random `Sec-WebSocket-Key`, and `Sec-WebSocket-Version: 13`. The server answers `101 Switching Protocols` with `Sec-WebSocket-Accept`, which is the Base64 of the SHA-1 hash of the key plus a fixed GUID. After the blank line that ends the response, the connection carries WebSocket frames and no more HTTP messages. The accept value proves only that the server read the handshake and understands WebSocket, so an ordinary HTTP server or a forged browser request cannot produce it by accident. It does not authenticate the server or protect the data. See [WebSockets and Real-Time Protocols](08-websockets-and-realtime.md#the-websocket-opening-handshake).

??? question "Why do WebSocket clients mask their frames? Is masking encryption?"

    Masking is not encryption, because the 32-bit key travels in the same frame. Every client-to-server frame carries a new unpredictable mask, and the payload is XORed with it. The purpose is to protect intermediaries: without it, a script in a browser could write bytes that look like an HTTP request, and an old proxy could take them for a real request and store a wrong response. The server never masks, because the threat is a script in the client, and a server that receives an unmasked client frame must close the connection. Confidentiality comes from `wss`, which is TLS. See [WebSockets and Real-Time Protocols](08-websockets-and-realtime.md#masking).

??? question "A WebSocket drops every 60 seconds behind a proxy. What is wrong, and what do you do?"

    A proxy or load balancer closes the connection after its idle timeout, for example 60 seconds for Nginx (`proxy_read_timeout`) and for an AWS Application Load Balancer by default, and 350 seconds for a Network Load Balancer TCP listener. The client sees close code 1006, which an API reports when the connection ends without a close frame. Send a ping at an interval of at most half of the shortest idle timeout on the path, so every 30 seconds or less for 60, and close and reconnect if no pong arrives. TCP keep-alive does not replace this, because it starts after 2 hours on Linux and a terminating proxy answers it for itself. The browser API cannot send a ping, so a browser client sends an application heartbeat message. See [WebSockets and Real-Time Protocols](08-websockets-and-realtime.md#long-lived-connections-behind-proxies-and-load-balancers).

??? question "How do you secure a WebSocket that authenticates with a cookie?"

    Check the `Origin` header on the handshake against an allow-list and answer `403 Forbidden` for other origins. The same-origin policy does not block a WebSocket connection to another origin, so without the check any page that the user visits can open a connection that carries the user's cookie (cross-site WebSocket hijacking). The browser `WebSocket` API cannot set custom headers, so clients authenticate with a cookie, a token in the query string, or a first message. A token in a URL can reach access logs, so use one that expires within seconds. See [WebSockets and Real-Time Protocols](08-websockets-and-realtime.md#the-websocket-opening-handshake).

??? question "After a rolling restart of a WebSocket gateway fleet, the servers carry unequal load. Why, and what do you do?"

    A load balancer assigns a connection to a server only when the connection opens and never moves it later. When a server restarts, its clients reconnect to the servers that still run, so the servers that restart last collect the clients of all earlier ones, and a newly started server stays nearly empty. Mitigate it with a drain that closes connections at a controlled rate with close code 1001, client reconnects with exponential backoff and jitter so that they do not arrive together, a least-connections algorithm for new connections, and a maximum connection age with random spread. See [WebSockets and Real-Time Protocols](08-websockets-and-realtime.md#long-lived-connections-behind-proxies-and-load-balancers).

## Proxies and NAT

??? question "What is the difference between a forward proxy and a reverse proxy?"

    A forward proxy acts for the client: the client chooses it, and the server sees the proxy's address. A reverse proxy acts for the server: the client does not know it, and the backends stay hidden. A layer 7 load balancer, a CDN edge, and an API gateway are reverse proxies. A client sends a request to a forward proxy in absolute-form (`GET http://host/path`), and a request to a reverse proxy in origin-form like a direct request. Both terminate the incoming TCP connection and open a separate one onward, so each leg has its own TCP state and timers. A reverse proxy that terminates TLS also gives each leg its own TLS session, but a `CONNECT` tunnel through a forward proxy keeps TLS end to end. See [Proxies and NAT](09-proxies-and-nat.md#forward-proxy-and-reverse-proxy).

??? question "How does HTTPS work through a forward proxy?"

    The client sends `CONNECT www.example.com:443` to the proxy, the proxy opens a TCP connection to that host and port, and it answers with a `2xx` response. After that the proxy relays raw bytes both ways, and the TLS handshake runs end to end between the client and the origin. The proxy sees only the host and port, the amount of data, and the timing, and the SNI name unless the client uses Encrypted Client Hello. The tunnel closes when the proxy sees that either side closed its connection, and the proxy then closes both. A proxy should allow `CONNECT` only to a small set of well-known ports. A company proxy that inspects HTTPS is a different mechanism: it ends the TLS session with a certificate from a company CA that the client trusts, which breaks certificate pinning. See [Proxies and NAT](09-proxies-and-nat.md#connect-https-through-a-forward-proxy).

??? question "How does a backend learn the real client IP behind proxies, and why is `X-Forwarded-For` dangerous?"

    The TCP peer is the last proxy, so each proxy appends the address of the peer it received the request from to `X-Forwarded-For` (or uses the standard `Forwarded` header, RFC 7239). A client can send any value, and proxies append and do not replace, so the forged entries come first and the true ones last. The backend must read the header only when the TCP peer is a trusted proxy, and read it from the right: skip entries while they are trusted proxies, and take the first address that is not. If it takes the first entry from an untrusted source, one request header bypasses an IP rule or a rate limit. A layer 4 proxy cannot add HTTP headers, so it uses the PROXY protocol, which both sides must enable and the backend must accept only from the proxy. See [Proxies and NAT](09-proxies-and-nat.md#headers-that-proxies-add).

??? question "What does NAT do, and why does a connection die after minutes of silence behind it?"

    A NAPT device keeps a translation table that ties an internal address and port to an external port, rewrites the source of outbound packets, and rewrites the destination of the replies. It drops an inbound packet that matches no entry, which is a side effect and not a security feature. The device deletes an entry after an idle timeout and tells neither endpoint, so both ends still show ESTABLISHED, and the next packet is dropped or reset. RFC 5382 asks for at least 2 hours 4 minutes for an established TCP mapping, but real devices differ: for example 350 seconds for an AWS Network Load Balancer by default, and you must look up each one. Send keep-alive probes or application heartbeats more often than the shortest timeout, and set the pool idle time below it. See [Proxies and NAT](09-proxies-and-nat.md#the-translation-table-and-its-timeouts).

??? question "How do two hosts behind NAT connect directly? What are STUN, TURN, and ICE?"

    Each NAT drops the unsolicited packet from the other side, so the hosts need help. STUN tells a host its server-reflexive address (the public address and port that its NAT gave it), and both hosts then send UDP packets to each other's address at the same time (hole punching). That works with endpoint-independent mapping and fails with a symmetric NAT, which gives each destination a new external port. TURN is the fallback: a server relays the traffic, which costs its bandwidth and an extra hop. ICE gathers host, server-reflexive, and relayed candidates, tests the pairs with STUN checks, and picks the best valid pair, so a direct path wins when it works and WebRTC uses it. See [Proxies and NAT](09-proxies-and-nat.md#nat-traversal-stun-turn-and-ice).
