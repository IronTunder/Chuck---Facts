package gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Popup;
import model.HistoryEntry;
import model.JokeResult;
import service.ChuckNorrisService;
import service.DadJokeService;
import service.DeepLService;
import service.UselessFactService;
import util.Config;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MainFrame {
    private static final String CHUCK_RANDOM = "Chuck Norris";
    private static final String CHUCK_CATEGORY = "Chuck Norris per categoria";
    private static final String DAD_JOKE = "Dad Joke";
    private static final String USELESS_FACT = "Useless Fact";

    private final Config config = Config.load();
    private final ChuckNorrisService chuckNorrisService = new ChuckNorrisService();
    private final DadJokeService dadJokeService = new DadJokeService();
    private final UselessFactService uselessFactService = new UselessFactService();
    private final DeepLService deepLService = new DeepLService(config);
    private final ObservableList<HistoryEntry> history = FXCollections.observableArrayList();

    private final ComboBox<String> sourceCombo = new ComboBox<>();
    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final Button generateButton = new Button("Genera");
    private final Button retryTranslateButton = new Button("Riprova traduzione");
    private final Button copyOriginalButton = new Button("Copia originale");
    private final Button copyTranslationButton = new Button("Copia traduzione");
    private final Button historyButton = new Button("↺");
    private final Label sourceBadge = new Label("Nuovo post");
    private final Label typeBadge = new Label("Scegli una sorgente");
    private final Label originalTextLabel = createBodyLabel("Genera un contenuto per creare una card pronta da condividere.");
    private final Label translationTextLabel = createBodyLabel("La traduzione apparira qui quando disponibile.");
    private final Label translationStatusLabel = new Label("Traduzione automatica per Chuck Norris e Useless Fact");
    private final Label statusLabel = new Label("Pronto");
    private final VBox translationCard = new VBox(8);
    private final TableView<HistoryEntry> historyTable = new TableView<>();
    private final Popup historyPopup = new Popup();

    private JokeResult currentResult;
    private String currentTranslation = "";

    public void show(Stage stage) {
        configureControls();
        configureHistoryTable();
        loadChuckCategories();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(createHeader());
        root.setCenter(createFeedScroll());

        Scene scene = new Scene(root, 1120, 800);
        URL stylesheet = MainFrame.class.getResource("/styles/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Chuck & Facts");
        stage.setMinWidth(940);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    private void configureControls() {
        sourceCombo.setItems(FXCollections.observableArrayList(CHUCK_RANDOM, CHUCK_CATEGORY, DAD_JOKE, USELESS_FACT));
        sourceCombo.getSelectionModel().select(CHUCK_RANDOM);
        sourceCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateCategoryState());
        sourceCombo.getStyleClass().add("combo-modern");
        sourceCombo.setPrefWidth(215);

        categoryCombo.setPromptText("Categoria");
        categoryCombo.getStyleClass().add("combo-modern");
        categoryCombo.setPrefWidth(180);
        categoryCombo.setDisable(true);

        generateButton.getStyleClass().addAll("button-primary", "action-button");
        retryTranslateButton.getStyleClass().addAll("button-secondary", "action-button");
        copyOriginalButton.getStyleClass().addAll("button-ghost", "action-button");
        copyTranslationButton.getStyleClass().addAll("button-ghost", "action-button");
        historyButton.getStyleClass().addAll("icon-button", "action-button");

        generateButton.setTooltip(new Tooltip("Recupera un contenuto e prepara la card"));
        retryTranslateButton.setTooltip(new Tooltip("Riprova DeepL per contenuti traducibili"));
        historyButton.setTooltip(new Tooltip("Mostra cronologia"));

        generateButton.setOnAction(event -> generateContent());
        retryTranslateButton.setOnAction(event -> retryTranslation());
        copyOriginalButton.setOnAction(event -> copyText(currentResult == null ? "" : currentResult.originalText(), "Originale copiato"));
        copyTranslationButton.setOnAction(event -> copyText(currentTranslation, "Traduzione copiata"));
        historyButton.setOnAction(event -> toggleHistoryPopup());

        retryTranslateButton.setDisable(true);
        copyOriginalButton.setDisable(true);
        copyTranslationButton.setDisable(true);

        sourceBadge.getStyleClass().add("badge-source");
        typeBadge.getStyleClass().add("badge-soft");
        translationStatusLabel.getStyleClass().add("section-note");
        statusLabel.getStyleClass().add("status-pill");
    }

    private HBox createHeader() {
        Label title = new Label("Chuck & Facts");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Crea contenuti brevi, traducibili e pronti per una chat social.");
        subtitle.getStyleClass().add("app-subtitle");

        VBox titleBlock = new VBox(3, title, subtitle);
        titleBlock.setAlignment(Pos.CENTER_LEFT);

        HBox controls = new HBox(10, sourceCombo, categoryCombo, generateButton, historyButton);
        controls.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(18, titleBlock, spacer, controls);
        header.getStyleClass().add("top-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private ScrollPane createFeedScroll() {
        VBox feed = new VBox(16);
        feed.setAlignment(Pos.TOP_CENTER);
        feed.getStyleClass().add("feed-wrap");
        feed.getChildren().add(createPostCard());

        ScrollPane scrollPane = new ScrollPane(feed);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("feed-scroll");
        return scrollPane;
    }

    private VBox createPostCard() {
        Label handle = new Label("@chuckandfacts");
        handle.getStyleClass().add("handle");

        HBox badges = new HBox(8, sourceBadge, typeBadge);
        badges.setAlignment(Pos.CENTER_LEFT);

        VBox identity = new VBox(4, handle, badges);
        identity.setAlignment(Pos.CENTER_LEFT);

        HBox postHeader = new HBox(identity);
        postHeader.setAlignment(Pos.CENTER_LEFT);

        VBox originalCard = createSection("Originale", originalTextLabel);
        translationCard.getStyleClass().addAll("mini-card", "translation-card");
        translationCard.getChildren().addAll(createSectionHeader("Traduzione"), translationStatusLabel, translationTextLabel);

        HBox primaryActions = new HBox(10, retryTranslateButton);
        primaryActions.setAlignment(Pos.CENTER_LEFT);

        HBox copyActions = new HBox(10, copyOriginalButton, copyTranslationButton);
        copyActions.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statusRow = new HBox(12, primaryActions, spacer, copyActions, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox post = new VBox(16, postHeader, originalCard, translationCard, statusRow);
        post.getStyleClass().add("post-card");
        post.setMaxWidth(880);
        return post;
    }

    private VBox createSection(String title, Label body) {
        VBox section = new VBox(8, createSectionHeader(title), body);
        section.getStyleClass().add("mini-card");
        return section;
    }

    private Label createSectionHeader(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label createBodyLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("body-copy");
        return label;
    }

    private VBox createHistoryPanel() {
        Label title = new Label("Cronologia");
        title.getStyleClass().add("history-title");
        historyTable.setPrefHeight(320);
        historyTable.setPrefWidth(780);

        VBox bottom = new VBox(8, title, historyTable);
        bottom.getStyleClass().add("history-panel");
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        return bottom;
    }

    private void configureHistoryTable() {
        TableColumn<HistoryEntry, String> dateColumn = new TableColumn<>("Data/Ora");
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dateTime()));
        dateColumn.setPrefWidth(145);

        TableColumn<HistoryEntry, String> sourceColumn = new TableColumn<>("Fonte");
        sourceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().source()));
        sourceColumn.setPrefWidth(140);

        TableColumn<HistoryEntry, String> originalColumn = new TableColumn<>("Testo originale");
        originalColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().originalText()));
        originalColumn.setPrefWidth(330);

        TableColumn<HistoryEntry, String> translationColumn = new TableColumn<>("Traduzione");
        translationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().translation()));
        translationColumn.setPrefWidth(300);

        historyTable.setItems(history);
        historyTable.getColumns().addAll(Arrays.asList(dateColumn, sourceColumn, originalColumn, translationColumn));
        historyTable.getStyleClass().add("history-table");

        historyPopup.setAutoHide(true);
        historyPopup.getContent().add(createHistoryPanel());
    }

    private void loadChuckCategories() {
        runAsync(chuckNorrisService::getCategories, categories -> {
            categoryCombo.setItems(FXCollections.observableArrayList(categories));
            if (!categories.isEmpty()) {
                categoryCombo.getSelectionModel().selectFirst();
            }
            updateCategoryState();
        }, "Categorie caricate");
    }

    private void updateCategoryState() {
        boolean categoryMode = CHUCK_CATEGORY.equals(sourceCombo.getValue());
        categoryCombo.setDisable(!categoryMode || categoryCombo.getItems().isEmpty());
    }

    private void generateContent() {
        Supplier<JokeResult> supplier = selectedSupplier();
        setBusy(true, "Recupero contenuto...");

        CompletableFuture.supplyAsync(supplier)
                .thenCompose(result -> {
                    if (!result.supportsTranslation()) {
                        return CompletableFuture.completedFuture(new GeneratedContent(result, "Non prevista", false));
                    }
                    return CompletableFuture.supplyAsync(() -> translateOrFallback(result));
                })
                .thenAccept(content -> Platform.runLater(() -> {
                    currentResult = content.result();
                    currentTranslation = content.translation();
                    renderContent(content);
                    addHistory(currentTranslation);
                    setBusy(false, content.translationFallback() ? "Contenuto generato, traduzione non disponibile" : "Contenuto generato");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setBusy(false, "Errore");
                        showError(userMessage(ex));
                    });
                    return null;
                });
    }

    private Supplier<JokeResult> selectedSupplier() {
        return switch (sourceCombo.getValue()) {
            case CHUCK_CATEGORY -> () -> chuckNorrisService.getRandomJokeByCategory(categoryCombo.getValue());
            case DAD_JOKE -> dadJokeService::getRandomJoke;
            case USELESS_FACT -> uselessFactService::getRandomFact;
            default -> chuckNorrisService::getRandomJoke;
        };
    }

    private GeneratedContent translateOrFallback(JokeResult result) {
        try {
            String translation = deepLService.translateToItalian(result.originalText());
            return new GeneratedContent(result, translation, false);
        } catch (RuntimeException ex) {
            return new GeneratedContent(result, result.originalText(), true);
        }
    }

    private void renderContent(GeneratedContent content) {
        JokeResult result = content.result();
        sourceBadge.setText(result.source());
        typeBadge.setText(result.contentType());
        originalTextLabel.setText(result.originalText());

        boolean translatable = result.supportsTranslation();
        translationCard.setManaged(translatable);
        translationCard.setVisible(translatable);
        retryTranslateButton.setVisible(translatable);
        retryTranslateButton.setManaged(translatable);
        retryTranslateButton.setDisable(!translatable);
        copyTranslationButton.setVisible(translatable);
        copyTranslationButton.setManaged(translatable);

        if (translatable) {
            translationTextLabel.setText(content.translation());
            translationStatusLabel.setText(content.translationFallback()
                    ? "Traduzione non disponibile: mostro il testo originale."
                    : "Traduzione automatica DeepL");
        } else {
            translationTextLabel.setText("");
            translationStatusLabel.setText("Traduzione non prevista per Dad Joke");
        }

        copyOriginalButton.setDisable(false);
        copyTranslationButton.setDisable(!translatable || currentTranslation.isBlank());
    }

    private void retryTranslation() {
        if (!hasCurrentResult() || !currentResult.supportsTranslation()) {
            return;
        }
        setBusy(true, "Riprovo DeepL...");
        CompletableFuture.supplyAsync(() -> translateOrFallback(currentResult))
                .thenAccept(content -> Platform.runLater(() -> {
                    currentTranslation = content.translation();
                    translationTextLabel.setText(content.translation());
                    translationStatusLabel.setText(content.translationFallback()
                            ? "Traduzione non disponibile: mostro il testo originale."
                            : "Traduzione automatica DeepL");
                    copyTranslationButton.setDisable(currentTranslation.isBlank());
                    addHistory(currentTranslation);
                    setBusy(false, content.translationFallback() ? "Traduzione non disponibile" : "Traduzione aggiornata");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> setBusy(false, "Traduzione non disponibile"));
                    return null;
                });
    }

    private boolean hasCurrentResult() {
        if (currentResult != null) {
            return true;
        }
        showError("Genera prima un contenuto.");
        return false;
    }

    private void addHistory(String translation) {
        if (currentResult == null) {
            return;
        }
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        history.add(0, new HistoryEntry(now, currentResult.source(), currentResult.originalText(), translation));
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

    private void copyText(String value, String successMessage) {
        if (value == null || value.isBlank()) {
            statusLabel.setText("Niente da copiare");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText(successMessage);
    }

    private void toggleHistoryPopup() {
        if (historyPopup.isShowing()) {
            historyPopup.hide();
            return;
        }
        historyPopup.show(historyButton, -760, 42);
    }

    private void setBusy(boolean busy, String status) {
        generateButton.setDisable(busy);
        retryTranslateButton.setDisable(busy || currentResult == null || !currentResult.supportsTranslation());
        copyOriginalButton.setDisable(busy || currentResult == null);
        copyTranslationButton.setDisable(busy || currentResult == null || !currentResult.supportsTranslation() || currentTranslation.isBlank());
        statusLabel.setText(status);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String userMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "Errore: impossibile completare l'operazione.";
        }
        return message.startsWith("Errore:") ? message : "Errore: " + message;
    }

    private record GeneratedContent(JokeResult result, String translation, boolean translationFallback) {
    }
}
