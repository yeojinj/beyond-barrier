package com.vd5.beyondb.model;

import com.vd5.beyondb.utils.EmptyStringToNullConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "caption_log")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaptionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", length = 1000)
    private String content;

    @Convert(converter = EmptyStringToNullConverter.class)
    @Column(name = "names", length = 1000)
    private String names;

    @Column(name = "img_path", length = 200)
    private String imgPath;

    @CreatedDate
    @Column(name = "log_time")
    private LocalDateTime log_time;

    @Builder
    public CaptionLog(String content, String names, String imgPath) {
        this.content = content;
        this.names = names;
        this.imgPath = imgPath;
    }

}
