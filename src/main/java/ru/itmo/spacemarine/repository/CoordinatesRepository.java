package ru.itmo.spacemarine.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.itmo.spacemarine.model.Coordinates;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CoordinatesRepository {

    @PersistenceContext(unitName = "spacemarinePU")
    private EntityManager em;

    public Optional<Coordinates> findById(Integer id) {
        return Optional.ofNullable(em.find(Coordinates.class, id));
    }

    public List<Coordinates> findAll() {
        return em.createQuery("SELECT c FROM Coordinates c ORDER BY c.id", Coordinates.class).getResultList();
    }

    public Coordinates save(Coordinates coordinates) {
        if (coordinates.getId() == null) {
            em.persist(coordinates);
            em.flush();
            return coordinates;
        }
        return em.merge(coordinates);
    }

    public void delete(Coordinates coordinates) {
        em.remove(em.contains(coordinates) ? coordinates : em.merge(coordinates));
    }

    public long countMarines(Integer coordinatesId) {
        return em.createQuery("SELECT COUNT(m) FROM SpaceMarine m WHERE m.coordinates.id = :id", Long.class)
                .setParameter("id", coordinatesId)
                .getSingleResult();
    }

    public int reassignMarines(Integer fromId, Integer toId) {
        return em.createQuery("UPDATE SpaceMarine m SET m.coordinates = :to WHERE m.coordinates.id = :from")
                .setParameter("to", em.getReference(Coordinates.class, toId))
                .setParameter("from", fromId)
                .executeUpdate();
    }
}
