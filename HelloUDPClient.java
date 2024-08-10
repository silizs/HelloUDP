package info.kgeorgiy.ja.petrova.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {
    private static final int SOCKET_TIMEOUT = 300;

    @Override
    public void run(final String host, final int port, final String prefix, final int countThreads, final int countRequests) {
        if (!BaseHello.validateRunArgs(countThreads, countRequests)) {
            return;
        }

        try (final ExecutorService service = Executors.newFixedThreadPool(countThreads)) {
            final InetAddress ip;
            try {
                ip = InetAddress.getByName(host);
            } catch (final IOException e) {
                BaseHello.print("Cannot get the ip address for " + host);
                return;
            }

            for (int idThread = 1; idThread <= countThreads; idThread++) {
                final int id = idThread;
                service.submit(() -> task(id, countRequests, prefix, ip, port));
            }
        }
    }

    private static void task(
            final int IdThread,
            final int countRequests,
            final String prefix,
            final InetAddress ip,
            final int port
    ) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            final int bufferSize = socket.getReceiveBufferSize();
            final DatagramPacket requestPacket = new DatagramPacket(new byte[bufferSize], bufferSize, ip, port);
            final DatagramPacket responsePacket = new DatagramPacket(new byte[bufferSize], bufferSize);

            for (int idRequest = 1; idRequest <= countRequests; idRequest++) {
                final String requestString = prefix + IdThread + "_" + idRequest;
                final byte[] requestData = requestString.getBytes(BaseHello.STANDART_CHARSET);
                requestPacket.setData(requestData);

                String responseString = "";
                final String expected = "Hello, " + requestString;
                while (!socket.isClosed() && !responseString.equals(expected)) {
                    BaseHello.send(socket, requestPacket, "Error with send packet in HelloUDPClient");

                    BaseHello.receive(socket, responsePacket, "Error with receive packet in HelloUDPClient");

                    responseString = BaseHello.getString(responsePacket);
                    if (!responseString.isEmpty() && !responseString.equals(expected)) {
                        responseString = recoverStringFromLanguage(responseString);
                    }
                }
                BaseHello.print("request : " + requestString + " --- response : " + responseString);
            }
        } catch (final SocketException | SecurityException se) {
            BaseHello.printError("Can not create socket in HelloUDPClient", se);
        }
    }

    private static String recoverStringFromLanguage(final String string) {
        final int indexTwo = string.lastIndexOf("_");
        if (indexTwo <= 0) {
            return string;
        }
        final int indexOne = string.substring(0, indexTwo).lastIndexOf("_");
        if (indexOne <= 0) {
            return string;
        }

        final String responsePrefix = string.substring(0, indexOne);
        try {
            final int responseIdThread = new Scanner(string.substring(indexOne + 1, indexTwo)).nextInt();
            final int responseIdRequest = new Scanner(string.substring(indexTwo + 1)).nextInt();
            return responsePrefix + "_" + responseIdThread + "_" + responseIdRequest;
        } catch (final InputMismatchException e) {
            return string;
        }
    }

    public static void main(final String[] args) {
        BaseHello.validateArgs(args, 5,
                "name_of_server/ip_server number_of_port prefix count_threads count_requests_in_thread");

        final HelloClient client = new HelloUDPClient();
        client.run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    }
}
