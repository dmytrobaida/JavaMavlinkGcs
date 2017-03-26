package main.gcs.interfaces;

import com.MAVLink.MAVLinkPacket;
import main.gcs.network.IpPortAddress;

public interface PacketHandler {
    void handlePacket(MAVLinkPacket packet);
    IpPortAddress getVehicleAddress();
}
