package org.cardioart.gateway.api;

import android.os.Handler;
import android.os.Message;

/**
 * Created by jirawat on 21/05/2014.
 */
public class BtHelperHandler extends Handler {
    public enum MessageType {
        STATE,
        READ,
        WRITE,
        DEVICE,
        NOTIFY;
    }
    public Message obtainMessage(MessageType message, int count, Object object) {
        return obtainMessage(message.ordinal(), count, -1, object);
    }
    public MessageType getMessageType(int ordinal) {
        return MessageType.values()[ordinal];
    }
}
