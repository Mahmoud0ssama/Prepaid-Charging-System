/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.mobilephone;

/**
 *
 * @author Omar Gabr
 */
import com.mycompany.mobilephone.Call;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Scanner;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.LineUnavailableException;

public class MobilePhone {

    public static void main(String[] args) {
        Call call = new Call(true);
        Scanner sc = new Scanner(System.in);
        //Thread 0: call start logic (connection and RTP )
        Thread voiceSender = new Thread(() -> {

            AudioFormat format = new AudioFormat(
                    44100.0f, // Sample Rate
                    16, // Sample Size
                    1, // Mono channel
                    true, // Signed
                    false // Little Endian
            );
            try {

                // initializing the port and address 
                InetAddress receiverIP = InetAddress.getByName("127.0.0.1");
                int receiverPort = 5000;

                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);

                microphone.open(format);
                microphone.start();

                DatagramSocket socket = new DatagramSocket();
                byte[] buffer = new byte[1024];
                while (call.getIsActive() && call.getStatus() != Call.Status.ENDED) {
                    microphone.read(buffer, 0, buffer.length);

                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, receiverIP, receiverPort);
                    socket.send(packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //Thread 1: for manage counting and printing call duration 
        Thread callLogThread = new Thread(() -> {
            System.out.println("Starting voice call as MSISDN" + args[0]);
            while (call.getIsActive()) {
                //every 1 minute print
                try {

                    Thread.sleep(1000);

                    call.incrementDuration();
                    System.out.println(call.getDuration() + " minutes elapsed ");
                } catch (InterruptedException ex) {
                    System.getLogger(MobilePhone.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }
        });

        /// thread 2 : for managing call ending
        Thread callStatusHandling = new Thread(() -> {

            System.out.println("for call ending: close");
            System.out.println("for call Start: start");
            System.out.println("For End: quit");

            while (true) {
                String userinput = sc.nextLine();
                if (userinput.equals("start")) {

                    //if no call in process 
                    if (call.getStatus() != Call.Status.IDLE) {
                        System.out.println("INVALID: Call is on process, Type [quit] to end it first");

                    }
                    call.setIsActive(true);
                    //for now , I'll set to answered directlly 
                    //in future there could be logic for calling and riningn  
                    call.setStatus(Call.Status.ANSWERED);

                    System.out.println("Call Started:");
                    //start the voice call and send packets 
                    voiceSender.start();
                    //start logging the call 
                    callLogThread.start();

                } else if (userinput.equals("close")) {
                    call.setIsActive(false);    
                    //implement call ending logic 

                    call.setStatus(Call.Status.IDLE);
                }else if(userinput.equals("quit")){
                    System.out.println("Bye.... ");
                    return; 
                }

                

            }
        });
        callStatusHandling.start();

        // if call anwered then start the thread 
//        while( true ){ 
//            if(call.getStatus() == Call.Status.ANSWERED){
//                System.out.println("Call Started:");
//                //start the voice call and send packets 
//                voiceSender.start();
//                //start logging the call 
//                callLogThread.start();
//                
//            }else if (call.getStatus() == Call.Status.ENDED)
//                System.out.println("Call Ended");
//            
//        }
    }
}
