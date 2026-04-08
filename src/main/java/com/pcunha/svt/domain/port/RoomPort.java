package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.Room;

import java.time.LocalDate;
import java.util.Optional;

public interface RoomPort {
    Room save(Room room);

    Optional<Room> findById(String id);

    Optional<Room> findDailyRoom(GameMode mode, LocalDate dateBucket);
}
