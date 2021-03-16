package io.github.lnossel;

import CMPC3M06.AudioRecorder;
import uk.ac.uea.cmp.voip.DatagramSocket4;
import uk.ac.uea.cmp.voip.DatagramSocket3;
import uk.ac.uea.cmp.voip.DatagramSocket2;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;


public class Sender implements Runnable {

    static DatagramSocket sending_socket;
    static DatagramSocket2 sending_socket2;
    static DatagramSocket3 sending_socket3;
    static DatagramSocket4 sending_socket4;
    static AudioRecorder recorder;
    static InetAddress clientIP;
    static int PORT = 55555;

    public Sender(String host, Analysis analysis) {
//        attempt to get the client IP address
        try {
            clientIP = InetAddress.getByName(host);
        } catch(UnknownHostException e) {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
        }
        this.analysis = analysis;
    }

    public Sender(String host, Analysis analysis, int dsNum) {

        try {
            clientIP = InetAddress.getByName(host);
        } catch(UnknownHostException e) {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
        }
        this.analysis = analysis;
        this.socketNum = dsNum;

    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        boolean connected = true;

//        prepare a socket for communication
        try {
            switch (this.socketNum) {
                case 1 -> sending_socket = new DatagramSocket();
                case 2 -> sending_socket2 = new DatagramSocket2();
                case 3 -> sending_socket3 = new DatagramSocket3();
                case 4 -> sending_socket4 = new DatagramSocket4();
            }
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
        switch (this.socketNum) {
            case 1 -> sending_socket.close();
            case 2 -> sending_socket2.close();
            case 3 -> sending_socket3.close();
            case 4 -> sending_socket4.close();
        }
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
            switch (this.socketNum) {
                case 1 -> sending_socket.send(packet);
                case 2 -> sending_socket2.send(packet);
                case 3 -> sending_socket3.send(packet);
                case 4 -> sending_socket4.send(packet);
            }

            counter++;
            System.out.println(counter);

            if(counter % 10 == 0){
                analysis.setSentCount(counter);
            }

        } catch (IOException e) {
            System.out.println("ERROR: VoiceSender: Some random IO error occurred!");
            e.printStackTrace();
        }
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
