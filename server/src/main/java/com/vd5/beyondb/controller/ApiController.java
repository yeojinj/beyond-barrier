package com.vd5.beyondb.controller;

import com.vd5.beyondb.model.dto.request.CaptureDto;
import com.vd5.beyondb.model.dto.request.CaptureMlDto;
import com.vd5.beyondb.model.dto.response.CaptionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("api")
public class ApiController {
    @PostMapping(path="/caption")
    public ResponseEntity<CaptionDto> captionImage(@RequestBody CaptureDto captureDto){
        String baseUrl = "http://70.12.130.121:5000/";      // ML server URL
        CaptureMlDto captureMlDto = new CaptureMlDto(captureDto.getImgPath());  // request dto to ML server
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.postForObject(baseUrl + "s3/imagecaption", captureMlDto, String.class);
        CaptionDto captionDto = new CaptionDto(result);
        return new ResponseEntity<>(captionDto, HttpStatus.OK);
    }
}
