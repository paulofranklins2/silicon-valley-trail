package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.RoomType;
import com.pcunha.svt.domain.model.Room;
import com.pcunha.svt.domain.port.RoomPort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class RoomAdapter implements RoomPort {
    private final RoomRepository repository;

    public RoomAdapter(RoomRepository repository) {
        this.repository = repository;
    }

    @Override
    public Room save(Room room) {
        return repository.save(room);
    }

    @Override
    public Optional<Room> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Room> findDailyRoom(GameMode mode, LocalDate dateBucket) {
        return repository.findByTypeAndModeAndDateBucket(RoomType.DAILY, mode, dateBucket);
    }
}
