package com.example.inventory.service;

import com.example.inventory.entity.Category;
import com.example.inventory.entity.Item;
import com.example.inventory.entity.User;
import com.example.inventory.form.ItemForm;
import com.example.inventory.repository.CategoryRepository;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private InventoryLogService logService;
    @Mock private StockMovementService movementService;

    @InjectMocks private ItemService itemService;

    private Category activeCategory;

    @BeforeEach
    void setup() {
        activeCategory = new Category();
        activeCategory.setId(1L);
        activeCategory.setName("Laptops");
        activeCategory.setActive(true);
    }

    private ItemForm validForm(int quantity) {
        ItemForm form = new ItemForm();
        form.setSku("LAP-001");
        form.setName("Test Laptop");
        form.setCategoryId(1L);
        form.setQuantity(quantity);
        form.setUnitPrice(new BigDecimal("1000.00"));
        form.setReorderLevel(5);
        return form;
    }

    // ---------- create ----------

    @Test
    void create_throwsWhenSkuExists() {
        when(itemRepository.existsBySkuIgnoreCase("LAP-001")).thenReturn(true);

        assertThatThrownBy(() -> itemService.create(validForm(10), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void create_rejectsInactiveCategory() {
        when(itemRepository.existsBySkuIgnoreCase(anyString())).thenReturn(false);
        activeCategory.setActive(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));

        assertThatThrownBy(() -> itemService.create(validForm(10), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void create_savesItemAndRecordsInitialMovement() {
        when(itemRepository.existsBySkuIgnoreCase(anyString())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(42L);
            return i;
        });

        Item result = itemService.create(validForm(25), "admin");

        assertThat(result.getQuantity()).isEqualTo(25);
        assertThat(result.getSku()).isEqualTo("LAP-001");

        verify(movementService).recordInitialStock(any(Item.class), eq(25), eq("admin"));
        verify(logService).logItemCreated(any(Item.class), eq("admin"));
    }

    @Test
    void create_withZeroQuantity_skipsInitialMovement() {
        when(itemRepository.existsBySkuIgnoreCase(anyString())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(42L);
            return i;
        });

        itemService.create(validForm(0), "admin");

        // recordInitialStock is called, but its own logic returns early on <= 0.
        // From ItemService's perspective, the call always happens.
        verify(movementService).recordInitialStock(any(Item.class), eq(0), eq("admin"));
    }

    // ---------- update ----------

    @Test
    void update_doesNotChangeQuantity() {
        Item existing = new Item();
        existing.setId(1L);
        existing.setSku("LAP-001");
        existing.setQuantity(50);
        existing.setCategory(activeCategory);

        when(itemRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemForm form = validForm(999); // attempted quantity change
        form.setId(1L);
        form.setName("Renamed");

        Item result = itemService.update(1L, form, "admin");

        assertThat(result.getQuantity()).isEqualTo(50);   // unchanged
        assertThat(result.getName()).isEqualTo("Renamed");
        verify(movementService, never()).recordInitialStock(any(), anyInt(), anyString());
    }

    // ---------- softDelete / restore ----------

    @Test
    void softDelete_setsActiveFalse() {
        Item item = new Item();
        item.setId(1L);
        item.setActive(true);
        item.setCategory(activeCategory);
        when(itemRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        itemService.softDelete(1L, "admin");

        assertThat(item.isActive()).isFalse();
        verify(logService).logItemDeleted(any(Item.class), eq("admin"));
    }

    @Test
    void restore_rejectsWhenCategoryInactive() {
        Item item = new Item();
        item.setId(1L);
        item.setActive(false);
        activeCategory.setActive(false);
        item.setCategory(activeCategory);
        when(itemRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.restore(1L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category");
    }
}