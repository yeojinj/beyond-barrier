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

}
