package com.rest.nelly.rest.controller;

import com.rest.nelly.rest.dto.OperationDTO;
import com.rest.nelly.rest.dto.OperationRequest;
import com.rest.nelly.rest.service.OperationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operations")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    // POST /operations
    @PostMapping
    public ResponseEntity<?> createOperation(
            @RequestBody OperationRequest request) {

        // Vérification des champs obligatoires
       if (request.getType() == null
        || request.getType().isBlank()
        || request.getA() == null
        || request.getB() == null) {

    return ResponseEntity
            .badRequest()
            .body(Map.of(
                    "error",
                    "Les champs 'type', 'a' et 'b' sont obligatoires."
            ));
}

        try {

            OperationDTO operation =
                    operationService.compute(
                            request.getType(),
                            request.getA(),
                            request.getB()
                    );

            URI location =
                    URI.create(
                            "/operations/" + operation.getId()
                    );

            return ResponseEntity
                    .created(location)
                    .body(operation);

        } catch (ArithmeticException e) {

            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    // GET /operations
    @GetMapping
    public ResponseEntity<List<OperationDTO>> getAllOperations() {

        return ResponseEntity.ok(
                operationService.getAll()
        );
    }

    // GET /operations/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getOperationById(
            @PathVariable long id) {

        return operationService
                .getById(id)
                .<ResponseEntity<?>>map(
                        operation -> ResponseEntity.ok(operation)
                )
                .orElseGet(() ->
                        ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                        "error",
                                        "Opération introuvable avec l'id : "
                                                + id
                                ))
                );
    }
}