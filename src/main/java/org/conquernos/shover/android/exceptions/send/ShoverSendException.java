package org.conquernos.shover.android.exceptions.send;


import org.conquernos.shover.android.ShoverMessage;
import org.conquernos.shover.android.exceptions.ShoverException;

import java.util.Arrays;

public class ShoverSendException extends ShoverException {

    public ShoverSendException(String topic, Object message) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n");
    }

    public ShoverSendException(String topic, Object message, Throwable cause) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n", cause);
    }

    public ShoverSendException(String topic, ShoverMessage message) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n");
    }

    public ShoverSendException(String topic, ShoverMessage message, Throwable cause) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n", cause);
    }

    public ShoverSendException(String topic, Object[] message) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + Arrays.toString(message) + "\n}\n");
    }

    public ShoverSendException(String topic, Object[] message, Throwable cause) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + Arrays.toString(message) + "\n}\n", cause);
    }

}
