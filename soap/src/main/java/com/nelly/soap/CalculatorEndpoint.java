package com.nelly.soap;

import com.nelly.soap.generated.AddRequest;
import com.nelly.soap.generated.AddResponse;
import com.nelly.soap.generated.DivRequest;
import com.nelly.soap.generated.DivResponse;
import com.nelly.soap.generated.GetHistoryResponse;
import com.nelly.soap.generated.MulRequest;
import com.nelly.soap.generated.MulResponse;
import com.nelly.soap.generated.ObjectFactory;
import com.nelly.soap.generated.SubRequest;
import com.nelly.soap.generated.SubResponse;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Endpoint
public class CalculatorEndpoint {

    private static final String NAMESPACE_URI =
            "http://nelly.com/calculator";

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final List<String> history = new ArrayList<>();

    @PayloadRoot(
            namespace = NAMESPACE_URI,
            localPart = "addRequest"
    )
    @ResponsePayload
    public AddResponse add(
            @RequestPayload AddRequest request) {

        double result = request.getA() + request.getB();

        record(
                "ADD(" + request.getA()
                        + ", " + request.getB()
                        + ") = " + result
        );

        AddResponse response =
                objectFactory.createAddResponse();

        response.setResult(result);

        return response;
    }

    @PayloadRoot(
            namespace = NAMESPACE_URI,
            localPart = "subRequest"
    )
    @ResponsePayload
    public SubResponse sub(
            @RequestPayload SubRequest request) {

        double result = request.getA() - request.getB();

        record(
                "SUB(" + request.getA()
                        + ", " + request.getB()
                        + ") = " + result
        );

        SubResponse response =
                objectFactory.createSubResponse();

        response.setResult(result);

        return response;
    }

    @PayloadRoot(
            namespace = NAMESPACE_URI,
            localPart = "mulRequest"
    )
    @ResponsePayload
    public MulResponse mul(
            @RequestPayload MulRequest request) {

        double result = request.getA() * request.getB();

        record(
                "MUL(" + request.getA()
                        + ", " + request.getB()
                        + ") = " + result
        );

        MulResponse response =
                objectFactory.createMulResponse();

        response.setResult(result);

        return response;
    }

    @PayloadRoot(
            namespace = NAMESPACE_URI,
            localPart = "divRequest"
    )
    @ResponsePayload
    public DivResponse div(
            @RequestPayload DivRequest request) {

        if (request.getB() == 0) {
            throw new IllegalArgumentException(
                    "Division par zéro impossible."
            );
        }

        double result = request.getA() / request.getB();

        record(
                "DIV(" + request.getA()
                        + ", " + request.getB()
                        + ") = " + result
        );

        DivResponse response =
                objectFactory.createDivResponse();

        response.setResult(result);

        return response;
    }

    @PayloadRoot(
            namespace = NAMESPACE_URI,
            localPart = "getHistoryRequest"
    )
    @ResponsePayload
    public GetHistoryResponse getHistory() {

        GetHistoryResponse response =
                objectFactory.createGetHistoryResponse();

        synchronized (history) {
            response.getOperation().addAll(history);
        }

        return response;
    }

    private void record(String operation) {

        String entry =
                LocalDateTime.now()
                        + " - "
                        + operation;

        synchronized (history) {
            history.add(entry);
        }
    }
}