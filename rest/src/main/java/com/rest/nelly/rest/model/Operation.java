package com.rest.nelly.rest.model;

import java.time.LocalDateTime;

public class Operation {

    private Long id;
    private String type;
    private double a;
    private double b;
    private double result;
    private LocalDateTime timestamp;

    public Operation() {
    }

    public Operation(
            Long id,
            String type,
            double a,
            double b,
            double result,
            LocalDateTime timestamp) {

        this.id = id;
        this.type = type;
        this.a = a;
        this.b = b;
        this.result = result;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}