# TSCrunch KickAssembler Plugin

This project provides a KickAssembler plugin archive that exposes the TSCrunch
packer as a modifier. It is a thin wrapper around the Java port of TSCrunch and
is intended for single-block use inside KickAssembler.

For the standalone cruncher itself and details on the format, see the TSCrunch project:
https://github.com/tonysavon/TSCrunch

## Features

- TS modifier for KickAssembler (single block only)
- Regular crunching, in-place crunching, and SFX output
- Segment modifier and .modify usage styles

The parser uses the same cycle-aware cost model as the standalone C, Go and
Java encoders: packed size takes priority, then estimated 6502 decoder cycles
select between equal-size parses. The compressed format and assembly decoders
are unchanged. Across the six-game TSCrunch benchmark, packed size remains
150,888 bytes and the extreme decoder runs 0.49% faster.

## Install

Copy the built plugin jar into your KickAssembler classpath and load it:

```
.plugin "tscrunch.kickass.CruncherPlugins"
```

## Assemble With KickAssembler

From the command line, include the plugin jar on the KickAssembler classpath:

```
java -cp "kickass.jar;tscrunch-kickass-plugin-1.0.1.jar" cml.kickass.KickAssembler yourfile.asm
```

Adjust paths as needed if your jar or `KickAss.jar` live elsewhere.

## Quick Start

```
.plugin "tscrunch.kickass.CruncherPlugins"

.label crunched = *
.modify TS() {
    .pc = $1000 "Payload"
    .fill 512, i & $ff
}
```

## Usage

### Modifier

```
.modify TS(inplace, sfx, jmpAddress) { ... }
```

- `inplace` (default: false)
- `sfx` (default: false)
- `jmpAddress` (default: start address of the first block. Relevant only if sfx = True)

### Segment modifier

```
.file [name="out.prg", segments="Code", modify="TS", _inplace=true]
```

For SFX:

```
.file [name="sfx.prg", segments="Code", modify="TS", _sfx=true, _jmpAdress=$0820]
```

Note: SFX output is written as a standard PRG with load address $0801.

## Examples

### Basic crunching

```
.plugin "tscrunch.kickass.CruncherPlugins"

.label crunchedData = *
.modify TS() {
    .pc = $2000 "Charset"
    .import binary "chars.bin"
}
```

This creates a single crunched block at the current program counter. Use this
for the simplest case where you do not need explicit sizing variables.

### In-place crunching

```
.plugin "tscrunch.kickass.CruncherPlugins"

* = $0820 "Main"
    
    :TS_DECRUNCH(test_data) // No target address needed
    
    jmp *

#define INPLACE
#import "decrunch.asm"

.pc = $10e6 "test data" //the load address will be provided by the plugin upon first crunching the file
test_data:
.modify TS(true, false) {
    .pc = $1000 "Payload"
    .import binary "payload.bin"
}
```

In-place crunching stores the compressed data within the same memory region
that will be decompressed. This can reduce load time but requires enough free
space at the end of the data block for the cruncher to operate safely.

### SFX (self-extracting PRG)

```
.plugin "tscrunch.kickass.CruncherPlugins"

.segment Code [start = $0820]
.import c64 "game.prg"

.file [name="sfx.prg", segments="Code", modify="TS", _sfx=true, _jmpAdress=$1000]
```

### Practical example (Koala picture)

This example loads a Koala picture, crunches three blocks, then decrunches
to bitmap, screen, and color RAM.

```
.plugin "tscrunch.kickass.CruncherPlugins"

.const picture = LoadBinary("picture.kla", BF_KOALA)

:BasicUpstart2(main)

.pc = $0810 "main"
main:

        :TS_DECRUNCH(bmpdata , $6000)
        :TS_DECRUNCH(scrdata , $4000)
        :TS_DECRUNCH(coldata , $d800)

        lda #0
        sta $d020
        lda #picture.getBackgroundColor()
        sta $d021

        lda #$02
        sta $dd00
        lda #%00001000
        sta $d018
        lda #$3b
        sta $d011
        lda #$d8
        sta $d016

        jmp *

#import "decrunch.asm"


scrdata:
.modify TS()
{
        .fill picture.getScreenRamSize(), picture.getScreenRam(i)
}

coldata:
.modify TS()
{
        .fill picture.getScreenRamSize(), picture.getColorRam(i)
}

bmpdata:
.modify TS()
{
        .fill picture.getBitmapSize(), picture.getBitmap(i)
}
```

## Build

This project depends on the KickAssembler jar. Place it in the repo root as:

```
KickAss.jar
```

Then build:

```
mvn clean package
```

The built plugin jar will be placed in `target/`.

To check the built archive through KickAssembler in raw, in-place and SFX modes:

```
python tools/verify_plugin.py path/to/game.prg
```

## License

MIT (see LICENSE-MIT.txt)
