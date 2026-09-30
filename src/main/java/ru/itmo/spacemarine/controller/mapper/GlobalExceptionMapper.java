package ru.itmo.spacemarine.controller.mapper;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.spacemarine.dto.ErrorDto;
import ru.itmo.spacemarine.service.exception.BadRequestException;
import ru.itmo.spacemarine.service.exception.LinkedObjectsException;
import ru.itmo.spacemarine.service.exception.NotFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Превращает любые исключения в понятный JSON для фронтенда.
 * Исключения из @Transactional/EclipseLink приходят обёрнутыми, поэтому идём по цепочке cause.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof WebApplicationException w) {
                Response r = w.getResponse();
                return json(r.getStatus(), r.getStatusInfo().getReasonPhrase(), null);
            }
            if (t instanceof ConstraintViolationException cve) {
                return ValidationExceptionMapper.build(cve);
            }
            if (t instanceof NotFoundException) {
                return json(404, t.getMessage(), null);
            }
            if (t instanceof BadRequestException) {
                return json(400, t.getMessage(), null);
            }
            if (t instanceof LinkedObjectsException l) {
                return json(409, l.getMessage(), List.of("linkedCount: " + l.getLinkedCount()));
            }
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return fromSql(sql);
            }
        }
        LOG.log(Level.SEVERE, "Unexpected error", e);
        return json(500, "Внутренняя ошибка сервера", null);
    }

    /** Ошибки ограничений БД и RAISE EXCEPTION из функций. */
    private Response fromSql(SQLException e) {
        String state = e.getSQLState();
        String msg = firstLine(e.getMessage());
        return switch (state) {
            case "23503" -> json(409, "Нарушена связь между объектами", List.of(msg));   // foreign key
            case "23505" -> json(409, "Объект уже существует", List.of(msg));            // unique
            case "23502", "23514", "22023", "22003", "P0001" ->
                    json(400, "Данные не прошли проверку в БД", List.of(msg));        // not null / check / raise
            case "P0002" -> json(404, msg, null);                                        // raise ... 'P0002'
            default -> {
                LOG.log(Level.SEVERE, "SQL error " + state, e);
                yield json(500, "Ошибка базы данных", List.of(msg));
            }
        };
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        String line = s.split("\\R", 2)[0];
        return line.startsWith("ERROR: ") ? line.substring(7) : line;
    }

    private static Response json(int status, String message, List<String> details) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(status, message, details))
                .build();
    }
}
