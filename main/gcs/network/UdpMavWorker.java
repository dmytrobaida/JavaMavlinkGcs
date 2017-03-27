package main.gcs.network;

import com.MAVLink.MAVLinkPacket;
import main.gcs.interfaces.PacketHandler;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class UdpMavWorker extends Thread {
    public static final int PACKET_SIZE = 512;
    private DatagramSocket udpSocket;
    private List<PacketHandler> packetHandlers = new ArrayList<>();
    private Thread currentThread;
    private Stack<PacketHandler> itemsToAdd = new Stack<>();
    private Stack<PacketHandler> itemsToDelete = new Stack<>();

    public UdpMavWorker(int udpServerPort) throws SocketException {
        udpSocket = new DatagramSocket(udpServerPort);
    }

    public void addPacketHandler(PacketHandler packetHandler) {
//        Collections.synchronizedList(packetHandlers).add(packetHandler);
        itemsToAdd.push(packetHandler);
    }

    public void deletePacketHandler(PacketHandler packetHandler) {
        // Collections.synchronizedList(packetHandlers).remove(packetHandler);
        itemsToDelete.push(packetHandler);
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
                    IpPortAddress senderAddress = new IpPortAddress(inputPacket.getAddress(), inputPacket.getPort());
                    ListIterator<PacketHandler> iterator = packetHandlers.listIterator();
                    if (itemsToAdd.size() > 0) {
                        iterator.add(itemsToAdd.pop());
                    }

                    while (iterator.hasNext()) {
                        PacketHandler packetHandler = iterator.next();
                        if (itemsToDelete.size() > 0 && packetHandler == itemsToDelete.peek()) {
                            iterator.remove();
                            itemsToDelete.pop();
                        } else {
                            if (packetHandler.getVehicleAddress().equals(senderAddress)) {
                                packetHandler.handlePacket(packet);
                            }
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }

    }

    public void sendPacket(IpPortAddress receiver, MAVLinkPacket packet) {
        byte[] data = packet.encodePacket();

        if (currentThread != null) {
            try {
                currentThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        currentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramPacket outputPacket = new DatagramPacket(data, data.length, receiver.getAddress(), receiver.getPort());
                try {
                    udpSocket.send(outputPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        currentThread.start();
    }

    public void stopWorker() {
        interrupt();
        udpSocket.close();
    }
}
