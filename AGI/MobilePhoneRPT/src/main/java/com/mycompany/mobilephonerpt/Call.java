/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.mycompany.mobilephoneRPT;

/**
 *
 * @author Omar Gabr
 */
public class Call {
 

    public Call(Boolean isActive) {
        this.isActive = isActive;
        this.duration=0;
    }
    
    // making it volatile so one thread can read and other right safelly
    private volatile Boolean isActive ; 
    
    private Integer duration;
    
    public void incrementDuration(){
        this.duration ++;
    }

    public  void setDuration(Integer duration) {
        this.duration = duration;
    }

    public  Integer getDuration() {
        return duration;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
   

}
