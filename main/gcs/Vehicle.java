package main.gcs;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.*;
import com.MAVLink.enums.MAV_MISSION_RESULT;
import main.gcs.interfaces.*;
import main.gcs.network.IpPortAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Vehicle implements PacketHandler {
    //Vehicle params
    private IpPortAddress vehicleAddress;
    private boolean connected = false;
    private short sysid;
    private short compid;
    private List<msg_mission_item> missionPoints = new ArrayList<>();
    private VehicleParameters vehicleParameters;

    //Util vars
    private final long connectionTime = 5000;
    private Timer timer = new Timer();
    private MessageSender messageSender;
    private int receiveCount;

    //Events
    private Action onHeartbeat;
    private ActionWithMessage<String> onPointsSent;
    private ActionWithMessage<List<msg_mission_item>> onPointsReceived;
    private ConnectionHandler connectionHandler;

    Vehicle(IpPortAddress vehicleAddress, ConnectionHandler connectionHandler, MessageSender messageSender) {
        this.vehicleAddress = vehicleAddress;
        this.messageSender = messageSender;
        this.connectionHandler = connectionHandler;
    }

    void connect() {
        Vehicle vehicle = this;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                connectionHandler.failure(vehicle);
                timer.cancel();
                timer.purge();
            }
        }, connectionTime);
        sendMessage(new msg_heartbeat());
    }

    @Override
    public IpPortAddress getVehicleAddress() {
        return vehicleAddress;
    }

    @Override
    public void handlePacket(MAVLinkPacket packet) {
        MAVLinkMessage message = packet.unpack();

        switch (message.msgid) {
            case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                msg_heartbeat heartbeat = (msg_heartbeat) message;
                if (!connected) {
                    timer.cancel();
                    sysid = (short) heartbeat.sysid;
                    compid = (short) heartbeat.compid;
                    connected = true;
                    connectionHandler.success(this);
                }
                if (onHeartbeat != null) {
                    onHeartbeat.handle();
                }

                break;

            case msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST:
                msg_mission_request sendRequest = (msg_mission_request) message;
                if (sendRequest.sysid == sysid) {
                    msg_mission_item itemToSend = missionPoints.get(sendRequest.seq);
                    itemToSend.target_component = compid;
                    itemToSend.target_system = sysid;
                    itemToSend.seq = sendRequest.seq;
                    sendMessage(itemToSend);
                }
                break;

            case msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK:
                msg_mission_ack ack = (msg_mission_ack) message;
                if (ack.sysid == sysid && onPointsSent != null) {
                    if (ack.type == MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED) {
                        onPointsSent.handle("Mission Accepted");
                    } else {
                        onPointsSent.handle(String.format("Error code: %d", ack.type));
                    }
                }
                break;

            case msg_mission_count.MAVLINK_MSG_ID_MISSION_COUNT:
                msg_mission_count msgMissionCount = (msg_mission_count) message;
                if (msgMissionCount.sysid == sysid) {
                    receiveCount = msgMissionCount.count;
                    msg_mission_request receiveRequest = new msg_mission_request();
                    receiveRequest.target_component = compid;
                    receiveRequest.target_system = sysid;
                    receiveRequest.seq = 0;
                    sendMessage(receiveRequest);
                }
                break;

            case msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM:
                msg_mission_item receivedItem = (msg_mission_item) message;
                if (receivedItem.sysid == sysid) {
                    missionPoints.add(receivedItem);
                    if (missionPoints.size() < receiveCount) {
                        msg_mission_request newRequest = new msg_mission_request();
                        newRequest.target_component = compid;
                        newRequest.target_system = sysid;
                        newRequest.seq = missionPoints.size();
                        sendMessage(newRequest);
                    } else {
                        msg_mission_ack receiveAck = new msg_mission_ack();
                        receiveAck.type = MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED;
                        receiveAck.target_component = compid;
                        receiveAck.target_system = sysid;
                        sendMessage(receiveAck);
                        onPointsReceived.handle(missionPoints);
                    }
                    break;
                }
        }
    }

    public void sendMessage(MAVLinkMessage message) {
        messageSender.sendMessage(this, message);
    }

    public void sendPoints(List<msg_mission_item> points, ActionWithMessage<String> onPointsSent) {
        this.onPointsSent = onPointsSent;
        missionPoints.clear();
        missionPoints.addAll(points);
        msg_mission_count missionCount = new msg_mission_count();
        missionCount.target_component = compid;
        missionCount.target_system = sysid;
        missionCount.count = missionPoints.size();
        sendMessage(missionCount);
    }

    public void receivePoints(ActionWithMessage<List<msg_mission_item>> onPointsReceived) {
        this.onPointsReceived = onPointsReceived;
        missionPoints.clear();
        msg_mission_request_list msgMissionRequestList = new msg_mission_request_list();
        msgMissionRequestList.target_component = compid;
        msgMissionRequestList.target_system = sysid;
        sendMessage(msgMissionRequestList);
    }

    public void setOnHeartbeatHandler(Action heartbeatHandler) {
        this.onHeartbeat = heartbeatHandler;
    }

    public int getSysid() {
        return sysid;
    }

    public int getCompid() {
        return compid;
    }

    @Override
    public String toString() {
        return String.format("Sysid = %d, Address = %s", sysid, vehicleAddress.toString());
    }

    public VehicleParameters getVehicleParameters() {
        return vehicleParameters;
    }
}
