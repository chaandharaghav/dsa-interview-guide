# Networking

This subject covers the network protocols that a backend developer uses: the layer models, IP addressing and routing, TCP and UDP, HTTP, TLS, DNS, WebSocket and server-sent events, and proxies and NAT. Each page explains the protocol from its standard and connects it to what a Java service sees (a timeout, a connection pool, a certificate error).

## How to Study This Subject

1. Read the P1 pages first, in this order: [TCP and UDP](03-tcp-and-udp.md), [HTTP](04-http.md), [TLS and HTTPS](05-tls-and-https.md), and [DNS](06-dns.md).
2. Then read [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md), which puts the four protocols together. Practice it aloud in 5 minutes, then in 15 minutes with the follow-up questions.
3. Read the P2 pages when a role or a design needs them: [OSI and TCP/IP Models](01-osi-and-tcp-ip-models.md), [IP Addressing and Routing](02-ip-addressing-and-routing.md), [WebSockets and Real-Time Protocols](08-websockets-and-realtime.md), and [Proxies and NAT](09-proxies-and-nat.md).
4. Use the [question bank](interview-question-bank.md) for revision. Each answer links to the page that explains it.

The pages cite the current RFCs (for example RFC 9293 for TCP, RFC 9110 to 9114 for HTTP, and RFC 9846 for TLS 1.3). The Java code targets Java 21. Programs with `main` are simulations that open no sockets, so their output is the same on every machine.

The design view of the same topics is on the HLD pages: [Load Balancing, DNS, and CDN](../hld/fundamentals/02-load-balancing-dns-cdn.md), [Communication Patterns](../hld/fundamentals/09-communication-patterns.md), and [Security in Design](../hld/fundamentals/21-security-in-design.md).

## Pages

Priority labels:

- **P1:** interviews ask about it often.
- **P2:** interviews ask about it sometimes, or the topic is important on the job.
- **P3:** useful on the job, rare in interviews.

| Page | Content | Priority |
|---|---|---|
| [OSI and TCP/IP Models](01-osi-and-tcp-ip-models.md) | The layers, encapsulation, what "L4" and "L7" mean, MTU and fragmentation | P2 |
| [IP Addressing and Routing](02-ip-addressing-and-routing.md) | IPv4 and IPv6, subnets and CIDR, longest prefix match, BGP, `traceroute` | P2 |
| [TCP and UDP](03-tcp-and-udp.md) | Handshake, TIME_WAIT, retransmission, flow control, congestion control, keep-alive, timeouts in Java | P1 |
| [HTTP](04-http.md) | Semantics, HTTP/1.1, HTTP/2, HTTP/3 and QUIC, caching headers, cookies, CORS | P1 |
| [TLS and HTTPS](05-tls-and-https.md) | The TLS 1.3 handshake, 0-RTT, certificates and the chain of trust, revocation, ECH, post-quantum key exchange | P1 |
| [DNS](06-dns.md) | Resolution, caching and TTL, record types, the JVM DNS cache, DNSSEC, encrypted DNS | P1 |
| [What Happens When You Load a URL](07-what-happens-when-you-load-a-url.md) | The full request path, step by step, with the round-trip cost of each step | P1 |
| [WebSockets and Real-Time Protocols](08-websockets-and-realtime.md) | The WebSocket handshake and frames, server-sent events, long polling, long-lived connections behind proxies | P2 |
| [Proxies and NAT](09-proxies-and-nat.md) | Forward and reverse proxies at the protocol level, forwarding headers, NAT and NAT traversal | P2 |
| [Interview Question Bank](interview-question-bank.md) | Short questions with model answers. Each answer links to the page that explains it | P1 |
