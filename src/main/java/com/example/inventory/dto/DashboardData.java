package com.example.inventory.dto;

import com.example.inventory.entity.Item;
import com.example.inventory.entity.StockMovement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class DashboardData {

    // Stat cards
    private long activeItemCount;
    private List<Item> lowStockItems;
    private List<Item> outOfStockItems;

    // Recent movements table
    private List<StockMovement> recentMovements;

    // Category doughnut
    private CategoryChart categoryChart;

    // Movement line chart
    private MovementChart movementChart;
    private int movementDays;

    @Getter @Setter
    public static class CategoryChart {
        private List<String> labels;
        private List<Long> counts;
        private List<java.math.BigDecimal> values;
        private boolean empty;

        public CategoryChart(List<String> labels, List<Long> counts,
                             List<java.math.BigDecimal> values) {
            this.labels = labels;
            this.counts = counts;
            this.values = values;
            this.empty = labels.isEmpty()
                    || counts.stream().allMatch(c -> c == 0L);
        }
    }

    @Getter @Setter
    public static class MovementChart {
        private List<String> labels;      // ISO dates
        private List<Long> stockIn;
        private List<Long> stockOut;
        private boolean empty;

        public MovementChart(List<String> labels, List<Long> stockIn, List<Long> stockOut) {
            this.labels = labels;
            this.stockIn = stockIn;
            this.stockOut = stockOut;
            this.empty = stockIn.stream().allMatch(q -> q == 0L)
                    && stockOut.stream().allMatch(q -> q == 0L);
        }
    }
}