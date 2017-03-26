package main.gcs.network;

import com.MAVLink.MAVLinkPacket;
import main.gcs.interfaces.PacketHandler;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UdpMavWorker extends Thread {
    public static final int PACKET_SIZE = 512;
    private DatagramSocket udpSocket;
    private List<PacketHandler> packetHandlers = new ArrayList<>();

    public UdpMavWorker(int udpServerPort) throws SocketException {
        udpSocket = new DatagramSocket(udpServerPort);
    }

    public void addPacketHandler(PacketHandler packetHandler) {
        Collections.synchronizedCollection(packetHandlers).add(packetHandler);
    }

    public void deletePacketHandler(PacketHandler packetHandler) {
        Collections.synchronizedCollection(packetHandlers).remove(packetHandler);
    }

    @Override
    public void run() {
        byte[] bytePacket = new byte[PACKET_SIZE];
        DatagramPacket inputPacket = new DatagramPacket(bytePacket, bytePacket.length);
        try {
            while (!isInterrupted()) {
                udpSocket.setSoTimeout(5000);
                udpSocket.receive(inputPacket);
                MAVLinkPacket packet = PacketParser.parse(bytePacket);
                if (packet != null) {
                    if (packetHandlers.size() > 0) {
                        IpPortAddress senderAddress = new IpPortAddress(inputPacket.getAddress(), inputPacket.getPort());

                        for (PacketHandler packetHandler : Collections.synchronizedCollection(packetHandlers)) {
                            if (packetHandler.getVehicleAddress().equals(senderAddress)) {
                                packetHandler.handlePacket(packet);
                            }
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
        System.out.println("Exit");
    }

    public void sendPacket(IpPortAddress receiver, MAVLinkPacket packet) {
        new Thread(() -> {
            byte[] data = packet.encodePacket();
            DatagramPacket outputPacket = new DatagramPacket(data, data.length, receiver.getAddress(), receiver.getPort());
            try {
                udpSocket.send(outputPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
