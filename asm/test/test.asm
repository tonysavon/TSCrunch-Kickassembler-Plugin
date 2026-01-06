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

