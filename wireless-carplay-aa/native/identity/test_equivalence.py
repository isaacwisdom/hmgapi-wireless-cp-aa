#!/usr/bin/env python3
"""Run the real ARM serializer in QEMU and compare it with EV Routing's former output."""
from pathlib import Path
import os, re, subprocess, tempfile

HERE=Path(__file__).resolve().parent
CLANG=os.environ.get('CLANG','clang'); LLD=os.environ.get('LLD','ld.lld')
QEMU=os.environ.get('QEMU','qemu-system-arm')
OBJCOPY=os.environ.get('OBJCOPY','llvm-objcopy')
JAVA_HOME=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home'))
with tempfile.TemporaryDirectory() as d:
    d=Path(d); obj=d/'h.o'; elf=d/'h.elf'; script=d/'arm.ld'
    gate_obj=d/'gate.o';gate_elf=d/'gate.elf';gate_bin=d/'gate.bin'
    subprocess.run([CLANG,'--target=armv7-none-eabi','-mthumb','-c',HERE/'recipe_gate.s','-o',gate_obj],check=True)
    subprocess.run([LLD,'--image-base=0','-Ttext=0','-e','gate','-o',gate_elf,gate_obj],check=True)
    subprocess.run([OBJCOPY,'--dump-section','.text='+str(gate_bin),gate_elf],check=True)
    recipe=(HERE.parents[1]/'recipes/carplay-identification-fc7.recipe').read_text()
    expected_gate=bytes.fromhex(re.search(r'^segment 0x500000 ([0-9a-f]+)$',recipe,re.M).group(1))
    if gate_bin.read_bytes()!=expected_gate:raise SystemExit('recipe gate differs from its reviewed ARM source')
    print('PASS recipe gate is byte-for-byte identical to its reviewed ARM source')
    arm=['--target=armv7a-linux-androideabi','-marm','-march=armv7-a','-mfloat-abi=soft','-O0','-ffreestanding','-fno-builtin']
    subprocess.run([CLANG,*arm,'-I'+str(HERE/'compat'),'-I'+str(JAVA_HOME/'include'),'-I'+str(JAVA_HOME/'include/darwin'),'-c',HERE/'equivalence_harness.c','-o',obj],check=True)
    script.write_text('SECTIONS { . = 0x00010000; .text : { *(.text*) } .rodata : { *(.rodata*) } .data : { *(.data*) } .bss : { *(.bss*) *(COMMON) } }')
    subprocess.run([LLD,'-static','-T',script,'-e','_start','-o',elf,obj],check=True)
    subprocess.run([QEMU,'-M','versatilepb,audiodev=audio0','-audiodev','none,id=audio0','-cpu','cortex-a9','-nographic','-semihosting-config','enable=on,target=native','-kernel',elf],check=True,timeout=20)
