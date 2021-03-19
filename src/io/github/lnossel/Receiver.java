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
import java.util.*;

import static io.github.lnossel.Sender.applyXOR;

public class Receiver implements Runnable {

    static DatagramSocket receiving_socket;
    static DatagramSocket2 receiving_socket2;
    static DatagramSocket3 receiving_socket3;
    static DatagramSocket4 receiving_socket4;
    static AudioPlayer player;
    static int PORT = 55555;
    static byte[] header = new byte[2];

    static Analysis analysis;
    static int socketNum;
    static int counter = 0;

    //used for reordering numbered packets
    static HashMap<Byte, byte[]> orderMap = new HashMap<Byte, byte[]>();
    static int minHeader = 0;
    static boolean mapConstructed;

    //used for packet loss concealment
    static boolean concealerConstructed;
    static byte[] previousPacket;
    static byte previousPacketHeader;
    static byte packetCounter;
    static List<byte[]> previousPackets = new ArrayList<byte[]>(10);

    //acknowledgement
    public static boolean ack = false;

    //Encryption Demo
    private static int encryptToggle;


    public Receiver(Analysis analysis, int dsNum, int encryptToggle) {
        this.analysis = analysis;
        this.socketNum = dsNum;
        this.encryptToggle = encryptToggle;
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

        //ByteBuffer buf = new ByteBuffer.allocate(512);

        while (connected) {
            byte[] encryptedData = receiveData();


            switch (socketNum) {
                case 2 -> encryptedData = packetLossConcealment(header[0], encryptedData);
                case 3 -> encryptedData = checkOrder(encryptedData);
            }

            //packet reordering for datasocket3

            //encryptedData = checkOrder(encryptedData);

            //packet loss check for datasocket 2,3
            //encryptedData = packetLossConcealment(header[0], encryptedData);


            if (header[1] == 10){
                if(encryptedData != null) {
                    byte[] data = decryptData(encryptedData);
                    System.out.println("packet played: " + Arrays.toString(header) + Arrays.toString(data));

                    //for encryption demo
                    if (encryptToggle == 1) {
                        data = applyXOR(data);
                    }

                    playData(data);


                }
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

    /**
     * PacketLossConcealment() [DatagramSocket2 & 3]
     *
     * ~ To store and replay the last X received packets in order where X is the amount of lost packets in a row
     *
     * 1. Generates an ArrayList of bytes[] of size 10.
     * 2. successfully received packets are then added to the ArrayList
     * 3. When the ArrayList reaches size 10 it acts as a Queue
     *
     * @param header
     * @param encryptedData
     * @return byte[]
     */

    private static synchronized byte[] packetLossConcealment(byte header, byte[] encryptedData){

        System.out.println("header" + header);

        if(!concealerConstructed){
            previousPacket = encryptedData;
            previousPacketHeader = header;
            concealerConstructed = true;
            previousPackets.add(previousPacket);
            return encryptedData;
        }
        else {
            if(header - previousPacketHeader == 1 || header - previousPacketHeader == 99){
                previousPacket = encryptedData;
                previousPacketHeader = header;

                System.out.println("packet played: " + header + Arrays.toString(previousPacket));
                if (previousPackets.size() == 10 && checkPacket(previousPacket)) {
                    previousPackets.remove(0);
                }
                if (checkPacket(previousPacket)) {
                    previousPackets.add(previousPacket);
                }
            }
            else{
                int header_difference = header - previousPacketHeader;
                System.out.println("previousHeader: " + previousPacketHeader);
                System.out.println("header diff: " + header_difference);


                if(previousPackets.size() < header_difference) {
                    for (int i = 1; i < header_difference; i++) {
                        byte[] data = decryptData(previousPackets.get(i%previousPackets.size()));
                        System.out.println("packet played: " + i%previousPackets.size() + "  " + header + Arrays.toString(data));
                        playData(data);
                    }
                }
                else{
                    for (int i = 1; i < header_difference; i++) {
                        byte[] data = decryptData(previousPackets.get(i));
                        System.out.println("packet played: " + header + Arrays.toString(data));
                        playData(data);
                    }
                }
                System.out.println("packet played: " + header + Arrays.toString(previousPacket));
                previousPacket = encryptedData;
                previousPacketHeader = header;
            }

            System.out.println(previousPackets.toString());
            return previousPacket;

        }
    }

    /**
     * checkPacket() [Datagram Socket 2 & 3]
     *
     * ~ Check if packet is most likely background noise and if so return false.
     * ~ Used in PacketLossConcealment to ensure no 'empty' packets are stored in queue to be replayed
     *
     * 1. Check all bytes found in packet
     * 2. If the byte number in the packet is lower than 10, higher than -10, this indicates that byte is most likely 'empty'
     * 3. If more than half of the packet is made up of these empty bytes, return false.
     *
     * @param packet
     * @return boolean
     */

    private static boolean checkPacket(byte[] packet){
        int checkCounter = 0;
        for(byte bite : packet){
            if(bite < 11 && bite > -11){
                checkCounter++;
            }
            if(checkCounter > packet.length/2){
                System.out.println("dropped: " + packet);
                return false;
            }
        }
        return true;
    }

    /**
     * checkOrder() [DatagramSocket 3]
     *
     * ~ Holds last 10 packets received before sending them to be played
     *
     * 1. Generate circular Hashmap (acting as circular queue) of key: header, value: encrypted data
     * 2. When requested, Hashmap returns encrypted data with the lowest Header (playing packets in order)
     *
     * @param encryptedData
     * @return byte[]
     */

    private synchronized byte[] checkOrder(byte[] encryptedData) {

        byte[] encryptedPayload;

        if(orderMap.size() == 10) {
            mapConstructed = true;
        }

        if(!mapConstructed) {

            orderMap.put(header[0], encryptedData);
            System.out.println(orderMap.toString());


            byte[] empty = new byte[512];
            return empty;

        }
        else {

            System.out.println(orderMap.toString());

            int intHeader;
            int newMinHeader = minHeader;

            for ( Byte key : orderMap.keySet() ) {

                intHeader = key;
                //included for chance of packet loss
                newMinHeader = minHeader;

                //System.out.println("intHeader" + intHeader);
                if(intHeader - minHeader == 1 || intHeader - minHeader == -99){
                    newMinHeader = intHeader;
                    break;
                }


            }

            if(newMinHeader == minHeader){
                minHeader++;
                encryptedPayload = checkOrder(encryptedData);
            }
            else {

                minHeader = newMinHeader;

                //System.out.println("MINHEADER:" + minHeader);

                encryptedPayload = orderMap.get((byte) minHeader);
                orderMap.remove((byte) minHeader);

                orderMap.put(header[0], encryptedData);

            }

            return encryptedPayload;

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


        System.out.println(Arrays.toString(buffer));

        byte[] data = new byte[512];
        System.arraycopy(buffer, 0, header, 0, header.length);
        System.arraycopy(buffer, header.length, data, 0, data.length);

        //System.out.println(Arrays.toString(data));
        //System.out.println(Arrays.toString(header));

        return data;
    }

    private static byte[] decryptData(byte[] data) {
        return applyXOR(data);
    }

    private static synchronized void playData(byte[] data) {
        //System.out.println(header[1]);
        //System.out.println("worked!");
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
