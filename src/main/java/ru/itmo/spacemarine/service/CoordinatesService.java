package ru.itmo.spacemarine.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.spacemarine.dto.CoordinatesDto;
import ru.itmo.spacemarine.dto.DtoMapper;
import ru.itmo.spacemarine.model.Coordinates;
import ru.itmo.spacemarine.repository.CoordinatesRepository;
import ru.itmo.spacemarine.service.event.ChangeEvent;
import ru.itmo.spacemarine.service.exception.BadRequestException;
import ru.itmo.spacemarine.service.exception.LinkedObjectsException;
import ru.itmo.spacemarine.service.exception.NotFoundException;

import java.util.List;

@ApplicationScoped
@Transactional
public class CoordinatesService {

    private static final String ENTITY = "coordinates";

    @Inject
    private CoordinatesRepository repository;

    @Inject
    private Event<ChangeEvent> events;

    public List<CoordinatesDto> findAll() {
        return repository.findAll().stream().map(DtoMapper::toDto).toList();
    }

    public CoordinatesDto findById(Integer id) {
        return DtoMapper.toDto(get(id));
    }

    public CoordinatesDto create(CoordinatesDto dto) {
        Coordinates saved = repository.save(DtoMapper.toEntity(dto));
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.CREATED, saved.getId()));
        return DtoMapper.toDto(saved);
    }

    public CoordinatesDto update(Integer id, CoordinatesDto dto) {
        Coordinates c = get(id);
        c.setX(dto.x);
        c.setY(dto.y);
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.UPDATED, id));
        return DtoMapper.toDto(c);
    }

    public void delete(Integer id, Integer reassignToId) {
        Coordinates c = get(id);
        long linked = repository.countMarines(id);
        if (linked > 0) {
            if (reassignToId == null) {
                throw new LinkedObjectsException(
                        "С координатами связано десантников: " + linked + ". Выберите координаты для перепривязки", linked);
            }
            if (reassignToId.equals(id)) {
                throw new BadRequestException("Нельзя перепривязать десантников на удаляемые координаты");
            }
            get(reassignToId);
            repository.reassignMarines(id, reassignToId);
            events.fire(new ChangeEvent("spaceMarine", ChangeEvent.Action.UPDATED, null));
        }
        repository.delete(c);
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.DELETED, id));
    }

    public Coordinates get(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Координаты с id=" + id + " не найдены"));
    }
}
