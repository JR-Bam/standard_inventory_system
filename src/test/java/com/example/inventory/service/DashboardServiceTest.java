package com.example.inventory.service;

import com.example.inventory.dto.DashboardData;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private StockMovementRepository movementRepository;
    @Mock private ItemService itemService;
    @Mock private StockMovementService movementService;

    @InjectMocks private DashboardService dashboardService;

    @Test
    void movementChart_fillsEveryDayInWindow() {
        when(itemService.activeCount()).thenReturn(0L);
        when(itemService.lowStockItems()).thenReturn(List.of());
        when(itemService.outOfStockItems()).thenReturn(List.of());
        when(movementService.recent(any(Integer.class))).thenReturn(List.of());
        when(itemRepository.categoryItemCounts()).thenReturn(List.of());
        when(itemRepository.categoryItemValues()).thenReturn(List.of());
        when(movementRepository.dailyMovementsSince(any())).thenReturn(List.of());

        DashboardData data = dashboardService.build(7);

        assertThat(data.getMovementChart().getLabels()).hasSize(7);
        assertThat(data.getMovementChart().getStockIn()).hasSize(7);
        assertThat(data.getMovementChart().getStockOut()).hasSize(7);
        assertThat(data.getMovementChart().getStockIn()).allMatch(q -> q == 0L);
    }
}