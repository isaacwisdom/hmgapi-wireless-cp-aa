#!/bin/sh
# Build the generic CarPlay identification serializer owned by wireless-carplay-aa.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
OUT=${1:-$HERE/../../files/libcarplayidentity.so}
case "$OUT" in /*) ;; *) OUT=$(pwd)/$OUT ;; esac
CLANG=${CLANG:-clang}
JAVA_HOME=${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}
case $(uname -s) in Darwin) JNI_OS=darwin;; *) JNI_OS=linux;; esac
T="--target=armv7a-linux-androideabi -mthumb -march=armv7-a -mfloat-abi=soft"
W=$(mktemp -d);trap 'rm -rf "$W"' EXIT
printf '%s\n' 'int mprotect(void *p,unsigned n,int f){return 0;} int pthread_mutex_lock(void *p){return 0;} int pthread_mutex_unlock(void *p){return 0;}' > "$W/c.c"
printf '%s\n' 'void *dlopen(const char *p,int f){return 0;} void *dlsym(void *h,const char *s){return 0;}' > "$W/dl.c"
for s in c:libc dl:libdl; do
    "$CLANG" $T -shared -nostdlib -fuse-ld=lld -Wl,-soname,${s#*:}.so -o "$W/${s#*:}.so" "$W/${s%%:*}.c"
done
(cd "$W" && "$CLANG" $T -Os -fPIC -ffreestanding -fno-builtin -fvisibility=hidden \
    -I"$HERE/compat" -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/$JNI_OS" -Wall -Wextra -Werror -shared -nostdlib -fuse-ld=lld \
    -Wl,-z,now -Wl,--no-rosegment -Wl,--hash-style=sysv -Wl,--build-id=none \
    -Wl,-soname,libcarplayidentity.so -o "$OUT" "$HERE/identity.c" libc.so libdl.so)
md5sum "$OUT"
