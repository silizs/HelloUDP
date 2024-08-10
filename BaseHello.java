package info.kgeorgiy.ja.petrova.hello;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BaseHello {
    public static final Charset STANDART_CHARSET = StandardCharsets.UTF_8;

    private static void printValidateError(String structureArgs) {
        System.err.println("Incorrect number of arguments." + File.separator +
                "The arguments should be like ' " + structureArgs + " '");
    }

    public static void validateArgs(String[] args, int size, String structureArgs) {
        if (args == null || args.length != size) {
            printValidateError(structureArgs);
            return;
        }
        for (int i = 0; i < size; i++) {
            if (args[i] == null) {
                printValidateError(structureArgs);
                return;
            }
        }
    }

    public static void printError(String message, Exception exception) {
        System.err.println(message + File.separator + exception.getMessage());
    }

    public static void print(String message) {
        System.err.println(message);
    }

    public static String getString(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), STANDART_CHARSET);
    }

    public static void send(DatagramSocket socket, DatagramPacket packet, String message) {
        try {
            socket.send(packet);
        } catch (IOException | SecurityException | IllegalBlockingModeException | IllegalArgumentException e) {
            BaseHello.printError(message, e);
        }
    }

    public static void receive(DatagramSocket socket, DatagramPacket packet, String message) {
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException ignore) {

        } catch (IOException | SecurityException |
                 IllegalBlockingModeException | IllegalArgumentException e) {
            BaseHello.printError(message, e);
        }
    }

    private static boolean validateCount(int count, String typeCount) {
        if (count <= 0) {
            BaseHello.print("Incorrect count " + typeCount);
            return false;
        }
        return true;
    }

    public static boolean validateRunArgs(int countThreads, int countRequests) {
        return validateCount(countThreads, "threads")
                && validateCount(countRequests, "requests");
    }

    public static boolean validateStartArgs(int countThreads, Map<Integer, String> mapPorts) {
        boolean threadsValidate = validateCount(countThreads, "threads");
        if (mapPorts == null || mapPorts.isEmpty()) {
            BaseHello.print("Map of ports must contains at least one port");
            return false;
        }
        return threadsValidate;
    }
}
