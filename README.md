# Demo Tối Ưu UDP (Java, không phụ thuộc)

Dự án mini này chứa **4 demo có thể chạy** để minh họa các **kỹ thuật tối ưu UDP**:
- **Echo (pacing, điều chỉnh buffer)** — `echo.UdpEchoServer`, `echo.UdpClientPaced`
- **Aggregation (Gộp message)** — `aggregation.UdpAggregateServer`, `aggregation.UdpClientAggregate`
- **FEC XOR** — `fec.UdpFecServer`, `fec.UdpClientFecXor`
- **Pipeline (đa luồng)** — `pipeline.UdpPipelineServer`, `pipeline.UdpLoadClient`

## Build (JDK 17+)
```bash
javac -d out $(find src -name "*.java")
```
Trên Windows (PowerShell):
```powershell
Get-ChildItem -Recurse src/*.java | %{ $_.FullName } | javac -d out -cp .
```

## Chạy

### 1) Echo + Pacing (port 9000)
Terminal A:
```bash
java -cp out echo.UdpEchoServer 9000
```
Terminal B:
```bash
java -cp out echo.UdpClientPaced 127.0.0.1 9000 20000 512 5000 10
# tham số: host port messages payload tps batch
```

### 2) Aggregation (port 9001)
A:
```bash
java -cp out aggregation.UdpAggregateServer 9001
```
B:
```bash
java -cp out aggregation.UdpClientAggregate 127.0.0.1 9001 8 100 1000
# tham số: host port msgsPerPacket smallMsgLen packets
```

### 3) FEC XOR (port 9002)
A:
```bash
java -cp out fec.UdpFecServer 9002
```
B:
```bash
java -cp out fec.UdpClientFecXor 127.0.0.1 9002 8 400 500
# tham số: host port blockN payloadLen blocks
```

### 4) Pipeline server + Load generator (port 9003)
A:
```bash
java -cp out pipeline.UdpPipelineServer 9003 4
# tham số: port workers
```
B:
```bash
java -cp out pipeline.UdpLoadClient 127.0.0.1 9003 512 20000 10
# tham số: host port size rate pkt_per_sec seconds
```

## Wireshark
Sử dụng bộ lọc để bắt gói tin cho từng demo:
```
udp.port == 9000
udp.port == 9001
udp.port == 9002
udp.port == 9003
```
Những gì cần quan sát:
- **Echo**: ảnh hưởng của payload, TPS, và batch lên tốc độ/mất gói tin.
- **Aggregation**: kích thước datagram ~1.0–1.3KB; ít packet hơn, nhiều message logic/dgram hơn.
- **FEC**: mẫu N data + 1 parity; bộ đếm `Recovered` tăng khi có mất gói tin.
- **Pipeline**: server ghi log `pkt/s` và kích thước queue khi tải tăng.


Technique | Params | Packets Sent | Bytes Sent | Logical Msgs | Goodput (msg/s) | Loss % (est.) | CPU % | Notes

## Lưu ý
- Giữ payload **<= 1200B** để tránh phân mảnh.
- Tăng buffer của OS nếu cần (Linux): `net.core.rmem_max`, `net.core.wmem_max`.
- Chạy client từ máy/mạng khác để tạo mất gói tin nhẹ cho demo FEC.
