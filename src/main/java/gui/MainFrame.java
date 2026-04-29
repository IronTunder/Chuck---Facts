package gui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.control.TextField;
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
import javafx.util.Duration;
import model.HistoryEntry;
import model.JokeResult;
import service.ChuckNorrisService;
import service.DadJokeService;
import service.DeepSeekService;
import service.UselessFactService;
import util.Config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MainFrame {
    private static final String RANDOM_CATEGORY = "Casuale";
    private static final Path FAVORITES_FILE = Path.of(
            System.getProperty("user.home"),
            ".chuck-and-facts",
            "favorites.json"
    );
    private static final Path GAME_HIGH_SCORE_FILE = Path.of(
            System.getProperty("user.home"),
            ".chuck-and-facts",
            "fact-game-high-score.txt"
    );
    private static final double NEXT_QUESTION_DELAY_SECONDS = 1.5;
    private static final double FALSE_FACT_REVEAL_DELAY_SECONDS = 4.0;
    private static final Type HISTORY_ENTRY_LIST_TYPE = new TypeToken<List<HistoryEntry>>() {
    }.getType();

    private final Gson gson = new Gson();
    private final Random random = new Random();
    private final Config config = Config.load();
    private final ChuckNorrisService chuckNorrisService = new ChuckNorrisService();
    private final DadJokeService dadJokeService = new DadJokeService();
    private final UselessFactService uselessFactService = new UselessFactService();
    private final DeepSeekService deepSeekService = new DeepSeekService(config);
    private final ObservableList<HistoryEntry> history = FXCollections.observableArrayList();
    private final ObservableList<HistoryEntry> favorites = FXCollections.observableArrayList();
    private final FilteredList<HistoryEntry> filteredHistory = new FilteredList<>(history, entry -> true);

    private final ToggleGroup navigationGroup = new ToggleGroup();
    private final ToggleButton chuckButton = createNavButton("Chuck Norris", ApiScreen.CHUCK);
    private final ToggleButton dadButton = createNavButton("Dad Joke", ApiScreen.DAD_JOKE);
    private final ToggleButton factButton = createNavButton("Facts", ApiScreen.USELESS_FACT);
    private final ToggleButton gameNavButton = createNavButton("Vero/Falso", ApiScreen.FACT_GAME);
    private final ToggleButton historyNavButton = createNavButton("Cronologia", ApiScreen.HISTORY);
    private final ToggleButton favoritesNavButton = createNavButton("Preferiti", ApiScreen.FAVORITES);

    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final Button generateButton = new Button("Genera");
    private final Button copyButton = new Button("Copia");
    private final Button favoriteButton = new Button("🔖 Salva");
    private final Button trueButton = new Button("Vero");
    private final Button falseButton = new Button("Falso");
    private final Button toggleOriginalButton = new Button("Visualizza originale");

    private final Label screenIconLabel = new Label();
    private final Label screenTitleLabel = new Label();
    private final Label screenSubtitleLabel = new Label();
    private final Label resultTitleLabel = new Label("Traduzione");
    private final Label resultTextLabel = createBodyLabel("");
    private final Label gameScoreLabel = new Label("Punteggio: 0/0");
    private final Label gameFeedbackLabel = createBodyLabel("");
    private final Label statusLabel = new Label("Pronto");
    private final VBox categoryPicker = new VBox(6);
    private final VBox contentView = new VBox(16);
    private final VBox historyView = new VBox(12);
    private final VBox favoritesView = new VBox(12);
    private final TableView<HistoryEntry> historyTable = new TableView<>();
    private final TableView<HistoryEntry> favoritesTable = new TableView<>();
    private final TextField historySearchField = new TextField();

    private BorderPane root;
    private ApiScreen currentScreen = ApiScreen.CHUCK;
    private JokeResult currentResult;
    private String currentTranslation = "";
    private boolean showingOriginal;
    private FactGameQuestion currentFactGameQuestion;
    private int gameScore;
    private int gameTotal;
    private int gameHighScore;
    private int gameRunId;
    private boolean gameRunActive;

    public void show(Stage stage) {
        configureControls();
        configureHistoryTable();
        configureFavoritesTable();
        loadFavorites();
        loadGameHighScore();
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
        favoriteButton.getStyleClass().addAll("button-ghost", "favorite-button");
        trueButton.getStyleClass().addAll("button-primary", "answer-button");
        falseButton.getStyleClass().addAll("button-ghost", "answer-button");
        toggleOriginalButton.getStyleClass().addAll("button-ghost", "translation-toggle");
        gameScoreLabel.getStyleClass().add("status-pill");
        gameFeedbackLabel.getStyleClass().add("game-feedback");
        statusLabel.getStyleClass().add("status-pill");

        generateButton.setOnAction(event -> generateContent());
        copyButton.setOnAction(event -> copyText(displayedText()));
        favoriteButton.setOnAction(event -> saveFavorite());
        trueButton.setOnAction(event -> answerFactQuestion(true));
        falseButton.setOnAction(event -> answerFactQuestion(false));
        toggleOriginalButton.setOnAction(event -> toggleOriginal());
        generateButton.setTooltip(new Tooltip("Genera un nuovo contenuto"));
        copyButton.setTooltip(new Tooltip("Copia il testo visibile"));
        favoriteButton.setTooltip(new Tooltip("Salva nei preferiti"));
        trueButton.setTooltip(new Tooltip("Rispondi vero"));
        falseButton.setTooltip(new Tooltip("Rispondi falso"));
        toggleOriginalButton.setTooltip(new Tooltip("Alterna tra traduzione e originale"));

        historySearchField.setPromptText("Cerca nella cronologia");
        historySearchField.getStyleClass().add("search-field");
        historySearchField.textProperty().addListener((observable, oldValue, newValue) -> applyHistoryFilter(newValue));

        setBusy(false, "Pronto");
    }

    private VBox createSideNav() {
        Label appName = new Label("Chuck & Facts");
        appName.getStyleClass().add("app-title");

        VBox navButtons = new VBox(8, chuckButton, dadButton, factButton, gameNavButton, historyNavButton, favoritesNavButton);
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

        favoritesView.getStyleClass().add("history-screen");
        favoritesView.getChildren().addAll(createFavoritesHeader(), favoritesTable);
        VBox.setVgrow(favoritesTable, Priority.ALWAYS);

        StackPane centerStack = new StackPane(contentScroll, historyView, favoritesView);
        centerStack.getStyleClass().add("center-stack");
        return centerStack;
    }

    private VBox createControlsPanel() {
        screenIconLabel.getStyleClass().add("screen-icon");
        screenTitleLabel.getStyleClass().add("screen-title");
        screenSubtitleLabel.getStyleClass().add("screen-subtitle");
        screenSubtitleLabel.setWrapText(true);

        HBox screenHeader = new HBox(12, screenIconLabel, screenTitleLabel);
        screenHeader.setAlignment(Pos.CENTER_LEFT);

        Label categoryLabel = new Label("Categoria");
        categoryLabel.getStyleClass().add("field-label");
        categoryPicker.getChildren().addAll(categoryLabel, categoryCombo);
        categoryPicker.getStyleClass().add("category-picker");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionRow = new HBox(12, categoryPicker, spacer, generateButton, copyButton, statusLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox controls = new VBox(16, screenHeader, screenSubtitleLabel, actionRow);
        controls.getStyleClass().add("panel");
        return controls;
    }

    private VBox createResultPanel() {
        resultTitleLabel.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox resultHeader = new HBox(10, resultTitleLabel, spacer, favoriteButton);
        resultHeader.setAlignment(Pos.CENTER_LEFT);

        HBox gameActions = new HBox(10, trueButton, falseButton, gameScoreLabel);
        gameActions.setAlignment(Pos.CENTER_LEFT);

        VBox textArea = new VBox(8, resultTextLabel, toggleOriginalButton);
        textArea.setAlignment(Pos.BOTTOM_LEFT);

        VBox result = new VBox(10, resultHeader, textArea, gameActions, gameFeedbackLabel);
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
        Label title = new Label(ApiScreen.HISTORY.icon + " Cronologia");
        title.getStyleClass().add("screen-title");

        VBox header = new VBox(14, title, historySearchField);
        header.getStyleClass().add("panel");
        return header;
    }

    private void configureHistoryTable() {
        configureEntryTable(historyTable);
        historyTable.setItems(filteredHistory);
    }

    private VBox createFavoritesHeader() {
        Label title = new Label(ApiScreen.FAVORITES.icon + " Preferiti");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("Le battute e i facts che vuoi tenere da parte.");
        subtitle.getStyleClass().add("screen-subtitle");

        VBox header = new VBox(6, title, subtitle);
        header.getStyleClass().add("panel");
        return header;
    }

    private void configureFavoritesTable() {
        configureEntryTable(favoritesTable);
        favoritesTable.setItems(favorites);
    }

    private void configureEntryTable(TableView<HistoryEntry> table) {
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

        table.getColumns().addAll(Arrays.asList(dateColumn, sourceColumn, originalColumn));
        table.getStyleClass().add("history-table");
        table.setFixedCellSize(34);
        table.setRowFactory(entryTable -> {
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
        button.setText(screen.icon + "  " + text);
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
        if (currentScreen == ApiScreen.FACT_GAME && screen != ApiScreen.FACT_GAME) {
            gameRunActive = false;
            gameRunId++;
        }
        currentScreen = screen;
        currentResult = null;
        currentTranslation = "";
        showingOriginal = false;
        currentFactGameQuestion = null;

        if (root != null) {
            root.getStyleClass().removeAll(ApiScreen.themeClasses());
            root.getStyleClass().add(screen.themeClass);
        }

        ToggleButton selectedButton = switch (screen) {
            case CHUCK -> chuckButton;
            case DAD_JOKE -> dadButton;
            case USELESS_FACT -> factButton;
            case FACT_GAME -> gameNavButton;
            case HISTORY -> historyNavButton;
            case FAVORITES -> favoritesNavButton;
        };
        selectedButton.setSelected(true);

        boolean historyScreen = screen == ApiScreen.HISTORY;
        boolean favoritesScreen = screen == ApiScreen.FAVORITES;
        // Le due viste sono nello stesso StackPane: managed=false libera spazio nel layout.
        contentView.setVisible(!historyScreen && !favoritesScreen);
        contentView.setManaged(!historyScreen && !favoritesScreen);
        historyView.setVisible(historyScreen);
        historyView.setManaged(historyScreen);
        favoritesView.setVisible(favoritesScreen);
        favoritesView.setManaged(favoritesScreen);
        if (historyScreen || favoritesScreen) {
            return;
        }

        screenIconLabel.setText(screen.icon);
        screenTitleLabel.setText(screen.title);
        screenSubtitleLabel.setText(screen.subtitle);
        generateButton.setText(screen.generateText);
        resultTextLabel.setText(screen.placeholderText);
        resultTitleLabel.setText(screen == ApiScreen.FACT_GAME ? "Domanda" : "Traduzione");
        toggleOriginalButton.setText("Visualizza originale");
        setToggleOriginalButtonVisible(false);

        boolean chuckScreen = screen == ApiScreen.CHUCK;
        boolean gameScreen = screen == ApiScreen.FACT_GAME;
        categoryPicker.setVisible(chuckScreen);
        categoryPicker.setManaged(chuckScreen);
        categoryCombo.setDisable(!chuckScreen || categoryCombo.getItems().isEmpty());
        copyButton.setVisible(!gameScreen);
        copyButton.setManaged(!gameScreen);
        generateButton.setText(gameScreen ? "Nuova partita" : screen.generateText);
        favoriteButton.setVisible(!gameScreen);
        favoriteButton.setManaged(!gameScreen);
        trueButton.setVisible(gameScreen);
        trueButton.setManaged(gameScreen);
        falseButton.setVisible(gameScreen);
        falseButton.setManaged(gameScreen);
        gameScoreLabel.setVisible(gameScreen);
        gameScoreLabel.setManaged(gameScreen);
        gameFeedbackLabel.setVisible(gameScreen);
        gameFeedbackLabel.setManaged(gameScreen);
        gameFeedbackLabel.setText("");
        updateGameScore();
        setBusy(false, "Pronto");
    }

    private void generateContent() {
        if (currentScreen == ApiScreen.FACT_GAME) {
            startFactGame();
            return;
        }

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
            case FACT_GAME -> uselessFactService::getRandomFact;
            case CHUCK -> this::getChuckResult;
            default -> chuckNorrisService::getRandomJoke;
        };
    }

    private void startFactGame() {
        gameRunId++;
        gameRunActive = true;
        gameScore = 0;
        gameTotal = 0;
        currentFactGameQuestion = null;
        resultTitleLabel.setText("Partita avviata");
        resultTextLabel.setText("Sto preparando la prima domanda...");
        gameFeedbackLabel.setText("");
        updateGameScore();
        generateFactQuestion(gameRunId);
    }

    private void generateFactQuestion(int runId) {
        if (!gameRunActive || currentScreen != ApiScreen.FACT_GAME || runId != gameRunId) {
            return;
        }

        setBusy(true, "Preparazione...");
        gameFeedbackLabel.setText("");
        currentFactGameQuestion = null;

        CompletableFuture.supplyAsync(this::buildFactGameQuestion)
                .thenAccept(question -> Platform.runLater(() -> {
                    if (gameRunActive && currentScreen == ApiScreen.FACT_GAME && runId == gameRunId) {
                        renderFactGameQuestion(question);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (gameRunActive && currentScreen == ApiScreen.FACT_GAME && runId == gameRunId) {
                            setBusy(false, "Errore");
                            showError(userMessage(ex));
                        }
                    });
                    return null;
                });
    }

    private FactGameQuestion buildFactGameQuestion() {
        JokeResult fact = uselessFactService.getRandomFact();
        String trueFact = deepSeekService.translateToItalian(fact.originalText());
        boolean trueQuestion = random.nextBoolean();
        String displayedFact = trueQuestion ? trueFact : deepSeekService.makePlausibleFalseFact(fact.originalText());
        return new FactGameQuestion(displayedFact, trueFact, trueQuestion);
    }

    private void renderFactGameQuestion(FactGameQuestion question) {
        currentFactGameQuestion = question;
        resultTitleLabel.setText("Vero o falso?");
        resultTextLabel.setText(question.displayedFact());
        trueButton.setDisable(false);
        falseButton.setDisable(false);
        setBusy(false, "Rispondi");
    }

    private void answerFactQuestion(boolean answer) {
        if (!gameRunActive || currentFactGameQuestion == null) {
            gameFeedbackLabel.setText("Genera una domanda prima di rispondere.");
            return;
        }

        boolean falseFactQuestion = !currentFactGameQuestion.trueFact();
        boolean correct = answer == currentFactGameQuestion.trueFact();
        gameTotal++;
        if (correct) {
            gameScore++;
        }

        resultTitleLabel.setText(correct ? "Risposta corretta" : "Risposta sbagliata");
        if (currentFactGameQuestion.trueFact()) {
            gameFeedbackLabel.setText(correct ? "Esatto: questo fatto era vero." : "Era vero.");
        } else {
            gameFeedbackLabel.setText((correct ? "Esatto: era falso. " : "Era falso. ")
                    + "Il fatto vero era: " + currentFactGameQuestion.trueFactText());
        }
        trueButton.setDisable(true);
        falseButton.setDisable(true);
        updateGameScore();
        statusLabel.setText(correct ? "Corretto" : "Sbagliato");
        currentFactGameQuestion = null;

        if (correct) {
            double delaySeconds = falseFactQuestion ? FALSE_FACT_REVEAL_DELAY_SECONDS : NEXT_QUESTION_DELAY_SECONDS;
            scheduleNextFactQuestion(gameRunId, delaySeconds);
        } else {
            finishFactGame();
        }
    }

    private void updateGameScore() {
        gameScoreLabel.setText("Punteggio: " + gameScore + " | Record: " + gameHighScore);
    }

    private void scheduleNextFactQuestion(int runId, double delaySeconds) {
        PauseTransition delay = new PauseTransition(Duration.seconds(delaySeconds));
        delay.setOnFinished(event -> generateFactQuestion(runId));
        delay.play();
    }

    private void finishFactGame() {
        gameRunActive = false;
        if (gameScore > gameHighScore) {
            gameHighScore = gameScore;
            persistGameHighScore();
            gameFeedbackLabel.setText(gameFeedbackLabel.getText() + " Nuovo record: " + gameHighScore + ".");
        } else {
            gameFeedbackLabel.setText(gameFeedbackLabel.getText() + " Partita finita.");
        }
        updateGameScore();
        setBusy(false, "Fine partita");
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

    private void saveFavorite() {
        if (currentResult == null) {
            statusLabel.setText("Niente da salvare");
            return;
        }

        String visibleText = displayedText();
        boolean alreadySaved = favorites.stream()
                .anyMatch(entry -> entry.source().equals(currentResult.source()) && entry.originalText().equals(visibleText));
        if (alreadySaved) {
            statusLabel.setText("Gia' nei preferiti");
            return;
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        favorites.add(0, new HistoryEntry(now, currentResult.source(), visibleText, currentResult.originalText()));
        persistFavorites();
        statusLabel.setText("Salvato");
    }

    private void loadFavorites() {
        if (!Files.exists(FAVORITES_FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(FAVORITES_FILE, StandardCharsets.UTF_8)) {
            List<HistoryEntry> savedFavorites = gson.fromJson(reader, HISTORY_ENTRY_LIST_TYPE);
            if (savedFavorites != null) {
                favorites.setAll(savedFavorites);
            }
        } catch (IOException | RuntimeException ex) {
            statusLabel.setText("Preferiti non caricati");
        }
    }

    private void persistFavorites() {
        try {
            Files.createDirectories(FAVORITES_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FAVORITES_FILE, StandardCharsets.UTF_8)) {
                gson.toJson(new ArrayList<>(favorites), HISTORY_ENTRY_LIST_TYPE, writer);
            }
        } catch (IOException ex) {
            statusLabel.setText("Preferiti non salvati");
            showError("Errore: impossibile salvare i preferiti.");
        }
    }

    private void loadGameHighScore() {
        if (!Files.exists(GAME_HIGH_SCORE_FILE)) {
            return;
        }

        try {
            String savedScore = Files.readString(GAME_HIGH_SCORE_FILE, StandardCharsets.UTF_8).trim();
            gameHighScore = savedScore.isBlank() ? 0 : Integer.parseInt(savedScore);
        } catch (IOException | NumberFormatException ex) {
            gameHighScore = 0;
        }
    }

    private void persistGameHighScore() {
        try {
            Files.createDirectories(GAME_HIGH_SCORE_FILE.getParent());
            Files.writeString(GAME_HIGH_SCORE_FILE, Integer.toString(gameHighScore), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            statusLabel.setText("Record non salvato");
        }
    }

    private void applyHistoryFilter(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        filteredHistory.setPredicate(entry -> {
            if (normalizedQuery.isBlank()) {
                return true;
            }
            return containsIgnoreCase(entry.dateTime(), normalizedQuery)
                    || containsIgnoreCase(entry.source(), normalizedQuery)
                    || containsIgnoreCase(entry.originalText(), normalizedQuery)
                    || containsIgnoreCase(entry.translation(), normalizedQuery);
        });
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase().contains(normalizedQuery);
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
        generateButton.setDisable(busy || currentScreen == ApiScreen.HISTORY || currentScreen == ApiScreen.FAVORITES);
        copyButton.setDisable(busy || currentResult == null);
        favoriteButton.setDisable(busy || currentResult == null);
        toggleOriginalButton.setDisable(busy || currentResult == null || currentTranslation.isBlank() || currentTranslation.equals(currentResult.originalText()));
        trueButton.setDisable(busy || currentScreen != ApiScreen.FACT_GAME || currentFactGameQuestion == null);
        falseButton.setDisable(busy || currentScreen != ApiScreen.FACT_GAME || currentFactGameQuestion == null);
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
                "🥋",
                "Chuck Norris",
                "Genera una battuta casuale oppure scegli una categoria.",
                "Genera Chuck",
                "Scegli una categoria o lascia Casuale, poi genera una battuta."
        ),
        DAD_JOKE(
                "dad-theme",
                "👔",
                "Dad Joke",
                "Freddure divertenti, o forse no.",
                "Genera Dad Joke",
                "Premi Genera Dad Joke per ottenere una battuta."
        ),
        USELESS_FACT(
                "fact-theme",
                "💡",
                "Useless Fact",
                "Fatti curiosi e inutili.",
                "Genera Fact",
                "Premi Genera Fact per ottenere un fatto curioso."
        ),
        FACT_GAME(
                "game-theme",
                "🎯",
                "Vero o falso?",
                "Rispondi finche' non sbagli: dopo ogni risposta corretta arriva automaticamente una nuova domanda.",
                "Nuova partita",
                "Premi Nuova partita per iniziare, poi scegli Vero o Falso."
        ),
        HISTORY(
                "history-theme",
                "🕘",
                "Cronologia",
                "",
                "",
                ""
        ),
        FAVORITES(
                "favorites-theme",
                "★",
                "Preferiti",
                "",
                "",
                ""
        );

        private final String themeClass;
        private final String icon;
        private final String title;
        private final String subtitle;
        private final String generateText;
        private final String placeholderText;

        ApiScreen(String themeClass, String icon, String title, String subtitle, String generateText, String placeholderText) {
            this.themeClass = themeClass;
            this.icon = icon;
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

    private record FactGameQuestion(String displayedFact, String trueFactText, boolean trueFact) {
    }
}
