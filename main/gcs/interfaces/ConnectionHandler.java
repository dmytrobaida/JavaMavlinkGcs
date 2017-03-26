package main.gcs.interfaces;

import main.gcs.Vehicle;

public interface ConnectionHandler{
    void success(Vehicle vehicle);
    void failure(Vehicle vehicle);
}
