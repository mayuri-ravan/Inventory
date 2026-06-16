package com.smartinventory.service;

import com.smartinventory.dto.request.OrderRequest;
import com.smartinventory.dto.response.OrderResponse;
import com.smartinventory.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest request);
    OrderResponse getOrderById(Long id);
    OrderResponse getOrderByNumber(String orderNumber);
    Page<OrderResponse> getMyOrders(OrderStatus status, Pageable pageable);
    Page<OrderResponse> getAllOrders(Long customerId, OrderStatus status,
            LocalDateTime from, LocalDateTime to, Pageable pageable);
    OrderResponse updateOrderStatus(Long id, OrderStatus status);
    OrderResponse cancelOrder(Long id);
}
