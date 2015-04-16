# network

This library implements reliable transport on top of UDP.
The following requirements are based on games that need minimal latency for location updates (otherwise, TCP would be fine):
- Small packet size - average 10 bytes, max 8192 bytes
- Frequent unreliable packets - around 30/second (i.e. every 33ms)
- Both guaranteed and best-effort - a packet can be marked guaranteed to ensure in-order delivery, or marked not guaranteed for packets that are quickly obsolete (e.g. location packet)
- Non-guaranteed packets are always sent immediately, guaranteed packets may be delayed if too many previous packets are unacked
- No priority given; all guaranteed packets queue in order
- Connection state (handshake, timeout)
- Small packet sizes => minimal congestion control

Implementation:
- Guaranteed packets use the Go-Back-N protocol for reliability
- Receiver sends cumulative ACKs when reliable packets are received. To avoid sending ack packets with no payload, acks can be piggybacked on both reliable and unreliable packets. PiggybackAck achieves this with a timer. This is particularly useful since we are sending frequent location updates anyway.
- No ACK for non-guaranteed packets
- Non-guaranteed packets have a separately growing sequence number, so that if we receive an old packet it can be dropped.
- A separate thread runs to constantly receive messages on the socket. Once messages are read, they are added to the inQueue on the correct stream. Note this is one thread per Server or per Connection.open()
- Connections step on the game event loop, and: 1. check if anything is in inQueue and callback for any packets read 2. check if ACKs need to be sent or reliable messages resent
- RTT is estimated as in TCP by looking at time of send vs time of ACK received and this is used for timeouts.
- Window size adjusts dynamically - halving on timeouts, and increasing linearly when we get ACKs.
- Fast Retransmit - as in TCP, if we receive 3 ACKs in a row for the same sequence number, assume that a packet was lost and resend.
