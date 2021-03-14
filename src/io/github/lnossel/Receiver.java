package io.github.lnossel;

import CMPC3M06.AudioPlayer;
import uk.ac.uea.cmp.voip.*;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import static io.github.lnossel.Sender.applyXOR;

public class Receiver implements Runnable {

    static DatagramSocket receiving_socket;
    static AudioPlayer player;
    static int PORT = 55555;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        boolean connected = true;

        try{
            receiving_socket = new DatagramSocket(PORT);
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
            byte[] data = decryptData(encryptedData);
            playData(data);
        }

        player.close();
        receiving_socket.close();
    }

    private byte[] receiveData() {
        byte[] buffer = new byte[512];
        DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);

        try {
            receiving_socket.receive(packet);
        } catch (IOException e) {
            System.out.println("ERROR: VoiceReceiver: Some random IO error occurred!");
            e.printStackTrace();
        }

        return buffer;
    }

    private byte[] decryptData(byte[] data) {
        return applyXOR(data);
    }

    private void playData(byte[] data) {
        try {
            player.playBlock(data);
        } catch (IOException e) {
            System.out.println("ERROR: TextReceiver: Some random IO error occurred!");
            e.printStackTrace();
        }
    }
}
