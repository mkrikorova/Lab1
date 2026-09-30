package ru.itmo.spacemarine.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.spacemarine.dto.ChapterDto;
import ru.itmo.spacemarine.dto.DtoMapper;
import ru.itmo.spacemarine.dto.SpaceMarineDto;
import ru.itmo.spacemarine.dto.WeaponGroupDto;
import ru.itmo.spacemarine.repository.SpecialOperationsRepository;
import ru.itmo.spacemarine.service.event.ChangeEvent;
import ru.itmo.spacemarine.service.exception.BadRequestException;

import java.util.List;

/** Специальные операции — бизнес-логика вызывает функции БД. */
@ApplicationScoped
@Transactional
public class SpecialOperationsService {

    @Inject
    private SpecialOperationsRepository repository;
    @Inject
    private ChapterService chapterService;
    @Inject
    private Event<ChangeEvent> events;

    public long sumHeartCount() {
        return repository.sumHeartCount();
    }

    public List<WeaponGroupDto> groupByMeleeWeapon() {
        return repository.groupByMeleeWeapon().stream()
                .map(r -> new WeaponGroupDto((String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    public List<SpaceMarineDto> findByNameSubstring(String substring) {
        if (substring == null || substring.isEmpty()) {
            throw new BadRequestException("Подстрока не может быть пустой");
        }
        return repository.findByNameSubstring(substring).stream().map(DtoMapper::toDto).toList();
    }

    public ChapterDto createChapter(ChapterDto dto) {
        int id = repository.createChapter(dto.name, dto.marinesCount);
        events.fire(new ChangeEvent("chapter", ChangeEvent.Action.CREATED, id));
        return chapterService.findById(id);
    }

    /** Возвращает количество удалённых вместе с орденом десантников. */
    public int disbandChapter(int chapterId) {
        chapterService.get(chapterId); // 404, если ордена нет
        int deleted = repository.disbandChapter(chapterId);
        events.fire(new ChangeEvent("chapter", ChangeEvent.Action.DELETED, chapterId));
        events.fire(new ChangeEvent("spaceMarine", ChangeEvent.Action.DELETED, null));
        return deleted;
    }
}
