package main.gcs.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpPortAddress {
    private InetAddress address;
    private int port;

    public IpPortAddress(int port) throws UnknownHostException {
        this("localhost", port);
    }

    public IpPortAddress(InetAddress address, int port){
        this.address = address;
        this.port = port;
    }

    public IpPortAddress(String address, int port) throws UnknownHostException {
        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    public InetAddress getAddress(){
        return address;
    }

    public int getPort(){
        return port;
    }

    @Override
    public String toString() {
        return String.format("%s:%d", address.toString(), port);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof IpPortAddress){
            IpPortAddress address = (IpPortAddress) o;
            if(this.address.equals(address.getAddress()) && this.port == address.getPort()){
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
}
