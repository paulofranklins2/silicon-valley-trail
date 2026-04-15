package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.RoomType;
import com.pcunha.svt.domain.model.Room;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface RoomPort {
    Room save(Room room);

    Optional<Room> findById(String id);

    Optional<Room> findDailyRoom(GameMode mode, LocalDate dateBucket);

    List<Room> findRoomsByTypeBeforeDate(RoomType type, LocalDate dateBucket);

    void deleteByIds(List<String> ids);
}
