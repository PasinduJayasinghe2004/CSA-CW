/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;


@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("api",         "Smart Campus Sensor & Room Management API");
        info.put("version",     "1.0.0");
        info.put("description", "RESTful API for managing university campus rooms and IoT sensors");
        info.put("contact",     "admin@smartcampus.ac.uk");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/SmartCampusAPI/api/v1/rooms");
        resources.put("sensors", "/SmartCampusAPI/api/v1/sensors");
        info.put("resources", resources);

        // HATEOAS links
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/SmartCampusAPI/api/v1/");
        links.put("rooms",   "/SmartCampusAPI/api/v1/rooms");
        links.put("sensors", "/SmartCampusAPI/api/v1/sensors");
        info.put("_links", links);

        return Response.ok(info).build();
    }
}