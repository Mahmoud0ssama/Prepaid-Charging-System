package msc;

import javax.sound.sampled.*;
import java.net.*;

public class AudioPlayer implements Runnable {
    private static final int UDP_PORT = 5011;
    private static final int PACKET_SIZE = 1024;
    private volatile boolean running = true;
    private DatagramSocket socket;

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 2, 8, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(format);
            speaker.start();

            socket = new DatagramSocket(UDP_PORT);
            byte[] buffer = new byte[PACKET_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                speaker.write(packet.getData(), packet.getOffset(), packet.getLength());
            }

            speaker.drain();
            speaker.close();
        } catch (Exception e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}