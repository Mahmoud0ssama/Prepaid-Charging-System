package com.omar.agi;

import org.asteriskjava.fastagi.AgiServer;
import org.asteriskjava.fastagi.DefaultAgiServer;
/**
 *
 * @author omar
 */
public class App {

    public static void main(String[] args) throws Exception {
        AgiServer server = new DefaultAgiServer();
        System.out.println("AGI Server starting on port 4573...");
        server.startup(); // Listens for Asterisk
    }
}
