package com.rest.nelly.rest.service;

import com.rest.nelly.rest.dto.OperationDTO;
import com.rest.nelly.rest.model.Operation;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OperationService {

    private final List<Operation> operations = new ArrayList<>();

    private final AtomicLong idGenerator =
            new AtomicLong(1);

    public OperationDTO compute(
            String type,
            double a,
            double b) {

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Le type de l'opération est obligatoire."
            );
        }

        String normalizedType =
                type.trim().toUpperCase();

        double result;

        switch (normalizedType) {

            case "ADD":
                result = a + b;
                break;

            case "SUB":
                result = a - b;
                break;

            case "MUL":
                result = a * b;
                break;

            case "DIV":

                if (b == 0) {
                    throw new ArithmeticException(
                            "Division par zéro impossible."
                    );
                }

                result = a / b;
                break;

            default:
                throw new IllegalArgumentException(
                        "Type d'opération invalide : "
                                + type
                );
        }

        Operation operation =
                new Operation(
                        idGenerator.getAndIncrement(),
                        normalizedType,
                        a,
                        b,
                        result,
                        LocalDateTime.now()
                );

        synchronized (operations) {
            operations.add(operation);
        }

        return toDTO(operation);
    }

    public List<OperationDTO> getAll() {

        synchronized (operations) {

            List<OperationDTO> result =
                    new ArrayList<>();

            for (Operation operation : operations) {
                result.add(toDTO(operation));
            }

            return result;
        }
    }

    public Optional<OperationDTO> getById(long id) {

        synchronized (operations) {

            for (Operation operation : operations) {

                if (operation.getId() == id) {
                    return Optional.of(
                            toDTO(operation)
                    );
                }
            }

            return Optional.empty();
        }
    }

    private OperationDTO toDTO(
            Operation operation) {

        return new OperationDTO(
                operation.getId(),
                operation.getType(),
                operation.getA(),
                operation.getB(),
                operation.getResult(),
                operation.getTimestamp()
        );
    }
}