package org.cardioart.gateway.api;

import java.io.IOException;

/**
 * Created by jirawat on 06/07/2014.
 */
public class MyMessage {
    private int id;
    private byte  channel;
    private byte opt;
    public short[] data;

    public MyMessage(int id, byte channel, byte opt) {
        setId(id);
        setChannel(channel);
        setOpt(opt);
    }
    public void setId(int id) {
        this.id = id;
    }

    public void setChannel(byte channel) {
        this.channel = channel;
    }

    public void setOpt(byte opt) {
        this.opt = opt;
    }
}
