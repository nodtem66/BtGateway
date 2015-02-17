package org.cardioart.gateway.api.constant;

import java.io.IOException;

/**
 * Created by jirawat on 06/07/2014.
 */
public class MyMessage {
    public int id;
    public byte  channel;
    public byte opt;
    public double[] time;
    public int[] data;

    public MyMessage(int id, byte channel, byte opt) {
        this.id = id;
        this.channel = channel;
        this.opt = opt;
    }
}
