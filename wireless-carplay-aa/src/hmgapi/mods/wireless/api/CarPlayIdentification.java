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
    int API_VERSION = 1;

    /** Register or replace one mod's vehicle identity contribution. */
    void registerVehicle(String modId, VehicleIdentity vehicle);

    /** Remove a mod's vehicle contribution, for example when it is disabled. */
    void removeVehicle(String modId);

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
