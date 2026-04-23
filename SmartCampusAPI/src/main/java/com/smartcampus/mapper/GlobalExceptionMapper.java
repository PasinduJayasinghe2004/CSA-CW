/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catches any unhandled Throwable and returns a safe 500 response.
 * Never exposes stack traces to clients — they reveal class names,
 * library versions, and internal paths that attackers can exploit.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable t) {
        LOG.log(Level.SEVERE, "Unhandled exception: " + t.getMessage(), t);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  500);
        body.put("error",   "INTERNAL_SERVER_ERROR");
        body.put("message", "An unexpected error occurred. Please contact the administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}

