/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     * Returns all sensors. Optionally filter by ?type= query parameter.
     *
     * @QueryParam is preferred over a path segment like /sensors/type/CO2 because:
     * - Query params are optional — omitting them returns all results
     * - Path segments imply a resource identity; filtering is not a resource
     * - Multiple filters can be combined: ?type=CO2&status=ACTIVE
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(DataStore.sensors.values());
        if (type != null && !type.isBlank()) {
            result = result.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(result).build();
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor. Validates that the roomId exists.
     * Throws LinkedResourceNotFoundException (422) if roomId is not found.
     *
     * @Consumes(APPLICATION_JSON) means JAX-RS will only accept requests
     * with Content-Type: application/json. Sending text/plain or
     * application/xml results in HTTP 415 Unsupported Media Type automatically.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            throw new BadRequestException("Request body must not be empty.");
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            throw new BadRequestException("roomId is required.");
        }
        // Validate the referenced room exists
        if (!DataStore.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(sensor.getRoomId());
        }
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(UUID.randomUUID().toString());
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }
        DataStore.sensors.put(sensor.getId(), sensor);
        // Link this sensor to its room
        DataStore.rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Retrieves a specific sensor by its ID.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        return Response.ok(sensor).build();
    }

    /**
     * PUT /api/v1/sensors/{sensorId}
     * Updates a sensor (e.g. change status from MAINTENANCE to ACTIVE).
     */
    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updated) {
        Sensor existing = DataStore.sensors.get(sensorId);
        if (existing == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        if (updated.getType()   != null) existing.setType(updated.getType());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        if (updated.getRoomId() != null) existing.setRoomId(updated.getRoomId());
        existing.setCurrentValue(updated.getCurrentValue());
        return Response.ok(existing).build();
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     * Removes a sensor and unlinks it from its room.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        // Unlink from parent room
        if (sensor.getRoomId() != null && DataStore.rooms.containsKey(sensor.getRoomId())) {
            DataStore.rooms.get(sensor.getRoomId()).getSensorIds().remove(sensorId);
        }
        DataStore.sensors.remove(sensorId);
        return Response.noContent().build();
    }

    /**
     * Sub-resource locator for /api/v1/sensors/{sensorId}/readings
     *
     * This method has NO HTTP method annotation (@GET, @POST etc.).
     * JAX-RS calls this locator first to get the SensorReadingResource
     * object, then dispatches the actual HTTP method to that object.
     *
     * This pattern keeps large APIs manageable by delegating sub-path
     * logic to dedicated classes rather than cramming everything into
     * one giant resource class.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        if (!DataStore.sensors.containsKey(sensorId)) {
            throw new NotFoundException("Sensor not found with ID: " + sensorId);
        }
        return new SensorReadingResource(sensorId);
    }
}
