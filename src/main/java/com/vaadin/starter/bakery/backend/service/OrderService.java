package com.vaadin.starter.bakery.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.DashboardData;
import com.vaadin.starter.bakery.backend.data.DeliveryStats;
import com.vaadin.starter.bakery.backend.data.OrderState;
import com.vaadin.starter.bakery.backend.data.entity.Order;
import com.vaadin.starter.bakery.backend.data.entity.OrderSummary;
import com.vaadin.starter.bakery.backend.data.entity.Product;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.OrderRepository;

/**
 * Serviço de regras de negócio para {@link Order}.
 * <p>
 * Responsável por criar/atualizar encomendas, adicionar comentários ao
 * histórico, consultar/paginar encomendas por filtros e produzir dados
 * agregados para o dashboard (KPIs, vendas por mês e produto, etc.).
 * </p>
 *
 * <p><b>Transações:</b> métodos de escrita são anotados com {@link Transactional}.</p>
 */
@Service
public class OrderService implements CrudService<Order> {

    private final OrderRepository orderRepository;

    /**
     * Construtor com injeção do repositório de encomendas.
     *
     * @param orderRepository repositório JPA de {@link Order}
     */
    @Autowired
    public OrderService(OrderRepository orderRepository) {
        super();
        this.orderRepository = orderRepository;
    }

    /** Conjunto de estados considerados "não disponíveis" para entrega hoje. */
    private static final Set<OrderState> notAvailableStates = Collections.unmodifiableSet(
            EnumSet.complementOf(EnumSet.of(OrderState.DELIVERED, OrderState.READY, OrderState.CANCELLED)));

    /**
     * Cria ou atualiza uma encomenda preenchida por um {@link BiConsumer}.
     * <p>
     * Se {@code id} for {@code null}, cria nova encomenda associada ao {@code currentUser};
     * caso contrário, carrega a encomenda existente e aplica o preenchimento através de
     * {@code orderFiller}. No final, persiste a encomenda.
     * </p>
     *
     * @param currentUser utilizador autenticado que executa a operação
     * @param id identificador da encomenda ou {@code null} para nova
     * @param orderFiller função que preenche/modifica a encomenda (ex.: campos do formulário)
     * @return encomenda persistida
     */
    @Transactional(rollbackOn = Exception.class)
    public Order saveOrder(User currentUser, Long id, BiConsumer<User, Order> orderFiller) {
        Order order;
        if (id == null) {
            order = new Order(currentUser);
        } else {
            order = load(id);
        }
        orderFiller.accept(currentUser, order);
        return orderRepository.save(order);
    }

    /**
     * Persiste uma encomenda já preparada.
     *
     * @param order encomenda a guardar
     * @return encomenda persistida
     */
    @Transactional(rollbackOn = Exception.class)
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Adiciona um comentário ao histórico da encomenda e persiste a alteração.
     *
     * @param currentUser utilizador que adiciona o comentário
     * @param order encomenda alvo
     * @param comment texto do comentário
     * @return encomenda atualizada
     */
    @Transactional(rollbackOn = Exception.class)
    public Order addComment(User currentUser, Order order, String comment) {
        order.addHistoryItem(currentUser, comment);
        return orderRepository.save(order);
    }

    /**
     * Procura encomendas por nome do cliente (contém, ignore case) e/ou por data
     * de entrega posterior a um limite, com paginação.
     *
     * @param optionalFilter filtro textual opcional para o nome completo do cliente
     * @param optionalFilterDate data mínima (exclusive) opcional para {@code dueDate}
     * @param pageable paginação/ordenação
     * @return página de encomendas que satisfazem os filtros
     */
    public Page<Order> findAnyMatchingAfterDueDate(Optional<String> optionalFilter,
                                                   Optional<LocalDate> optionalFilterDate, Pageable pageable) {
        if (optionalFilter.isPresent() && !optionalFilter.get().isEmpty()) {
            if (optionalFilterDate.isPresent()) {
                return orderRepository.findByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(
                        optionalFilter.get(), optionalFilterDate.get(), pageable);
            } else {
                return orderRepository.findByCustomerFullNameContainingIgnoreCase(optionalFilter.get(), pageable);
            }
        } else {
            if (optionalFilterDate.isPresent()) {
                return orderRepository.findByDueDateAfter(optionalFilterDate.get(), pageable);
            } else {
                return orderRepository.findAll(pageable);
            }
        }
    }

    /**
     * Obtém um resumo das encomendas com data de entrega a partir de hoje (inclusive).
     *
     * @return lista de {@link OrderSummary} a partir da data corrente
     */
    @Transactional
    public List<OrderSummary> findAnyMatchingStartingToday() {
        return orderRepository.findByDueDateGreaterThanEqual(LocalDate.now());
    }

