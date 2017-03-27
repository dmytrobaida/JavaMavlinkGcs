package main;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.enums.MAV_CMD;
import main.gcs.Gcs;
import main.gcs.Vehicle;
import main.gcs.exceptions.GcsNotListeningException;
import main.gcs.exceptions.VehicleBusyException;
import main.gcs.interfaces.ActionWithMessage;
import main.gcs.interfaces.ConnectionHandler;
import main.gcs.network.IpPortAddress;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
                System.out.println("Success");
                List<msg_mission_item> items = new ArrayList<>();

                msg_mission_item i1 = new msg_mission_item();
                i1.x = 10;
                i1.y = 10;
                i1.z = 10;
                i1.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT;
                items.add(i1);

                msg_mission_item i2 = new msg_mission_item();
                i2.x = 15;
                i2.y = 15;
                i2.z = 15;
                i2.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT;

                items.add(i1);
                items.add(i2);

                try {
                    vehicle.sendPoints(items, new ActionWithMessage<String>() {
                        @Override
                        public void handle(String message) {
                            System.out.println(message);
                        }
                    });
                } catch (VehicleBusyException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}

