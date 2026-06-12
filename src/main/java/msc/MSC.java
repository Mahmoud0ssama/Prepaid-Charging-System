package msc;

import java.io.*;
import java.net.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class MSC extends DatabaseManager {
    private static final int TCP_PORT = 5010;
    private static final int UDP_PORT = 5011;

    static {
        System.out.println("Waiting for voice call Signaling start message via TCP");
    }
private static final Logger logger =
            Logger.getLogger(MSC.class.getName());
    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleCall(clientSocket)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void handleCall(Socket socket) {
        String msisdn = null;
        LocalDateTime callStartTime = null;
        BigDecimal initialBalance = null;
        ScheduledExecutorService scheduler = null;
        AudioPlayer audioPlayer = null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null && msisdn == null) {
                if (line.startsWith("Start Call ")) {
                      msisdn = line.substring("Start Call ".length()).trim();
                      logger.info("[MSC] Recieved MSISDN= "+ msisdn);
                    System.out.println("Accept Voice call start signaling message from MSISDN " + msisdn);
                    break;
                }
            }

            if (msisdn == null) return;

            if (!userExists(msisdn)) {
                System.out.println("User not found on DB for MSISDN: " + msisdn);
                generateCDR(msisdn, LocalDateTime.now(), LocalDateTime.now(), BigDecimal.ZERO, "user not found on DB");
                socket.close();
                return;
            }

            initialBalance = getBalance(msisdn);
            callStartTime = LocalDateTime.now();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            final String msisdnFinal = msisdn;
            scheduler.scheduleAtFixedRate(() -> chargeMinute(msisdnFinal), 1, 1, TimeUnit.MINUTES);

            audioPlayer = new AudioPlayer();
            new Thread(audioPlayer).start();

            System.out.println("Capturing UDP traffic and play via speaker …..");

            boolean endCallReceived = false;
            while ((line = in.readLine()) != null) {
                if ("End Call".equals(line)) {
                    System.out.println("Call End after receiving end call signaling message");
                    endCallReceived = true;
                    break;
                }
            }

            if (!endCallReceived) {
                System.out.println("Call End due to client disconnect");
            }

            if (scheduler != null) scheduler.shutdown();
            if (audioPlayer != null) audioPlayer.stop();
            generateCDR(msisdn, callStartTime, LocalDateTime.now(), initialBalance, "Normal call Clearing");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdown();
            if (audioPlayer != null) audioPlayer.stop();
        }
    }

    private static void generateCDR(String msisdn, LocalDateTime startTime, LocalDateTime endTime, BigDecimal initialBalance, String callResult) {
        long duration = 0;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal balanceAfter = initialBalance;

        if ("Normal call Clearing".equals(callResult)) {
            long seconds = java.time.Duration.between(startTime, endTime).getSeconds();
            duration = (long) Math.ceil(seconds / 60.0);
            if (duration < 1) duration = 1;
            
            cost = BigDecimal.valueOf(duration);
            balanceAfter = initialBalance.subtract(cost);
        }

        try (Connection conn = getConnection()) {
            insertCDR(msisdn, startTime, endTime, (int) duration, callResult, cost, balanceAfter, conn);
        } catch (Exception ignored) {}

        String cdrLine = String.format("%s, %s, %s, %d, %s, %s, %s",
                msisdn, startTime, endTime, duration, callResult, cost, balanceAfter);
        System.out.println("Generating CDR line: " + cdrLine);

        try (FileWriter fw = new FileWriter("/tmp/calls.cdr", true)) {
            fw.write(cdrLine + "\n");
        } catch (IOException ignored) {}
    }
}