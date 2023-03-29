package com.vd5.beyondb.model.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DetectDto {

    private int programId;
    private String programName;
    private String programContent;
    private List<String> programCasting;
}
