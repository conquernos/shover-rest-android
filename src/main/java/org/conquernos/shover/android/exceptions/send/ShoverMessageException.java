package org.conquernos.shover.android.exceptions.send;


import org.conquernos.shover.android.ShoverMessage;
import org.conquernos.shover.android.exceptions.ShoverException;


public class ShoverMessageException extends ShoverException {

    public ShoverMessageException(String topic, Object message) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n");
    }

    public ShoverMessageException(String topic, Object message, Throwable cause) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n", cause);
    }

    public ShoverMessageException(String topic, ShoverMessage message) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n");
    }

    public ShoverMessageException(String topic, ShoverMessage message, Throwable cause) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n", cause);
    }

    public ShoverMessageException(String topic, Object[] message) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n");
    }

    public ShoverMessageException(String topic, Object[] message, Throwable cause) {
        super("{\n\ttopic : " + topic + "\n\t, message : " + message + "\n}\n", cause);
    }

}
