package com.linebook.common.web;

import com.linebook.agent.exception.AgentNotFoundException;
import com.linebook.agent.exception.InvalidAgentException;
import com.linebook.billing.exception.BillNotFoundException;
import com.linebook.billing.exception.InvalidPaymentException;
import com.linebook.collection.exception.CollectionNotFoundException;
import com.linebook.collection.exception.InvalidCollectionException;
import com.linebook.customer.exception.CustomerNotFoundException;
import com.linebook.customer.exception.InvalidCustomerException;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps errors from every module to RFC 9457 problem responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({CustomerNotFoundException.class, BillNotFoundException.class,
            CollectionNotFoundException.class, AgentNotFoundException.class})
    ProblemDetail notFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** A collection that is not allowed: bad amount, settled or disconnected bill, unknown agent. */
    @ExceptionHandler({InvalidPaymentException.class, InvalidCollectionException.class})
    ProblemDetail unprocessable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), ex.getMessage());
    }

    @ExceptionHandler({InvalidCustomerException.class, InvalidAgentException.class})
    ProblemDetail invalid(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Malformed request body (status must be pending, active, suspended or disconnected; method must be cash or upi)");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail badParameter(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid value for '" + ex.getName() + "' (dates are yyyy-MM-dd)");
    }

    @ExceptionHandler(DateTimeParseException.class)
    ProblemDetail badCycle(DateTimeParseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "cycle must be formatted as yyyy-MM");
    }

    /** e.g. an unknown ?status= filter value. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid value: status must be one of paid, partial, pending, overdue (bills) or "
                        + "pending, active, suspended, disconnected (customers)");
    }

    /** A unique key was hit, e.g. two requests racing for the same account number or the same collectionId. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflicting concurrent update; please retry");
    }
}
