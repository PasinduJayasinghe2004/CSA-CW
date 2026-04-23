/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for /api/v1/sensors/{sensorId}/readings
 *
 * No @Path annotation on the class — path is set by the locator
 * method in SensorResource. No @Singleton — JAX-RS instantiates
 * this per-request via the locator.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns all historical readings for this sensor.
     */
    @GET
    public Response getReadings() {
        List<SensorReading> list = DataStore.readings.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(list).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading.
     *
     * Throws SensorUnavailableException (403) if sensor is in MAINTENANCE.
     * Side effect: updates currentValue on the parent Sensor object.
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.sensors.get(sensorId);

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }
        if (reading == null) {
            throw new BadRequestException("Request body must not be empty.");
        }

        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());

        DataStore.readings
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        // Side effect: keep sensor's currentValue in sync
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings/{readingId}
     * Returns a specific reading by ID.
     */
    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        List<SensorReading> list = DataStore.readings.getOrDefault(sensorId, new ArrayList<>());
        return list.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElseThrow(() -> new NotFoundException("Reading not found: " + readingId));
    }
}
