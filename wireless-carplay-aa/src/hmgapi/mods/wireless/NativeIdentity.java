package hmgapi.mods.wireless;

/** JNI boundary for the verified libiap2session.so identification serializer hook. */
final class NativeIdentity {
    private NativeIdentity() {}
    /** Load through Runtime so HMG-API can apply the queued in-memory recipe. */
    static void loadTarget() { System.load("/system/lib/libiap2session.so"); }
    static void load() { System.load("/system/lib/libcarplayidentity.so"); }
    static native int configure(byte[] displayName, int engineType, int dcConnector,
            int dcPowerW, int acConnector, int acPowerW, byte[] themeIdentity);
    static native void shutdown();
}
