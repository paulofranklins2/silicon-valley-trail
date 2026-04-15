package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.RoomType;
import com.pcunha.svt.domain.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findByTypeAndModeAndDateBucket(RoomType type, GameMode mode, LocalDate dateBucket);

    List<Room> findAllByTypeAndDateBucketBefore(RoomType type, LocalDate dateBucket);

    void deleteByIdIn(Collection<String> ids);
}
