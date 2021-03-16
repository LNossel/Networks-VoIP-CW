package io.github.lnossel;

public class Analysis {
    private int sentCount;
    private int receivedCount;

    public Analysis(){}

    public void setSentCount(int sentCount){
        this.sentCount = sentCount;
        this.getInfo();
    }

    public void setReceivedCount(int receivedCount){
        this.receivedCount = receivedCount;
    }


    public void getInfo(){
        if (sentCount != 0){
            System.out.println("Packets Sent: " + this.sentCount +
                             "\nPackets Received: " + this.receivedCount);
        }

    }

}
