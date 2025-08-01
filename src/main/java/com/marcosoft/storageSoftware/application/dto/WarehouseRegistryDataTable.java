package com.marcosoft.storageSoftware.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseRegistryDataTable {
    private String registryType;
    private LocalDateTime registryDateTime;
    private String warehouseName;
    private String productName;
    private Integer amount;
}
