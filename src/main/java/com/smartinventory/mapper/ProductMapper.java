package com.smartinventory.mapper;

import com.smartinventory.dto.response.ProductResponse;
import com.smartinventory.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.companyName")
    @Mapping(target = "totalStock", ignore = true) // populated in service layer
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);
}
