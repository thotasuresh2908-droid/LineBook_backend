package com.linebook.customer.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.linebook.customer.dto.PackageResponse;
import com.linebook.customer.service.CustomerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Packages", description = "Package catalogue")
@RestController
@RequestMapping("/api/packages")
public class PackageController {

    private final CustomerService service;

    public PackageController(CustomerService service) {
        this.service = service;
    }

    @Operation(summary = "List the packages")
    @GetMapping
    public List<PackageResponse> list() {
        return service.packages();
    }
}
