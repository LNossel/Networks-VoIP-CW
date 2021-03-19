package io.github.lnossel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {

    public static void main (String[] args) throws UnknownHostException {

        System.out.print("Enter the IP or hostname of the computer you want to connect to: ");
        Scanner sc = new Scanner(System.in);
        String calleeAddress = sc.nextLine();

        System.out.println("Which DatagramSocket is to be used? (1/2/3");
        int dsNum = sc.nextInt();

        System.out.println("Play Encrypted Data? (1/0): ");
        int encryptToggle = sc.nextInt();

        Analysis analysis = new Analysis();
        Sender sender = new Sender(calleeAddress, analysis, dsNum);
        Receiver receiver = new Receiver(analysis, dsNum, encryptToggle);

        sender.start();
        receiver.start();
    }
}