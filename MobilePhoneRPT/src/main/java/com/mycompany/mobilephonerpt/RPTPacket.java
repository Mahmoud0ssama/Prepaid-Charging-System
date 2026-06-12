/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.mycompany.mobilephonerpt;

/**
 *
 * @author Omar Gabr
 */
public class RPTPacket {
    
    // 16-bit sequence number (0 to 65535)
    private  static int sequence = 0; 
    
    // 32-bit timestamp tracking audio samples
    private static int timestamp = 0; 
    
    // 32-bit constant identifier for this stream session
    private static final int ssrc = 123456; 

    public static byte[] createRPTPacket(byte[] voicePayload) {
        // RTP Fixed Header is 12 bytes + the size of your voice samples
        int headerSize = 12;
        byte[] packet = new byte[headerSize + voicePayload.length]; 
        
        // BYTE 0 & 1
        packet[0] = (byte) 0x80; // Version 2, no padding, no extension, no CSRC
        packet[1] = (byte) 96;   // Marker = 0, Payload Type = 96
        
        // BYTES 2 & 3: Sequence Number (16-bit)
        // saving sequence in byte 2 and 3
        packet[2] = (byte) (sequence >> 8);
        packet[3] = (byte) (sequence);
        
        // BYTES 4 ,5,6, 7: Timestamp (32-bit) 
        packet[4] = (byte) (timestamp >> 24);
        packet[5] = (byte) (timestamp >> 16);
        packet[6] = (byte) (timestamp >> 8);
        packet[7] = (byte) (timestamp);
        
        //  BYTES 8 9,10, 11: SSRC Identifier (32-bit) 
        packet[8] = (byte) (ssrc >> 24);
        packet[9] = (byte) (ssrc >> 16);
        packet[10] = (byte) (ssrc >> 8);
        packet[11] = (byte) (ssrc);
        
        //  PAYLOAD: Copy voice bytes starting right after the header 
        System.arraycopy(voicePayload, 0, packet, headerSize, voicePayload.length);
        
        // --- UPDATE INTERNAL STATES FOR THE NEXT PACKET ---
        // Crucial: Increment sequence by 1 for the next packet
        sequence++; 
        //set back to zero each (2^16) -1 --> each two bytes 
        if (sequence > 65535) {
            sequence = 0; 
        }
        
        // Increment timestamp by the number of samples sent in this frame
        timestamp += voicePayload.length; 
        
        return packet;
    }
}