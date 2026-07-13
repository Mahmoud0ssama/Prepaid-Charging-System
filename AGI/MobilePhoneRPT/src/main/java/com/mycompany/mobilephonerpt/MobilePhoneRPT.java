/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.mobilephoneRPT;

/**
 *
 * @author Omar Gabr
 */
import com.mycompany.mobilephonerpt.Call;
import com.mycompany.mobilephonerpt.RPTPacket;
import java.net.DatagramSocket;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Scanner;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.LineUnavailableException;

public class MobilePhoneRPT {
 
    public static void main(String[] args) {
        Call call = new Call(true);
        
        
         Scanner sc = new Scanner(System.in);
         //Thread 0: call start logic (connection and RTP )
         Thread voiceSender =  new Thread ( ()-> {
        
               InetAddress receiverIP = InetAddress.getByName("127.0.0.1");
               int receiverPort = 5000;
               
                 AudioFormat format = new AudioFormat(
                    44100.0f, // Sample Rate
                    16,       // Sample Size
                    1,        // Mono channel
                    true,     // Signed
                    false     // Little Endian
                    );
                  
                try {
                 DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                 TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);

                 microphone.open(format);
                 microphone.start();
                 
                 // reading the 160 bytes 
                 byte[] voiceData = new byte[160];
                 
                 DatagramSocket datagramSocket =new  DatagramSocket();

                 while (true) {

                  microphone.read(voiceData, 0, voiceData.length);
                  byte[] RPT= RPTPacket.createRPTPacket(voiceData);
                  
                  //creating the udp packet and send through the socket
                  DatagramPacket packet=new DatagramPacket(RPT, RPT.length, receiverIP, receiverPort);
                  datagramSocket.send(packet);
                     
                 }
                 
                 
                } catch (Exception e){ 
                    e.printStackTrace();
                }
         });
         
        //Thread 1: for manage counting and printing call duration 
        Thread callLogThread = new Thread (()-> { 
            System.out.println("Starting voice call as MSISDN" + args[0]);
        while (call.getIsActive()){
            //every 1 minute print
            try {
                

                Thread.sleep(1000);
                
                call.incrementDuration();
                System.out.println(call.getDuration()+" minutes elapsed ");
            } catch (InterruptedException ex) {
                System.getLogger(MobilePhone.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
        });
        callLogThread.start();
        
        /// thread 2 : for managing call ending
        Thread callEndingThread = new Thread ( ()-> { 
            System.out.print("for call ending press q");
           while(true){
            String userinput = sc.nextLine(); 
            if(userinput.equals("q")){
                call.setIsActive(false);
                //implement call ending logic 
                
                break; 
            }
           }
        });
        callEndingThread.start(); 
    }
}