    /**
     * Conta encomendas que correspondem a um filtro opcional por nome do cliente e/ou
     * a uma data mínima de entrega.
     *
     * @param optionalFilter filtro textual opcional para o nome completo do cliente
     * @param optionalFilterDate data mínima (exclusive) opcional para {@code dueDate}
     * @return total de encomendas que satisfazem os filtros
     */
    public long countAnyMatchingAfterDueDate(Optional<String> optionalFilter, Optional<LocalDate> optionalFilterDate) {
        if (optionalFilter.isPresent() && optionalFilterDate.isPresent()) {
            return orderRepository.countByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(optionalFilter.get(),
                    optionalFilterDate.get());
        } else if (optionalFilter.isPresent()) {
            return orderRepository.countByCustomerFullNameContainingIgnoreCase(optionalFilter.get());
        } else if (optionalFilterDate.isPresent()) {
            return orderRepository.countByDueDateAfter(optionalFilterDate.get());
        } else {
            return orderRepository.count();
        }
    }

    /**
     * Calcula estatísticas rápidas de entrega (KPIs) para o dia atual.
     *
     * @return estatísticas de entrega agregadas
     */
    private DeliveryStats getDeliveryStats() {
        DeliveryStats stats = new DeliveryStats();
        LocalDate today = LocalDate.now();
        stats.setDueToday((int) orderRepository.countByDueDate(today));
        stats.setDueTomorrow((int) orderRepository.countByDueDate(today.plusDays(1)));
        stats.setDeliveredToday((int) orderRepository.countByDueDateAndStateIn(today,
                Collections.singleton(OrderState.DELIVERED)));

        stats.setNotAvailableToday((int) orderRepository.countByDueDateAndStateIn(today, notAvailableStates));
        stats.setNewOrders((int) orderRepository.countByState(OrderState.NEW));

        return stats;
    }

    /**
     * Produz os dados necessários ao dashboard:
     * <ul>
     *   <li>KPIs de entregas de hoje/amanhã/novas;</li>
     *   <li>Entregas por dia no mês selecionado;</li>
     *   <li>Entregas por mês no ano selecionado;</li>
     *   <li>Vendas (entregas) por mês para os últimos 3 anos;</li>
     *   <li>Top de produtos entregues no mês/ano.</li>
     * </ul>
     *
     * @param month mês (1–12)
     * @param year ano (ex.: 2025)
     * @return estrutura {@link DashboardData} preenchida
     */
    public DashboardData getDashboardData(int month, int year) {
        DashboardData data = new DashboardData();
        data.setDeliveryStats(getDeliveryStats());
        data.setDeliveriesThisMonth(getDeliveriesPerDay(month, year));
        data.setDeliveriesThisYear(getDeliveriesPerMonth(year));

        Number[][] salesPerMonth = new Number[3][12];
        data.setSalesPerMonth(salesPerMonth);
        List<Object[]> sales = orderRepository.sumPerMonthLastThreeYears(OrderState.DELIVERED, year);

        for (Object[] salesData : sales) {
            // year, month, deliveries
            int y = year - (int) salesData[0];
            int m = (int) salesData[1] - 1;
            if (y == 0 && m == month - 1) {
                // ignorar mês corrente por estar incompleto
                continue;
            }
            long count = (long) salesData[2];
            salesPerMonth[y][m] = count;
        }

        LinkedHashMap<Product, Integer> productDeliveries = new LinkedHashMap<>();
        data.setProductDeliveries(productDeliveries);
        for (Object[] result : orderRepository.countPerProduct(OrderState.DELIVERED, year, month)) {
            int sum = ((Long) result[0]).intValue();
            Product p = (Product) result[1];
            productDeliveries.put(p, sum);
        }

        return data;
    }

    /**
     * Agrega contagens de entregas por dia para um mês/ano, substituindo dias sem dados por {@code null}.
     *
     * @param month mês (1–12)
     * @param year ano
     * @return lista de tamanho = número de dias do mês (índice = dia-1)
     */
    private List<Number> getDeliveriesPerDay(int month, int year) {
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        return flattenAndReplaceMissingWithNull(daysInMonth,
                orderRepository.countPerDay(OrderState.DELIVERED, year, month));
    }

    /**
     * Agrega contagens de entregas por mês para um ano, substituindo meses sem dados por {@code null}.
     *
     * @param year ano
     * @return lista de tamanho 12 (índice 0 = janeiro)
     */
    private List<Number> getDeliveriesPerMonth(int year) {
        return flattenAndReplaceMissingWithNull(12, orderRepository.countPerMonth(OrderState.DELIVERED, year));
    }

    /**
     * Converte uma lista de pares (posição, valor) numa lista densa de tamanho fixo,
     * preenchendo posições em falta com {@code null}.
     *
     * @param length tamanho da lista final
     * @param list lista de resultados onde {@code result[0]} é a posição (1-based) e {@code result[1]} é o valor
     * @return lista densa com {@code length} posições
     */
    private List<Number> flattenAndReplaceMissingWithNull(int length, List<Object[]> list) {
        List<Number> counts = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            counts.add(null);
        }

        for (Object[] result : list) {
            counts.set((Integer) result[0] - 1, (Number) result[1]);
        }
        return counts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JpaRepository<Order, Long> getRepository() {
        return orderRepository;
    }

    /**
     * Cria uma nova encomenda com data de entrega = hoje e hora padrão 16:00.
     *
     * @param currentUser utilizador autenticado associado ao registo
     * @return nova instância de {@link Order}
     */
    @Override
    @Transactional
    public Order createNew(User currentUser) {
        Order order = new Order(currentUser);
        order.setDueTime(LocalTime.of(16, 0));
        order.setDueDate(LocalDate.now());
        return order;
    }

}
