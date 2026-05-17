package oxy.geyser.fp.util;

import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import oxy.geyser.fp.GeyserFloatingPoints;

public class MathUtil {
    public static Vector3i calculateOffset(Vector3d position) {
        final int CAPPED_VALUE = GeyserFloatingPoints.config().maxPosition();

        int x = GenericMath.floor(relative(position.getX()));
        int z = GenericMath.floor(relative(position.getZ()));

        int offsetX = (position.getFloorX() - x) >> 4;
        int offsetZ = (position.getFloorZ() - z) >> 4;

        return Vector3i.from(Math.abs(position.getX()) > CAPPED_VALUE ? offsetX << 4 : 0, 0, Math.abs(position.getZ()) > CAPPED_VALUE ? offsetZ << 4 : 0);
    }

    private static double relative(double position) {
        double sign = Math.signum(position);

        while (Math.abs(position) > 100) {
            position = Math.sqrt(Math.abs(position));
        }

        return position * sign;
    }
}
