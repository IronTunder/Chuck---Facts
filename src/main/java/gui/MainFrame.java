package gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.HistoryEntry;
import model.JokeResult;
import service.ChuckNorrisService;
import service.DadJokeService;
import service.DeepSeekService;
import service.UselessFactService;
import util.Config;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MainFrame {
    private static final String RANDOM_CATEGORY = "Casuale";

    private final Config config = Config.load();
    private final ChuckNorrisService chuckNorrisService = new ChuckNorrisService();
    private final DadJokeService dadJokeService = new DadJokeService();
    private final UselessFactService uselessFactService = new UselessFactService();
    private final DeepSeekService deepSeekService = new DeepSeekService(config);
    private final ObservableList<HistoryEntry> history = FXCollections.observableArrayList();

    private final ToggleGroup navigationGroup = new ToggleGroup();
    private final ToggleButton chuckButton = createNavButton("Chuck Norris", ApiScreen.CHUCK);
    private final ToggleButton dadButton = createNavButton("Dad Joke", ApiScreen.DAD_JOKE);
    private final ToggleButton factButton = createNavButton("Facts", ApiScreen.USELESS_FACT);
    private final ToggleButton historyNavButton = createNavButton("Cronologia", ApiScreen.HISTORY);

    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final Button generateButton = new Button("Genera");
    private final Button copyButton = new Button("Copia");
    private final Button toggleOriginalButton = new Button("Visualizza originale");

    private final Label screenTitleLabel = new Label();
    private final Label screenSubtitleLabel = new Label();
    private final Label resultTitleLabel = new Label("Traduzione");
    private final Label resultTextLabel = createBodyLabel("");
    private final Label statusLabel = new Label("Pronto");
    private final VBox categoryPicker = new VBox(6);
    private final VBox contentView = new VBox(16);
    private final VBox historyView = new VBox(12);
    private final TableView<HistoryEntry> historyTable = new TableView<>();

    private BorderPane root;
    private ApiScreen currentScreen = ApiScreen.CHUCK;
    private JokeResult currentResult;
    private String currentTranslation = "";
    private boolean showingOriginal;

    public void show(Stage stage) {
        configureControls();
        configureHistoryTable();
        loadChuckCategories();

        root = new BorderPane();
        root.getStyleClass().addAll("app-root", currentScreen.themeClass);
        root.setLeft(createSideNav());
        root.setCenter(createCenter());

        Scene scene = new Scene(root, 1040, 720);
        URL stylesheet = MainFrame.class.getResource("/styles/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Chuck & Facts");
        URL icon = MainFrame.class.getResource("/icons/app-icon.png");
        if (icon != null) {
            stage.getIcons().add(new Image(icon.toExternalForm()));
        }
        stage.setMaxWidth(1040);
        stage.setMaxHeight(720);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
        showScreen(ApiScreen.CHUCK);
    }

    private void configureControls() {
        categoryCombo.setPromptText("Categoria");
        categoryCombo.getStyleClass().add("combo-modern");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);

        generateButton.getStyleClass().addAll("button-primary", "action-button");
        copyButton.getStyleClass().addAll("button-ghost", "action-button");
        toggleOriginalButton.getStyleClass().addAll("button-ghost", "translation-toggle");
        statusLabel.getStyleClass().add("status-pill");

        generateButton.setOnAction(event -> generateContent());
        copyButton.setOnAction(event -> copyText(displayedText()));
        toggleOriginalButton.setOnAction(event -> toggleOriginal());
        generateButton.setTooltip(new Tooltip("Genera un nuovo contenuto"));
        copyButton.setTooltip(new Tooltip("Copia il testo visibile"));
        toggleOriginalButton.setTooltip(new Tooltip("Alterna tra traduzione e originale"));
        setBusy(false, "Pronto");
    }

    private VBox createSideNav() {
        Label appName = new Label("Chuck & Facts");
        appName.getStyleClass().add("app-title");

        VBox navButtons = new VBox(8, chuckButton, dadButton, factButton, historyNavButton);
        navButtons.setFillWidth(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sideNav = new VBox(18, appName, navButtons, spacer);
        sideNav.getStyleClass().add("side-nav");
        return sideNav;
    }

    private StackPane createCenter() {
        // StackPane permette di alternare contenuto principale e cronologia senza ricreare la scena.
        contentView.getStyleClass().add("screen-scroll-content");
        contentView.getChildren().addAll(createControlsPanel(), createResultPanel());

        ScrollPane contentScroll = new ScrollPane(contentView);
        contentScroll.setFitToWidth(true);
        contentScroll.getStyleClass().add("feed-scroll");

        historyView.getStyleClass().add("history-screen");
        historyView.getChildren().addAll(createHistoryHeader(), historyTable);
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        StackPane centerStack = new StackPane(contentScroll, historyView);
        centerStack.getStyleClass().add("center-stack");
        return centerStack;
    }

    private VBox createControlsPanel() {
        screenTitleLabel.getStyleClass().add("screen-title");
        screenSubtitleLabel.getStyleClass().add("screen-subtitle");
        screenSubtitleLabel.setWrapText(true);

        Label categoryLabel = new Label("Categoria");
        categoryLabel.getStyleClass().add("field-label");
        categoryPicker.getChildren().addAll(categoryLabel, categoryCombo);
        categoryPicker.getStyleClass().add("category-picker");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionRow = new HBox(12, categoryPicker, spacer, generateButton, copyButton, statusLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox controls = new VBox(16, screenTitleLabel, screenSubtitleLabel, actionRow);
        controls.getStyleClass().add("panel");
        return controls;
    }

    private VBox createResultPanel() {
        resultTitleLabel.getStyleClass().add("section-title");

        VBox textArea = new VBox(8, resultTextLabel, toggleOriginalButton);
        textArea.setAlignment(Pos.BOTTOM_LEFT);

        VBox result = new VBox(10, resultTitleLabel, textArea);
        result.getStyleClass().add("result-panel");
        return result;
    }

    private Label createBodyLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("body-copy");
        return label;
    }

    private VBox createHistoryHeader() {
        Label title = new Label("Cronologia");
        title.getStyleClass().add("screen-title");

        VBox header = new VBox(6, title);
        header.getStyleClass().add("panel");
        return header;
    }

    private void configureHistoryTable() {
        // La cronologia resta compatta: la colonna grande mostra solo un'anteprima.
        TableColumn<HistoryEntry, String> dateColumn = new TableColumn<>("Data/Ora");
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dateTime()));
        dateColumn.setPrefWidth(125);

        TableColumn<HistoryEntry, String> sourceColumn = new TableColumn<>("Fonte");
        sourceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().source()));
        sourceColumn.setPrefWidth(135);

        TableColumn<HistoryEntry, String> originalColumn = new TableColumn<>("Anteprima");
        originalColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().originalText()));
        originalColumn.setPrefWidth(520);
        originalColumn.setCellFactory(column -> new CompactHistoryCell());

        historyTable.setItems(history);
        historyTable.getColumns().addAll(Arrays.asList(dateColumn, sourceColumn, originalColumn));
        historyTable.getStyleClass().add("history-table");
        historyTable.setFixedCellSize(34);
        historyTable.setRowFactory(table -> {
            TableRow<HistoryEntry> row = new TableRow<>();
            row.setTooltip(new Tooltip("Doppio clic per leggere il testo completo"));
            // Il popup evita di allargare la tabella quando il contenuto e' lungo.
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    showHistoryEntry(row.getItem());
                }
            });
            return row;
        });
    }

    private ToggleButton createNavButton(String text, ApiScreen screen) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(navigationGroup);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        button.setOnAction(event -> showScreen(screen));
        return button;
    }

    private void loadChuckCategories() {
        runAsync(chuckNorrisService::getCategories, categories -> {
            ObservableList<String> items = FXCollections.observableArrayList();
            items.add(RANDOM_CATEGORY);
            items.addAll(categories);
            categoryCombo.setItems(items);
            categoryCombo.getSelectionModel().selectFirst();
        }, "Pronto");
    }

    private void showScreen(ApiScreen screen) {
        // Ogni cambio sezione riparte da uno stato pulito per evitare contenuti vecchi.
        currentScreen = screen;
        currentResult = null;
        currentTranslation = "";
        showingOriginal = false;

        if (root != null) {
            root.getStyleClass().removeAll(ApiScreen.themeClasses());
            root.getStyleClass().add(screen.themeClass);
        }

        ToggleButton selectedButton = switch (screen) {
            case CHUCK -> chuckButton;
            case DAD_JOKE -> dadButton;
            case USELESS_FACT -> factButton;
            case HISTORY -> historyNavButton;
        };
        selectedButton.setSelected(true);

        boolean historyScreen = screen == ApiScreen.HISTORY;
        // Le due viste sono nello stesso StackPane: managed=false libera spazio nel layout.
        contentView.setVisible(!historyScreen);
        contentView.setManaged(!historyScreen);
        historyView.setVisible(historyScreen);
        historyView.setManaged(historyScreen);
        if (historyScreen) {
            return;
        }

        screenTitleLabel.setText(screen.title);
        screenSubtitleLabel.setText(screen.subtitle);
        generateButton.setText(screen.generateText);
        resultTextLabel.setText(screen.placeholderText);
        resultTitleLabel.setText("Traduzione");
        toggleOriginalButton.setText("Visualizza originale");
        setToggleOriginalButtonVisible(false);

        boolean chuckScreen = screen == ApiScreen.CHUCK;
        categoryPicker.setVisible(chuckScreen);
        categoryPicker.setManaged(chuckScreen);
        categoryCombo.setDisable(!chuckScreen || categoryCombo.getItems().isEmpty());
        setBusy(false, "Pronto");
    }

    private void generateContent() {
        setBusy(true, "Caricamento...");

    
        CompletableFuture.supplyAsync(selectedSupplier())
                .thenCompose(result -> CompletableFuture.supplyAsync(() -> translateResult(result)))
                .thenAccept(content -> Platform.runLater(() -> renderGeneratedContent(content)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setBusy(false, "Errore");
                        showError(userMessage(ex));
                    });
                    return null;
                });
    }

    private Supplier<JokeResult> selectedSupplier() {
        // Isola la scelta del provider: generateContent non deve conoscere i dettagli delle API.
        return switch (currentScreen) {
            case DAD_JOKE -> dadJokeService::getRandomJoke;
            case USELESS_FACT -> uselessFactService::getRandomFact;
            case CHUCK -> this::getChuckResult;
            default -> chuckNorrisService::getRandomJoke;
        };
    }

    private JokeResult getChuckResult() {
        String category = categoryCombo.getValue();
        if (category == null || category.isBlank() || RANDOM_CATEGORY.equals(category)) {
            return chuckNorrisService.getRandomJoke();
        }
        return chuckNorrisService.getRandomJokeByCategory(category);
    }

    private GeneratedContent translateResult(JokeResult result) {
        if (currentScreen == ApiScreen.DAD_JOKE) {
            return new GeneratedContent(result, result.originalText(), true);
        }
        try {
            return new GeneratedContent(result, deepSeekService.translateToItalian(result.originalText()), false);
        } catch (RuntimeException ex) {
            return new GeneratedContent(result, result.originalText(), true);
        }
    }

    private void renderGeneratedContent(GeneratedContent content) {
        currentResult = content.result();
        currentTranslation = content.translation();
        boolean originalOnly = !currentResult.supportsTranslation() || content.translationFallback();
        showingOriginal = originalOnly;

        resultTitleLabel.setText(showingOriginal ? "Originale" : "Traduzione");
        resultTextLabel.setText(showingOriginal ? currentResult.originalText() : currentTranslation);
        toggleOriginalButton.setText("Visualizza originale");
        toggleOriginalButton.setDisable(originalOnly);
        setToggleOriginalButtonVisible(!originalOnly);

        addHistory();
        setBusy(false, currentResult.supportsTranslation() && content.translationFallback() ? "Traduzione non disponibile" : "Generato");
    }

    private void toggleOriginal() {
        if (currentResult == null || currentTranslation.isBlank()) {
            return;
        }

        showingOriginal = !showingOriginal;
        resultTitleLabel.setText(showingOriginal ? "Originale" : "Traduzione");
        resultTextLabel.setText(showingOriginal ? currentResult.originalText() : currentTranslation);
        toggleOriginalButton.setText(showingOriginal ? "Visualizza traduzione" : "Visualizza originale");
    }

    private void setToggleOriginalButtonVisible(boolean visible) {
        toggleOriginalButton.setVisible(visible);
        toggleOriginalButton.setManaged(visible);
    }

    private String displayedText() {
        if (currentResult == null) {
            return "";
        }
        return showingOriginal ? currentResult.originalText() : currentTranslation;
    }

    private void addHistory() {
        if (currentResult == null) {
            return;
        }
        // Nuovi elementi in cima, cosi' la cronologia mostra subito l'ultima richiesta.
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        history.add(0, new HistoryEntry(now, currentResult.source(), displayedText(), currentResult.originalText()));
    }

    private <T> void runAsync(Supplier<T> supplier, java.util.function.Consumer<T> onSuccess, String successMessage) {
        CompletableFuture.supplyAsync(supplier)
                .thenAccept(result -> Platform.runLater(() -> {
                    onSuccess.accept(result);
                    setBusy(false, successMessage);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setBusy(false, "Errore");
                        showError(userMessage(ex));
                    });
                    return null;
                });
    }

    private void copyText(String value) {
        if (value == null || value.isBlank()) {
            statusLabel.setText("Niente da copiare");
            return;
        }
        // Usa la clipboard di sistema, quindi il testo e' disponibile anche fuori dall'app.
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Copiato");
    }

    private void setBusy(boolean busy, String status) {
        // Stato unico per evitare click concorrenti durante chiamate HTTP/traduzione.
        generateButton.setDisable(busy || currentScreen == ApiScreen.HISTORY);
        copyButton.setDisable(busy || currentResult == null);
        toggleOriginalButton.setDisable(busy || currentResult == null || currentTranslation.isBlank() || currentTranslation.equals(currentResult.originalText()));
        statusLabel.setText(status);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showHistoryEntry(HistoryEntry entry) {
        // TextArea non editabile: selezionabile, copiabile e piu' comoda di un Label lungo.
        TextArea textArea = new TextArea(entry.originalText());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(620, 260);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Testo completo");
        alert.setHeaderText(entry.source() + " - " + entry.dateTime());
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private static class CompactHistoryCell extends javafx.scene.control.TableCell<HistoryEntry, String> {
        private static final int PREVIEW_LIMIT = 115;

        @Override
        protected void updateItem(String text, boolean empty) {
            super.updateItem(text, empty);
            if (empty || text == null) {
                setText(null);
                return;
            }
            // L'anteprima mantiene l'altezza riga stabile; il testo completo e' nel popup.
            setText(text.length() <= PREVIEW_LIMIT ? text : text.substring(0, PREVIEW_LIMIT - 3) + "...");
        }
    }

    private String userMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "Errore: impossibile completare l'operazione.";
        }
        return message.startsWith("Errore:") ? message : "Errore: " + message;
    }

    private enum ApiScreen {
        CHUCK(
                "chuck-theme",
                "Chuck Norris",
                "Genera una battuta casuale oppure scegli una categoria.",
                "Genera Chuck",
                "Scegli una categoria o lascia Casuale, poi genera una battuta."
        ),
        DAD_JOKE(
                "dad-theme",
                "Dad Joke",
                "Freddure divertenti, o forse no.",
                "Genera Dad Joke",
                "Premi Genera Dad Joke per ottenere una battuta."
        ),
        USELESS_FACT(
                "fact-theme",
                "Useless Fact",
                "Fatti curiosi e inutili.",
                "Genera Fact",
                "Premi Genera Fact per ottenere un fatto curioso."
        ),
        HISTORY(
                "history-theme",
                "Cronologia",
                "",
                "",
                ""
        );

        private final String themeClass;
        private final String title;
        private final String subtitle;
        private final String generateText;
        private final String placeholderText;

        ApiScreen(String themeClass, String title, String subtitle, String generateText, String placeholderText) {
            this.themeClass = themeClass;
            this.title = title;
            this.subtitle = subtitle;
            this.generateText = generateText;
            this.placeholderText = placeholderText;
        }

        private static String[] themeClasses() {
            return Arrays.stream(values())
                    .map(screen -> screen.themeClass)
                    .toArray(String[]::new);
        }
    }

    private record GeneratedContent(JokeResult result, String translation, boolean translationFallback) {
    }
}
