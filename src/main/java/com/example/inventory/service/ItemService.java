package com.example.inventory.service;

import com.example.inventory.entity.Category;
import com.example.inventory.entity.Item;
import com.example.inventory.entity.User;
import com.example.inventory.form.ItemForm;
import com.example.inventory.repository.CategoryRepository;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final InventoryLogService logService;
    private final StockMovementService movementService;

    public ItemService(ItemRepository itemRepository,
                       CategoryRepository categoryRepository,
                       UserRepository userRepository,
                       InventoryLogService logService,
                       StockMovementService movementService) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.logService = logService;
        this.movementService = movementService;
    }

    @Transactional(readOnly = true)
    public Page<Item> search(String search,
                             Long categoryId,
                             Boolean active,
                             Boolean lowStock,
                             Boolean outOfStock,
                             Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        // Default to showing active items unless caller explicitly asks for another state
        return itemRepository.search(normalizedSearch, categoryId, active,
                lowStock, outOfStock, pageable);
    }

    @Transactional(readOnly = true)
    public Item getById(Long id) {
        return itemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
    }

    @Transactional
    public Item create(ItemForm form, String username) {
        if (itemRepository.existsBySkuIgnoreCase(form.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Cannot assign item to an inactive category");
        }

        int initialQuantity = form.getQuantity() == null ? 0 : form.getQuantity();

        Item item = Item.builder()
                .sku(form.getSku().trim().toUpperCase())
                .name(form.getName().trim())
                .description(form.getDescription())
                .category(category)
                .quantity(initialQuantity)
                .unitPrice(form.getUnitPrice() == null ? BigDecimal.ZERO : form.getUnitPrice())
                .reorderLevel(form.getReorderLevel())
                .location(form.getLocation())
                .active(true)
                .createdBy(resolveUser(username))
                .build();

        Item saved = itemRepository.save(item);

        // Record the initial stock as a movement in the same transaction.
        // If this fails, the item insert rolls back too — no orphaned quantities.
        movementService.recordInitialStock(saved, initialQuantity, username);

        logService.logItemCreated(saved, username);
        return saved;
    }

    @Transactional
    public Item update(Long id, ItemForm form, String username) {
        Item item = getById(id);

        if (!item.getSku().equalsIgnoreCase(form.getSku())
                && itemRepository.existsBySkuIgnoreCase(form.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        item.setSku(form.getSku().trim().toUpperCase());
        item.setName(form.getName().trim());
        item.setDescription(form.getDescription());
        item.setCategory(category);
        // NOTE: quantity is intentionally NOT set here.
        // Quantity changes flow through StockMovementService.
        item.setUnitPrice(form.getUnitPrice() == null ? BigDecimal.ZERO : form.getUnitPrice());
        item.setReorderLevel(form.getReorderLevel());
        item.setLocation(form.getLocation());
        item.setActive(form.isActive());

        Item saved = itemRepository.save(item);
        logService.logItemUpdated(saved, username);
        return saved;
    }

    @Transactional
    public void softDelete(Long id, String username) {
        Item item = getById(id);
        item.setActive(false);
        Item saved = itemRepository.save(item);
        logService.logItemDeleted(saved, username);
    }

    @Transactional
    public void restore(Long id, String username) {
        Item item = getById(id);
        if (!item.getCategory().isActive()) {
            throw new IllegalArgumentException(
                    "Cannot restore item: its category is inactive");
        }
        item.setActive(true);
        Item saved = itemRepository.save(item);
        logService.logItemRestored(saved, username);
    }

    @Transactional(readOnly = true)
    public List<Item> lowStockItems() {
        return itemRepository.findLowStockItems();
    }

    @Transactional(readOnly = true)
    public List<Item> outOfStockItems() {
        return itemRepository.findOutOfStockItems();
    }

    @Transactional(readOnly = true)
    public long activeCount() {
        return itemRepository.countByActiveTrue();
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}