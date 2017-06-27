package com.kgdsoftware.gopigo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by hank on 1/1/17.
 */

public class WriteCommand implements Runnable {
    private int port = 8000;

    private String command;
    private String address;

    public WriteCommand(String command, String address) {
        this.command = command;
        this.address = address;
    }

    @Override
    public void run() {
        try {
            if (address != null) {
                DatagramSocket socket = new DatagramSocket();
                InetAddress inetAddress = InetAddress.getByName(address);
                int length = command.length();
                byte[] bytes = command.getBytes();
                DatagramPacket packet = new DatagramPacket(bytes, length, inetAddress, port);
                socket.send(packet);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
