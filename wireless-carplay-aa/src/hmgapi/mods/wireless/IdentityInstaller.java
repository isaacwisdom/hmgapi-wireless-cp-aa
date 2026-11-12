package hmgapi.mods.wireless;

import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import hmgapi.hook.Points;
import hmgapi.mods.wireless.api.CarPlayIdentification;

/** Arms the native serializer from one validated snapshot of all contributions. */
final class IdentityInstaller {
    private IdentityInstaller() {}
    private static boolean loaded;

    static void install(final ModContext mod, final LoadedApp app, IdentityRegistry registry) {
        final IdentityRegistry contributions = registry;
        registry.onChange(new Runnable() {
            @Override public void run() {
                try {
                    apply(contributions);
                    if (contributions.vehicle() != null) enableGroups(app.classLoader);
                }
                catch (Throwable t) { mod.error("CarPlay identification contribution change", t); }
            }
        });
        // The verified gate is safe while unarmed and permits consumers loaded live later.
        mod.nativePatch(app, "libiap2session.so");
        apply(registry);
        MethodHook groups = new MethodHook() {
            @Override protected void afterHookedMethod(Param p) throws Throwable {
                if (contributions.vehicle() != null) enableGroups(app.classLoader);
            }
        };
        mod.hook(app.classLoader, "iap2.init_info.check_supports_features", groups);
        try { if (registry.vehicle() != null) enableGroups(app.classLoader); }
        catch (Exception e) { throw new IllegalStateException("CarPlay identification groups", e); }
        mod.log("CarPlay identification: contribution serializer installed");
    }

    private static synchronized void apply(IdentityRegistry registry) {
        CarPlayIdentification.VehicleIdentity vehicle = registry.vehicle();
        CarPlayIdentification.ThemeIdentity theme = registry.theme();
        if (vehicle == null && theme == null) { if (loaded) NativeIdentity.shutdown(); return; }
        int dcType = -1, dcPower = 0, acType = -1, acPower = 0;
        if (vehicle != null) {
            for (CarPlayIdentification.Connector connector : vehicle.connectors) {
                if (connector.type == CarPlayIdentification.VehicleIdentity.CONNECTOR_AC) {
                    if (acType != -1) throw new IllegalStateException("more than one CarPlay AC connector");
                    acType = connector.type; acPower = connector.maximumPowerW;
                } else {
                    if (dcType != -1) throw new IllegalStateException("more than one CarPlay DC connector");
                    dcType = connector.type; dcPower = connector.maximumPowerW;
                }
            }
            if (dcType == -1 || acType == -1)
                throw new IllegalStateException("CarPlay EV identity requires one DC and one AC connector");
        }
        if (!loaded) { NativeIdentity.load(); loaded = true; }
        try {
            byte[] name = vehicle == null ? new byte[0] : vehicle.displayName.getBytes("UTF-8");
            byte[] themeId = theme == null ? new byte[0] : theme.encoded().getBytes("US-ASCII");
            int rc = NativeIdentity.configure(name, vehicle == null ? -1 : vehicle.engineType,
                    dcType, dcPower, acType, acPower, themeId);
            if (rc != 0) throw new IllegalStateException("native CarPlay identification=" + rc);
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void enableGroups(ClassLoader cl) throws Exception {
        Points.field(cl, "iap2.init_info.use_vehicle_information").setInt(null, 1);
        Points.field(cl, "iap2.init_info.use_vehicle_status").setInt(null, 1);
    }
}
