package com.vd5.beyondb.service;

import com.vd5.beyondb.model.Casting;
import com.vd5.beyondb.model.Program;
import com.vd5.beyondb.repository.CastingRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CastingService {

    @Autowired
    private CastingRepository castingRepository;

    public List<Casting> findByProgram(Program program) {
        return castingRepository.findByProgram(program);
    }

}