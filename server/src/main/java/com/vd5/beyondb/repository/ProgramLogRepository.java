package com.vd5.beyondb.repository;

import com.vd5.beyondb.model.ProgramLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramLogRepository extends JpaRepository<ProgramLog, Long> {

    Optional<ProgramLog> findProgramLogById(Long id);
}
