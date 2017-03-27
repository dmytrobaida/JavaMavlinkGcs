package main.gcs;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.*;
import com.MAVLink.enums.MAV_MISSION_RESULT;
import main.gcs.exceptions.VehicleBusyException;
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
    private List<msg_mission_item> pointsBuffer = new ArrayList<>();
    private VehicleParameters vehicleParameters = new VehicleParameters();

    //Util vars
    private final long connectionTime = 5000;
    private Timer timer = new Timer();
    private MessageSender messageSender;
    private int receiveCount;
    private boolean receiving = false;
    private boolean sending = false;

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
                    msg_mission_item itemToSend = pointsBuffer.get(sendRequest.seq);
                    itemToSend.target_component = compid;
                    itemToSend.target_system = sysid;
                    itemToSend.seq = sendRequest.seq;
                    sendMessage(itemToSend);
                }
                break;

            case msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK:
                msg_mission_ack ack = (msg_mission_ack) message;
                if (ack.sysid == sysid && onPointsSent != null) {
                    sending = false;
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
                    if (pointsBuffer.size() > 0) {
                        pointsBuffer.clear();
                    }
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
                if (receivedItem.sysid == sysid && onPointsReceived != null) {
                    pointsBuffer.add(receivedItem);
                    if (pointsBuffer.size() < receiveCount) {
                        msg_mission_request newRequest = new msg_mission_request();
                        newRequest.target_component = compid;
                        newRequest.target_system = sysid;
                        newRequest.seq = pointsBuffer.size();
                        sendMessage(newRequest);
                    } else {
                        msg_mission_ack receiveAck = new msg_mission_ack();
                        receiveAck.type = MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED;
                        receiveAck.target_component = compid;
                        receiveAck.target_system = sysid;
                        sendMessage(receiveAck);
                        receiving = false;
                        onPointsReceived.handle(pointsBuffer);
                    }
                }
                break;

            case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS:
                vehicleParameters.setSysStatus((msg_sys_status) message);
                break;

            case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                vehicleParameters.setAttitude((msg_attitude) message);
                break;

            case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                vehicleParameters.setGlobalPosition((msg_global_position_int) message);
                break;

            case msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT:
                vehicleParameters.setMissionCurrent((msg_mission_current) message);
                break;
        }
    }

    public void sendMessage(MAVLinkMessage message) {
        messageSender.sendMessage(this, message);
    }

    public void sendPoints(List<msg_mission_item> points, ActionWithMessage<String> onPointsSent) throws VehicleBusyException {
        if (!sending && !receiving) {
            this.onPointsSent = onPointsSent;
            if (pointsBuffer.size() > 0) {
                pointsBuffer.clear();
            }
            // pointsBuffer.add(new msg_mission_item());
            pointsBuffer.addAll(points);
            msg_mission_count missionCount = new msg_mission_count();
            missionCount.target_component = compid;
            missionCount.target_system = sysid;
            missionCount.count = pointsBuffer.size();
            sending = true;
            sendMessage(missionCount);
        } else {
            if (sending) throw new VehicleBusyException(this, "Sending now");
            else throw new VehicleBusyException(this, "Receiving now");
        }
    }

    public void receivePoints(ActionWithMessage<List<msg_mission_item>> onPointsReceived) throws VehicleBusyException {
        if (!sending && !receiving) {
            this.onPointsReceived = onPointsReceived;
            if (pointsBuffer.size() > 0) {
                pointsBuffer.clear();
            }
            msg_mission_request_list msgMissionRequestList = new msg_mission_request_list();
            msgMissionRequestList.target_component = compid;
            msgMissionRequestList.target_system = sysid;
            receiving = true;
            sendMessage(msgMissionRequestList);
        } else {
            if (receiving) throw new VehicleBusyException(this, "Receiving now");
            else throw new VehicleBusyException(this, "Sending now");
        }
    }

    public void clearPoints() throws VehicleBusyException {
        if (!sending && !receiving) {
            msg_mission_clear_all clearAll = new msg_mission_clear_all();
            clearAll.target_component = compid;
            clearAll.target_system = sysid;
            sendMessage(clearAll);
        } else {
            if (receiving) throw new VehicleBusyException(this, "Receiving now");
            else throw new VehicleBusyException(this, "Sending now");
        }
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
