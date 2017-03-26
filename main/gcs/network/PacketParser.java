package main.gcs.network;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.sun.istack.internal.Nullable;

import java.io.IOException;

public class PacketParser {
    private static Parser parser = new Parser();

    @Nullable
    public static MAVLinkPacket parse(byte[] bytes) throws IOException {
        MAVLinkPacket pac;

        for (byte b : bytes) {
            pac = parser.mavlink_parse_char(b & 0xFF);
            if (pac != null) {
                return pac;
            }
        }

        return null;
    }
}
