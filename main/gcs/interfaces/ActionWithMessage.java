package main.gcs.interfaces;

public interface ActionWithMessage<T> {
    void handle(T message);
}
