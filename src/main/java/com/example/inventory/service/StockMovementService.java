package com.example.inventory.service;

import com.example.inventory.entity.Item;
import com.example.inventory.entity.MovementType;
import com.example.inventory.entity.StockMovement;
import com.example.inventory.entity.User;
import com.example.inventory.form.StockMovementForm;
import com.example.inventory.repository.ItemRepository;
import com.example.inventory.repository.StockMovementRepository;
import com.example.inventory.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public StockMovementService(StockMovementRepository movementRepository,
                                ItemRepository itemRepository,
                                UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> findByItem(Long itemId, Pageable pageable) {
        return movementRepository.findByItemIdWithUser(itemId, pageable);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> recent(int limit) {
        return movementRepository.findRecent(Pageable.ofSize(limit));
    }

    @Transactional
    public StockMovement record(Long itemId, StockMovementForm form, String username) {
        Item item = itemRepository.findByIdWithDetails(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        if (!item.isActive()) {
            throw new IllegalArgumentException("Cannot record a movement for an inactive item");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        int previous = item.getQuantity();
        int delta;
        int next;

        switch (form.getType()) {
            case STOCK_IN -> {
                delta = form.getQuantity();
                next = previous + delta;
            }
            case STOCK_OUT -> {
                if (form.getQuantity() > previous) {
                    throw new IllegalArgumentException(
                            "Cannot remove " + form.getQuantity() + " units — only " + previous + " in stock");
                }
                delta = form.getQuantity();
                next = previous - delta;
            }
            case ADJUSTMENT -> {
                // Not exposed in v1 UI, but supported at the service layer.
                delta = Math.abs(form.getQuantity() - previous);
                next = form.getQuantity();
            }
            default -> throw new IllegalArgumentException("Unsupported movement type");
        }

        item.setQuantity(next);
        itemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .item(item)
                .user(user)
                .type(form.getType())
                .quantity(delta)
                .previousQuantity(previous)
                .newQuantity(next)
                .reason(form.getReason())
                .reference(form.getReference())
                .build();

        return movementRepository.save(movement);
    }

    /**
     * Records the initial stock-in for a newly created item.
     * Called from ItemService.create(...) inside the same transaction
     * as the item insert, so the ledger and the item are written atomically.
     * Does not modify item.quantity — the caller is expected to have set it.
     */
    @Transactional
    public void recordInitialStock(Item item, int quantity, String username) {
        if (quantity <= 0) {
            return; // 0 initial stock is not a meaningful movement
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        StockMovement movement = StockMovement.builder()
                .item(item)
                .user(user)
                .type(MovementType.STOCK_IN)
                .quantity(quantity)
                .previousQuantity(0)
                .newQuantity(quantity)
                .reason("Initial stock")
                .reference(null)
                .build();

        movementRepository.save(movement);
    }
}