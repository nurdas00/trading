package com.example.trading.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.bytebuddy.asm.Advice;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class Candle {
    Float open;
    Float close;
    Float high;
    Float low;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", shape = JsonFormat.Shape.STRING)
    OffsetDateTime startDateTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", shape = JsonFormat.Shape.STRING)
    OffsetDateTime endDateTime;
}
