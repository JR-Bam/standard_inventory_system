package com.example.inventory.service;

import com.example.inventory.entity.*;
import com.example.inventory.form.StockMovementForm;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.StockMovementRepository;
import com.example.inventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock private StockMovementRepository movementRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private StockMovementService service;

    private Item item;

    @BeforeEach
    void setup() {
        item = new Item();
        item.setId(1L);
        item.setName("Test Item");
        item.setQuantity(50);
        item.setActive(true);
    }

    @Test
    void record_stockIn_incrementsQuantity() {
        when(itemRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockMovementForm form = new StockMovementForm();
        form.setType(MovementType.STOCK_IN);
        form.setQuantity(20);

        service.record(1L, form, "admin");

        assertThat(item.getQuantity()).isEqualTo(70);
    }

    @Test
    void record_stockOut_decrementsQuantity() {
        when(itemRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StockMovementForm form = new StockMovementForm();
        form.setType(MovementType.STOCK_OUT);
        form.setQuantity(30);

        service.record(1L, form, "admin");

        assertThat(item.getQuantity()).isEqualTo(20);
    }

    @Test
    void record_stockOut_rejectsOverdraw() {
        when(itemRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));

        StockMovementForm form = new StockMovementForm();
        form.setType(MovementType.STOCK_OUT);
        form.setQuantity(999);

        assertThatThrownBy(() -> service.record(1L, form, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot remove");

        assertThat(item.getQuantity()).isEqualTo(50);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void recordInitialStock_skipsZeroOrNegative() {
        service.recordInitialStock(item, 0, "admin");
        service.recordInitialStock(item, -5, "admin");

        verify(movementRepository, never()).save(any());
    }

    @Test
    void recordInitialStock_writesInboundMovement() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordInitialStock(item, 25, "admin");

        verify(movementRepository).save(argThat(m ->
                m.getType() == MovementType.STOCK_IN
                        && m.getQuantity() == 25
                        && m.getPreviousQuantity() == 0
                        && m.getNewQuantity() == 25
        ));
    }
}