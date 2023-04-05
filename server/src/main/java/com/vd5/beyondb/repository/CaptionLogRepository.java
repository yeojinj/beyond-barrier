package com.vd5.beyondb.repository;

import com.vd5.beyondb.model.CaptionLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptionLogRepository extends JpaRepository<CaptionLog, Long> {

    Optional<CaptionLog> findCaptionLogById(Long id);
}
