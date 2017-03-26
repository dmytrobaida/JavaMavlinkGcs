package main.gcs.interfaces;

import com.MAVLink.Messages.MAVLinkMessage;
import main.gcs.Vehicle;

public interface MessageSender {
    void sendMessage(Vehicle vehicle, MAVLinkMessage message);
}
