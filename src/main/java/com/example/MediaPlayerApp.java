package com.mediaplayer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerApp extends Application {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private final List<File> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private ListView<File> playlistView;
    private Label timeLabel;
    private Slider progressSlider;
    private Slider volumeSlider;
    private boolean isMuted = false;
    private double prevVolume = 0.5;
    private Label trackTitleLabel;
    private Label artistLabel;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Media Player");

        // === MAIN CONTAINER ===
        VBox root = new VBox(8);
        root.setPadding(new Insets(12, 16, 12, 16));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("""
            -fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #4A90D9 0%, #6BB0E8 100%);
            """);

        // === TOP BAR ===
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(0, 0, 6, 0));

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE dd MMM"));
        Label dateTimeLabel = new Label(dateStr + " · Media Player");
        dateTimeLabel.setTextFill(Color.WHITE);
        dateTimeLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox windowControls = new HBox(5);
        Button minBtn = new Button("─");
        Button maxBtn = new Button("□");
        Button closeBtn = new Button("✕");

        minBtn.setStyle(winBtnStyle("#ffffff"));
        maxBtn.setStyle(winBtnStyle("#ffffff"));
        closeBtn.setStyle(winBtnStyle("#ff5555"));

        minBtn.setOnAction(e -> primaryStage.setIconified(true));
        maxBtn.setOnAction(e -> {
            primaryStage.setMaximized(!primaryStage.isMaximized());
            maxBtn.setText(primaryStage.isMaximized() ? "❐" : "□");
        });
        closeBtn.setOnAction(e -> System.exit(0));

        windowControls.getChildren().addAll(minBtn, maxBtn, closeBtn);
        topBar.getChildren().addAll(dateTimeLabel, spacer, windowControls);

        // === MEDIA DISPLAY ===
        StackPane artContainer = new StackPane();
        artContainer.setMinSize(260, 180);
        artContainer.setPrefSize(320, 220);
        artContainer.setMaxSize(340, 240);
        artContainer.setStyle("""
            -fx-background-color: rgba(255,255,255,0.15);
            -fx-background-radius: 20;
            -fx-padding: 6;
            """);

        mediaView = new MediaView();
        mediaView.setFitWidth(290);
        mediaView.setFitHeight(200);
        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);
        StackPane.setAlignment(mediaView, Pos.CENTER);
        artContainer.getChildren().add(mediaView);

        // === TRACK INFO ===
        VBox trackInfoBox = new VBox(2);
        trackInfoBox.setAlignment(Pos.CENTER);
        trackInfoBox.setPadding(new Insets(6, 0, 3, 0));
        trackInfoBox.setMaxWidth(320);

        trackTitleLabel = new Label("No Track Selected");
        trackTitleLabel.setTextFill(Color.WHITE);
        trackTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        trackTitleLabel.setWrapText(true);

        artistLabel = new Label("Add files to begin playback");
        artistLabel.setTextFill(Color.WHITE.deriveColor(0, 1, 1, 0.7));
        artistLabel.setFont(Font.font("Segoe UI", 10));

        trackInfoBox.getChildren().addAll(trackTitleLabel, artistLabel);

        // === PROGRESS BAR ===
        progressSlider = new Slider(0, 100, 0);
        progressSlider.setPrefWidth(300);
        progressSlider.getStyleClass().add("progress-slider");
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && progressSlider.isValueChanging()) {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && total.greaterThan(Duration.ZERO)) {
                    mediaPlayer.seek(total.multiply(newVal.doubleValue() / 100.0));
                }
            }
        });

        timeLabel = new Label("0:00 / 0:00");
        timeLabel.setTextFill(Color.WHITE.deriveColor(0, 1, 1, 0.7));
        timeLabel.setFont(Font.font("Segoe UI", 9));

        HBox timeRow = new HBox(timeLabel);
        timeRow.setPadding(new Insets(2, 0, 0, 0));
        timeRow.setAlignment(Pos.CENTER);

        // === CONTROLS CARD ===
        VBox controlsCard = new VBox(8);
        controlsCard.setPadding(new Insets(12, 15, 12, 15));
        controlsCard.setAlignment(Pos.CENTER);
        controlsCard.setStyle("""
            -fx-background-color: rgba(25, 35, 60, 0.7);
            -fx-background-radius: 18;
            """);
        controlsCard.setPrefWidth(320);

        VBox progressBox = new VBox(4, progressSlider, timeRow);
        progressBox.setAlignment(Pos.CENTER);

        Button prevBtn = createIconButton("◀◀", 13);
        Button playBtn = createIconButton("▶", 16);
        Button pauseBtn = createIconButton("⏸", 14);
        Button nextBtn = createIconButton("▶▶", 13);

        playBtn.setOnAction(e -> play());
        pauseBtn.setOnAction(e -> pause());
        prevBtn.setOnAction(e -> playPrevious());
        nextBtn.setOnAction(e -> playNext());

        HBox mainControls = new HBox(15, prevBtn, playBtn, pauseBtn, nextBtn);
        mainControls.setAlignment(Pos.CENTER);

        volumeSlider = new Slider(0, 1, 0.5);
        volumeSlider.setPrefWidth(90);
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(val.doubleValue());
            prevVolume = val.doubleValue();
        });

        Button muteBtn = createIconButton("🔊", 11);
        muteBtn.setOnAction(e -> toggleMute());
        muteBtn.textProperty().bind(volumeSlider.valueProperty().map(v -> v.doubleValue() < 0.05 ? "🔇" : "🔊"));

        HBox volRow = new HBox(6, muteBtn, volumeSlider);
        volRow.setAlignment(Pos.CENTER);

        controlsCard.getChildren().addAll(progressBox, mainControls, volRow);

        // === PLAYLIST — FIXED SIZING SO BUTTONS NEVER HIDE ===
        Label playlistHeader = new Label("Playlist");
        playlistHeader.setTextFill(Color.WHITE);
        playlistHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        playlistView = new ListView<>();
        playlistView.setMinHeight(60);
        playlistView.setPrefHeight(90);
        playlistView.setMaxHeight(110); // ✅ STRICT LIMIT — never grows beyond this
        playlistView.setStyle("""
            -fx-background-color: rgba(255,255,255,0.15);
            -fx-background-radius: 12;
            -fx-padding: 3;
            """);
        playlistView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getName());
                    setTextFill(Color.WHITE);
                    setFont(Font.font("Segoe UI", 10));
                    if (playlist.indexOf(file) == currentIndex) {
                        setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 6; -fx-padding: 5; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-padding: 5;");
                    }
                }
            }
        });
        playlistView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                int idx = playlistView.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < playlist.size() && idx != currentIndex) {
                    loadAndPlay(playlist.get(idx));
                }
            }
        });

        // === BUTTONS — ALWAYS VISIBLE ===
        Button addBtn = createTextButton("Add");
        Button removeBtn = createTextButton("Remove");
        Button clearAllBtn = createTextButton("Clear");

        addBtn.setOnAction(e -> addFiles(primaryStage));
        removeBtn.setOnAction(e -> removeSelected());
        clearAllBtn.setOnAction(e -> clearAllFiles());

        HBox listBtns = new HBox(8, addBtn, removeBtn, clearAllBtn);
        listBtns.setAlignment(Pos.CENTER);
        listBtns.setMinHeight(35); // ✅ Fixed height — never collapses

        VBox playlistBox = new VBox(6, playlistHeader, playlistView, listBtns);
        playlistBox.setPadding(new Insets(6, 0, 0, 0));
        VBox.setVgrow(playlistBox, Priority.NEVER); // ✅ Does NOT take extra space

        // === SHORTCUTS ===
        Label shortcuts = new Label("Space=Play/Pause  S=Stop  N=Next  P=Prev  ↑↓=Vol  M=Mute");
        shortcuts.setTextFill(Color.WHITE.deriveColor(0, 1, 1, 0.6));
        shortcuts.setFont(Font.font("Segoe UI", 8.5));
        shortcuts.setPadding(new Insets(4, 0, 0, 0));

        // === SCROLLPANE — Fallback for very small windows ===
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(root);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        root.getChildren().addAll(
            topBar,
            artContainer,
            trackInfoBox,
            controlsCard,
            playlistBox,
            shortcuts
        );

        // === FINAL SCENE ===
        Scene scene = new Scene(scrollPane, 380, 620);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(360);
        primaryStage.setMinHeight(540); // ✅ Minimum height keeps buttons visible
        primaryStage.show();

        // Draggable window
        final double[] xOffset = {0}, yOffset = {0};
        root.setOnMousePressed(e -> {
            xOffset[0] = e.getSceneX();
            yOffset[0] = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            primaryStage.setX(e.getScreenX() - xOffset[0]);
            primaryStage.setY(e.getScreenY() - yOffset[0]);
        });
    }

    private String winBtnStyle(String color) {
        return String.format("""
            -fx-background-color: transparent;
            -fx-text-fill: %s;
            -fx-font-size: 12px;
            -fx-cursor: hand;
            -fx-padding: 2 7;
            -fx-background-radius: 4;
            """, color);
    }

    private Button createIconButton(String symbol, int fontSize) {
        Button btn = new Button(symbol);
        btn.setStyle(String.format("""
            -fx-background-color: transparent;
            -fx-text-fill: white;
            -fx-font-size: %dpx;
            -fx-cursor: hand;
            -fx-min-width: 32px;
            -fx-min-height: 32px;
            """, fontSize));
        return btn;
    }

    private Button createTextButton(String label) {
        Button btn = new Button(label);
        btn.setStyle("""
            -fx-background-color: rgba(255,255,255,0.25);
            -fx-text-fill: white;
            -fx-background-radius: 8;
            -fx-padding: 6 12;
            -fx-font-size: 11px;
            -fx-cursor: hand;
            -fx-min-width: 70px;
            """);
        return btn;
    }

    private void addFiles(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Media Files");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Media Files", "*.mp4", "*.mp3", "*.wav", "*.avi", "*.m4a")
        );
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files != null) {
            playlist.addAll(files);
            refreshPlaylistDisplay();
            if (currentIndex == -1 && !playlist.isEmpty()) {
                loadAndPlay(playlist.get(0));
            }
        }
    }

    private void removeSelected() {
        int idx = playlistView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            playlist.remove(idx);
            if (idx == currentIndex) {
                stopPlayback();
                currentIndex = -1;
                trackTitleLabel.setText("No Track Selected");
                artistLabel.setText("Add files to begin playback");
            } else if (idx < currentIndex) {
                currentIndex--;
            }
            refreshPlaylistDisplay();
        } else {
            showAlert("Please select a file from the list first!");
        }
    }

    private void clearAllFiles() {
        if (!playlist.isEmpty()) {
            stopPlayback();
            playlist.clear();
            currentIndex = -1;
            trackTitleLabel.setText("No Track Selected");
            artistLabel.setText("Add files to begin playback");
            refreshPlaylistDisplay();
        } else {
            showAlert("Playlist is already empty!");
        }
    }

    private void refreshPlaylistDisplay() {
        playlistView.getItems().clear();
        playlistView.getItems().addAll(playlist);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Media Player");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadAndPlay(File file) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            currentIndex = playlist.indexOf(file);

            String name = file.getName();
            trackTitleLabel.setText(name.length() > 26 ? name.substring(0, 23) + "..." : name);
            artistLabel.setText("Now Playing");

            mediaPlayer.currentTimeProperty().addListener((obs, old, cur) -> {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && total.greaterThan(Duration.ZERO)) {
                    progressSlider.setValue((cur.toMillis() / total.toMillis()) * 100);
                    timeLabel.setText(formatTime(cur) + " / " + formatTime(total));
                }
            });

            mediaPlayer.setOnEndOfMedia(this::playNext);
            mediaPlayer.setVolume(volumeSlider.getValue());
            mediaPlayer.play();
            refreshPlaylistDisplay();

        } catch (Exception e) {
            showAlert("Cannot load file:\n" + e.getMessage());
        }
    }

    private String formatTime(Duration d) {
        int s = (int) Math.round(d.toSeconds());
        return "%d:%02d".formatted(s / 60, s % 60);
    }

    private void play() {
        if (mediaPlayer != null) mediaPlayer.play();
        else if (!playlist.isEmpty()) loadAndPlay(playlist.get(0));
    }

    private void pause() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) {
            if (!playlist.isEmpty()) loadAndPlay(playlist.get(0));
            return;
        }
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) pause();
        else play();
    }

    private void stopPlayback() {
        if (mediaPlayer != null) mediaPlayer.stop();
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        int next = (currentIndex + 1) % playlist.size();
        loadAndPlay(playlist.get(next));
    }

    private void playPrevious() {
        if (playlist.isEmpty()) return;
        int prev = (currentIndex - 1 + playlist.size()) % playlist.size();
        loadAndPlay(playlist.get(prev));
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        isMuted = !isMuted;
        if (isMuted) {
            prevVolume = volumeSlider.getValue();
            volumeSlider.setValue(0);
            mediaPlayer.setVolume(0);
        } else {
            volumeSlider.setValue(prevVolume);
            mediaPlayer.setVolume(prevVolume);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}