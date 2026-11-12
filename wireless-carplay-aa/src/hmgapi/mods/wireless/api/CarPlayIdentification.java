package hmgapi.mods.wireless.api;

/**
 * Contributions to the iAP2 accessory identification used by CarPlay.
 *
 * The Wireless mod owns serialization and the native patch. Other mods retain
 * ownership of the facts they contribute. Values are semantic rather than raw
 * iAP2 group/parameter bytes so the serializer can evolve independently.
 */
public interface CarPlayIdentification {
    String SERVICE = "carplay-identification";
    int API_VERSION = 2;

    /** Register or replace one mod's vehicle identity contribution. */
    void registerVehicle(String modId, VehicleIdentity vehicle);

    /** Remove a mod's vehicle contribution, for example when it is disabled. */
    void removeVehicle(String modId);

    /** Register or replace one mod's CarPlay theme-asset capability. */
    void registerTheme(String modId, ThemeIdentity theme);

    /** Remove a mod's theme contribution without affecting other identity fields. */
    void removeTheme(String modId);

    /** A theme asset known to the phone, kept separate from any mod's display implementation. */
    final class ThemeIdentity {
        public final String assetIdentifier;
        public final long assetVersion;
        public final String sdkVersion;

        public ThemeIdentity(String assetIdentifier, long assetVersion, String sdkVersion) {
            ascii("assetIdentifier", assetIdentifier, 1, 64, false);
            if (assetVersion < 0 || assetVersion > 0xffffffffL)
                throw new IllegalArgumentException("assetVersion");
            ascii("sdkVersion", sdkVersion, 1, 24, true);
            String encoded = assetIdentifier + ":" + assetVersion + ":" + sdkVersion;
            if (encoded.length() > 96) throw new IllegalArgumentException("encoded theme identity");
            this.assetIdentifier = assetIdentifier;
            this.assetVersion = assetVersion;
            this.sdkVersion = sdkVersion;
        }

        /** Canonical form used by Apple's ThemeAssetIdentifier identification parameter. */
        public String encoded() { return assetIdentifier + ":" + assetVersion + ":" + sdkVersion; }

        private static void ascii(String field, String value, int min, int max, boolean dot) {
            if (value == null || value.length() < min || value.length() > max)
                throw new IllegalArgumentException(field);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                boolean ok = c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                        || c == '-' || c == '_' || dot && c == '.';
                if (!ok) throw new IllegalArgumentException(field);
            }
        }
    }

    /** Immutable EV identity fields represented in the iAP2 VehicleInformation group. */
    final class VehicleIdentity {
        public static final int ENGINE_GASOLINE = 0;
        public static final int ENGINE_DIESEL = 1;
        public static final int ENGINE_ELECTRIC = 2;
        public static final int ENGINE_CNG = 3;
        public static final int CONNECTOR_CCS = 0;
        public static final int CONNECTOR_CHADEMO = 1;
        public static final int CONNECTOR_AC = 2;

        public final String displayName;
        public final int engineType;
        public final Connector[] connectors;

        public VehicleIdentity(String displayName, int engineType, Connector[] connectors) {
            if (displayName == null || displayName.length() == 0 || displayName.length() > 320
                    || displayName.indexOf(0) >= 0)
                throw new IllegalArgumentException("displayName");
            if (engineType < ENGINE_GASOLINE || engineType > ENGINE_CNG)
                throw new IllegalArgumentException("engineType");
            if (connectors == null || connectors.length == 0)
                throw new IllegalArgumentException("connectors");
            this.displayName = displayName;
            this.engineType = engineType;
            this.connectors = connectors.clone();
            for (Connector connector : this.connectors)
                if (connector == null) throw new IllegalArgumentException("connector");
        }
    }

    /** One physical charging connector and its advertised maximum input power. */
    final class Connector {
        public final int type;
        public final int maximumPowerW;

        public Connector(int type, int maximumPowerW) {
            if (type < VehicleIdentity.CONNECTOR_CCS || type > VehicleIdentity.CONNECTOR_AC)
                throw new IllegalArgumentException("connector type");
            if (maximumPowerW < 1 || maximumPowerW > 1000000)
                throw new IllegalArgumentException("maximumPowerW");
            this.type = type;
            this.maximumPowerW = maximumPowerW;
        }
    }
}
