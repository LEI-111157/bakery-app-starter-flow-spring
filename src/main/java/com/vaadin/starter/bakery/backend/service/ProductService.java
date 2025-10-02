package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.entity.Product;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.ProductRepository;

/**
 * Serviço para gerir {@link Product} (produtos da pastelaria).
 * <p>
 * Permite criar, guardar, pesquisar e contar produtos através de filtros textuais.
 * Implementa {@link FilterableCrudService} para disponibilizar operações CRUD
 * com suporte a filtros e paginação.
 * </p>
 */
@Service
public class ProductService implements FilterableCrudService<Product> {

    private final ProductRepository productRepository;

    /**
     * Construtor com injeção do repositório de produtos.
     *
     * @param productRepository repositório JPA de {@link Product}
     */
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Procura produtos cujo nome corresponda parcialmente ao filtro fornecido
     * (ignora maiúsculas/minúsculas).
     *
     * @param filter filtro opcional (parte do nome do produto)
     * @param pageable configuração de paginação e ordenação
     * @return página de {@link Product} que correspondem ao filtro
     */
    @Override
    public Page<Product> findAnyMatching(Optional<String> filter, Pageable pageable) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return productRepository.findByNameLikeIgnoreCase(repositoryFilter, pageable);
        } else {
            return find(pageable);
        }
    }

    /**
     * Conta os produtos que correspondem ao filtro fornecido.
     *
     * @param filter filtro opcional (parte do nome do produto)
     * @return número de produtos que correspondem ao filtro
     */
    @Override
    public long countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return productRepository.countByNameLikeIgnoreCase(repositoryFilter);
        } else {
            return count();
        }
    }

    /**
     * Obtém todos os produtos de forma paginada.
     *
     * @param pageable configuração de paginação e ordenação
     * @return página de produtos
     */
    public Page<Product> find(Pageable pageable) {
        return productRepository.findBy(pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JpaRepository<Product, Long> getRepository() {
        return productRepository;
    }

    /**
     * Cria uma nova instância de {@link Product}.
     *
     * @param currentUser utilizador autenticado (não usado nesta operação)
     * @return nova instância de {@link Product}
     */
    @Override
    public Product createNew(User currentUser) {
        return new Product();
    }

    /**
     * Persiste um produto garantindo que o nome é único.
     * <p>
     * Caso o nome já exista, lança uma exceção amigável para o utilizador,
     * em vez de expor a exceção técnica de integridade de dados.
     * </p>
     *
     * @param currentUser utilizador autenticado que executa a operação
     * @param entity produto a guardar
     * @return produto persistido
     * @throws UserFriendlyDataException se já existir um produto com o mesmo nome
     */
    @Override
    public Product save(User currentUser, Product entity) {
        try {
            return FilterableCrudService.super.save(currentUser, entity);
        } catch (DataIntegrityViolationException e) {
            throw new UserFriendlyDataException(
                    "There is already a product with that name. Please select a unique name for the product.");
        }
    }
}
