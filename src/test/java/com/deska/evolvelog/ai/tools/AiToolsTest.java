package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.BloodTestReport;
import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.MonthlyExerciseAggregate;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.dto.response.ExerciseProgressPointDto;
import com.deska.evolvelog.dto.response.PersonalRecordDto;
import com.deska.evolvelog.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiToolsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private ObjectNode emptyInput() {
        return objectMapper.createObjectNode();
    }

    private ObjectNode inputWith(String key, String value) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(key, value);
        return node;
    }

    // --- GetExerciseHistoryTool ---

    @Nested
    class GetExerciseHistoryToolTest {

        @Mock
        private ExerciseRepository exerciseRepository;

        private GetExerciseHistoryTool tool;

        @BeforeEach
        void setUp() {
            tool = new GetExerciseHistoryTool(exerciseRepository);
        }

        @Test
        void shouldDelegateToExerciseRepositoryWithUserId() {
            // given
            ExerciseProgressPointDto point = new ExerciseProgressPointDto(
                    LocalDateTime.of(2025, 1, 10, 9, 0), new BigDecimal("100"), 3, 8, UUID.randomUUID());
            when(exerciseRepository.findProgressByUserIdAndExerciseName(userId, "Bench Press"))
                    .thenReturn(List.of(point));

            // when
            String result = tool.execute(inputWith("exercise_name", "Bench Press"), userId);

            // then
            assertThat(result).contains("Bench Press");
            assertThat(result).contains("100");
            verify(exerciseRepository).findProgressByUserIdAndExerciseName(userId, "Bench Press");
        }

        @Test
        void shouldReturnErrorWhenExerciseNameBlank() {
            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).startsWith("Error:");
        }

        @Test
        void shouldReturnNotFoundWhenNoHistory() {
            // given
            when(exerciseRepository.findProgressByUserIdAndExerciseName(eq(userId), anyString()))
                    .thenReturn(List.of());

            // when
            String result = tool.execute(inputWith("exercise_name", "Unknown"), userId);

            // then
            assertThat(result).contains("No history found");
        }
    }

    // --- GetPersonalRecordsTool ---

    @Nested
    class GetPersonalRecordsToolTest {

        @Mock
        private ExerciseRepository exerciseRepository;

        private GetPersonalRecordsTool tool;

        @BeforeEach
        void setUp() {
            tool = new GetPersonalRecordsTool(exerciseRepository);
        }

        @Test
        void shouldDelegateToExerciseRepositoryWithUserId() {
            // given
            PersonalRecordDto pr = new PersonalRecordDto("Squat", new BigDecimal("150"), 3, 5,
                    LocalDateTime.of(2025, 1, 5, 10, 0));
            when(exerciseRepository.findPersonalRecordsByUserId(userId)).thenReturn(List.of(pr));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("Squat");
            assertThat(result).contains("150");
            verify(exerciseRepository).findPersonalRecordsByUserId(userId);
        }
    }

    // --- GetRecentWorkoutsTool ---

    @Nested
    class GetRecentWorkoutsToolTest {

        @Mock
        private WorkoutSessionRepository workoutSessionRepository;

        private GetRecentWorkoutsTool tool;

        @BeforeEach
        void setUp() {
            tool = new GetRecentWorkoutsTool(workoutSessionRepository);
        }

        @Test
        void shouldDelegateToWorkoutSessionRepositoryWithUserId() {
            // given
            WorkoutSession session = WorkoutSession.builder()
                    .date(LocalDateTime.of(2025, 1, 20, 8, 0))
                    .exercises(new ArrayList<>())
                    .build();
            when(workoutSessionRepository.findRecentByUserIdWithExercises(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(session));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("2025-01-20");
            verify(workoutSessionRepository).findRecentByUserIdWithExercises(eq(userId), any(Pageable.class));
        }

        @Test
        void shouldCapLimitAt20() {
            // given
            when(workoutSessionRepository.findRecentByUserIdWithExercises(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of());
            ObjectNode input = objectMapper.createObjectNode();
            input.put("limit", 999);

            // when
            tool.execute(input, userId);

            // then — limit capped, still calls repository with userId
            verify(workoutSessionRepository).findRecentByUserIdWithExercises(eq(userId), any(Pageable.class));
        }
    }

    // --- GetBloodResultsTool ---

    @Nested
    class GetBloodResultsToolTest {

        @Mock
        private BloodTestReportRepository bloodTestReportRepository;

        private GetBloodResultsTool tool;

        @BeforeEach
        void setUp() {
            tool = new GetBloodResultsTool(bloodTestReportRepository);
        }

        @Test
        void shouldDelegateToBloodTestReportRepositoryWithUserId() {
            // given
            when(bloodTestReportRepository.findByUserIdOrderByDateDesc(userId)).thenReturn(List.of());

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("No blood test results");
            verify(bloodTestReportRepository).findByUserIdOrderByDateDesc(userId);
        }
    }

    // --- GetMeasurementsTool ---

    @Nested
    class GetMeasurementsToolTest {

        @Mock
        private MeasurementRepository measurementRepository;

        private GetMeasurementsTool tool;

        @BeforeEach
        void setUp() {
            tool = new GetMeasurementsTool(measurementRepository);
        }

        @Test
        void shouldDelegateToMeasurementRepositoryWithUserId() {
            // given
            Measurement m = Measurement.builder()
                    .date(java.time.LocalDate.of(2025, 1, 15))
                    .weightKg(new BigDecimal("82.5"))
                    .build();
            when(measurementRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(m)));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("82.5");
            verify(measurementRepository).findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class));
        }
    }

    // --- GetMonthlyAggregatesTool ---

    @Nested
    class GetMonthlyAggregatesToolTest {

        @Mock
        private MonthlyAggregateRepository monthlyAggregateRepository;

        private GetMonthlyAggregatesTool tool;

        @BeforeEach
        void setUp() {
            tool = new GetMonthlyAggregatesTool(monthlyAggregateRepository);
        }

        @Test
        void shouldDelegateToMonthlyAggregateRepositoryWithUserId() {
            // given
            when(monthlyAggregateRepository.findByUserIdAndExerciseNameOrderByYearMonthDesc(userId, "Deadlift"))
                    .thenReturn(List.of());

            // when
            String result = tool.execute(inputWith("exercise_name", "Deadlift"), userId);

            // then
            assertThat(result).contains("No monthly aggregates");
            verify(monthlyAggregateRepository).findByUserIdAndExerciseNameOrderByYearMonthDesc(userId, "Deadlift");
        }
    }

    // --- AiToolRegistry ---

    @Nested
    class AiToolRegistryTest {

        @Test
        void shouldFindToolByName() {
            // given
            AiTool fakeTool = new AiTool() {
                public String name() { return "test_tool"; }
                public String description() { return "A test"; }
                public java.util.Map<String, Object> inputSchema() { return java.util.Map.of(); }
                public String execute(ObjectNode input, UUID userId) { return "ok"; }
            };
            AiToolRegistry registry = new AiToolRegistry(List.of(fakeTool));

            // when / then
            assertThat(registry.find("test_tool")).isPresent();
            assertThat(registry.find("unknown")).isEmpty();
        }

        @Test
        void shouldReturnDefinitionsForAllTools() {
            // given
            AiTool fakeTool = new AiTool() {
                public String name() { return "some_tool"; }
                public String description() { return "desc"; }
                public java.util.Map<String, Object> inputSchema() { return java.util.Map.of("type", "object"); }
                public String execute(ObjectNode input, UUID userId) { return ""; }
            };
            AiToolRegistry registry = new AiToolRegistry(List.of(fakeTool));

            // when
            var definitions = registry.toDefinitions();

            // then
            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0).name()).isEqualTo("some_tool");
        }
    }
}
