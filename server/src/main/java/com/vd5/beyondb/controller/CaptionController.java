package com.vd5.beyondb.controller;

import com.vd5.beyondb.model.CaptionLog;
import com.vd5.beyondb.model.dto.request.CaptureDto;
import com.vd5.beyondb.model.dto.request.CaptureMlDto;
import com.vd5.beyondb.model.dto.response.CaptionDto;
import com.vd5.beyondb.service.CaptionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("api/caption")
public class CaptionController {

    @Autowired
    private CaptionLogService captionLogService;

    String mlBaseUrl = "http://70.12.130.121:5000/";      // ML server URL

    @PostMapping(path = "")
    public String captionImage(@RequestBody CaptureDto captureDto) {
        log.info("===== captionImage() =====");
        CaptureMlDto captureMlDto = new CaptureMlDto(
            captureDto.getImgPath());  // request dto to ML server
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(mlBaseUrl + "s3/imagecaption", captureMlDto,
            String.class);
        Long logId = captionLogService.addCaptionLog(result);
        return Long.toString(logId);
    }

    @GetMapping(path = "/{logId}")
    public ResponseEntity<CaptionDto> captionResult(@PathVariable("logId") String logId) {
        CaptionLog captionLog = captionLogService.findCaptionLogById(Long.parseLong(logId));
        CaptionDto captionDto = new CaptionDto(captionLog.getContent());
        return new ResponseEntity<>(captionDto, HttpStatus.OK);
    }
}
