package com.vd5.beyondb.controller;

import com.vd5.beyondb.model.Program;
import com.vd5.beyondb.model.dto.request.CaptureDto;
import com.vd5.beyondb.model.dto.request.CaptureMlDto;
import com.vd5.beyondb.model.dto.response.CaptionDto;
import com.vd5.beyondb.model.dto.response.DetectDto;
import com.vd5.beyondb.service.ProgramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("api")
public class ApiController {

    @Autowired
    private ProgramService programService;

    String mlBaseUrl = "http://70.12.130.121:5000/";      // ML server URL
    String crawlingBaseUrl = "https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=0&ie=utf8&query=";        // Naver search URL

    @PostMapping(path="/caption")
    public ResponseEntity<CaptionDto> captionImage(@RequestBody CaptureDto captureDto){
        CaptureMlDto captureMlDto = new CaptureMlDto(captureDto.getImgPath());  // request dto to ML server
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(mlBaseUrl + "s3/imagecaption", captureMlDto, String.class);
        CaptionDto captionDto = new CaptionDto(result);
        return new ResponseEntity<>(captionDto, HttpStatus.OK);
    }

    @PostMapping(path = "/program")
    public ResponseEntity<DetectDto> detectLogo(@RequestBody CaptureDto captureDto){
        log.info("===== detectLogo() =====");
        CaptureMlDto captureMlDto = new CaptureMlDto(captureDto.getImgPath());  // request dto to ML server
        RestTemplate restTemplate = new RestTemplate();
        int programId = Integer.parseInt(restTemplate.postForObject(mlBaseUrl + "s3/logodetect", captureMlDto, String.class));
        Program program = programService.findById(programId);       // get program name from DB by program id
        DetectDto detectDto = new DetectDto(program.getId(), program.getName());
        return new ResponseEntity<>(detectDto, HttpStatus.OK);
    }
}
