package com.smartinventory.mapper;

import com.smartinventory.dto.response.SupplierResponse;
import com.smartinventory.entity.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    SupplierResponse toResponse(Supplier supplier);

    List<SupplierResponse> toResponseList(List<Supplier> suppliers);
}
