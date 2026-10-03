package com.pyjudge.dao;

import java.util.List;
import java.util.Optional;

import com.pyjudge.model.Identifiable;

/**
 * Generic CRUD contract shared by the data-access objects.
 *
 * @param <T> the entity type
 */
public interface Repository<T extends Identifiable> {

    Optional<T> findById(long id);

    List<T> findAll();

    /** Inserts when the entity has no id yet, otherwise updates. Returns the saved entity (with its id). */
    T save(T entity);

    boolean delete(long id);
}
