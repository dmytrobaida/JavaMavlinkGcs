package main.gcs;

import com.MAVLink.Messages.MAVLinkMessage;
import main.gcs.exceptions.GcsNotListeningException;
import main.gcs.interfaces.ConnectionHandler;
import main.gcs.interfaces.MessageSender;
import main.gcs.network.IpPortAddress;
import main.gcs.network.UdpMavWorker;

import java.net.SocketException;

public class Gcs {
    private UdpMavWorker udpMavWorker;
    private final int sysid = 213;
    private final int compid = 1;
    private boolean listening = false;

    public Gcs(int port) throws SocketException {
        udpMavWorker = new UdpMavWorker(port);
    }

    public void startListening() {
        udpMavWorker.start();
        listening = true;
    }

    public void stopListening() {
        udpMavWorker.interrupt();
        listening = false;
    }

    public void connectToVehicle(IpPortAddress vehicleAddress, ConnectionHandler connectionHandler) throws GcsNotListeningException {
        if (listening) {
            Vehicle vehicle = new Vehicle(vehicleAddress, connectionHandler, new MessageSender() {
                @Override
                public void sendMessage(Vehicle vehicle, MAVLinkMessage message) {
                    message.sysid = sysid;
                    message.compid = compid;
                    if (udpMavWorker != null) {
                        udpMavWorker.sendPacket(vehicle.getVehicleAddress(), message.pack());
                    }
                }
            });
            udpMavWorker.addPacketHandler(vehicle);
            vehicle.connect();
        } else {
            throw new GcsNotListeningException();
        }
    }

    public void disconnectVehicle(Vehicle vehicle) {
        udpMavWorker.deletePacketHandler(vehicle);
    }
}
