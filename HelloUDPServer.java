package info.kgeorgiy.ja.petrova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer, NewHelloServer {
    private ExecutorService handlingService;
    private ExecutorService portService;
    private ConcurrentHashMap<Integer, DatagramSocket> sockets;
    private boolean isStarted;

    @Override
    public void start(final int countThreads, final Map<Integer, String> mapPorts) {
        if (isStarted) {
            throw new IllegalStateException("Server is started");
        }
        if (!BaseHello.validateStartArgs(countThreads, mapPorts)) {
            return;
        }

        handlingService = Executors.newFixedThreadPool(countThreads);
        portService = Executors.newFixedThreadPool(mapPorts.size());
        sockets = new ConcurrentHashMap<>();

        mapPorts.forEach((port, ignored) -> {
            try {
                sockets.put(port, new DatagramSocket(port));
            } catch (final SocketException | SecurityException | IllegalArgumentException e) {
                BaseHello.printError("Can not create socket on port " + port + " in HelloUDPServer", e);
            }
        });
        isStarted = true;

        mapPorts.forEach((port, template) -> portService.submit(() -> receiveTask(port, template)));
    }

    private void receiveTask(final int port, final String template) {
        final DatagramSocket socket = sockets.get(port);
        final int bufferSize;
        try {
            bufferSize = socket.getReceiveBufferSize();
        } catch (final SocketException e) {
            BaseHello.printError("Can not interact with the socket on port " + port + " in HelloUDPServer", e);
            return;
        }

        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            final DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);
            BaseHello.receive(socket, packet, "Error with receive packet in HelloUDPServer");

            handlingService.submit(() -> sendTask(port, packet, template));
        }
    }

    private void sendTask(final int port, final DatagramPacket packet, final String template) {
        final String sendString = template.replace("$", BaseHello.getString(packet));
        final byte[] sendData = sendString.getBytes(BaseHello.STANDART_CHARSET);

        packet.setData(sendData);
        BaseHello.send(sockets.get(port), packet, "Error with send packet in HelloUDPServer");
    }

    @Override
    public void start(final int port, final int countThreads) {
        start(countThreads, Map.of(port, "Hello, $"));
    }

    @Override
    public void close() {
        if (isStarted) {
            sockets.forEach((ignored, socket) -> socket.close());
            sockets.clear();
            portService.close();
            handlingService.close();
        }
        isStarted = false;
    }

    public HelloUDPServer() {
        isStarted = false;
    }

    public static void main(final String[] args) {
        BaseHello.validateArgs(args, 2, "number_of_port count_threads");

        final HelloUDPServer server = new HelloUDPServer();
        server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }
}

