package com.vd5.beyondb.repository;

import com.vd5.beyondb.model.Program;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Integer> {

    Optional<Program> findById(int id);
}
