package com.vaadin.starter.bakery.ui;

import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_DASHBOARD;
import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_LOGOUT;
import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_PRODUCTS;
import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_STOREFRONT;
import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_USERS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.starter.bakery.ui.utils.BakeryConst;
import com.vaadin.starter.bakery.ui.views.HasConfirmation;
import com.vaadin.starter.bakery.ui.views.admin.products.ProductsView;
import com.vaadin.starter.bakery.ui.views.admin.users.UsersView;
import com.vaadin.starter.bakery.ui.views.dashboard.DashboardView;
import com.vaadin.starter.bakery.ui.views.storefront.StorefrontView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Layout principal da aplicação (top bar + tabs), responsável por:
 * <ul>
 *   <li>Apresentar o nome da app e a barra de navegação (tabs);</li>
 *   <li>Construir as tabs disponíveis consoante permissões do utilizador (via {@link AccessAnnotationChecker});</li>
 *   <li>Fornecer um {@link ConfirmDialog} partilhado com vistas que implementem {@link HasConfirmation};</li>
 *   <li>Gerir o fluxo de logout e a seleção visual da tab de navegação após mudanças de rota.</li>
 * </ul>
 */
public class MainView extends AppLayout {

    @Autowired
    private AccessAnnotationChecker accessChecker;

    /** Diálogo de confirmação partilhado com as vistas que o solicitem. */
    private final ConfirmDialog confirmDialog = new ConfirmDialog();

    /** Conjunto de tabs de navegação do topo. */
    private Tabs menu;

    /** URL de destino após logout bem-sucedido. */
    private static final String LOGOUT_SUCCESS_URL = "/" + BakeryConst.PAGE_ROOT;

    /**
     * Inicializa o layout após a construção do bean:
     * <ul>
     *   <li>Configura o {@link ConfirmDialog};</li>
     *   <li>Cria o menu de tabs conforme as permissões do utilizador;</li>
     *   <li>Regista o handler de logout na tab dedicada;</li>
     *   <li>Adiciona componentes à app bar e listeners para ocultar/mostrar a navbar durante pesquisa.</li>
     * </ul>
     */
    @PostConstruct
    public void init() {
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmButtonTheme("raised tertiary error");
        confirmDialog.setCancelButtonTheme("raised tertiary");

        this.setDrawerOpened(false);

        // Título da app na barra superior
        Span appName = new Span("###Bakery###");
        appName.addClassName("hide-on-mobile");

        // Tabs de navegação
        menu = createMenuTabs();

        // Handler de logout quando a tab "logout" é selecionada
        menu.addSelectedChangeListener(e -> {
            if (e.getSelectedTab() == null) {
                return;
            }
            e.getSelectedTab().getId().ifPresent(id -> {
                if ("logout-tab".equals(id)) {
                    UI.getCurrent().getPage().setLocation(LOGOUT_SUCCESS_URL);
                    SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
                    logoutHandler.logout(
                            VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
                }
            });
        });

        this.addToNavbar(appName);
        this.addToNavbar(true, menu);
        this.getElement().appendChild(confirmDialog.getElement());

        // Oculta/mostra a navbar quando a pesquisa recebe/perde foco
        getElement().addEventListener("search-focus", e -> getElement().getClassList().add("hide-navbar"));
        getElement().addEventListener("search-blur", e -> getElement().getClassList().remove("hide-navbar"));
    }

    /**
     * Após a navegação, fecha o diálogo de confirmação, disponibiliza-o à vista corrente
     * (se implementar {@link HasConfirmation}) e sincroniza a tab selecionada com a rota ativa.
     */
    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        confirmDialog.setOpened(false);

        if (getContent() instanceof HasConfirmation) {
            ((HasConfirmation) getContent()).setConfirmDialog(confirmDialog);
        }

        RouteConfiguration configuration = RouteConfiguration.forSessionScope();
        if (configuration.isRouteRegistered(this.getContent().getClass())) {
            String target = configuration.getUrl(this.getContent().getClass());
            Optional<Component> tabToSelect = menu.getChildren().filter(tab -> {
                Component child = tab.getChildren().findFirst().get();
                return child instanceof RouterLink && ((RouterLink) child).getHref().equals(target);
            }).findFirst();
            tabToSelect.ifPresent(tab -> menu.setSelectedTab((Tab) tab));
        } else {
            menu.setSelectedTab(null);
        }
    }

