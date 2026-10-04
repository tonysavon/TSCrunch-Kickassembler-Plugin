#!/usr/bin/env python3
"""Verify the built plugin through KickAssembler in raw, inplace and SFX modes."""
import argparse
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]


def run(command):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("prg", type=pathlib.Path, nargs="?", default=ROOT.parent / "kabutocrunch/game.prg")
    parser.add_argument("--kickass-jar", type=pathlib.Path, default=ROOT / "KickAss.jar")
    parser.add_argument("--plugin-jar", type=pathlib.Path, default=ROOT / "target/tscrunch-kickass-plugin-1.0.1.jar")
    args = parser.parse_args()
    prg = args.prg.resolve()
    data = prg.read_bytes()
    address = int.from_bytes(data[:2], "little")
    classpath = str(args.kickass_jar.resolve()) + (";" if __import__("os").name == "nt" else ":") + str(args.plugin_jar.resolve())
    with tempfile.TemporaryDirectory(prefix="tscrunch-plugin-test-") as temp_name:
        temp = pathlib.Path(temp_name)
        for mode in ("raw", "inplace", "sfx"):
            expected = temp / "expected.bin"
            flags = {"raw": ["-p"], "inplace": ["-i"], "sfx": ["-x", f"0x{address:04x}"]}[mode]
            run(["java", "-cp", args.plugin_jar.resolve(), "tscrunch.TSCrunch", "-q", *flags, prg, expected])
            expected_bytes = expected.read_bytes()
            start = 0x0200 if mode == "raw" else int.from_bytes(expected_bytes[:2], "little")
            options = {"raw": "", "inplace": "true, false", "sfx": f"false, true, ${address:04x}"}[mode]
            source = temp / "plugin.asm"
            output = temp / "plugin.prg"
            source.write_text(
                '.plugin "tscrunch.kickass.CruncherPlugins"\n'
                f'.pc = ${start:04x}\n.modify TS({options}) {{\n'
                f'    .pc = ${address:04x}\n    .import c64 "{prg.as_posix()}"\n}}\n')
            run(["java", "-cp", classpath, "cml.kickass.KickAssembler", source, "-o", output])
            actual = output.read_bytes()
            if mode == "raw":
                actual = actual[2:]
            if actual != expected_bytes:
                raise RuntimeError(f"KickAssembler output differs from encoder: {mode}")
            print(f"{mode}: KickAssembler plugin output matches encoder ({len(actual)} bytes)", flush=True)


if __name__ == "__main__":
    main()
