package io.github.lnossel;

import CMPC3M06.AudioRecorder;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class Sender implements Runnable {

    static DatagramSocket sending_socket;
    static AudioRecorder recorder;
    static InetAddress clientIP;
    static int PORT = 55555;


    public Sender(String host) {
//        attempt to get the client IP address
        try {
            clientIP = InetAddress.getByName(host);
        } catch(UnknownHostException e) {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        boolean connected = true;

//        prepare a socket for communication
        try {
            sending_socket = new DatagramSocket();
        } catch (SocketException e){
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }

//        prepare an audio recorder
        try {
            recorder = new AudioRecorder();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(0);
        }

        while (connected) {
            byte[] data = recordData();
            byte[] encryptedData = encryptData(data);
            sendData(encryptedData);
        }

        recorder.close();
        sending_socket.close();
    }

    private byte[] recordData() {
        byte[] data = new byte[512];
        try {
            data = recorder.getBlock();
        } catch (IOException e) {
            System.out.println("ERROR: VoiceSender: Some random IO error occurred!");
            e.printStackTrace();
        }
        return data;
    }

    private byte[] encryptData(byte[] data) {
        return applyXOR(data);
    }

    private void sendData(byte[] data) {
//        add authentication header
        ByteBuffer buf = ByteBuffer.allocate(514);
        short authenticationKey = 10;
        buf.putShort(authenticationKey);
        buf.put(data);
        data = buf.array();

        DatagramPacket packet = new DatagramPacket(data, data.length, clientIP, PORT);

        try {
            sending_socket.send(packet);
        } catch (IOException e) {
            System.out.println("ERROR: VoiceSender: Some random IO error occurred!");
            e.printStackTrace();
        }
    }

    static byte[] addHeader(byte[] header, byte[] data){
        //create new payload
        byte[] payload = new byte[data.length + header.length];

        System.arraycopy(header, 0, payload, 0, header.length);
        System.arraycopy(payload, 0, header, header.length, payload.length);

        return payload;
    }

    static byte[] applyXOR(byte[] data) {
        int key = 10;
        ByteBuffer unwrapDecrypt;
        ByteBuffer cipherText;
        int fourByte;
        unwrapDecrypt = ByteBuffer.allocate(data.length);

        cipherText = ByteBuffer.wrap(data);
        for (int j = 0; j < data.length/4; j++) {
            fourByte = cipherText.getInt();
            fourByte = fourByte ^ key;
            unwrapDecrypt.putInt(fourByte);
        }
        return unwrapDecrypt.array();
    }
}
