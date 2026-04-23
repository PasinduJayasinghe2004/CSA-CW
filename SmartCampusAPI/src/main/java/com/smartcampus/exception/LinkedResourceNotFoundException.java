/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {

    private final String referencedId;

    public LinkedResourceNotFoundException(String referencedId) {
        super("Referenced resource with ID '" + referencedId + "' does not exist.");
        this.referencedId = referencedId;
    }

    public String getReferencedId() { return referencedId; }
}

