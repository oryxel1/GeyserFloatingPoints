package org.oryxel.gfp.util;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.util.MathUtils;

public class MathUtil {
    // Make sure offset not going to mess chunk up and BLOCK POSITION WILL BE 1:1 TO THE SERVER.
    public static Vector3i makeOffsetChunkSafe(Vector3d offset) {
        long l = MathUtils.chunkPositionToLong(offset.getFloorX() >> 4, offset.getFloorZ() >> 4);
        int reversedX = getX(l) << 4, reversedZ = getZ(l) << 4;
        return Vector3i.from(reversedX, 0, reversedZ);
    }

    // Same as makeOffsetChunkSafe
    public static double findNewPosition(double position) {
        double sign = Math.signum(position);

        while (Math.abs(position) > 2000) {
            position = Math.sqrt(Math.abs(position));
        }

        return position * sign;
    }

    public static int getX(long l) {
        return (int)(l >>> 32 & 0xFFFFFFFFL);
    }

    public static int getZ(long l) {
        return (int)(l & 0xFFFFFFFFL);
    }
}
