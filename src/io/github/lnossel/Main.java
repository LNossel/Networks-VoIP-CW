package io.github.lnossel;

public class Main {

    public static void main (String[] args){

        Receiver receiver = new Receiver();
        Sender sender = new Sender();

        receiver.start();
        sender.start();

    }
}