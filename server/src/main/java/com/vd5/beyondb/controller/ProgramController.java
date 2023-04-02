package com.vd5.beyondb.controller;

import com.vd5.beyondb.model.Casting;
import com.vd5.beyondb.model.Program;
import com.vd5.beyondb.model.ProgramLog;
import com.vd5.beyondb.model.dto.request.CaptureDto;
import com.vd5.beyondb.model.dto.request.CaptureMlDto;
import com.vd5.beyondb.model.dto.response.DetectDto;
import com.vd5.beyondb.service.CastingService;
import com.vd5.beyondb.service.ProgramLogService;
import com.vd5.beyondb.service.ProgramService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("api/program")
public class ProgramController {

    @Autowired
    private ProgramLogService programLogService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private CastingService castingService;

    String mlBaseUrl = "http://70.12.130.121:5000/";      // ML server URL
    String crawlingBaseUrl = "https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=0&ie=utf8&query=";        // Naver search URL

    @PostMapping(path = "")
    public String detectLogo(@RequestBody CaptureDto captureDto) {
        log.info("===== detectLogo() =====");
        CaptureMlDto captureMlDto = new CaptureMlDto(
            captureDto.getImgPath());  // request dto to ML server
        RestTemplate restTemplate = new RestTemplate();
        int programId = Integer.parseInt(
            restTemplate.postForObject(mlBaseUrl + "s3/logodetect", captureMlDto, String.class));
        if (programId == -1) {
            return "-2";
        } else {
            Program program = programService.findById(programId);
            Long logId = programLogService.addProgramLog(program);
            return Long.toString(logId);
        }
    }

    @GetMapping(path = "/{logId}")
    public ResponseEntity<DetectDto> detectResult(@PathVariable("logId") String logId) {
        ProgramLog programLog = programLogService.findProgramLogById(Long.parseLong(logId));
        Program program = programLog.getProgram();
        List<Casting> castingList = castingService.findByProgram(program);
        List<String> programCasting = new ArrayList<>();
        for (Casting c : castingList) {
            programCasting.add(c.getCast_name());
        }
        DetectDto detectDto = new DetectDto(program.getId(), program.getName(), program.getImgPath(),
            program.getContent(), programCasting);
        return new ResponseEntity<>(detectDto, HttpStatus.OK);
    }

    @Scheduled(cron = "0 25 11 29 * ?", zone = "Asia/Seoul")     // 한국 시간으로 매달 29일 11시 25분마다 크롤링
    public String crawlingProgram() {
        log.info("===== crawlingProgram() =====");
        List<Program> programList = programService.getProgramList();
        for (Program p : programList) {
            String programName = p.getName().replace(" ", "+");   // handle spacing
            try {
                Document rawData = Jsoup.connect(crawlingBaseUrl + programName).timeout(5000).get();
                Elements descElem = rawData.select("div.detail_info span.desc._text");
                if (descElem.size() == 0) {
                    log.info("프로그램 " + programName + "에 대한 정보가 없습니다.");
                } else {
                    String desc = descElem.get(0).text();
                    programService.updateContent(p.getId(), desc);
                }
                Document rawCastData = Jsoup.connect(crawlingBaseUrl + programName + "+출연진")
                    .timeout(5000).get();
                Elements castElem = rawCastData.select("div.item div.title_box strong");
                for (int i = 0; i < castElem.size(); i++) {
                    String castName = castElem.get(i).text();
                    castingService.addCast(p, castName);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return "크롤링 완료";
    }
}
