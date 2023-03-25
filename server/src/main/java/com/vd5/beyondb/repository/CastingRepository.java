package com.vd5.beyondb.repository;

import com.vd5.beyondb.model.Casting;
import com.vd5.beyondb.model.Program;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CastingRepository extends JpaRepository<Casting, Long> {

    List<Casting> findByProgram(Program program);
}

