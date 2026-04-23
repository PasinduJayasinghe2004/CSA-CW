/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Logs every incoming request and outgoing response.
 *
 * Using a JAX-RS filter for cross-cutting concerns like logging is
 * far better than inserting Logger calls in every resource method:
 * - One place to change log format across the entire API
 * - No risk of missing a new endpoint
 * - Resource methods stay focused on business logic only
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        LOG.info("[REQUEST]  " + req.getMethod() + " " + req.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        LOG.info("[RESPONSE] " + req.getMethod() + " "
                + req.getUriInfo().getRequestUri() + " -> " + res.getStatus());
    }
}
