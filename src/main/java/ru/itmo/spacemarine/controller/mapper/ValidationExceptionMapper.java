package ru.itmo.spacemarine.controller.mapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.spacemarine.dto.ErrorDto;

import java.util.List;

/** Ошибки Bean Validation -> 400 со списком «поле: сообщение». */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException e) {
        return build(e);
    }

    static Response build(ConstraintViolationException e) {
        List<String> details = e.getConstraintViolations().stream()
                .map(ValidationExceptionMapper::format)
                .sorted()
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(400, "Некорректные данные", details))
                .build();
    }

    private static String format(ConstraintViolation<?> v) {
        // путь вида "create.request.health" -> "health"
        String path = v.getPropertyPath().toString();
        String[] parts = path.split("\\.");
        String field = parts.length > 2 ? String.join(".", java.util.Arrays.copyOfRange(parts, 2, parts.length)) : path;
        return field + ": " + v.getMessage();
    }
}
