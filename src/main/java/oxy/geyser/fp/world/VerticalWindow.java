package oxy.geyser.fp.world;

import oxy.geyser.fp.GeyserFloatingPoints;

public final class VerticalWindow {
    public static final int BEDROCK_MIN_Y = -64;
    public static final int BEDROCK_MAX_Y = 319;
    public static final int BEDROCK_MAX_Y_EXCLUSIVE = 320;
    public static final int BEDROCK_SECTION_COUNT = 24;
    public static final int BEDROCK_HEIGHT = 384;
    public static final int TARGET_FAKE_Y = 128;

    private VerticalWindow() {
    }

    public static int reOffsetLowerBound() {
        return TARGET_FAKE_Y - GeyserFloatingPoints.config().effectiveVerticalRemapThreshold();
    }

    public static int reOffsetUpperBound() {
        return TARGET_FAKE_Y + GeyserFloatingPoints.config().effectiveVerticalRemapThreshold();
    }

    public static boolean isNativeBedrockY(double realY) {
        return realY >= BEDROCK_MIN_Y && realY <= BEDROCK_MAX_Y;
    }

    public static boolean needsVerticalRemap(double realY) {
        return !isNativeBedrockY(realY);
    }

    public static int realViewMinY(int offsetY) {
        return BEDROCK_MIN_Y + offsetY;
    }

    public static int sourceStartSection(int worldMinY, int offsetY) {
        return (realViewMinY(offsetY) - worldMinY) >> 4;
    }
}