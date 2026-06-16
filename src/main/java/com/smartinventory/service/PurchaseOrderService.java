package com.smartinventory.service;

import com.smartinventory.dto.request.PurchaseActionRequest;
import com.smartinventory.dto.request.PurchaseOrderRequest;
import com.smartinventory.dto.request.StockUpdateRequest;
import com.smartinventory.dto.response.PurchaseOrderResponse;
import com.smartinventory.entity.*;
import com.smartinventory.enums.NotificationPriority;
import com.smartinventory.enums.PurchaseStatus;
import com.smartinventory.enums.Role;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.*;
import com.smartinventory.util.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final AtomicLong PO_SEQUENCE = new AtomicLong();
    private static final DateTimeFormatter PO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final AppUtils appUtils;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        User actor = appUtils.getCurrentUser();
        if (actor.getRole() == Role.ROLE_SUPPLIER || actor.getRole() == Role.ROLE_CUSTOMER) {
            throw new BusinessException("Only admin, manager, and staff can create purchase orders");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.getWarehouseId()));
        enforceWarehouseScope(actor, warehouse);

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.getSupplierId()));

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .purchaseOrderNumber(generatePoNumber())
                .warehouse(warehouse)
                .supplier(supplier)
                .requestedBy(actor)
                .status(initialStatus(actor, request.isAdminApprovalRequired()))
                .managerApprovalRequired(actor.getRole() == Role.ROLE_STAFF)
                .adminApprovalRequired(request.isAdminApprovalRequired())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .notes(request.getNotes())
                .build();

        for (PurchaseOrderRequest.Item itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemRequest.getProductId()));
            if (!product.getSupplier().getId().equals(supplier.getId())) {
                throw new BusinessException("Product '" + product.getName() + "' does not belong to supplier "
                        + supplier.getCompanyName());
            }
            if (itemRequest.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Unit cost must be greater than zero");
            }
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(purchaseOrder)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitCost(itemRequest.getUnitCost())
                    .build();
            item.calculateLineTotal();
            purchaseOrder.getItems().add(item);
        }
        purchaseOrder.recalculateTotal();
        addApproval(purchaseOrder, actor, purchaseOrder.getStatus(), "Purchase order created");

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.record("PURCHASE_ORDER_CREATED", "PurchaseOrder", saved.getId(),
                "Created " + saved.getPurchaseOrderNumber());

        if (saved.getStatus() == PurchaseStatus.PENDING_APPROVAL) {
            notificationService.notifyRole(Role.ROLE_MANAGER, "Purchase approval pending",
                    saved.getPurchaseOrderNumber() + " is waiting for manager/admin approval",
                    NotificationPriority.HIGH, "PURCHASE_APPROVAL_PENDING", "PurchaseOrder", saved.getId());
        } else {
            notificationService.notifyRole(Role.ROLE_SUPPLIER, "New purchase order approved",
                    saved.getPurchaseOrderNumber() + " is ready to send to supplier",
                    NotificationPriority.NORMAL, "PURCHASE_APPROVED", "PurchaseOrder", saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> search(Long warehouseId, Long supplierId, PurchaseStatus status,
                                              Pageable pageable) {
        User actor = appUtils.getCurrentUser();
        Long scopedWarehouseId = warehouseId;
        Long scopedSupplierId = supplierId;

        if ((actor.getRole() == Role.ROLE_MANAGER || actor.getRole() == Role.ROLE_STAFF)
                && actor.getAssignedWarehouse() != null) {
            scopedWarehouseId = actor.getAssignedWarehouse().getId();
        }
        if (actor.getRole() == Role.ROLE_SUPPLIER) {
            Supplier supplier = supplierRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new BusinessException("Supplier profile is not configured for this user"));
            scopedSupplierId = supplier.getId();
        }

        return purchaseOrderRepository.search(scopedWarehouseId, scopedSupplierId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));
        enforceReadAccess(appUtils.getCurrentUser(), purchaseOrder);
        return toResponse(purchaseOrder);
    }

    @Transactional
    public PurchaseOrderResponse approve(Long id, PurchaseActionRequest request) {
        PurchaseOrder purchaseOrder = getEntityForWrite(id);
        User actor = appUtils.getCurrentUser();
        if (actor.getRole() != Role.ROLE_MANAGER && actor.getRole() != Role.ROLE_ADMIN) {
            throw new BusinessException("Only managers and admins can approve purchase orders");
        }
        enforceWarehouseScope(actor, purchaseOrder.getWarehouse());
        if (purchaseOrder.getStatus() != PurchaseStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending purchase orders can be approved");
        }

        if (purchaseOrder.isAdminApprovalRequired() && actor.getRole() == Role.ROLE_MANAGER) {
            addApproval(purchaseOrder, actor, PurchaseStatus.APPROVED,
                    valueOrDefault(request.getComments(), "Manager approved; waiting for admin approval"));
        } else {
            purchaseOrder.setStatus(PurchaseStatus.APPROVED);
            addApproval(purchaseOrder, actor, PurchaseStatus.APPROVED,
                    valueOrDefault(request.getComments(), "Approved"));
        }
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.record("PURCHASE_ORDER_APPROVED", "PurchaseOrder", saved.getId(),
                saved.getPurchaseOrderNumber() + " approved by " + actor.getUsername());
        notificationService.notifyRole(Role.ROLE_SUPPLIER, "Purchase order approved",
                saved.getPurchaseOrderNumber() + " can now be sent to supplier",
                NotificationPriority.NORMAL, "PURCHASE_APPROVED", "PurchaseOrder", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse reject(Long id, PurchaseActionRequest request) {
        PurchaseOrder purchaseOrder = getEntityForWrite(id);
        User actor = appUtils.getCurrentUser();
        if (actor.getRole() != Role.ROLE_MANAGER && actor.getRole() != Role.ROLE_ADMIN) {
            throw new BusinessException("Only managers and admins can reject purchase orders");
        }
        enforceWarehouseScope(actor, purchaseOrder.getWarehouse());
        purchaseOrder.setStatus(PurchaseStatus.REJECTED);
        addApproval(purchaseOrder, actor, PurchaseStatus.REJECTED,
                valueOrDefault(request.getComments(), "Rejected"));
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.record("PURCHASE_ORDER_REJECTED", "PurchaseOrder", saved.getId(),
                saved.getPurchaseOrderNumber() + " rejected");
        notificationService.notifyUser(saved.getRequestedBy(), "Purchase order rejected",
                saved.getPurchaseOrderNumber() + " was rejected",
                NotificationPriority.HIGH, "PURCHASE_REJECTED", "PurchaseOrder", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse sendToSupplier(Long id) {
        PurchaseOrder purchaseOrder = getEntityForWrite(id);
        User actor = appUtils.getCurrentUser();
        enforceWarehouseScope(actor, purchaseOrder.getWarehouse());
        if (purchaseOrder.getStatus() != PurchaseStatus.APPROVED) {
            throw new BusinessException("Only approved purchase orders can be sent to suppliers");
        }
        purchaseOrder.setStatus(PurchaseStatus.SENT_TO_SUPPLIER);
        addApproval(purchaseOrder, actor, PurchaseStatus.SENT_TO_SUPPLIER, "Sent to supplier");
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.record("PURCHASE_ORDER_SENT", "PurchaseOrder", saved.getId(),
                saved.getPurchaseOrderNumber() + " sent to supplier");
        notificationService.notifyUser(saved.getSupplier().getUser(), "Purchase request received",
                saved.getPurchaseOrderNumber() + " is waiting for supplier response",
                NotificationPriority.HIGH, "SUPPLIER_REQUEST", "PurchaseOrder", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse supplierResponse(Long id, boolean accepted, PurchaseActionRequest request) {
        PurchaseOrder purchaseOrder = getEntityForWrite(id);
        User actor = appUtils.getCurrentUser();
        Supplier supplier = supplierRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new BusinessException("Supplier profile is not configured for this user"));
        if (!purchaseOrder.getSupplier().getId().equals(supplier.getId())) {
            throw new BusinessException("You can respond only to your own purchase orders");
        }
        if (purchaseOrder.getStatus() != PurchaseStatus.SENT_TO_SUPPLIER) {
            throw new BusinessException("Only sent purchase orders can receive a supplier response");
        }
        purchaseOrder.setStatus(accepted ? PurchaseStatus.SUPPLIER_ACCEPTED : PurchaseStatus.SUPPLIER_REJECTED);
        purchaseOrder.setSupplierNotes(request.getSupplierNotes());
        addApproval(purchaseOrder, actor, purchaseOrder.getStatus(),
                valueOrDefault(request.getComments(), accepted ? "Supplier accepted" : "Supplier rejected"));
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.record(accepted ? "SUPPLIER_ACCEPTED_PO" : "SUPPLIER_REJECTED_PO", "PurchaseOrder",
                saved.getId(), saved.getPurchaseOrderNumber());
        notificationService.notifyRole(Role.ROLE_MANAGER, "Supplier response received",
                saved.getSupplier().getCompanyName() + " " + (accepted ? "accepted " : "rejected ")
                        + saved.getPurchaseOrderNumber(),
                accepted ? NotificationPriority.NORMAL : NotificationPriority.HIGH,
                "SUPPLIER_RESPONSE", "PurchaseOrder", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse dispatch(Long id, PurchaseActionRequest request) {
        PurchaseOrder purchaseOrder = getEntityForWrite(id);
        User actor = appUtils.getCurrentUser();
        if (actor.getRole() == Role.ROLE_SUPPLIER) {
            Supplier supplier = supplierRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new BusinessException("Supplier profile is not configured for this user"));
            if (!purchaseOrder.getSupplier().getId().equals(supplier.getId())) {
                throw new BusinessException("You can dispatch only your own purchase orders");
            }
        }
        if (purchaseOrder.getStatus() != PurchaseStatus.SUPPLIER_ACCEPTED
                && purchaseOrder.getStatus() != PurchaseStatus.IN_PROGRESS) {
            throw new BusinessException("Only accepted purchase orders can be dispatched");
        }
        purchaseOrder.setStatus(PurchaseStatus.DISPATCHED);
        purchaseOrder.setSupplierNotes(request.getSupplierNotes());
        addApproval(purchaseOrder, actor, PurchaseStatus.DISPATCHED,
                valueOrDefault(request.getComments(), "Goods dispatched"));
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        auditLogService.record("PURCHASE_ORDER_DISPATCHED", "PurchaseOrder", saved.getId(),
                saved.getPurchaseOrderNumber());
        notificationService.notifyRole(Role.ROLE_MANAGER, "Purchase order dispatched",
                saved.getPurchaseOrderNumber() + " has been dispatched",
                NotificationPriority.NORMAL, "PURCHASE_DISPATCHED", "PurchaseOrder", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse receive(Long id, PurchaseActionRequest request) {
        PurchaseOrder purchaseOrder = getEntityForWrite(id);
        User actor = appUtils.getCurrentUser();
        enforceWarehouseScope(actor, purchaseOrder.getWarehouse());
        if (purchaseOrder.getStatus() != PurchaseStatus.DISPATCHED
                && purchaseOrder.getStatus() != PurchaseStatus.DELIVERED) {
            throw new BusinessException("Only dispatched purchase orders can be received");
        }

        purchaseOrder.setStatus(PurchaseStatus.COMPLETED);
        purchaseOrder.setDeliveredDate(LocalDateTime.now());
        addApproval(purchaseOrder, actor, PurchaseStatus.COMPLETED,
                valueOrDefault(request.getComments(), "Warehouse received goods"));
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);

        for (PurchaseOrderItem item : saved.getItems()) {
            StockUpdateRequest stock = new StockUpdateRequest();
            stock.setProductId(item.getProduct().getId());
            stock.setWarehouseId(saved.getWarehouse().getId());
            stock.setQuantity(item.getQuantity());
            stock.setMovementType("INWARD");
            stock.setReferenceId(saved.getId());
            stock.setReferenceType("PURCHASE_ORDER");
            stock.setRemarks("Received from purchase order " + saved.getPurchaseOrderNumber());
            inventoryService.updateStock(stock);
        }

        auditLogService.record("PURCHASE_ORDER_COMPLETED", "PurchaseOrder", saved.getId(),
                saved.getPurchaseOrderNumber() + " completed and inventory updated");
        notificationService.notifyRole(Role.ROLE_ADMIN, "Purchase order completed",
                saved.getPurchaseOrderNumber() + " updated inventory automatically",
                NotificationPriority.NORMAL, "PURCHASE_COMPLETED", "PurchaseOrder", saved.getId());
        return toResponse(saved);
    }

    private PurchaseOrder getEntityForWrite(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));
        enforceReadAccess(appUtils.getCurrentUser(), purchaseOrder);
        return purchaseOrder;
    }

    private PurchaseStatus initialStatus(User actor, boolean adminApprovalRequired) {
        if (actor.getRole() == Role.ROLE_ADMIN) {
            return PurchaseStatus.APPROVED;
        }
        if (actor.getRole() == Role.ROLE_MANAGER && !adminApprovalRequired) {
            return PurchaseStatus.APPROVED;
        }
        return PurchaseStatus.PENDING_APPROVAL;
    }

    private void enforceWarehouseScope(User actor, Warehouse warehouse) {
        if ((actor.getRole() == Role.ROLE_MANAGER || actor.getRole() == Role.ROLE_STAFF)
                && actor.getAssignedWarehouse() != null
                && !actor.getAssignedWarehouse().getId().equals(warehouse.getId())) {
            throw new BusinessException("You can access only your assigned warehouse");
        }
    }

    private void enforceReadAccess(User actor, PurchaseOrder purchaseOrder) {
        enforceWarehouseScope(actor, purchaseOrder.getWarehouse());
        if (actor.getRole() == Role.ROLE_SUPPLIER) {
            Supplier supplier = supplierRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new BusinessException("Supplier profile is not configured for this user"));
            if (!purchaseOrder.getSupplier().getId().equals(supplier.getId())) {
                throw new BusinessException("You do not have access to this purchase order");
            }
        }
    }

    private void addApproval(PurchaseOrder purchaseOrder, User actor, PurchaseStatus status, String comments) {
        ApprovalRecord approval = ApprovalRecord.builder()
                .purchaseOrder(purchaseOrder)
                .actor(actor)
                .actorName(actor.getFullName())
                .actorRole(actor.getRole().name())
                .status(status)
                .comments(comments)
                .createdAt(LocalDateTime.now())
                .build();
        purchaseOrder.getApprovals().add(approval);
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder purchaseOrder) {
        return PurchaseOrderResponse.builder()
                .id(purchaseOrder.getId())
                .purchaseOrderNumber(purchaseOrder.getPurchaseOrderNumber())
                .warehouseId(purchaseOrder.getWarehouse().getId())
                .warehouseName(purchaseOrder.getWarehouse().getName())
                .supplierId(purchaseOrder.getSupplier().getId())
                .supplierName(purchaseOrder.getSupplier().getCompanyName())
                .requestedById(purchaseOrder.getRequestedBy().getId())
                .requestedByName(purchaseOrder.getRequestedBy().getFullName())
                .status(purchaseOrder.getStatus())
                .managerApprovalRequired(purchaseOrder.isManagerApprovalRequired())
                .adminApprovalRequired(purchaseOrder.isAdminApprovalRequired())
                .totalAmount(purchaseOrder.getTotalAmount())
                .notes(purchaseOrder.getNotes())
                .supplierNotes(purchaseOrder.getSupplierNotes())
                .expectedDeliveryDate(purchaseOrder.getExpectedDeliveryDate())
                .deliveredDate(purchaseOrder.getDeliveredDate())
                .createdAt(purchaseOrder.getCreatedAt())
                .items(purchaseOrder.getItems().stream().map(item -> PurchaseOrderResponse.ItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .lineTotal(item.getLineTotal())
                        .build()).toList())
                .approvals(purchaseOrder.getApprovals().stream().map(approval -> PurchaseOrderResponse.ApprovalResponse.builder()
                        .actorName(approval.getActorName())
                        .actorRole(approval.getActorRole())
                        .status(approval.getStatus())
                        .comments(approval.getComments())
                        .createdAt(approval.getCreatedAt())
                        .build()).toList())
                .build();
    }

    private String generatePoNumber() {
        long seq = PO_SEQUENCE.incrementAndGet() % 10000;
        return String.format("PO-%s-%04d", LocalDateTime.now().format(PO_FORMAT), seq);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
