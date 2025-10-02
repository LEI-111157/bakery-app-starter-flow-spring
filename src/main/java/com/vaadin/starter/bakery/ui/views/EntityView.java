package com.vaadin.starter.bakery.ui.views;

import com.vaadin.flow.data.binder.ValidationException;

/**
 * Interface genérica para uma vista "Master/Detail" de entidades do tipo {@code T}.
 * <p>
 * Uma EntityView é composta por:
 * <ul>
 *   <li>Um painel/lista de entidades (parte "master");</li>
 *   <li>Um diálogo de detalhe para visualizar ou editar uma entidade (parte "detail").</li>
 * </ul>
 *
 * O diálogo de detalhe pode estar em dois modos:
 * <ul>
 *   <li><b>View</b>: apenas leitura;</li>
 *   <li><b>Edit</b>: permite alterações, que podem ser persistidas.</li>
 * </ul>
 *
 * Além disso, a vista suporta:
 * <ul>
 *   <li>Notificações de sucesso/erro;</li>
 *   <li>Mensagens de confirmação (via {@link HasConfirmation});</li>
 *   <li>Gestão do estado "dirty" (alterações não guardadas).</li>
 * </ul>
 *
 * @param <T> tipo de entidade gerida pela vista
 */
public interface EntityView<T> extends HasConfirmation, HasNotifications {

    /**
     * Mostra uma notificação de erro com o texto fornecido.
     *
     * @param message mensagem de erro legível pelo utilizador
     * @param isPersistent se {@code true}, o utilizador deve fechá-la manualmente;
     *                     se {@code false}, desaparece automaticamente após alguns segundos
     */
    default void showError(String message, boolean isPersistent) {
        showNotification(message, isPersistent);
    }

    /**
     * Indica se o diálogo da entidade tem alterações não guardadas (estado "dirty").
     *
     * @return {@code true} se o diálogo estiver aberto em modo edição e com alterações por guardar,
     *         caso contrário {@code false}
     */
    boolean isDirty();

    /**
     * Remove a referência à entidade atual e limpa o estado de alterações (dirty flag).
     */
    void clear();

    /**
     * Copia os valores do diálogo da entidade para a instância da entidade fornecida.
     * <p>
     * Internamente deve usar {@link com.vaadin.flow.data.binder.Binder#writeBean(Object)}.
     * </p>
     *
     * @param entity entidade de destino onde os valores do formulário serão gravados
     * @throws ValidationException se algum valor do diálogo não puder ser convertido
     *                             para a propriedade correspondente da entidade
     */
    void write(T entity) throws ValidationException;

    /**
     * Obtém o nome legível da entidade (ex.: "User", "Product").
     *
     * @return nome da entidade gerida pela vista
     */
    String getEntityName();

    /**
     * Mostra uma notificação padrão quando uma entidade é criada.
     */
    default void showCreatedNotification() {
        showNotification(getEntityName() + " was created");
    }

    /**
     * Mostra uma notificação padrão quando uma entidade é atualizada.
     */
    default void showUpdatedNotification() {
        showNotification(getEntityName() + " was updated");
    }

    /**
     * Mostra uma notificação padrão quando uma entidade é removida.
     */
    default void showDeletedNotification() {
        showNotification(getEntityName() + " was deleted");
    }
}
