package me.justin.bedrockserverquery.data;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public record BedrockQuery(boolean online, String motd, int protocolVersion, String minecraftVersion, int playerCount, int maxPlayers, String software, String gamemode) {

    private static final byte IDUnconnectedPing = 0x01;
    private static final byte[] unconnectedMessageSequence = {0x00, (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, (byte) 0xfd, 0x12, 0x34, 0x56, 0x78};
    private static long dialerID = new Random().nextLong();

    public static BedrockQuery create(String serverAddress, int port) {
        try {
            InetAddress address = InetAddress.getByName(serverAddress);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

            dataOutputStream.writeByte(IDUnconnectedPing);
            dataOutputStream.writeLong(System.currentTimeMillis() / 1000);
            dataOutputStream.write(unconnectedMessageSequence);
            dataOutputStream.writeLong(dialerID++);

            byte[] requestData = outputStream.toByteArray();
            byte[] responseData = new byte[1024 * 1024 * 4];

            DatagramSocket socket = new DatagramSocket();
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, address, port);
            socket.send(requestPacket);

            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
            socket.setSoTimeout(2000);
            socket.receive(responsePacket);

            // MCPE;<motd>;<protocol>;<version>;<players>;<max players>;<id>;<sub motd>;<gamemode>;<not limited>;<port>;<port>
            String[] splittedData = new String(responsePacket.getData(), 35, responsePacket.getLength()).split(";");

            int protocol = Integer.parseInt(splittedData[2]);
            int playerCount = Integer.parseInt(splittedData[4]);
            int maxPlayers = Integer.parseInt(splittedData[5]);

            return new BedrockQuery(true, splittedData[1], protocol, splittedData[3], playerCount, maxPlayers, splittedData[7], splittedData[8]);
        } catch (Exception e) {
            return new BedrockQuery(false, "", -1, "", 0, 0, "", "");
        }
    }

}
