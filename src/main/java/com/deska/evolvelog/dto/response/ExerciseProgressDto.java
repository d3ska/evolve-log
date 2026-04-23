package com.deska.evolvelog.dto.response;

import java.util.List;

public record ExerciseProgressDto(
        String exerciseName,
        List<ExerciseProgressPointDto> history,
        PersonalRecordDto personalRecord
) {}
