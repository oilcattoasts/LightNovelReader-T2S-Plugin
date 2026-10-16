#!/usr/bin/env python3
"""Check the Android DEX ABI that JVM unit tests cannot exercise (no dependencies)."""
import argparse
import struct
from zipfile import ZipFile


class Dex:
    def __init__(self, data):
        if not data.startswith(b"dex\n"):
            raise ValueError("Not a DEX file")
        self.data = data
        self.strings = self.u32(0x3C)
        self.types = self.u32(0x44)
        self.protos = self.u32(0x4C)
        self.methods = self.u32(0x5C)

    def u32(self, offset):
        return struct.unpack_from("<I", self.data, offset)[0]

    def uleb(self, offset):
        result = shift = 0
        while True:
            value = self.data[offset]
            offset += 1
            result |= (value & 127) << shift
            if value < 128:
                return result, offset
            shift += 7

    def string(self, index):
        offset = self.u32(self.strings + index * 4)
        _, offset = self.uleb(offset)
        # ABI names/descriptors are ASCII, independent of DEX modified UTF-8.
        return self.data[offset:self.data.index(b"\0", offset)].decode("utf-8", errors="replace")

    def type(self, index):
        return self.string(self.u32(self.types + index * 4))

    def method(self, index):
        owner, proto, name = struct.unpack_from("<HHI", self.data, self.methods + index * 8)
        proto_offset = self.protos + proto * 12
        returns = self.type(self.u32(proto_offset + 4))
        parameters = self.u32(proto_offset + 8)
        arguments = []
        if parameters:
            count = self.u32(parameters)
            for i in range(count):
                arguments.append(self.type(struct.unpack_from("<H", self.data, parameters + 4 + i * 2)[0]))
        return self.type(owner), self.string(name), "(" + "".join(arguments) + ")" + returns

    def defined(self):
        for i in range(self.u32(0x60)):
            entry = self.u32(0x64) + i * 32
            owner = self.type(self.u32(entry))
            offset = self.u32(entry + 24)
            methods = []
            if offset:
                counts = []
                for _ in range(4):
                    count, offset = self.uleb(offset)
                    counts.append(count)
                for _ in range(counts[0] + counts[1]):
                    _, offset = self.uleb(offset)
                    _, offset = self.uleb(offset)
                for count in counts[2:]:
                    index = 0
                    for _ in range(count):
                        delta, offset = self.uleb(offset)
                        _, offset = self.uleb(offset)  # access flags
                        _, offset = self.uleb(offset)  # code offset
                        index += delta
                        methods.append(self.method(index))
            yield owner, methods


def verify(path):
    defined = {}
    with ZipFile(path) as apk:
        if apk.testzip() is not None:
            raise ValueError("Corrupt APK archive")
        for name in apk.namelist():
            if name.startswith("classes") and name.endswith(".dex"):
                defined.update(Dex(apk.read(name)).defined())
        for name in ("assets/opencc/TSCharacters.txt", "assets/opencc/TSPhrases.txt"):
            if not apk.read(name):
                raise ValueError("Missing dictionary: " + name)
    if any(name.startswith("Lio/nightfish/lightnovelreader/api/") for name in defined):
        raise ValueError("APK must use the host API instead of packaging its own copy")
    owner = "Lorg/lnr/t2s/ConversionProcessor$BookView;"
    methods = defined.get(owner, [])
    actual = [signature for _, name, signature in methods if name == "getLastUpdated"]
    expected = "()Lj$/time/LocalDateTime;"
    if actual != [expected]:
        raise ValueError("Host expects getLastUpdated" + expected + "; APK defines " + str(actual))
    print("PASS: BookView.getLastUpdated" + expected)
    print("PASS: DEX contains no duplicate host API; dictionaries are present")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk")
    try:
        verify(parser.parse_args().apk)
    except (ValueError, KeyError) as error:
        parser.exit(1, "FAIL: " + str(error) + "\n")
