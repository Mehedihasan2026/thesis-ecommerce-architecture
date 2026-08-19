package com.thesis.microservices.inventory.web;

import com.thesis.microservices.inventory.service.InventoryResponse;
import com.thesis.microservices.inventory.service.InventoryService;
import com.thesis.microservices.inventory.service.ReleaseInventoryRequest;
import com.thesis.microservices.inventory.service.ReservationResponse;
import com.thesis.microservices.inventory.service.ReserveInventoryRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public InventoryResponse getInventory(@PathVariable Long productId) {
        return inventoryService.getInventory(productId);
    }

    @PostMapping("/reserve")
    public ReservationResponse reserve(
            @RequestBody ReserveInventoryRequest request,
            @RequestParam(defaultValue = "false") boolean simulateInventoryFailure
    ) {
        return inventoryService.reserve(request, simulateInventoryFailure);
    }

    @PostMapping("/release")
    public ReservationResponse release(@RequestBody ReleaseInventoryRequest request) {
        return inventoryService.release(request);
    }
}
