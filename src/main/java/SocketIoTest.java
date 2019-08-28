import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

import javax.net.ssl.*;

public class SocketIoTest {

    public static HostnameVerifier getMyHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
    }

    public static X509TrustManager getTrustManager() {
        return new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }
            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };
    }

    public static SSLContext createSSLContext(X509TrustManager trustManager) throws GeneralSecurityException, IOException {
//        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{trustManager};
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        String url = "https://10.88.32.118:443";
        try {
            X509TrustManager trustManager = getTrustManager();
            SSLContext sslContext = createSSLContext(trustManager);
            HostnameVerifier myHostnameVerifier = getMyHostnameVerifier();
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(myHostnameVerifier)
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .build();

            // default settings for all sockets
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
            IO.setDefaultOkHttpCallFactory(okHttpClient);

            IO.Options options = new IO.Options();
            options.callFactory = okHttpClient;
            options.webSocketFactory = okHttpClient;

            options.transports = new String[]{"websocket"};
            //失败重试次数
            options.reconnectionAttempts = 10;
            //失败重连的时间间隔
            options.reconnectionDelay = 1000;
            //连接超时时间(ms)
            options.timeout = 500;

            final Socket socket = IO.socket(url, options);
            //监听自定义订阅事件
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener(){

                public void call(Object... objects) {
                    System.out.println("client: " + "连接成功");
                }
            });

            socket.on(Socket.EVENT_CONNECTING, new Emitter.Listener(){

                public void call(Object... objects) {
                    System.out.println("client: " + "连接中...");
                }
            });

            socket.on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener(){

                public void call(Object... objects) {
                    System.out.println("client: " + "连接超时!");
                }
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener(){

                public void call(Object... objects) {
                    System.out.println("client: " + "连接失败");
                }
            });

            System.out.println("client: " + "开始连接 url="+url);
            socket.connect();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
