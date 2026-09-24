package com.example.inventory.service;

import com.example.inventory.dto.DashboardData;
import com.example.inventory.entity.MovementType;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private static final int RECENT_MOVEMENTS_LIMIT = 10;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    private final ItemRepository itemRepository;
    private final StockMovementRepository movementRepository;
    private final ItemService itemService;
    private final StockMovementService movementService;

    public DashboardService(ItemRepository itemRepository,
                            StockMovementRepository movementRepository,
                            ItemService itemService,
                            StockMovementService movementService) {
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.itemService = itemService;
        this.movementService = movementService;
    }

    @Transactional(readOnly = true)
    public DashboardData build(int movementDays) {
        DashboardData data = new DashboardData();

        // --- Stat cards ---
        data.setActiveItemCount(itemService.activeCount());
        data.setLowStockItems(itemService.lowStockItems());
        data.setOutOfStockItems(itemService.outOfStockItems());

        // --- Recent movements ---
        data.setRecentMovements(movementService.recent(RECENT_MOVEMENTS_LIMIT));

        // --- Category doughnut ---
        data.setCategoryChart(buildCategoryChart());

        // --- Movement line chart ---
        data.setMovementChart(buildMovementChart(movementDays));
        data.setMovementDays(movementDays);

        return data;
    }

    private DashboardData.CategoryChart buildCategoryChart() {
        List<Object[]> countRows = itemRepository.categoryItemCounts();
        List<Object[]> valueRows = itemRepository.categoryItemValues();

        // Same ordering (by name) from both queries, so indices align.
        List<String> labels = new ArrayList<>(countRows.size());
        List<Long> counts = new ArrayList<>(countRows.size());
        List<BigDecimal> values = new ArrayList<>(countRows.size());

        for (int i = 0; i < countRows.size(); i++) {
            labels.add((String) countRows.get(i)[0]);
            counts.add(((Number) countRows.get(i)[1]).longValue());
            values.add((BigDecimal) valueRows.get(i)[1]);
        }

        return new DashboardData.CategoryChart(labels, counts, values);
    }

    private DashboardData.MovementChart buildMovementChart(int days) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(days - 1L);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.atTime(LocalTime.MAX);

        // Fill every day in the window with 0 so the line chart doesn't skip days.
        List<String> labels = new ArrayList<>(days);
        Map<LocalDate, long[]> byDate = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = fromDate.plusDays(i);
            labels.add(d.format(ISO_DATE));
            byDate.put(d, new long[]{0L, 0L}); // [in, out]
        }

        List<Object[]> rows = movementRepository.dailyMovementsSince(from);
        for (Object[] row : rows) {
            // row[0] is java.sql.Date, java.time.LocalDate, or LocalDateTime — normalize.
            LocalDate d = toLocalDate(row[0]);
            if (d == null) continue;
            long[] bucket = byDate.get(d);
            if (bucket == null) continue;

            MovementType type = (MovementType) row[1];
            long qty = ((Number) row[2]).longValue();

            if (type == MovementType.STOCK_IN) {
                bucket[0] += qty;
            } else if (type == MovementType.STOCK_OUT) {
                bucket[1] += qty;
            }
            // ADJUSTMENT is not plotted — the line chart only shows in/out.
        }

        List<Long> stockIn = new ArrayList<>(days);
        List<Long> stockOut = new ArrayList<>(days);
        for (long[] bucket : byDate.values()) {
            stockIn.add(bucket[0]);
            stockOut.add(bucket[1]);
        }

        return new DashboardData.MovementChart(labels, stockIn, stockOut);
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (value instanceof java.util.Date date) {
            return date.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        return null;
    }
}