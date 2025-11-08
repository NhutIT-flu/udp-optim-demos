@echo off
mkdir out 2>nul
for /r src %%f in (*.java) do (
  echo Compiling %%f
)
javac -d out -cp . src\common\Util.java src\echo\UdpEchoServer.java src\echo\UdpClientPaced.java src\aggregation\UdpAggregateServer.java src\aggregation\UdpClientAggregate.java src\fec\UdpFecServer.java src\fec\UdpClientFecXor.java src\pipeline\UdpPipelineServer.java src\pipeline\UdpLoadClient.java
echo Build OK -> out\
