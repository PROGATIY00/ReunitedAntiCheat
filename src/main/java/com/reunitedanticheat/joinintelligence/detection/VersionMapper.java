package com.reunitedanticheat.joinintelligence.detection;

import java.util.HashMap;
import java.util.Map;

public class VersionMapper {

    private static final Map<Integer, String> PROTOCOL_VERSIONS = new HashMap<>();

    static {
        PROTOCOL_VERSIONS.put(47, "1.8.x");
        PROTOCOL_VERSIONS.put(107, "1.9");
        PROTOCOL_VERSIONS.put(108, "1.9.1");
        PROTOCOL_VERSIONS.put(109, "1.9.2");
        PROTOCOL_VERSIONS.put(110, "1.9.3-1.9.4");
        PROTOCOL_VERSIONS.put(201, "1.10");
        PROTOCOL_VERSIONS.put(202, "1.10.1");
        PROTOCOL_VERSIONS.put(203, "1.10.2");
        PROTOCOL_VERSIONS.put(204, "1.10.x");
        PROTOCOL_VERSIONS.put(210, "1.10.x");
        PROTOCOL_VERSIONS.put(301, "1.11");
        PROTOCOL_VERSIONS.put(302, "1.11.1");
        PROTOCOL_VERSIONS.put(303, "1.11.2");
        PROTOCOL_VERSIONS.put(304, "1.11.x");
        PROTOCOL_VERSIONS.put(315, "1.11.x");
        PROTOCOL_VERSIONS.put(316, "1.11.x");
        PROTOCOL_VERSIONS.put(335, "1.12");
        PROTOCOL_VERSIONS.put(338, "1.12.1");
        PROTOCOL_VERSIONS.put(340, "1.12.2");
        PROTOCOL_VERSIONS.put(393, "1.13");
        PROTOCOL_VERSIONS.put(401, "1.13.1");
        PROTOCOL_VERSIONS.put(404, "1.13.2");
        PROTOCOL_VERSIONS.put(477, "1.14");
        PROTOCOL_VERSIONS.put(478, "1.14.1");
        PROTOCOL_VERSIONS.put(480, "1.14.2");
        PROTOCOL_VERSIONS.put(481, "1.14.3");
        PROTOCOL_VERSIONS.put(482, "1.14.4");
        PROTOCOL_VERSIONS.put(485, "1.14.x");
        PROTOCOL_VERSIONS.put(490, "1.14.x");
        PROTOCOL_VERSIONS.put(498, "1.14.x");
        PROTOCOL_VERSIONS.put(573, "1.15");
        PROTOCOL_VERSIONS.put(575, "1.15.1");
        PROTOCOL_VERSIONS.put(576, "1.15.2");
        PROTOCOL_VERSIONS.put(578, "1.15.x");
        PROTOCOL_VERSIONS.put(735, "1.16");
        PROTOCOL_VERSIONS.put(736, "1.16.1");
        PROTOCOL_VERSIONS.put(751, "1.16.2");
        PROTOCOL_VERSIONS.put(752, "1.16.3");
        PROTOCOL_VERSIONS.put(753, "1.16.4");
        PROTOCOL_VERSIONS.put(754, "1.16.5");
        PROTOCOL_VERSIONS.put(755, "1.17");
        PROTOCOL_VERSIONS.put(756, "1.17.1");
        PROTOCOL_VERSIONS.put(757, "1.18-1.18.1");
        PROTOCOL_VERSIONS.put(758, "1.18.2");
        PROTOCOL_VERSIONS.put(759, "1.19");
        PROTOCOL_VERSIONS.put(760, "1.19.1-1.19.2");
        PROTOCOL_VERSIONS.put(761, "1.19.3");
        PROTOCOL_VERSIONS.put(762, "1.19.4");
        PROTOCOL_VERSIONS.put(763, "1.20-1.20.1");
        PROTOCOL_VERSIONS.put(764, "1.20.2");
        PROTOCOL_VERSIONS.put(765, "1.20.3-1.20.4");
        PROTOCOL_VERSIONS.put(766, "1.20.5-1.20.6");
        PROTOCOL_VERSIONS.put(767, "1.21-1.21.1");
        PROTOCOL_VERSIONS.put(768, "1.21.2-1.21.3");
        PROTOCOL_VERSIONS.put(769, "1.21.4");
    }

    public static String getVersion(int protocolVersion) {
        return PROTOCOL_VERSIONS.getOrDefault(protocolVersion, protocolVersion + " (unknown)");
    }

    public static boolean isValidProtocol(int protocolVersion) {
        return protocolVersion > 0;
    }
}
