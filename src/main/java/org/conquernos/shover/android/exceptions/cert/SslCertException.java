package org.conquernos.shover.android.exceptions.cert;


public class SslCertException extends RuntimeException {

    public SslCertException(String host, int port) {
        super("{\n\thost : " + host + "\n\t, port : " + port + "\n}\n");
    }

    public SslCertException(Throwable cause) {
        super(cause);
    }

    public SslCertException(String host, int port, Throwable cause) {
        super("{\n\thost : " + host + "\n\t, port : " + port + "\n}\n", cause);
    }

    public SslCertException(String message, Throwable cause) {
        super(message, cause);
    }

    public SslCertException(String message) {
        super(message);
    }

}
