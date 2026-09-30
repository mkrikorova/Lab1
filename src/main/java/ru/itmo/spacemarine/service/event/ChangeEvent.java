package ru.itmo.spacemarine.service.event;

/**
 * CDI-событие об изменении данных. Сервисы его отправляют; позже на него
 * подпишется WebSocket-эндпоинт (@Observes(during = TransactionPhase.AFTER_SUCCESS))
 * и разошлёт уведомление всем клиентам.
 */
public record ChangeEvent(String entity, Action action, Integer id) {
    public enum Action { CREATED, UPDATED, DELETED }
}
