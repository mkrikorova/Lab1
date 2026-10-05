package ru.itmo.spacemarine.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.spacemarine.dto.*;
import ru.itmo.spacemarine.model.Chapter;
import ru.itmo.spacemarine.model.Coordinates;
import ru.itmo.spacemarine.model.MeleeWeapon;
import ru.itmo.spacemarine.model.SpaceMarine;
import ru.itmo.spacemarine.repository.ChapterRepository;
import ru.itmo.spacemarine.repository.CoordinatesRepository;
import ru.itmo.spacemarine.repository.SpaceMarineRepository;
import ru.itmo.spacemarine.service.event.ChangeEvent;
import ru.itmo.spacemarine.service.exception.BadRequestException;
import ru.itmo.spacemarine.service.exception.NotFoundException;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
@Transactional
public class SpaceMarineService {

    private static final String ENTITY = "spaceMarine";
    private static final int MAX_PAGE_SIZE = 100;

    @Inject
    private SpaceMarineRepository marines;
    @Inject
    private ChapterRepository chapters;
    @Inject
    private CoordinatesRepository coordinates;
    @Inject
    private ChapterService chapterService;
    @Inject
    private CoordinatesService coordinatesService;
    @Inject
    private Event<ChangeEvent> events;

    public PageDto<SpaceMarineDto> findPage(SpaceMarineFilter f) {
        if (f.page < 0) throw new BadRequestException("page не может быть отрицательным");
        if (f.size < 1 || f.size > MAX_PAGE_SIZE) throw new BadRequestException("size должен быть от 1 до " + MAX_PAGE_SIZE);
        if (f.meleeWeapon != null && !f.meleeWeapon.isEmpty()) {
            parseWeapon(f.meleeWeapon);
        }
        List<SpaceMarineDto> content = marines.findPage(f).stream().map(DtoMapper::toDto).toList();
        return new PageDto<>(content, f.page, f.size, marines.count(f));
    }

    /** Все объекты без пагинации — для визуализации на карте. */
    public List<SpaceMarineDto> findAll() {
        return marines.findAll().stream().map(DtoMapper::toDto).toList();
    }

    public SpaceMarineDto findById(Integer id) {
        return DtoMapper.toDto(get(id));
    }

    public SpaceMarineDto create(SpaceMarineRequest req) {
        SpaceMarine m = new SpaceMarine();
        apply(m, req);
        marines.save(m);
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.CREATED, m.getId()));
        return DtoMapper.toDto(m);
    }

    public SpaceMarineDto update(Integer id, SpaceMarineRequest req) {
        SpaceMarine m = get(id);
        apply(m, req); // id и creationDate не меняются
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.UPDATED, id));
        return DtoMapper.toDto(m);
    }

    public void delete(Integer id) {
        marines.delete(get(id));
        events.fire(new ChangeEvent(ENTITY, ChangeEvent.Action.DELETED, id));
    }

    private void apply(SpaceMarine m, SpaceMarineRequest req) {
        m.setName(req.name);
        m.setCoordinates(resolveCoordinates(req));
        m.setChapter(resolveChapter(req));
        m.setHealth(req.health);
        m.setHeartCount(req.heartCount);
        m.setHeight(req.height);
        m.setMeleeWeapon(req.meleeWeapon);
    }

    /** Либо существующие координаты по id, либо новые. */
    private Coordinates resolveCoordinates(SpaceMarineRequest req) {
        if ((req.coordinatesId == null) == (req.coordinates == null)) {
            throw new BadRequestException("Укажите либо coordinatesId существующих координат, либо новые coordinates");
        }
        if (req.coordinatesId != null) {
            return coordinatesService.get(req.coordinatesId);
        }
        Coordinates created = coordinates.save(DtoMapper.toEntity(req.coordinates));
        events.fire(new ChangeEvent("coordinates", ChangeEvent.Action.CREATED, created.getId()));
        return created;
    }

    /** Либо существующий орден по id, либо новый. */
    private Chapter resolveChapter(SpaceMarineRequest req) {
        if ((req.chapterId == null) == (req.chapter == null)) {
            throw new BadRequestException("Укажите либо chapterId существующего ордена, либо новый chapter");
        }
        if (req.chapterId != null) {
            return chapterService.get(req.chapterId);
        }
        Chapter created = chapters.save(DtoMapper.toEntity(req.chapter));
        events.fire(new ChangeEvent("chapter", ChangeEvent.Action.CREATED, created.getId()));
        return created;
    }

    private SpaceMarine get(Integer id) {
        return marines.findById(id)
                .orElseThrow(() -> new NotFoundException("Десантник с id=" + id + " не найден"));
    }

    static MeleeWeapon parseWeapon(String value) {
        try {
            return MeleeWeapon.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Неизвестное оружие '" + value + "'. Допустимо: "
                    + Arrays.toString(MeleeWeapon.values()));
        }
    }
}
