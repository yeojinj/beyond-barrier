package com.vd5.beyondb.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CaptureDto {
    private String deviceId;
    private String imgPath;
    private String captureTime;
}
