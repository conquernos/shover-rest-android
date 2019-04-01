package org.conquernos.shover.android.cert;

import org.conquernos.shover.android.exceptions.cert.SslCertException;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


public class CertInstaller {

    private KeyStore keyStore;
    private SSLContext context;
    private TrustManagerFactory trustManagerFactory;
    private X509TrustManager x509TrustManager;
    private SavingTrustManager savingTrustManager;

    public CertInstaller() {
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            context = SSLContext.getInstance("TLS");
        } catch (Exception e) {
            throw new SslCertException(e);
        }

        initialize(keyStore);
    }

    public void certificate(String host, int port, int milliSecs) {
        SSLSocketFactory factory = context.getSocketFactory();
        try {
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            socket.setSoTimeout(milliSecs);
            try {
                socket.startHandshake();
            } catch (SSLException ignored) {
            } finally {
                socket.close();
            }

            X509Certificate[] chain = savingTrustManager.chain;
            if (chain != null) {
                keyStore.setCertificateEntry(host, chain[0]);
                initialize(keyStore);
            } else {
                throw new SslCertException("Could not obtain server certificate chain");
            }
        } catch (Exception e) {
            throw new SslCertException(host, port, e);
        }
    }

    public void certificate(InputStream certInputStream) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            Certificate cert = certFactory.generateCertificate(certInputStream);
            keyStore.setCertificateEntry("rest-proxy-server", cert);
            initialize(keyStore);
        } catch (Exception e) {
            throw new SslCertException(e);
        }
    }

    public SSLContext getContext() {
        return context;
    }

    public X509TrustManager getX509TrustManager() {
        return x509TrustManager;
    }

    private void initialize(KeyStore keyStore) {
        try {
            trustManagerFactory.init(keyStore);
            x509TrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            savingTrustManager = new SavingTrustManager(x509TrustManager);
            context.init(null, new TrustManager[]{savingTrustManager}, null);
        } catch (Exception e) {
            throw new SslCertException(e);
        }
    }

    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            return tm.getAcceptedIssuers();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            tm.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }

}
