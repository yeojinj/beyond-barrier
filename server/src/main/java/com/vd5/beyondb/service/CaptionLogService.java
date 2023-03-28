package com.vd5.beyondb.service;

import com.vd5.beyondb.model.CaptionLog;
import com.vd5.beyondb.repository.CaptionLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CaptionLogService {

    @Autowired
    private CaptionLogRepository captionLogRepository;

    public Long addCaptionLog(String content) {
        CaptionLog captionLog = CaptionLog.builder().content(content).build();
        captionLogRepository.save(captionLog);
        return captionLog.getId();
    }

    public CaptionLog findCaptionLogById(Long id) {
        CaptionLog captionLog = captionLogRepository.findCaptionLogById(id)
            .orElseThrow(() -> new RuntimeException("해당 로그가 없습니다."));
        return captionLog;
    }

}