    /**
     * Cria o container de tabs horizontais que compõem o menu.
     *
     * @return componente {@link Tabs} com as tabs disponíveis
     */
    private Tabs createMenuTabs() {
        final Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.HORIZONTAL);
        tabs.add(getAvailableTabs());
        return tabs;
    }

    /**
     * Constrói a lista de tabs de acordo com as permissões do utilizador corrente:
     * <ul>
     *   <li>Storefront;</li>
     *   <li>Dashboard;</li>
     *   <li>Users (se tiver acesso);</li>
     *   <li>Products (se tiver acesso);</li>
     *   <li>Logout.</li>
     * </ul>
     *
     * @return array de {@link Tab} para adicionar ao menu
     */
    private Tab[] getAvailableTabs() {
        final List<Tab> tabs = new ArrayList<>(4);
        tabs.add(createTab(VaadinIcon.EDIT, TITLE_STOREFRONT, StorefrontView.class));
        tabs.add(createTab(VaadinIcon.CLOCK, TITLE_DASHBOARD, DashboardView.class));

        if (accessChecker.hasAccess(UsersView.class,
                VaadinServletRequest.getCurrent().getHttpServletRequest())) {
            tabs.add(createTab(VaadinIcon.USER, TITLE_USERS, UsersView.class));
        }
        if (accessChecker.hasAccess(ProductsView.class,
                VaadinServletRequest.getCurrent().getHttpServletRequest())) {
            tabs.add(createTab(VaadinIcon.CALENDAR, TITLE_PRODUCTS, ProductsView.class));
        }

        final String contextPath = VaadinServlet.getCurrent().getServletContext().getContextPath();
        final Tab logoutTab = createTab(createLogoutLink(contextPath));
        logoutTab.setId("logout-tab");
        tabs.add(logoutTab);
        return tabs.toArray(new Tab[tabs.size()]);
    }

    /**
     * Cria uma tab para uma dada vista/rota Vaadin, com ícone e título.
     *
     * @param icon  ícone da tab
     * @param title título visível
     * @param viewClass classe da vista alvo (rota)
     * @return tab configurada
     */
    private static Tab createTab(VaadinIcon icon, String title, Class<? extends Component> viewClass) {
        return createTab(populateLink(new RouterLink("", viewClass), icon, title));
    }

    /**
     * Envolve um conteúdo (link/componente) numa tab com o tema de ícone no topo.
     *
     * @param content componente a colocar dentro da tab
     * @return tab configurada
     */
    private static Tab createTab(Component content) {
        final Tab tab = new Tab();
        tab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
        tab.add(content);
        return tab;
    }

    /**
     * Cria o link de logout (conteúdo da tab). O route/URL efetivo é tratado no listener,
     * que chama o {@link SecurityContextLogoutHandler}.
     *
     * @param contextPath context path da aplicação (não utilizado aqui, mas mantido para extensões futuras)
     * @return link (anchor) estilizado como tab de logout
     */
    private static Anchor createLogoutLink(String contextPath) {
        final Anchor a = populateLink(new Anchor(), VaadinIcon.ARROW_RIGHT, TITLE_LOGOUT);
        return a;
    }

    /**
     * Popula um componente "linkável" com ícone e título.
     *
     * @param a componente que aceita children (ex.: {@link RouterLink} ou {@link Anchor})
     * @param icon ícone a apresentar
     * @param title texto do título
     * @param <T> tipo de componente que implementa {@link HasComponents}
     * @return o próprio componente, para encadeamento
     */
    private static <T extends HasComponents> T populateLink(T a, VaadinIcon icon, String title) {
        a.add(icon.create());
        a.add(title);
        return a;
    }
}
