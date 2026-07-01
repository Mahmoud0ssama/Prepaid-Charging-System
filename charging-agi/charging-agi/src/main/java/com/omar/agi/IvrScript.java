/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.omar.agi;

import java.math.BigDecimal;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;
/**
 * @author omar
 */
public class IvrScript extends BaseAgiScript {
        private static final Logger logger = Logger.getLogger(IvrScript.class.getName());

    @Override
    public void service(AgiRequest request, AgiChannel channel) throws AgiException {
        //  Answer the inbound line
        answer(); 
        // give user interval of time to open the speaker 
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //  Play greeting audio file found in /usr/share/asterisk/sounds/en/
        streamFile("hello"); 
        
        // requesting msisdn from user 
        String msisdn = channel.getData(
            "enter-num-blacklist", // sound file
            60000,                       // ms timeout = 1 minute
            11                           // max digits
        );        

        
        // get the balance using API 
      //  BigDecimal balance = null ;
      int balance = 0 ; 
      String body=null ; 
        try {
         String url = "http://localhost:8080/api/users/balance?msisdn=" + msisdn;

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        HttpResponse<String> res =
                client.send(req,
                        HttpResponse.BodyHandlers.ofString());
        //converting the body to bigdecimal 
         body = res.body();
         balance = (int) Double.parseDouble(body);

       //  balance = new BigDecimal(body);
        logger.info("[IvrScript] Status: " + res.statusCode());
        logger.info("[IvrScript] Body: " + res.body());

        } catch (Exception e){
            e.printStackTrace();
        }
        
        streamFile("vm-youhave");
        sayNumber(String.valueOf(balance));
        
       try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
                
        streamFile("goodbye");
        

        
   /*     
        // 3. Loop menu to wait up to 5 seconds for keypad numbers
        while (true) {
            char digit = (char) waitForDigit(5000); 
            
            // If the timeout expires without a keypress, waitForDigit returns (char)-1 or 0
            if (digit == (char)-1 || digit == 0) {
                streamFile("invalid");
                continue;
            }
            
            if (digit == '1') {
                streamFile("transfer"); 
            } 
            else if (digit == '2') {
                // Option 2 placeholder: Add custom behavior or playback here
                streamFile("beep"); 
            } 
            else if (digit == '0') {
                streamFile("vm-goodbye"); // Standard asterisk file for goodbye
                break; // Breaks the loop so it proceeds to hangup()
            } 
            else {
                // Speaks back the unexpected digit pressed (e.g., if they press 3, 4, 5, etc.)
                sayDigits(String.valueOf(digit)); 
            }
        }
        
        // 4. Hang up the line cleanly
        hangup(); 
    */
    }

}