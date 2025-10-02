package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.entity.PickupLocation;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.PickupLocationRepository;

/**
 * Serviço para gerir {@link PickupLocation} (locais de recolha de encomendas).
 * <p>
 * Permite criar, pesquisar e contar locais de recolha através de filtros textuais,
 * além de fornecer o local de recolha por defeito. Implementa
 * {@link FilterableCrudService} para operações CRUD genéricas com filtro.
 * </p>
 */
@Service
public class PickupLocationService implements FilterableCrudService<PickupLocation> {

    private final PickupLocationRepository pickupLocationRepository;

    /**
     * Construtor com injeção do repositório de locais de recolha.
     *
     * @param pickupLocationRepository repositório JPA de {@link PickupLocation}
     */
    @Autowired
    public PickupLocationService(PickupLocationRepository pickupLocationRepository) {
        this.pickupLocationRepository = pickupLocationRepository;
    }

    /**
     * Procura locais de recolha pelo nome (ignora maiúsculas/minúsculas).
     * <p>
     * Se o filtro estiver presente, procura nomes que contenham o texto;
     * caso contrário, devolve todos os locais de forma paginada.
     * </p>
     *
     * @param filter filtro opcional (parte do nome do local)
     * @param pageable configuração de paginação e ordenação
     * @return página de {@link PickupLocation} que correspondem ao filtro
     */
    public Page<PickupLocation> findAnyMatching(Optional<String> filter, Pageable pageable) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return pickupLocationRepository.findByNameLikeIgnoreCase(repositoryFilter, pageable);
        } else {
            return pickupLocationRepository.findAll(pageable);
        }
    }

    /**
     * Conta o número de locais de recolha que correspondem ao filtro fornecido.
     *
     * @param filter filtro opcional (parte do nome do local)
     * @return número de locais de recolha que correspondem ao filtro
     */
    public long countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return pickupLocationRepository.countByNameLikeIgnoreCase(repositoryFilter);
        } else {
            return pickupLocationRepository.count();
        }
    }

    /**
     * Obtém o local de recolha por defeito (primeiro resultado encontrado).
     *
     * @return {@link PickupLocation} por defeito
     */
    public PickupLocation getDefault() {
        return findAnyMatching(Optional.empty(), PageRequest.of(0, 1)).iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JpaRepository<PickupLocation, Long> getRepository() {
        return pickupLocationRepository;
    }

    /**
     * Cria uma nova instância de {@link PickupLocation}.
     *
     * @param currentUser utilizador autenticado (não usado nesta operação)
     * @return nova instância de {@link PickupLocation}
     */
    @Override
    public PickupLocation createNew(User currentUser) {
        return new PickupLocation();
    }
}
