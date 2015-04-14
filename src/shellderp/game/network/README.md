# network

This library implements reliable transport on top of UDP

The requirements are based on the needs for certain games and are as follows:
- Small packet size - average 10 bytes, max 8192 bytes
- Frequent unreliable packets - around 30/second (i.e. every 33ms)
- Both guaranteed and best-effort - a packet can be marked guaranteed to ensure in-order delivery, or marked not guaranteed for packets that are quickly obsolete (e.g. location packet)
- Non-guaranteed packets are always sent immediately, guaranteed packets may be delayed if too many previous packets are unacked
- No priority given; all guaranteed packets queue in order
- Connection state - handshake, timeout
- Small packet sizes => no congestion control (or minimal)
