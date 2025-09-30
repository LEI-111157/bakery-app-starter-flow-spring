package com.vaadin.starter.bakery.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import com.vaadin.starter.bakery.backend.data.entity.AbstractEntity;
import com.vaadin.starter.bakery.backend.data.entity.User;

/**
 * Serviço genérico para operações CRUD (Create, Read, Update, Delete)
 * sobre entidades que estendem {@link AbstractEntity}.
 * <p>
 * Esta interface define métodos básicos para persistência e gestão
 * de entidades, delegando as operações a um {@link JpaRepository}.
 * </p>
 *
 * @param <T> tipo da entidade que herda de {@link AbstractEntity}
 */
public interface CrudService<T extends AbstractEntity> {

    /**
     * Obtém o repositório JPA associado a esta entidade.
     *
     * @return instância de {@link JpaRepository} para a entidade {@code T}
     */
    JpaRepository<T, Long> getRepository();

    /**
     * Guarda ou atualiza uma entidade no repositório.
     *
     * @param currentUser utilizador autenticado que executa a operação
     * @param entity entidade a ser guardada
     * @return a entidade persistida
     */
    default T save(User currentUser, T entity) {
        return getRepository().saveAndFlush(entity);
    }

    /**
     * Remove uma entidade existente.
     *
     * @param currentUser utilizador autenticado que executa a operação
     * @param entity entidade a ser removida; não pode ser {@code null}
     * @throws EntityNotFoundException se a entidade for {@code null}
     */
    default void delete(User currentUser, T entity) {
        if (entity == null) {
            throw new EntityNotFoundException();
        }
        getRepository().delete(entity);
    }

    /**
     * Remove uma entidade pelo seu identificador.
     *
     * @param currentUser utilizador autenticado que executa a operação
     * @param id identificador único da entidade
     * @throws EntityNotFoundException se não existir entidade com o id fornecido
     */
    default void delete(User currentUser, long id) {
        delete(currentUser, load(id));
    }

    /**
     * Conta o número total de entidades existentes.
     *
     * @return quantidade de entidades persistidas
     */
    default long count() {
        return getRepository().count();
    }

    /**
     * Carrega uma entidade pelo seu identificador.
     *
     * @param id identificador único da entidade
     * @return entidade encontrada
     * @throws EntityNotFoundException se não existir entidade com o id fornecido
     */
    default T load(long id) {
        T entity = getRepository().findById(id).orElse(null);
        if (entity == null) {
            throw new EntityNotFoundException();
        }
        return entity;
    }

    /**
     * Cria uma nova instância da entidade {@code T}.
     * <p>
     * O comportamento concreto deve ser definido pelas classes que
     * implementam esta interface.
     * </p>
     *
     * @param currentUser utilizador autenticado que executa a operação
     * @return nova instância da entidade
     */
    T createNew(User currentUser);
}
