package com.vd5.beyondb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "casting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Casting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(name = "cast_name", length = 20, nullable = false)
    private String cast_name;

    @Builder
    public Casting(Program program, String cast_name) {
        this.program = program;
        this.cast_name = cast_name;
    }
}
