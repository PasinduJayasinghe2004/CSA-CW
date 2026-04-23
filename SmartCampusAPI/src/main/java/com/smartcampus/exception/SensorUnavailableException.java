/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;

    public SensorUnavailableException(String sensorId) {
        super("Sensor '" + sensorId + "' is in MAINTENANCE and cannot accept readings.");
        this.sensorId = sensorId;
    }

    public String getSensorId() { return sensorId; }
}
