package ru.itmo.spacemarine.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.spacemarine.dto.ChapterDto;
import ru.itmo.spacemarine.dto.DtoMapper;
import ru.itmo.spacemarine.model.Chapter;
import ru.itmo.spacemarine.repository.ChapterRepository;
import ru.itmo.spacemarine.service.event.ChangeEvent;
import ru.itmo.spacemarine.service.exception.BadRequestException;
import ru.itmo.spacemarine.service.exception.LinkedObjectsException;
import ru.itmo.spacemarine.service.exception.NotFoundException;

import java.util.List;

@ApplicationScoped
@Transactional
public class ChapterService {

    private static final String ENTITY = "chapter";

    @Inject
    private ChapterRepository repository;

    @Inject
    private Event<ChangeEvent> events;

    public List<ChapterDto> findAll() {
        return repository.findAll().stream().map(DtoMapper::toDto).toList();
    }

    public ChapterDto findById(Integer id) {
        return DtoMapper.toDto(get(id));
    }

    public ChapterDto create(ChapterDto dto) {
        Chapter saved = repository.save(DtoMapper.toEntity(dto));
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.CREATED, saved.getId()));
        return DtoMapper.toDto(saved);
    }

    public ChapterDto update(Integer id, ChapterDto dto) {
        Chapter c = get(id);
        c.setName(dto.name);
        c.setMarinesCount(dto.marinesCount);
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.UPDATED, id));
        return DtoMapper.toDto(c);
    }

    /**
     * Удаление с перепривязкой: если на орден ссылаются десантники,
     * их нужно перевести в орден reassignToId (выбирает пользователь).
     */
    public void delete(Integer id, Integer reassignToId) {
        Chapter c = get(id);
        long linked = repository.countMarines(id);
        if (linked > 0) {
            if (reassignToId == null) {
                throw new LinkedObjectsException(
                        "С орденом связано десантников: " + linked + ". Выберите орден для перепривязки", linked);
            }
            if (reassignToId.equals(id)) {
                throw new BadRequestException("Нельзя перепривязать десантников на удаляемый орден");
            }
            get(reassignToId);
            repository.reassignMarines(id, reassignToId);
            events.fire(new ChangeEvent("spaceMarine", ChangeEvent.Action.UPDATED, null));
        }
        repository.delete(c);
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.DELETED, id));
    }

    public Chapter get(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Орден с id=" + id + " не найден"));
    }
}
