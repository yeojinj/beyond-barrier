package com.vd5.beyondb.service;

import com.vd5.beyondb.model.Program;
import com.vd5.beyondb.repository.ProgramRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProgramService {

    @Autowired
    private ProgramRepository programRepository;

    public List<Program> getProgramList() {
        return (List<Program>) programRepository.findAll();
    }

    public Program findById(int id) {
        programRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("DB에 존재하지 않는 프로그램 ID 입니다"));
        return programRepository.findById(id).get();
    }

    public void updateContent(int id, String content) {
        Program program = findById(id);
        program.updateContent(content);
    }

}
