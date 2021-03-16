package io.github.lnossel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {

    public static void main (String[] args) throws UnknownHostException {

        System.out.print("Enter the IP or hostname of the computer you want to connect to: ");
        Scanner sc = new Scanner(System.in);
        String calleeAddress = sc.nextLine();

        System.out.println("Which DatagramSocket is to be used? (1/2/3/4)");
        int dsNum = sc.nextInt();

        System.out.println("Toggle on System Analysis? (1/0): ");
        int analysisToggle = sc.nextInt();


        Analysis analysis = new Analysis();
        Sender sender = new Sender(calleeAddress, analysis, dsNum);
        Receiver receiver = new Receiver(analysis, dsNum);

        sender.start();
        receiver.start();
    }
}