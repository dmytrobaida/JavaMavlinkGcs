package main.gcs;

import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_mission_current;
import com.MAVLink.common.msg_sys_status;

public class VehicleParameters {
    private int currentMissionSeq;
    private float alt, relativeAlt, lat, lng;
    private float roll, pitch, yaw; //Крен, Тангаж, Рысканье
    private float batteryVoltage, batteryCurrent;
    private int batteryRemaining;

    public void setMissionCurrent(msg_mission_current missionCurrent) {
        currentMissionSeq = missionCurrent.seq;
    }

    public void setGlobalPosition(msg_global_position_int globalPosition) {
        lat = globalPosition.lat / 10000000;
        lng = globalPosition.lon / 10000000;
        alt = globalPosition.alt / 1000;
        relativeAlt = globalPosition.relative_alt / 1000;
    }

    public void setAttitude(msg_attitude attitude) {
        roll = attitude.roll;
        pitch = attitude.pitch;
        yaw = attitude.yaw;
    }

    public void setSysStatus(msg_sys_status sysStatus) {
        batteryVoltage = sysStatus.voltage_battery / 1000;
        batteryCurrent = sysStatus.current_battery / 100;
        batteryRemaining = sysStatus.battery_remaining;
    }

    public int getCurrentMissionSeq() {
        return currentMissionSeq;
    }

    public float getAlt() {
        return alt;
    }

    public float getRelativeAlt() {
        return relativeAlt;
    }

    public float getLat() {
        return lat;
    }

    public float getLng() {
        return lng;
    }

    public float getRoll() {
        return roll;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getBatteryVoltage() {
        return batteryVoltage;
    }

    public float getBatteryCurrent() {
        return batteryCurrent;
    }

    public int getBatteryRemaining() {
        return batteryRemaining;
    }
}
