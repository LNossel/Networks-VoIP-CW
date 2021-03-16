package io.github.lnossel;

import CMPC3M06.AudioPlayer;
import uk.ac.uea.cmp.voip.*;

import javax.sound.sampled.LineUnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static io.github.lnossel.Sender.applyXOR;

public class Receiver implements Runnable {

    static DatagramSocket receiving_socket;
    static DatagramSocket2 receiving_socket2;
    static DatagramSocket3 receiving_socket3;
    static DatagramSocket4 receiving_socket4;
    static AudioPlayer player;
    static int PORT = 55555;
    static byte[] header = new byte[2];

    Analysis analysis;
    int socketNum;
    int counter = 0;

    public Receiver(Analysis analysis, int dsNum) {
        this.analysis = analysis;
        this.socketNum = dsNum;
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        boolean connected = true;

        try{
            switch (this.socketNum) {
                case 1 -> receiving_socket = new DatagramSocket(PORT);
                case 2 -> receiving_socket2 = new DatagramSocket2(PORT);
                case 3 -> receiving_socket3 = new DatagramSocket3(PORT);
                case 4 -> receiving_socket4 = new DatagramSocket4(PORT);
            }
        } catch (SocketException e){
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }

        try {
            player = new AudioPlayer();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        while (connected) {
            byte[] encryptedData = receiveData();
            if (header[1] == 10){

                byte[] data = decryptData(encryptedData);
                playData(data);
            }
        }

        player.close();
        switch (this.socketNum) {
            case 1 -> receiving_socket.close();
            case 2 -> receiving_socket2.close();
            case 3 -> receiving_socket3.close();
            case 4 -> receiving_socket4.close();
        }
    }

    private byte[] receiveData() {
        byte[] buffer = new byte[514];
        DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);

        try {
            switch (this.socketNum) {
                case 1 -> receiving_socket.receive(packet);
                case 2 -> receiving_socket2.receive(packet);
                case 3 -> receiving_socket3.receive(packet);
                case 4 -> receiving_socket4.receive(packet);
            }

        } catch (IOException e) {
            System.out.println("ERROR: VoiceReceiver: Some random IO error occurred!");
            e.printStackTrace();
        }

        byte[] data = new byte[512];
        System.arraycopy(buffer, 0, header, 0, header.length);
        System.arraycopy(buffer, header.length, data, 0, data.length);

        System.out.println(new String(header));
        System.out.println(new String(data));

        return buffer;
    }

    private byte[] decryptData(byte[] data) {
        return applyXOR(data);
    }

    private void playData(byte[] data) {
        System.out.println(header[1]);
        System.out.println("worked!");
        try {
            player.playBlock(data);
            counter++;

            analysis.setReceivedCount(counter);

        } catch (IOException e) {
            System.out.println("ERROR: TextReceiver: Some random IO error occurred!");
            e.printStackTrace();
        }
    }


}
