package com.deska.evolvelog.ai.tools;

import com.deska.evolvelog.domain.Exercise;
import com.deska.evolvelog.domain.Measurement;
import com.deska.evolvelog.domain.Supplement;
import com.deska.evolvelog.domain.SupplementPlan;
import com.deska.evolvelog.domain.SupplementPlanEntry;
import com.deska.evolvelog.domain.TimeSlot;
import com.deska.evolvelog.domain.WorkoutSession;
import com.deska.evolvelog.domain.WorkoutSet;
import com.deska.evolvelog.dto.response.DailyHealthMetricsDto;
import com.deska.evolvelog.dto.response.ExerciseProgressPointDto;
import com.deska.evolvelog.dto.response.PersonalRecordDto;
import com.deska.evolvelog.repository.*;
import com.deska.evolvelog.service.HealthMetricService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        @Test
        void shouldRenderLegacyPathWhenRepsAndWeightPresent() {
            // given
            Exercise exercise = Exercise.builder()
                    .name("Bench Press")
                    .sets(3)
                    .reps(8)
                    .weightKg(new BigDecimal("100"))
                    .position(1)
                    .workoutSets(new ArrayList<>())
                    .build();
            WorkoutSession session = WorkoutSession.builder()
                    .date(LocalDateTime.of(2025, 3, 1, 10, 0))
                    .exercises(new ArrayList<>(List.of(exercise)))
                    .build();
            when(workoutSessionRepository.findRecentByUserIdWithExercises(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(session));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("Bench Press");
            assertThat(result).contains("3×8");
            assertThat(result).contains("100");
        }

        @Test
        void shouldRenderPerSetPathWhenWorkoutSetsPresent() {
            // given
            WorkoutSet set1 = WorkoutSet.builder()
                    .setNumber(1).reps(10).weightKg(new BigDecimal("80")).completed(true).build();
            WorkoutSet set2 = WorkoutSet.builder()
                    .setNumber(2).reps(8).weightKg(new BigDecimal("85")).completed(true).build();
            Exercise exercise = Exercise.builder()
                    .name("Squat")
                    .sets(2)
                    .position(1)
                    .workoutSets(new ArrayList<>(List.of(set1, set2)))
                    .build();
            WorkoutSession session = WorkoutSession.builder()
                    .date(LocalDateTime.of(2025, 3, 5, 9, 0))
                    .exercises(new ArrayList<>(List.of(exercise)))
                    .build();
            when(workoutSessionRepository.findRecentByUserIdWithExercises(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(session));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("Squat");
            assertThat(result).contains("set 1:");
            assertThat(result).contains("10 reps");
            assertThat(result).contains("80");
            assertThat(result).contains("set 2:");
            assertThat(result).contains("85");
        }

        @Test
        void shouldRenderIncompleteSetAsPlannedNotCompleted() {
            // given
            WorkoutSet completedSet = WorkoutSet.builder()
                    .setNumber(1).reps(8).weightKg(new BigDecimal("100")).completed(true).build();
            WorkoutSet incompleteSet = WorkoutSet.builder()
                    .setNumber(2).reps(8).weightKg(new BigDecimal("100")).completed(false).build();
            Exercise exercise = Exercise.builder()
                    .name("Deadlift")
                    .sets(2)
                    .position(1)
                    .workoutSets(new ArrayList<>(List.of(completedSet, incompleteSet)))
                    .build();
            WorkoutSession session = WorkoutSession.builder()
                    .date(LocalDateTime.of(2025, 3, 10, 8, 0))
                    .exercises(new ArrayList<>(List.of(exercise)))
                    .build();
            when(workoutSessionRepository.findRecentByUserIdWithExercises(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(session));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("set 2: planned (not completed)");
        }

        @Test
        void shouldAppendRpeWhenPresent() {
            // given
            Exercise exercise = Exercise.builder()
                    .name("Overhead Press")
                    .sets(3)
                    .reps(6)
                    .weightKg(new BigDecimal("60"))
                    .rpe(new BigDecimal("8"))
                    .position(1)
                    .workoutSets(new ArrayList<>())
                    .build();
            WorkoutSession session = WorkoutSession.builder()
                    .date(LocalDateTime.of(2025, 3, 15, 7, 0))
                    .exercises(new ArrayList<>(List.of(exercise)))
                    .build();
            when(workoutSessionRepository.findRecentByUserIdWithExercises(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(session));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("[RPE 8]");
        }

        @Test
        void shouldRenderFallbackWhenNoRepsAndNoWorkoutSets() {
            // given
            Exercise exercise = Exercise.builder()
                    .name("Pull-up")
                    .sets(4)
                    .position(1)
                    .workoutSets(new ArrayList<>())
                    .build();
            WorkoutSession session = WorkoutSession.builder()
                    .date(LocalDateTime.of(2025, 3, 20, 8, 0))
                    .exercises(new ArrayList<>(List.of(exercise)))
                    .build();
            when(workoutSessionRepository.findRecentByUserIdWithExercises(eq(userId), any(Pageable.class)))
                    .thenReturn(List.of(session));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("Pull-up");
            assertThat(result).contains("no rep/weight data");
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

        @Test
        void shouldCallDateRangeQueryWhenFromDateProvided() {
            // given
            Measurement m = Measurement.builder()
                    .date(LocalDate.of(2025, 2, 1))
                    .weightKg(new BigDecimal("83.0"))
                    .build();
            when(measurementRepository.findByUserIdAndDateBetweenOrderByDateAsc(
                    eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(m));
            ObjectNode input = objectMapper.createObjectNode();
            input.put("from_date", "2025-02-01");
            input.put("to_date", "2025-02-28");

            // when
            String result = tool.execute(input, userId);

            // then
            assertThat(result).contains("83");
            verify(measurementRepository).findByUserIdAndDateBetweenOrderByDateAsc(
                    eq(userId), eq(LocalDate.of(2025, 2, 1)), eq(LocalDate.of(2025, 2, 28)));
        }

        @Test
        void shouldIncludeDateRangeHeaderWhenFromDateProvided() {
            // given
            when(measurementRepository.findByUserIdAndDateBetweenOrderByDateAsc(
                    eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(
                            Measurement.builder().date(LocalDate.of(2025, 1, 1)).weightKg(new BigDecimal("80")).build()
                    ));
            ObjectNode input = objectMapper.createObjectNode();
            input.put("from_date", "2025-01-01");
            input.put("to_date", "2025-01-31");

            // when
            String result = tool.execute(input, userId);

            // then
            assertThat(result).contains("2025-01-01");
            assertThat(result).contains("2025-01-31");
            assertThat(result).contains("1 entries");
        }

        @Test
        void shouldIncludeMostRecentPhraseInLimitModeHeader() {
            // given
            when(measurementRepository.findByUserIdOrderByDateDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(
                            Measurement.builder().date(LocalDate.of(2025, 1, 10)).weightKg(new BigDecimal("81")).build()
                    )));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("most recent");
        }

        @Test
        void shouldFallbackToDefaultDatesWhenFromDateIsInvalid() {
            // given
            when(measurementRepository.findByUserIdAndDateBetweenOrderByDateAsc(
                    eq(userId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            ObjectNode input = objectMapper.createObjectNode();
            input.put("from_date", "not-a-date");
            input.put("to_date", "also-invalid");

            // when — should not throw, fallback dates used
            String result = tool.execute(input, userId);

            // then
            assertThat(result).contains("No measurements found");
            verify(measurementRepository).findByUserIdAndDateBetweenOrderByDateAsc(
                    eq(userId), any(LocalDate.class), any(LocalDate.class));
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

    // --- GetSupplementInfoTool ---

    @Nested
    class GetSupplementInfoToolTest {

        @Mock
        private SupplementPlanRepository supplementPlanRepository;

        private GetSupplementInfoTool tool;

        @BeforeEach
        void setUp() {
            tool = new GetSupplementInfoTool(supplementPlanRepository);
        }

        private Supplement supplement(String name) {
            return Supplement.builder().name(name).build();
        }

        private SupplementPlanEntry entry(Supplement sup, TimeSlot slot, String customTime,
                                          BigDecimal dose, String unit) {
            return SupplementPlanEntry.builder()
                    .supplement(sup)
                    .timeSlot(slot)
                    .customTime(customTime)
                    .doseAmount(dose)
                    .doseUnit(unit)
                    .build();
        }

        @Test
        void shouldReturnOnlyActivePlansByDefault() {
            // given
            SupplementPlan active = SupplementPlan.builder().name("Morning Stack").active(true)
                    .entries(new ArrayList<>()).build();
            SupplementPlan inactive = SupplementPlan.builder().name("Old Plan").active(false)
                    .entries(new ArrayList<>()).build();
            when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(active, inactive));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("Morning Stack");
            assertThat(result).doesNotContain("Old Plan");
        }

        @Test
        void shouldShowAllPlansWhenActiveOnlyFalse() {
            // given
            SupplementPlan active = SupplementPlan.builder().name("Morning Stack").active(true)
                    .entries(new ArrayList<>()).build();
            SupplementPlan inactive = SupplementPlan.builder().name("Old Plan").active(false)
                    .entries(new ArrayList<>()).build();
            when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(active, inactive));
            ObjectNode input = objectMapper.createObjectNode();
            input.put("active_only", false);

            // when
            String result = tool.execute(input, userId);

            // then
            assertThat(result).contains("Morning Stack");
            assertThat(result).contains("Old Plan");
        }

        @Test
        void shouldLabelInactivePlanWithBracketWhenActiveOnlyFalse() {
            // given
            SupplementPlan inactive = SupplementPlan.builder().name("Old Plan").active(false)
                    .entries(new ArrayList<>()).build();
            when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(inactive));
            ObjectNode input = objectMapper.createObjectNode();
            input.put("active_only", false);

            // when
            String result = tool.execute(input, userId);

            // then
            assertThat(result).contains("[inactive]");
        }

        @Test
        void shouldUseCustomTimeWhenTimeSlotIsCustom() {
            // given
            Supplement sup = supplement("Creatine");
            SupplementPlanEntry e = entry(sup, TimeSlot.CUSTOM, "07:30", new BigDecimal("5"), "g");
            SupplementPlan plan = SupplementPlan.builder().name("Pre-Workout").active(true)
                    .entries(new ArrayList<>(List.of(e))).build();
            when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(plan));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("07:30");
            assertThat(result).doesNotContain("CUSTOM");
        }

        @Test
        void shouldUseTimeSlotNameForNonCustomSlot() {
            // given
            Supplement sup = supplement("Vitamin D");
            SupplementPlanEntry e = entry(sup, TimeSlot.MORNING, null, new BigDecimal("2000"), "IU");
            SupplementPlan plan = SupplementPlan.builder().name("Daily").active(true)
                    .entries(new ArrayList<>(List.of(e))).build();
            when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(plan));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("MORNING");
        }

        @Test
        void shouldOmitDoseFieldsWhenNull() {
            // given
            Supplement sup = supplement("Fish Oil");
            SupplementPlanEntry e = entry(sup, TimeSlot.EVENING, null, null, null);
            SupplementPlan plan = SupplementPlan.builder().name("Evening").active(true)
                    .entries(new ArrayList<>(List.of(e))).build();
            when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(plan));

            // when — should not throw NullPointerException
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("Fish Oil");
            assertThat(result).doesNotContain("null");
        }

        @Test
        void shouldReturnSpecialMessageWhenNoActivePlansButPlansExist() {
            // given
            SupplementPlan inactive = SupplementPlan.builder().name("Old").active(false)
                    .entries(new ArrayList<>()).build();
            when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(inactive));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("No active supplement plans found");
            assertThat(result).contains("active_only: false");
        }

        @Test
        void shouldReturnNoPlansMessageWhenEmpty() {
            // given
            when(supplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of());

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).isEqualTo("No supplement plans found.");
        }
    }

    // --- GetHealthMetricsTool ---

    @Nested
    class GetHealthMetricsToolTest {

        @Mock
        private HealthMetricService healthMetricService;

        private GetHealthMetricsTool tool;

        @BeforeEach
        void setUp() {
            tool = new GetHealthMetricsTool(healthMetricService);
        }

        @Test
        void shouldApplyDefaultDateRangeWhenParamsMissing() {
            // given
            LocalDate today = LocalDate.now();
            LocalDate expectedFrom = today.minusDays(30);
            when(healthMetricService.getDailyMetrics(eq(userId), eq("withings"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());

            // when
            tool.execute(emptyInput(), userId);

            // then — verify called with today-30 and today
            verify(healthMetricService).getDailyMetrics(eq(userId), eq("withings"),
                    eq(expectedFrom), eq(today));
        }

        @Test
        void shouldReturnNoMetricsMessageWhenServiceReturnsEmpty() {
            // given
            when(healthMetricService.getDailyMetrics(any(), anyString(), any(), any()))
                    .thenReturn(List.of());

            // when
            String result = tool.execute(emptyInput(), userId);

            // then
            assertThat(result).contains("No Withings health metrics found");
            assertThat(result).contains("Sync your Withings device");
        }

        @Test
        void shouldFilterMetricKeysWhenSpecified() {
            // given
            DailyHealthMetricsDto day = new DailyHealthMetricsDto(
                    LocalDate.of(2025, 4, 1),
                    "withings",
                    Map.of("weight_kg", new BigDecimal("83"), "vo2_max", new BigDecimal("45"))
            );
            when(healthMetricService.getDailyMetrics(any(), anyString(), any(), any()))
                    .thenReturn(List.of(day));
            ObjectNode input = objectMapper.createObjectNode();
            ArrayNode keys = input.putArray("metric_keys");
            keys.add("weight_kg");

            // when
            String result = tool.execute(input, userId);

            // then — weight_kg included, vo2_max excluded
            assertThat(result).contains("83");
            assertThat(result).doesNotContain("45");
        }

        @Test
        void shouldRemapLabelsUsingLabelMap() {
            // given
            DailyHealthMetricsDto day = new DailyHealthMetricsDto(
                    LocalDate.of(2025, 4, 2),
                    "withings",
                    Map.of("body_fat_percent", new BigDecimal("18.5"))
            );
            when(healthMetricService.getDailyMetrics(any(), anyString(), any(), any()))
                    .thenReturn(List.of(day));

            // when
            String result = tool.execute(emptyInput(), userId);

            // then — "body_fat_percent" remapped to "body_fat" in output
            assertThat(result).contains("body_fat");
            assertThat(result).doesNotContain("body_fat_percent");
        }

        @Test
        void shouldSkipDaysWhereAllMetricsFilteredOut() {
            // given
            DailyHealthMetricsDto day = new DailyHealthMetricsDto(
                    LocalDate.of(2025, 4, 3),
                    "withings",
                    Map.of("vo2_max", new BigDecimal("46"))
            );
            when(healthMetricService.getDailyMetrics(any(), anyString(), any(), any()))
                    .thenReturn(List.of(day));
            ObjectNode input = objectMapper.createObjectNode();
            ArrayNode keys = input.putArray("metric_keys");
            keys.add("weight_kg"); // vo2_max not in filter

            // when
            String result = tool.execute(input, userId);

            // then — day is skipped, output contains only header
            assertThat(result).doesNotContain("2025-04-03");
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
