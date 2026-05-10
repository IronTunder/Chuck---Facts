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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.HistoryEntry;
import model.JokeResult;
import model.WhoaResponse;
import service.ChuckNorrisService;
import service.DadJokeService;
import service.DeepSeekService;
import service.UselessFactService;
import service.WhoaService;
import util.Config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.awt.Desktop;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final Path WHOA_GAME_HIGH_SCORE_FILE = Path.of(
            System.getProperty("user.home"),
            ".chuck-and-facts",
            "whoa-game-high-score.txt"
    );
    private static final double NEXT_QUESTION_DELAY_SECONDS = 1.5;
    private static final double FALSE_FACT_REVEAL_DELAY_SECONDS = 4.0;
    private static final double NEXT_WHOA_DELAY_SECONDS = 2.0;
    private static final Type HISTORY_ENTRY_LIST_TYPE = new TypeToken<List<HistoryEntry>>() {
    }.getType();

    private final Gson gson = new Gson();
    private final Random random = new Random();
    private final Config config = Config.load();
    private final ChuckNorrisService chuckNorrisService = new ChuckNorrisService();
    private final DadJokeService dadJokeService = new DadJokeService();
    private final UselessFactService uselessFactService = new UselessFactService();
    private final WhoaService whoaService = new WhoaService();
    private final DeepSeekService deepSeekService = new DeepSeekService(config);
    private final ObservableList<HistoryEntry> history = FXCollections.observableArrayList();
    private final ObservableList<HistoryEntry> favorites = FXCollections.observableArrayList();
    private final FilteredList<HistoryEntry> filteredHistory = new FilteredList<>(history, entry -> true);

    private final ToggleGroup navigationGroup = new ToggleGroup();
    private final ToggleButton chuckButton = createNavButton("Chuck Norris", ApiScreen.CHUCK);
    private final ToggleButton dadButton = createNavButton("Dad Joke", ApiScreen.DAD_JOKE);
    private final ToggleButton factButton = createNavButton("Facts", ApiScreen.USELESS_FACT);
    private final ToggleButton gameNavButton = createNavButton("Vero/Falso", ApiScreen.FACT_GAME);
    private final ToggleButton whoaGameNavButton = createNavButton("Whoa Game", ApiScreen.WHOA_GAME);
    private final ToggleButton historyNavButton = createNavButton("Cronologia", ApiScreen.HISTORY);
    private final ToggleButton favoritesNavButton = createNavButton("Preferiti", ApiScreen.FAVORITES);

    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final Button generateButton = new Button("Genera");
    private final Button copyButton = new Button("Copia");
    private final Button favoriteButton = new Button("🔖 Salva");
    private final Button trueButton = new Button("Vero");
    private final Button falseButton = new Button("Falso");
    private final Button whoaReplayButton = new Button("Replay");
    private final Button whoaAnswerOneButton = createWhoaAnswerButton();
    private final Button whoaAnswerTwoButton = createWhoaAnswerButton();
    private final Button whoaAnswerThreeButton = createWhoaAnswerButton();
    private final Button whoaAnswerFourButton = createWhoaAnswerButton();
    private final List<Button> whoaAnswerButtons = Arrays.asList(
            whoaAnswerOneButton,
            whoaAnswerTwoButton,
            whoaAnswerThreeButton,
            whoaAnswerFourButton
    );
    private final Button toggleOriginalButton = new Button("Visualizza originale");

    private final Label screenIconLabel = new Label();
    private final Label screenTitleLabel = new Label();
    private final Label screenSubtitleLabel = new Label();
    private final Label resultTitleLabel = new Label("Traduzione");
    private final Label resultTextLabel = createBodyLabel("");
    private final Label gameScoreLabel = new Label("Punteggio: 0/0");
    private final Label whoaScoreLabel = new Label("Score: 0 | Record: 0");
    private final Label gameFeedbackLabel = createBodyLabel("");
    private final Label statusLabel = new Label("Pronto");
    private final VBox categoryPicker = new VBox(6);
    private final MediaView whoaMediaView = new MediaView();
    private final Label whoaVideoPlaceholderLabel = new Label("La clip apparira' qui.");
    private final HBox whoaMediaControls = new HBox(8, whoaReplayButton);
    private final StackPane whoaVideoPane = new StackPane(whoaMediaView, whoaVideoPlaceholderLabel);
    private final GridPane whoaAnswersGrid = new GridPane();
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
    private int contentRequestId;
    private WhoaGameQuestion currentWhoaGameQuestion;
    private List<String> whoaMovies = List.of();
    private int whoaScore;
    private int whoaHighScore;
    private int whoaRunId;
    private boolean whoaRunActive;
    private MediaPlayer whoaMediaPlayer;
    private String currentWhoaExternalVideoUrl;

    public void show(Stage stage) {
        configureControls();
        configureHistoryTable();
        configureFavoritesTable();
        loadFavorites();
        loadGameHighScore();
        loadWhoaHighScore();
        loadChuckCategories();

        root = new BorderPane();
        root.getStyleClass().addAll("app-root", currentScreen.themeClass);
        root.setTop(createTopNav());
        root.setCenter(createCenter());

        Scene scene = new Scene(root, 1120, 720);
        URL stylesheet = MainFrame.class.getResource("/styles/app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Chuck & Facts");
        URL icon = MainFrame.class.getResource("/icons/app-icon.png");
        if (icon != null) {
            stage.getIcons().add(new Image(icon.toExternalForm()));
        }
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setOnHidden(event -> disposeWhoaMediaPlayer());
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
        trueButton.setText("✓\nVero");
        falseButton.setText("✕\nFalso");
        trueButton.getStyleClass().addAll("answer-button", "true-answer-button");
        falseButton.getStyleClass().addAll("answer-button", "false-answer-button");
        whoaReplayButton.getStyleClass().addAll("button-primary", "compact-button");
        whoaAnswerButtons.forEach(button -> button.getStyleClass().addAll("button-ghost", "whoa-answer-button"));
        toggleOriginalButton.getStyleClass().addAll("button-ghost", "translation-toggle");
        gameScoreLabel.getStyleClass().add("status-pill");
        whoaScoreLabel.getStyleClass().add("whoa-score-label");
        gameFeedbackLabel.getStyleClass().add("game-feedback");
        statusLabel.getStyleClass().add("status-pill");
        whoaVideoPane.getStyleClass().add("whoa-video-pane");
        whoaVideoPlaceholderLabel.getStyleClass().add("whoa-video-placeholder");
        whoaMediaControls.getStyleClass().add("whoa-media-controls");
        whoaAnswersGrid.getStyleClass().add("whoa-answers-grid");
        whoaMediaView.setPreserveRatio(true);
        whoaMediaView.setFitWidth(760);
        whoaMediaView.setFitHeight(248);

        generateButton.setOnAction(event -> generateContent());
        copyButton.setOnAction(event -> copyText(displayedText()));
        favoriteButton.setOnAction(event -> saveFavorite());
        trueButton.setOnAction(event -> answerFactQuestion(true));
        falseButton.setOnAction(event -> answerFactQuestion(false));
        whoaReplayButton.setOnAction(event -> replayWhoaClip());
        for (Button button : whoaAnswerButtons) {
            button.setOnAction(event -> answerWhoaQuestion((String) button.getUserData()));
            button.setTooltip(new Tooltip("Scegli questo film"));
        }
        toggleOriginalButton.setOnAction(event -> toggleOriginal());
        generateButton.setTooltip(new Tooltip("Genera un nuovo contenuto"));
        copyButton.setTooltip(new Tooltip("Copia il testo visibile"));
        favoriteButton.setTooltip(new Tooltip("Salva nei preferiti"));
        trueButton.setTooltip(new Tooltip("Rispondi vero"));
        falseButton.setTooltip(new Tooltip("Rispondi falso"));
        whoaReplayButton.setTooltip(new Tooltip("Rivedi la clip dall'inizio"));
        toggleOriginalButton.setTooltip(new Tooltip("Alterna tra traduzione e originale"));

        historySearchField.setPromptText("Cerca nella cronologia");
        historySearchField.getStyleClass().add("search-field");
        historySearchField.textProperty().addListener((observable, oldValue, newValue) -> applyHistoryFilter(newValue));

        setBusy(false, "Pronto");
    }

    private VBox createTopNav() {
        Label appName = new Label("Chuck & Facts");
        appName.getStyleClass().add("app-title");

        Label contentLabel = new Label("Chuck & Facts");
        contentLabel.getStyleClass().add("nav-section-label");
        Label minigamesLabel = new Label("Minigames");
        minigamesLabel.getStyleClass().add("nav-section-label");

        HBox contentButtons = new HBox(6, chuckButton, dadButton, factButton, historyNavButton, favoritesNavButton);
        contentButtons.setAlignment(Pos.CENTER_LEFT);
        HBox gameButtons = new HBox(6, gameNavButton, whoaGameNavButton);
        gameButtons.setAlignment(Pos.CENTER_LEFT);

        VBox contentSection = new VBox(4, contentLabel, contentButtons);
        VBox minigamesSection = new VBox(4, minigamesLabel, gameButtons);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox navRow = new HBox(22, appName, contentSection, minigamesSection, spacer);
        navRow.setAlignment(Pos.CENTER_LEFT);
        navRow.getStyleClass().add("top-nav");
        return new VBox(navRow);
    }

    private StackPane createCenter() {
        
        contentView.getStyleClass().add("screen-scroll-content");
        contentView.setSpacing(10);
        contentView.setMaxWidth(900);
        contentView.getChildren().addAll(createControlsPanel(), createResultPanel());

        StackPane contentFrame = new StackPane(contentView);
        contentFrame.getStyleClass().add("content-frame");
        contentFrame.setAlignment(Pos.TOP_CENTER);

        ScrollPane contentScroll = new ScrollPane(contentFrame);
        contentScroll.setFitToWidth(true);
        contentScroll.getStyleClass().add("feed-scroll");

        historyView.getStyleClass().add("history-screen");
        historyView.setMaxWidth(980);
        historyView.getChildren().addAll(createHistoryHeader(), historyTable);
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        favoritesView.getStyleClass().add("history-screen");
        favoritesView.setMaxWidth(980);
        favoritesView.getChildren().addAll(createFavoritesHeader(), favoritesTable);
        VBox.setVgrow(favoritesTable, Priority.ALWAYS);

        StackPane centerStack = new StackPane(contentScroll, historyView, favoritesView);
        centerStack.getStyleClass().add("center-stack");
        StackPane.setAlignment(historyView, Pos.TOP_CENTER);
        StackPane.setAlignment(favoritesView, Pos.TOP_CENTER);
        return centerStack;
    }

    private HBox createControlsPanel() {
        screenIconLabel.getStyleClass().add("screen-icon");
        screenTitleLabel.getStyleClass().add("screen-title");
        screenSubtitleLabel.getStyleClass().add("screen-subtitle");
        screenSubtitleLabel.setWrapText(true);

        VBox titleGroup = new VBox(3, screenTitleLabel, screenSubtitleLabel);
        HBox screenHeader = new HBox(10, screenIconLabel, titleGroup);
        screenHeader.setAlignment(Pos.CENTER_LEFT);

        Label categoryLabel = new Label("Categoria");
        categoryLabel.getStyleClass().add("field-label");
        categoryPicker.getChildren().addAll(categoryLabel, categoryCombo);
        categoryPicker.getStyleClass().add("category-picker");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionRow = new HBox(8, generateButton, gameScoreLabel, whoaScoreLabel, copyButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox rightActions = new VBox(6, categoryPicker, actionRow);
        rightActions.setAlignment(Pos.CENTER_RIGHT);

        HBox controls = new HBox(14, screenHeader, spacer, rightActions);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("panel");
        return controls;
    }

    private VBox createResultPanel() {
        resultTitleLabel.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox resultHeader = new HBox(10, resultTitleLabel, spacer, favoriteButton);
        resultHeader.setAlignment(Pos.CENTER_LEFT);

        HBox gameActions = new HBox(16, trueButton, falseButton);
        gameActions.setAlignment(Pos.CENTER);

        whoaAnswersGrid.setHgap(8);
        whoaAnswersGrid.setVgap(6);
        whoaAnswersGrid.add(whoaAnswerOneButton, 0, 0);
        whoaAnswersGrid.add(whoaAnswerTwoButton, 1, 0);
        whoaAnswersGrid.add(whoaAnswerThreeButton, 0, 1);
        whoaAnswersGrid.add(whoaAnswerFourButton, 1, 1);
        whoaAnswerButtons.forEach(button -> {
            button.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(button, Priority.ALWAYS);
        });

        VBox textArea = new VBox(5, resultTextLabel, toggleOriginalButton);
        textArea.setAlignment(Pos.BOTTOM_LEFT);

        VBox result = new VBox(7, resultHeader, whoaVideoPane, whoaMediaControls, textArea, gameActions, whoaAnswersGrid, gameFeedbackLabel);
        result.getStyleClass().add("result-panel");
        return result;
    }

    private Label createBodyLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("body-copy");
        return label;
    }

    private Button createWhoaAnswerButton() {
        Button button = new Button();
        button.setWrapText(true);
        button.setMinHeight(48);
        return button;
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
        
        if (currentScreen == ApiScreen.FACT_GAME && screen != ApiScreen.FACT_GAME) {
            saveInterruptedFactGameRecord();
            gameRunActive = false;
            gameRunId++;
        }
        if (currentScreen == ApiScreen.WHOA_GAME && screen != ApiScreen.WHOA_GAME) {
            saveInterruptedWhoaGameRecord();
            whoaRunActive = false;
            whoaRunId++;
            disposeWhoaMediaPlayer();
        }
        currentScreen = screen;
        currentResult = null;
        currentTranslation = "";
        contentRequestId++;
        showingOriginal = false;
        currentFactGameQuestion = null;
        currentWhoaGameQuestion = null;

        if (root != null) {
            root.getStyleClass().removeAll(ApiScreen.themeClasses());
            root.getStyleClass().add(screen.themeClass);
        }

        ToggleButton selectedButton = switch (screen) {
            case CHUCK -> chuckButton;
            case DAD_JOKE -> dadButton;
            case USELESS_FACT -> factButton;
            case FACT_GAME -> gameNavButton;
            case WHOA_GAME -> whoaGameNavButton;
            case HISTORY -> historyNavButton;
            case FAVORITES -> favoritesNavButton;
        };
        selectedButton.setSelected(true);

        boolean historyScreen = screen == ApiScreen.HISTORY;
        boolean favoritesScreen = screen == ApiScreen.FAVORITES;
        
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
        resultTitleLabel.setText(switch (screen) {
            case FACT_GAME -> "Domanda";
            case WHOA_GAME -> "Clip";
            default -> "Traduzione";
        });
        toggleOriginalButton.setText("Visualizza originale");
        setToggleOriginalButtonVisible(false);

        boolean chuckScreen = screen == ApiScreen.CHUCK;
        boolean gameScreen = screen == ApiScreen.FACT_GAME;
        boolean whoaGameScreen = screen == ApiScreen.WHOA_GAME;
        categoryPicker.setVisible(chuckScreen);
        categoryPicker.setManaged(chuckScreen);
        categoryCombo.setDisable(!chuckScreen || categoryCombo.getItems().isEmpty());
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        copyButton.setVisible(!gameScreen && !whoaGameScreen);
        copyButton.setManaged(!gameScreen && !whoaGameScreen);
        generateButton.setText(gameScreen || whoaGameScreen ? "Nuova partita" : screen.generateText);
        favoriteButton.setVisible(!gameScreen && !whoaGameScreen);
        favoriteButton.setManaged(!gameScreen && !whoaGameScreen);
        trueButton.setVisible(gameScreen);
        trueButton.setManaged(gameScreen);
        falseButton.setVisible(gameScreen);
        falseButton.setManaged(gameScreen);
        whoaReplayButton.setVisible(whoaGameScreen);
        whoaReplayButton.setManaged(whoaGameScreen);
        whoaMediaControls.setVisible(whoaGameScreen);
        whoaMediaControls.setManaged(whoaGameScreen);
        whoaVideoPane.setVisible(whoaGameScreen);
        whoaVideoPane.setManaged(whoaGameScreen);
        whoaAnswersGrid.setVisible(whoaGameScreen);
        whoaAnswersGrid.setManaged(whoaGameScreen);
        whoaAnswerButtons.forEach(button -> {
            button.setText("");
            button.setUserData(null);
        });
        whoaReplayButton.setText("Replay");
        currentWhoaExternalVideoUrl = null;
        gameScoreLabel.setVisible(gameScreen);
        gameScoreLabel.setManaged(gameScreen);
        whoaScoreLabel.setVisible(whoaGameScreen);
        whoaScoreLabel.setManaged(whoaGameScreen);
        gameFeedbackLabel.setVisible(gameScreen || whoaGameScreen);
        gameFeedbackLabel.setManaged(gameScreen || whoaGameScreen);
        gameFeedbackLabel.setText("");
        if (whoaGameScreen) {
            whoaVideoPlaceholderLabel.setText("La clip apparira' qui.");
            whoaVideoPlaceholderLabel.setVisible(true);
            updateWhoaScore();
        } else {
            updateGameScore();
        }
        setBusy(false, "Pronto");
    }

    private void generateContent() {
        if (currentScreen == ApiScreen.FACT_GAME) {
            if (gameRunActive) {
                gameFeedbackLabel.setText("Partita gia' in corso.");
                return;
            }
            startFactGame();
            return;
        }
        if (currentScreen == ApiScreen.WHOA_GAME) {
            if (whoaRunActive) {
                gameFeedbackLabel.setText("Partita gia' in corso.");
                return;
            }
            startWhoaGame();
            return;
        }

        setBusy(true, "Caricamento...");

    
        ApiScreen requestScreen = currentScreen;
        int requestId = ++contentRequestId;
        Supplier<JokeResult> supplier = selectedSupplier();

        CompletableFuture.supplyAsync(supplier)
                .thenCompose(result -> CompletableFuture.supplyAsync(() -> translateResult(result, requestScreen)))
                .thenAccept(content -> Platform.runLater(() -> {
                    if (currentScreen == requestScreen && requestId == contentRequestId) {
                        renderGeneratedContent(content);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (currentScreen == requestScreen && requestId == contentRequestId) {
                            setBusy(false, "Errore");
                            showError(userMessage(ex));
                        }
                    });
                    return null;
                });
    }

    private Supplier<JokeResult> selectedSupplier() {
        
        return switch (currentScreen) {
            case DAD_JOKE -> dadJokeService::getRandomJoke;
            case USELESS_FACT -> uselessFactService::getRandomFact;
            case FACT_GAME -> uselessFactService::getRandomFact;
            case WHOA_GAME -> uselessFactService::getRandomFact;
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
                            gameRunActive = false;
                            currentFactGameQuestion = null;
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

    private void saveInterruptedFactGameRecord() {
        if (!gameRunActive || gameScore <= gameHighScore) {
            return;
        }
        gameHighScore = gameScore;
        persistGameHighScore();
        updateGameScore();
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

    private void startWhoaGame() {
        whoaRunId++;
        whoaRunActive = true;
        whoaScore = 0;
        currentWhoaGameQuestion = null;
        disposeWhoaMediaPlayer();
        gameFeedbackLabel.setText("");
        currentWhoaExternalVideoUrl = null;
        whoaVideoPlaceholderLabel.setText("Caricamento clip...");
        whoaVideoPlaceholderLabel.setVisible(true);
        updateWhoaScore();
        generateWhoaQuestion(whoaRunId);
    }

    private void generateWhoaQuestion(int runId) {
        if (!whoaRunActive || currentScreen != ApiScreen.WHOA_GAME || runId != whoaRunId) {
            return;
        }

        setBusy(true, "Preparazione...");
        disposeWhoaMediaPlayer();
        gameFeedbackLabel.setText("");
        currentWhoaGameQuestion = null;
        whoaVideoPlaceholderLabel.setText("Caricamento clip...");
        whoaVideoPlaceholderLabel.setVisible(true);

        CompletableFuture.supplyAsync(this::buildWhoaGameQuestion)
                .thenAccept(question -> Platform.runLater(() -> {
                    if (whoaRunActive && currentScreen == ApiScreen.WHOA_GAME && runId == whoaRunId) {
                        renderWhoaGameQuestion(question);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (whoaRunActive && currentScreen == ApiScreen.WHOA_GAME && runId == whoaRunId) {
                            whoaRunActive = false;
                            currentWhoaGameQuestion = null;
                            disposeWhoaMediaPlayer();
                            currentWhoaExternalVideoUrl = null;
                            whoaVideoPlaceholderLabel.setText("Clip non disponibile.");
                            setBusy(false, "Errore");
                            showError(userMessage(ex));
                        }
                    });
                    return null;
                });
    }

    private WhoaGameQuestion buildWhoaGameQuestion() {
        if (whoaMovies.size() < 4) {
            whoaMovies = whoaService.getMovies();
        }
        WhoaResponse whoa = whoaService.getRandomWhoa();
        List<String> choices = buildWhoaChoices(whoa.movie());
        return new WhoaGameQuestion(whoa, choices);
    }

    private List<String> buildWhoaChoices(String correctMovie) {
        List<String> wrongMovies = whoaMovies.stream()
                .filter(movie -> !movie.equalsIgnoreCase(correctMovie))
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (wrongMovies.size() < 3) {
            throw new IllegalStateException("lista film Whoa insufficiente.");
        }
        Collections.shuffle(wrongMovies, random);
        List<String> choices = new ArrayList<>(wrongMovies.subList(0, 3));
        choices.add(correctMovie);
        Collections.shuffle(choices, random);
        return choices;
    }

    private void renderWhoaGameQuestion(WhoaGameQuestion question) {
        currentWhoaGameQuestion = question;
        for (int i = 0; i < whoaAnswerButtons.size(); i++) {
            Button button = whoaAnswerButtons.get(i);
            String choice = question.choices().get(i);
            button.setText(choice);
            button.setUserData(choice);
        }
        playWhoaVideo(question.whoa(), question.whoa().playableVideoUrls());
        setBusy(false, "In gioco");
    }

    private void playWhoaVideo(WhoaResponse whoa, List<String> mediaUrls) {
        disposeWhoaMediaPlayer();
        if (mediaUrls.isEmpty()) {
            currentWhoaExternalVideoUrl = whoa.firstPlayableVideoUrl();
            whoaReplayButton.setText("Apri video");
            whoaVideoPlaceholderLabel.setText("Video non supportato da JavaFX. Aprilo nel browser.");
            whoaVideoPlaceholderLabel.setVisible(true);
            setBusy(false, "Apri video");
            return;
        }

        String mediaUrl = mediaUrls.get(0);
        currentWhoaExternalVideoUrl = mediaUrl;
        whoaReplayButton.setText("Replay");
        whoaVideoPlaceholderLabel.setText("Caricamento video...");
        whoaVideoPlaceholderLabel.setVisible(true);

        MediaPlayer player;
        try {
            player = new MediaPlayer(new Media(mediaUrl));
        } catch (MediaException | IllegalArgumentException ex) {
            playWhoaVideo(whoa, mediaUrls.subList(1, mediaUrls.size()));
            return;
        }
        whoaMediaPlayer = player;
        player.setOnReady(() -> {
            whoaVideoPlaceholderLabel.setVisible(false);
            player.play();
        });
        player.setOnError(() -> Platform.runLater(() -> {
            if (player != whoaMediaPlayer) {
                return;
            }
            playWhoaVideo(whoa, mediaUrls.subList(1, mediaUrls.size()));
        }));
        whoaMediaView.setMediaPlayer(player);
    }

    private void replayWhoaClip() {
        if (whoaMediaPlayer == null) {
            openCurrentWhoaVideoExternally();
            return;
        }
        whoaMediaPlayer.seek(Duration.ZERO);
        whoaMediaPlayer.play();
    }

    private void openCurrentWhoaVideoExternally() {
        if (currentWhoaExternalVideoUrl == null || currentWhoaExternalVideoUrl.isBlank()) {
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                showError("Errore: apertura del browser non supportata su questo sistema.");
                return;
            }
            Desktop.getDesktop().browse(URI.create(currentWhoaExternalVideoUrl));
        } catch (IOException | IllegalArgumentException ex) {
            showError("Errore: impossibile aprire il video nel browser.");
        }
    }

    private void answerWhoaQuestion(String selectedMovie) {
        if (!whoaRunActive || currentWhoaGameQuestion == null || selectedMovie == null) {
            gameFeedbackLabel.setText("Avvia una partita prima di rispondere.");
            return;
        }

        WhoaResponse whoa = currentWhoaGameQuestion.whoa();
        boolean correct = selectedMovie.equals(whoa.movie());
        if (correct) {
            whoaScore++;
        }

        resultTitleLabel.setText(correct ? "Risposta corretta" : "Risposta sbagliata");
        gameFeedbackLabel.setText((correct ? "Esatto: " : "Era: ")
                + whoa.movie()
                + (whoa.year() == null ? "" : " (" + whoa.year() + ")")
                + ".");
        whoaAnswerButtons.forEach(button -> button.setDisable(true));
        updateWhoaScore();
        statusLabel.setText(correct ? "Corretto" : "Sbagliato");
        currentWhoaGameQuestion = null;

        if (correct) {
            scheduleNextWhoaQuestion(whoaRunId);
        } else {
            finishWhoaGame();
        }
    }

    private void updateWhoaScore() {
        whoaScoreLabel.setText("Score: " + whoaScore + " | Record: " + whoaHighScore);
    }

    private void saveInterruptedWhoaGameRecord() {
        if (!whoaRunActive || whoaScore <= whoaHighScore) {
            return;
        }
        whoaHighScore = whoaScore;
        persistWhoaHighScore();
        updateWhoaScore();
    }

    private void scheduleNextWhoaQuestion(int runId) {
        PauseTransition delay = new PauseTransition(Duration.seconds(NEXT_WHOA_DELAY_SECONDS));
        delay.setOnFinished(event -> generateWhoaQuestion(runId));
        delay.play();
    }

    private void finishWhoaGame() {
        whoaRunActive = false;
        if (whoaScore > whoaHighScore) {
            whoaHighScore = whoaScore;
            persistWhoaHighScore();
            gameFeedbackLabel.setText(gameFeedbackLabel.getText() + " Nuovo record: " + whoaHighScore + ".");
        } else {
            gameFeedbackLabel.setText(gameFeedbackLabel.getText() + " Partita finita.");
        }
        updateWhoaScore();
        setBusy(false, "Fine partita");
    }

    private void disposeWhoaMediaPlayer() {
        if (whoaMediaPlayer == null) {
            return;
        }
        whoaMediaPlayer.stop();
        whoaMediaPlayer.dispose();
        whoaMediaPlayer = null;
        whoaMediaView.setMediaPlayer(null);
    }

    private JokeResult getChuckResult() {
        String category = categoryCombo.getValue();
        if (category == null || category.isBlank() || RANDOM_CATEGORY.equals(category)) {
            return chuckNorrisService.getRandomJoke();
        }
        return chuckNorrisService.getRandomJokeByCategory(category);
    }

    private GeneratedContent translateResult(JokeResult result, ApiScreen requestScreen) {
        if (requestScreen == ApiScreen.DAD_JOKE) {
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

    private void loadWhoaHighScore() {
        if (!Files.exists(WHOA_GAME_HIGH_SCORE_FILE)) {
            return;
        }

        try {
            String savedScore = Files.readString(WHOA_GAME_HIGH_SCORE_FILE, StandardCharsets.UTF_8).trim();
            whoaHighScore = savedScore.isBlank() ? 0 : Integer.parseInt(savedScore);
        } catch (IOException | NumberFormatException ex) {
            whoaHighScore = 0;
        }
    }

    private void persistWhoaHighScore() {
        try {
            Files.createDirectories(WHOA_GAME_HIGH_SCORE_FILE.getParent());
            Files.writeString(WHOA_GAME_HIGH_SCORE_FILE, Integer.toString(whoaHighScore), StandardCharsets.UTF_8);
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
        
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Copiato");
    }

    private void setBusy(boolean busy, String status) {
        
        boolean gameAlreadyRunning = currentScreen == ApiScreen.FACT_GAME && gameRunActive;
        boolean whoaAlreadyRunning = currentScreen == ApiScreen.WHOA_GAME && whoaRunActive;
        generateButton.setDisable(busy
                || currentScreen == ApiScreen.HISTORY
                || currentScreen == ApiScreen.FAVORITES
                || gameAlreadyRunning
                || whoaAlreadyRunning);
        copyButton.setDisable(busy || currentResult == null);
        favoriteButton.setDisable(busy || currentResult == null);
        toggleOriginalButton.setDisable(busy || currentResult == null || currentTranslation.isBlank() || currentTranslation.equals(currentResult.originalText()));
        trueButton.setDisable(busy || currentScreen != ApiScreen.FACT_GAME || currentFactGameQuestion == null);
        falseButton.setDisable(busy || currentScreen != ApiScreen.FACT_GAME || currentFactGameQuestion == null);
        whoaReplayButton.setDisable(busy
                || currentScreen != ApiScreen.WHOA_GAME
                || (whoaMediaPlayer == null && currentWhoaExternalVideoUrl == null));
        whoaAnswerButtons.forEach(button ->
                button.setDisable(busy || currentScreen != ApiScreen.WHOA_GAME || currentWhoaGameQuestion == null)
        );
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
                "Rendi omaggio a Chuck Norris facendoti due risate.",
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
                "Verifica se il fatto mostrato è vero o falso.",
                "Nuova partita",
                "Premi Nuova partita per iniziare, poi scegli Vero o Falso."
        ),
        WHOA_GAME(
                "whoa-theme",
                "!",
                "Whoa Game",
                "In quale film Keanu Reeves dice \"Whoa\".",
                "Nuova partita",
                ""
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

    private record WhoaGameQuestion(WhoaResponse whoa, List<String> choices) {
    }
}
