package com.smartinventory.mapper;

import com.smartinventory.dto.response.WarehouseResponse;
import com.smartinventory.entity.Warehouse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    WarehouseResponse toResponse(Warehouse warehouse);

    List<WarehouseResponse> toResponseList(List<Warehouse> warehouses);
}
