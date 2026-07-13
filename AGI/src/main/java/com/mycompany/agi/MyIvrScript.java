package com.mycompany.agi;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;

/**
 * Cleaned and corrected IVR Script
 *
 * @author omar
 */
public class MyIvrScript extends BaseAgiScript {

    @Override
    public void service(AgiRequest request, AgiChannel channel) throws AgiException {
        // 1. Answer the inbound line
        answer();

        // 2. Play greeting audio file found in /usr/share/asterisk/sounds/en/
        streamFile("hello-world");

        // 3. Loop menu to wait up to 5 seconds for keypad numbers
        while (true) {
            char digit = (char) waitForDigit(5000);

            // If the timeout expires without a keypress, waitForDigit returns (char)-1 or 0
            if (digit == (char) -1 || digit == 0) {
                streamFile("invalid");
                continue;
            }

            if (digit == '1') {
                streamFile("transfer");
            } else if (digit == '2') {
                // Option 2 placeholder: Add custom behavior or playback here
                streamFile("beep");
            } else if (digit == '0') {
                streamFile("vm-goodbye"); // Standard asterisk file for goodbye
                break; // Breaks the loop so it proceeds to hangup()
            } else {
                // Speaks back the unexpected digit pressed (e.g., if they press 3, 4, 5, etc.)
                sayDigits(String.valueOf(digit));
            }
        }

        // 4. Hang up the line cleanly
        hangup();
    }
}
