package main;

import com.MAVLink.common.msg_mission_item;
import main.gcs.Gcs;
import main.gcs.Vehicle;
import main.gcs.exceptions.GcsNotListeningException;
import main.gcs.interfaces.Action;
import main.gcs.interfaces.ActionWithMessage;
import main.gcs.interfaces.ConnectionHandler;
import main.gcs.network.IpPortAddress;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SocketException, UnknownHostException, GcsNotListeningException, InterruptedException {
        Gcs gcs = new Gcs(22841);
        gcs.startListening();
        gcs.connectToVehicle(new IpPortAddress("localhost", 14555), new ConnectionHandler() {
            @Override
            public void failure(Vehicle vehicle) {
                System.out.println("Failure");
                 // gcs.stopListening();
                gcs.disconnectVehicle(vehicle);
            }

            @Override
            public void success(Vehicle vehicle) {
                vehicle.receivePoints(new ActionWithMessage<List<msg_mission_item>>() {
                    @Override
                    public void handle(List<msg_mission_item> message) {
                        System.out.println(message);
                    }
                });

                vehicle.setOnHeartbeatHandler(new Action() {
                    @Override
                    public void handle() {
                        System.out.println(vehicle);
                    }
                });
            }
        });

    }
}

