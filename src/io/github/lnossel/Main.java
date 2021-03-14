package io.github.lnossel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {

    public static void main (String[] args) throws UnknownHostException {

        System.out.print("Enter the IP or hostname of the computer you want to connect to: ");
        Scanner sc = new Scanner(System.in);
        String calleeAddress = sc.nextLine();

        Sender sender = new Sender(calleeAddress);
        Receiver receiver = new Receiver();

        sender.start();
        receiver.start();
    }
}