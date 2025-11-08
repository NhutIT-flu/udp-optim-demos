
# UDP Optimization Demos (Java, no deps)

This mini project contains **4 runnable demos** to illustrate **UDP optimization techniques**:
- **Echo (pacing, buffer tuning)** — `echo.UdpEchoServer`, `echo.UdpClientPaced`
- **Aggregation** — `aggregation.UdpAggregateServer`, `aggregation.UdpClientAggregate`
- **FEC XOR** — `fec.UdpFecServer`, `fec.UdpClientFecXor`
- **Pipeline (multi-thread)** — `pipeline.UdpPipelineServer`, `pipeline.UdpLoadClient`

## Build (JDK 17+)
```bash
javac -d out $(find src -name "*.java")
```
On Windows (PowerShell):
```powershell
Get-ChildItem -Recurse src/*.java | %{ $_.FullName } | javac -d out -cp .
```

## Run

### 1) Echo + Pacing (port 9000)
Terminal A:
```bash
java -cp out echo.UdpEchoServer 9000
```
Terminal B:
```bash
java -cp out echo.UdpClientPaced 127.0.0.1 9000 20000 512 5000 10
# args: host port messages payload tps batch
```

### 2) Aggregation (port 9001)
A:
```bash
java -cp out aggregation.UdpAggregateServer 9001
```
B:
```bash
java -cp out aggregation.UdpClientAggregate 127.0.0.1 9001 8 100 1000
# args: host port msgsPerPacket smallMsgLen packets
```

### 3) FEC XOR (port 9002)
A:
```bash
java -cp out fec.UdpFecServer 9002
```
B:
```bash
java -cp out fec.UdpClientFecXor 127.0.0.1 9002 8 400 500
# args: host port blockN payloadLen blocks
```

### 4) Pipeline server + Load generator (port 9003)
A:
```bash
java -cp out pipeline.UdpPipelineServer 9003 4
# args: port workers
```
B:
```bash
java -cp out pipeline.UdpLoadClient 127.0.0.1 9003 512 20000 10
# args: host port size rate pkt_per_sec seconds
```

## Wireshark
Use filters to capture each demo:
```
udp.port == 9000
udp.port == 9001
udp.port == 9002
udp.port == 9003
```
What to observe:
- **Echo**: effect of payload, TPS, and batch on packet rate/loss.
- **Aggregation**: datagram size ~1.0–1.3KB; fewer packets, higher logical msgs/dgram.
- **FEC**: pattern of N data + 1 parity; `Recovered` counters increase when there is packet loss.
- **Pipeline**: server logs `pkt/s` and queue size as load scales.

## Suggested Excel columns (for your report)
Technique | Params | Packets Sent | Bytes Sent | Logical Msgs | Goodput (msg/s) | Loss % (est.) | CPU % | Notes

## Notes
- Keep payload **<= 1200B** to avoid fragmentation.
- Increase OS buffers if needed (Linux): `net.core.rmem_max`, `net.core.wmem_max`.
- Run clients from a different machine/network to induce mild packet loss for FEC demo.
