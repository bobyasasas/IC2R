#!/usr/bin/env python3
"""Minimal Minecraft RCON client (stdlib only), used by the P19 server soak.

Protocol: little-endian packets, <int32 length><int32 request id><int32 type>
<payload><null><null>. Type 3 = login, 2 = command, 0 = response.
"""

import argparse
import socket
import struct
import sys


SERVERDATA_AUTH = 3
SERVERDATA_EXECCOMMAND = 2
SERVERDATA_RESPONSE_VALUE = 0


def _send(sock, request_id: int, ptype: int, payload: bytes) -> None:
    body = struct.pack("<ii", request_id, ptype) + payload + b"\0\0"
    sock.sendall(struct.pack("<i", len(body)) + body)


def _recv_exact(sock, n: int) -> bytes:
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("RCON connection closed mid-packet")
        buf += chunk
    return buf


def _recv(sock) -> tuple[int, int, bytes]:
    (length,) = struct.unpack("<i", _recv_exact(sock, 4))
    rest = _recv_exact(sock, length)  # id + type + payload + trailing 2 nulls
    request_id, ptype = struct.unpack("<ii", rest[:8])
    payload = rest[8:-2]
    return request_id, ptype, payload


class Rcon:
    def __init__(self, host: str, port: int, password: str) -> None:
        self.sock = socket.create_connection((host, port), timeout=15)
        _send(self.sock, 1, SERVERDATA_AUTH, password.encode("utf-8"))
        # vanilla answers exactly one auth packet: id=request id on success, -1 on failure
        request_id, _, _ = _recv(self.sock)
        if request_id == -1:
            raise PermissionError("RCON auth failed")

    def command(self, line: str) -> str:
        _send(self.sock, 7, SERVERDATA_EXECCOMMAND, line.encode("utf-8"))
        request_id, _, payload = _recv(self.sock)
        if request_id == -1:
            raise RuntimeError(f"RCON command rejected: {line}")
        return payload.rstrip(b"\0").decode("utf-8", "replace")

    def close(self) -> None:
        try:
            self.sock.close()
        except OSError:
            pass


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=25575)
    parser.add_argument("--password", required=True)
    parser.add_argument("command")
    args = parser.parse_args(argv)
    rcon = Rcon(args.host, args.port, args.password)
    try:
        print(rcon.command(args.command))
    finally:
        rcon.close()
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
