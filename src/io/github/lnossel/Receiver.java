package io.github.lnossel;

import CMPC3M06.AudioPlayer;

import uk.ac.uea.cmp.voip.*;

import javax.sound.sampled.LineUnavailableException;
import java.net.*;
import java.io.*;

public class Receiver implements Runnable {

    static DatagramSocket receiving_socket;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run (){

        int PORT = 55555;

        try{
            receiving_socket = new DatagramSocket3(PORT);
        } catch (SocketException e){
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }

        boolean running = true;

        AudioPlayer player = null;
        try {
            player = new AudioPlayer();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        while (running){

            try{
                //Receive a DatagramPacket (note that the string cant be more than 80 chars)
                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, 0, 512);

                receiving_socket.receive(packet);

                player.playBlock(buffer);

            } catch (IOException e){
                System.out.println("ERROR: TextReceiver: Some random IO error occured!");
                e.printStackTrace();
            }
        }
        //Close the socket
        player.close();
        receiving_socket.close();
        //***************************************************
    }

    private DatagramPacket Decrypt(DatagramPacket packet){
        return packet;
    }

}