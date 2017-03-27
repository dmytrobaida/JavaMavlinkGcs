package main.gcs.exceptions;

import main.gcs.Vehicle;

public class VehicleBusyException extends Exception {
    Vehicle vehicle;
    String message;

    public VehicleBusyException(Vehicle vehicle, String message) {
        this.vehicle = vehicle;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return String.format("Vehicle: %s\n Message: %s", vehicle, message);
    }
}
