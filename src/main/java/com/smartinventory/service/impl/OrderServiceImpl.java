package com.smartinventory.service.impl;

import com.smartinventory.dto.request.OrderRequest;
import com.smartinventory.dto.request.StockUpdateRequest;
import com.smartinventory.dto.response.OrderResponse;
import com.smartinventory.entity.*;
import com.smartinventory.enums.OrderStatus;
import com.smartinventory.enums.ProductStatus;
import com.smartinventory.enums.Role;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.InsufficientStockException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.OrderRepository;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.service.InventoryService;
import com.smartinventory.service.OrderService;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final AppUtils appUtils;

    @Override
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User customer = appUtils.getCurrentUser();

        // Build order items and validate stock
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemReq.getProductId()));

            if (product.getStatus() != ProductStatus.APPROVED)
                throw new BusinessException("Product '" + product.getName() + "' is not available for purchase");

            // Check total available stock across all warehouses
            Integer totalStock = inventoryRepository.getTotalStockForProduct(product.getId()).orElse(0);
            if (totalStock < itemReq.getQuantity())
                throw new InsufficientStockException(product.getName(), totalStock, itemReq.getQuantity());

            BigDecimal discountPct = itemReq.getDiscountPercentage() != null
                    ? itemReq.getDiscountPercentage() : BigDecimal.ZERO;

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .discountPercentage(discountPct)
                    .build();
            item.calculateTotalPrice();
            orderItems.add(item);
            totalAmount = totalAmount.add(item.getTotalPrice());
        }

        // Create and save the order
        Order order = Order.builder()
                .orderNumber(appUtils.generateOrderNumber())
                .customer(customer)
                .orderItems(orderItems)
                .totalAmount(totalAmount)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .orderDate(LocalDateTime.now())
                .build();
        order.calculateFinalAmount();

        // Link items back to order
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }

        Order savedOrder = orderRepository.save(order);

        // Deduct stock from warehouses (FIFO - pick from first warehouse with enough stock)
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            deductStockForOrder(itemReq.getProductId(), itemReq.getQuantity(), savedOrder.getId());
        }

        log.info("Order {} placed by customer '{}'", savedOrder.getOrderNumber(), customer.getUsername());
        return mapToResponse(savedOrder);
    }

    /**
     * Deducts stock from the first warehouse that has sufficient quantity.
     */
    private void deductStockForOrder(Long productId, int quantity, Long orderId) {
        List<Inventory> warehouses = inventoryRepository.findWarehousesWithSufficientStock(productId, quantity);
        if (warehouses.isEmpty()) {
            // Fallback: deduct from warehouses in order until fulfilled
            List<Inventory> allInventories = inventoryRepository.findByProductId(productId);
            int remaining = quantity;
            for (Inventory inv : allInventories) {
                if (remaining <= 0) break;
                int deduct = Math.min(remaining, inv.getQuantityAvailable());
                if (deduct > 0) {
                    StockUpdateRequest stockReq = new StockUpdateRequest();
                    stockReq.setProductId(productId);
                    stockReq.setWarehouseId(inv.getWarehouse().getId());
                    stockReq.setQuantity(deduct);
                    stockReq.setMovementType("OUTWARD");
                    stockReq.setReferenceId(orderId);
                    stockReq.setReferenceType("ORDER");
                    stockReq.setRemarks("Order fulfillment");
                    inventoryService.updateStock(stockReq);
                    remaining -= deduct;
                }
            }
        } else {
            // Deduct from first warehouse with enough stock
            Inventory inv = warehouses.get(0);
            StockUpdateRequest stockReq = new StockUpdateRequest();
            stockReq.setProductId(productId);
            stockReq.setWarehouseId(inv.getWarehouse().getId());
            stockReq.setQuantity(quantity);
            stockReq.setMovementType("OUTWARD");
            stockReq.setReferenceId(orderId);
            stockReq.setReferenceType("ORDER");
            stockReq.setRemarks("Order fulfillment");
            inventoryService.updateStock(stockReq);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        enforceOrderAccess(order);
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        enforceOrderAccess(order);
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(OrderStatus status, Pageable pageable) {
        Long customerId = appUtils.getCurrentUser().getId();
        return orderRepository.findByCustomerIdAndStatus(customerId, status, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Long customerId, OrderStatus status,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return orderRepository.searchOrders(customerId, status, from, to, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        validateStatusTransition(order.getStatus(), status);

        if (status == OrderStatus.DELIVERED) order.setDeliveredDate(LocalDateTime.now());
        order.setStatus(status);
        order = orderRepository.save(order);
        log.info("Order {} status updated to {} by {}", order.getOrderNumber(), status, appUtils.getCurrentUsername());
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        // Only customer (owner) or admin/manager can cancel
        User currentUser = appUtils.getCurrentUser();
        if (currentUser.getRole() == Role.ROLE_CUSTOMER
                && !order.getCustomer().getId().equals(currentUser.getId()))
            throw new BusinessException("You can only cancel your own orders");

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED)
            throw new BusinessException("Cannot cancel a shipped or delivered order");

        if (order.getStatus() == OrderStatus.CANCELLED)
            throw new BusinessException("Order is already cancelled");

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        // Restock items back to inventory
        restockCancelledOrder(order);

        log.info("Order {} cancelled by {}", order.getOrderNumber(), currentUser.getUsername());
        return mapToResponse(order);
    }

    private void restockCancelledOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            List<Inventory> inventories = inventoryRepository.findByProductId(item.getProduct().getId());
            if (!inventories.isEmpty()) {
                StockUpdateRequest req = new StockUpdateRequest();
                req.setProductId(item.getProduct().getId());
                req.setWarehouseId(inventories.get(0).getWarehouse().getId());
                req.setQuantity(item.getQuantity());
                req.setMovementType("INWARD");
                req.setReferenceId(order.getId());
                req.setReferenceType("ORDER_CANCEL");
                req.setRemarks("Restock from cancelled order " + order.getOrderNumber());
                inventoryService.updateStock(req);
            }
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        // Enforce valid state machine transitions
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED -> next == OrderStatus.REFUNDED;
            default -> false;
        };
        if (!valid)
            throw new BusinessException("Invalid status transition from " + current + " to " + next);
    }

    private void enforceOrderAccess(Order order) {
        User currentUser = appUtils.getCurrentUser();
        if (currentUser.getRole() == Role.ROLE_CUSTOMER
                && !order.getCustomer().getId().equals(currentUser.getId()))
            throw new BusinessException("You do not have access to this order");
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .discountPercentage(item.getDiscountPercentage())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFullName())
                .customerEmail(order.getCustomer().getEmail())
                .items(items)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .shippingAmount(order.getShippingAmount())
                .finalAmount(order.getFinalAmount())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .orderDate(order.getOrderDate())
                .deliveredDate(order.getDeliveredDate())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
