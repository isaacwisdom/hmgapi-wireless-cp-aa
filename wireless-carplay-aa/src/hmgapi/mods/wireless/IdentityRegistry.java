package hmgapi.mods.wireless;

import java.util.LinkedHashMap;
import java.util.Map;

import hmgapi.mods.wireless.api.CarPlayIdentification;

/** Process-local, validated contributions consumed by the native serializer. */
final class IdentityRegistry implements CarPlayIdentification {
    private final Map<String, VehicleIdentity> vehicles = new LinkedHashMap<String, VehicleIdentity>();
    private Runnable changed;

    @Override
    public void registerVehicle(String modId, VehicleIdentity vehicle) {
        checkModId(modId);
        if (vehicle == null) throw new IllegalArgumentException("vehicle");
        Runnable notify;
        synchronized (this) {
            if (!vehicles.isEmpty() && !vehicles.containsKey(modId))
                throw new IllegalStateException("CarPlay vehicle identity already belongs to " + vehicles.keySet());
            vehicles.put(modId, vehicle); notify = changed;
        }
        if (notify != null) notify.run();
    }

    @Override
    public void removeVehicle(String modId) {
        checkModId(modId);
        Runnable notify;
        synchronized (this) { vehicles.remove(modId); notify = changed; }
        if (notify != null) notify.run();
    }

    /** Exactly one vehicle identity may win; ambiguity is an installation/configuration error. */
    synchronized VehicleIdentity vehicle() {
        if (vehicles.isEmpty()) return null;
        if (vehicles.size() != 1)
            throw new IllegalStateException("conflicting CarPlay vehicle identity contributors: " + vehicles.keySet());
        return vehicles.values().iterator().next();
    }

    synchronized void onChange(Runnable listener) { changed = listener; }

    private static void checkModId(String modId) {
        if (modId == null || modId.length() == 0 || modId.length() > 128 || modId.indexOf(0) >= 0)
            throw new IllegalArgumentException("modId");
    }
}
