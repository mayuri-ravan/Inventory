package com.smartinventory.mapper;

import com.smartinventory.dto.response.StockMovementResponse;
import com.smartinventory.entity.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSku", source = "product.sku")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    StockMovementResponse toResponse(StockMovement stockMovement);

    List<StockMovementResponse> toResponseList(List<StockMovement> movements);
}
