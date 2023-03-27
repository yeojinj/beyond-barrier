package com.vd5.beyondb.service;

import com.vd5.beyondb.model.Program;
import com.vd5.beyondb.model.ProgramLog;
import com.vd5.beyondb.repository.ProgramLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProgramLogService {

    @Autowired
    private ProgramLogRepository programLogRepository;

    public Long addProgramLog(Program program) {
        ProgramLog programLog = ProgramLog.builder().program(program).build();
        programLogRepository.save(programLog);
        return programLog.getId();
    }

}
