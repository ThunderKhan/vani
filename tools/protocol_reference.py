#!/usr/bin/env python3
"""Independent reference encoder for the experimental VĀṆI M4 v1 bundle vector."""

import struct
import uuid

def s(value):
    if value is None:
        return struct.pack(">H", 0xFFFF)
    b = value.encode("utf-8")
    return struct.pack(">H", len(b)) + b

def encode():
    out = bytearray()
    out += struct.pack(">I4B", 0x56414E34, 1, 0, 0, 0)
    u = uuid.UUID("123e4567-e89b-12d3-a456-426614174000")
    out += struct.pack(">QQ", (u.int >> 64) & ((1 << 64) - 1), u.int & ((1 << 64) - 1))
    out += s("incident-01") + s("sender") + s("receiver")
    out += struct.pack(">QQ", 1700000000000, 1700000060000)
    out += bytes([4, 1, 3, 1, 4])
    out += s("hi-IN") + s("Do not enter Sector 13 before 18:30.")
    out += bytes([3])
    for typ, start, end, conf, value in [
        (0, 0, 7, 92, "Do not"),
        (5, 8, 17, 82, "Sector 13"),
        (3, 25, 30, 94, "18:30"),
    ]:
        out += bytes([typ]) + struct.pack(">HHB", start, end, conf) + s(value)
    return bytes(out)

if __name__ == "__main__":
    print(encode().hex())
