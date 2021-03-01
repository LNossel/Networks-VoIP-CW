package io.github.lnossel;

/*
 * Sender.java
 */

/**
 *
 * @author  abj
 */
import java.net.*;
import java.io.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import javax.sound.sampled.LineUnavailableException;

public class Sender implements Runnable{

    static DatagramSocket sending_socket;

    public void start(){
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run (){

        //***************************************************
        //Port to send to
        int PORT = 55555;
        //IP ADDRESS to send to
        InetAddress clientIP = null;
        try {
            clientIP = InetAddress.getByName("DESKTOP-SISN8NK");  //CHANGE localhost to IP or NAME of client machine
        } catch (UnknownHostException e) {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Open a socket to send from
        //We dont need to know its port number as we never send anything to it.
        //We need the try and catch block to make sure no errors occur.

        //DatagramSocket sending_socket;
        try{
            sending_socket = new DatagramSocket();
        } catch (SocketException e){
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }

        boolean running = true;

        //initialising new AudioRecorder Object
        AudioRecorder recorder = null;
        try {
            recorder = new AudioRecorder();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        while (running){
            try{
                //Read in a string from the standard input
                //String str = in.readLine(); //
                TimeUnit.SECONDS.sleep((long) 0.32);

                //Convert it to an array of bytes
                byte[] buffer = recorder.getBlock();

                //Make a DatagramPacket from it, with client address and port number
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientIP, PORT);

                //Send it
                sending_socket.send(packet);

                //The user can type EXIT to quit

            } catch (IOException | InterruptedException e){
                System.out.println("ERROR: VoiceSender: Some random IO error occured!");
                e.printStackTrace();
            }
        }
        //Close the socket and recorder
        recorder.close();
        sending_socket.close();
        //***************************************************
    }

}
