package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import com.vaadin.starter.bakery.backend.data.entity.AbstractEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Extensão do {@link CrudService} que adiciona funcionalidades
 * de pesquisa com filtros, permitindo obter subconjuntos de entidades.
 *
 * @param <T> o tipo de entidade que estende {@link AbstractEntity}
 */
public interface FilterableCrudService<T extends AbstractEntity> extends CrudService<T> {

    /**
     * Procura entidades que correspondam a um determinado filtro textual.
     * 
     * @param filter filtro opcional (ex.: nome ou parte do nome da entidade).
     *               Se vazio, devolve todas as entidades.
     * @param pageable objeto {@link Pageable} que define paginação e ordenação.
     * @return uma página ({@link Page}) de entidades encontradas.
     */
    Page<T> findAnyMatching(Optional<String> filter, Pageable pageable);

    /**
     * Conta o número de entidades que correspondem ao filtro fornecido.
     *
     * @param filter filtro opcional (ex.: nome ou parte do nome da entidade).
     *               Se vazio, conta todas as entidades.
     * @return número de entidades que correspondem ao filtro.
     */
    long countAnyMatching(Optional<String> filter);

}
