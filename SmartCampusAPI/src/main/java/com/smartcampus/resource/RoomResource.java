/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    /**
     * GET /api/v1/rooms
     * Returns the full list of all rooms.
     */
    @GET
    public Response getAllRooms() {
        return Response.ok(new ArrayList<>(DataStore.rooms.values())).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getName() == null || room.getName().isBlank()) {
            throw new BadRequestException("Room name is required.");
        }
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId(UUID.randomUUID().toString());
        }
        if (DataStore.rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of(
                        "status",  409,
                        "error",   "CONFLICT",
                        "message", "A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        DataStore.rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Retrieves a specific room by its ID.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            throw new NotFoundException("Room not found with ID: " + roomId);
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Deletes a room only if it has no sensors assigned.
     * Throws RoomNotEmptyException (409) if sensors still exist in the room.
     *
     * Idempotency note: DELETE is idempotent in terms of side effects.
     * The first call removes the room (204). Every subsequent call returns 404
     * because the room no longer exists. No unintended state changes occur.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            throw new NotFoundException("Room not found with ID: " + roomId);
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }
        DataStore.rooms.remove(roomId);
        return Response.noContent().build();
    }
}
