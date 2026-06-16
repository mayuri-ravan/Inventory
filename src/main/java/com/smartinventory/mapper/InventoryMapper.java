package com.smartinventory.mapper;

import com.smartinventory.dto.response.InventoryResponse;
import com.smartinventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    @Mapping(target = "totalStock", expression = "java(inventory.getTotalStock())")
    @Mapping(target = "lowStock",
             expression = "java(inventory.getQuantityAvailable() <= inventory.getReorderPoint())")
    InventoryResponse toResponse(Inventory inventory);

    List<InventoryResponse> toResponseList(List<Inventory> inventories);
}
