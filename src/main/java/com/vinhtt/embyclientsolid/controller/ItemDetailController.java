package com.vinhtt.embyclientsolid.controller;

import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.services.JsonFileHandler;
import com.vinhtt.embyclientsolid.view.controls.BackdropChip;
import com.vinhtt.embyclientsolid.view.controls.TagChip;
import com.vinhtt.embyclientsolid.viewmodel.IItemDetailViewModel;
import embyclient.model.ImageInfo;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller cho ItemDetailView.fxml (Cột 3).
 * (Cập nhật GĐ 10: Binding các nút Review (✓/✗) ).
 */
public class ItemDetailController {

    // --- FXML UI Components ---
    @FXML private StackPane rootPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private ScrollPane mainScrollPane;
    @FXML private StackPane primaryImageContainer;
    @FXML private ImageView primaryImageView;
    @FXML private Button savePrimaryImageButton;
    @FXML private TextField titleTextField;
    @FXML private FlowPane criticRatingPane;
    @FXML private TextArea overviewTextArea;
    @FXML private Label backdropGalleryLabel;
    @FXML private HBox backdropHeaderHBox;
    @FXML private ScrollPane imageGalleryScrollPane;
    @FXML private Button addBackdropButton;
    @FXML private FlowPane imageGalleryPane;
    @FXML private HBox pathContainer;
    @FXML private Label pathLabel;
    @FXML private TextField pathTextField;
    @FXML private Button openButton;
    @FXML private Button openSubtitleButton;
    @FXML private HBox releaseDateContainer;
    @FXML private Label releaseDateLabel;
    @FXML private TextField releaseDateTextField;
    @FXML private Label originalTitleLabel;
    @FXML private TextField originalTitleTextField;
    @FXML private Button fetchReleaseDateButton;
    @FXML private Label tagsLabel;
    @FXML private FlowPane tagsFlowPane;
    @FXML private Button addTagButton;
    @FXML private Button cloneTagButton;
    @FXML private Label studiosLabel;
    @FXML private FlowPane studiosFlowPane;
    @FXML private Button addStudioButton;
    @FXML private Button cloneStudioButton;
    @FXML private Label peopleLabel;
    @FXML private FlowPane peopleFlowPane;
    @FXML private Button addPeopleButton;
    @FXML private Button clonePeopleButton;
    @FXML private Label genresLabelText;
    @FXML private FlowPane genresFlowPane;
    @FXML private Button addGenreButton;
    @FXML private Button cloneGenreButton;
    @FXML private HBox bottomButtonBar;
    @FXML private Button saveButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;

    // (MỚI) FXML fields cho các nút Review (✓/✗) (UR-46)
    @FXML private HBox reviewTitleContainer;
    @FXML private Button acceptTitleButton;
    @FXML private Button rejectTitleButton;
    @FXML private HBox reviewCriticRatingContainer;
    @FXML private Button acceptCriticRatingButton;
    @FXML private Button rejectCriticRatingButton;
    @FXML private HBox reviewOverviewContainer;
    @FXML private Button acceptOverviewButton;
    @FXML private Button rejectOverviewButton;
    @FXML private HBox reviewReleaseDateContainer;
    @FXML private Button acceptReleaseDateButton;
    @FXML private Button rejectReleaseDateButton;
    @FXML private HBox reviewOriginalTitleContainer;
    @FXML private Button acceptOriginalTitleButton;
    @FXML private Button rejectOriginalTitleButton;
    @FXML private HBox reviewTagsContainer;
    @FXML private Button acceptTagsButton;
    @FXML private Button rejectTagsButton;
    @FXML private HBox reviewGenresContainer;
    @FXML private Button acceptGenresButton;
    @FXML private Button rejectGenresButton;
    @FXML private HBox reviewStudiosContainer;
    @FXML private Button acceptStudiosButton;
    @FXML private Button rejectStudiosButton;
    @FXML private HBox reviewPeopleContainer;
    @FXML private Button acceptPeopleButton;
    @FXML private Button rejectPeopleButton;


    private IItemDetailViewModel viewModel;
    private IConfigurationService configService; // Cần cho I18n
    private JsonFileHandler jsonFileHandler;

    /**
     * Khởi tạo Controller.
     */
    public ItemDetailController() {
    }

    @FXML
    public void initialize() {
        // (Chờ setViewModel)
    }

    /**
     * Được gọi bởi MainController (View-Coordinator) để tiêm ViewModel và Services.
     */
    public void setViewModel(IItemDetailViewModel viewModel, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.configService = configService;
        this.jsonFileHandler = new JsonFileHandler(configService);

        // 1. Cài đặt I18n
        setupLocalization();

        // 2. Binding UI cơ bản
        bindUIState();

        // (MỚI) 3. Binding các nút Review (✓/✗)
        bindReviewButtons();

        // 4. Binding các FlowPane (UR-34, UR-41)
        bindFlowPanes();

        // 5. Cài đặt các nút Rating (UR-32, UR-33)
        setupCriticRatingButtons();

        // 6. Cài đặt các hành động (Commands)
        setupButtonActions();

        // 7. Cài đặt Drag-and-Drop (UR-41, UR-42)
        setupDragAndDrop();
    }

    /**
     * Cài đặt I18n (UR-11).
     */
    private void setupLocalization() {
        statusLabel.setText(configService.getString("itemDetailView", "statusDefault"));
        savePrimaryImageButton.setText(configService.getString("itemDetailView", "saveImageButton"));
        titleTextField.setPromptText(configService.getString("itemDetailView", "titlePrompt"));
        releaseDateLabel.setText(configService.getString("itemDetailView", "releaseDateLabel"));
        releaseDateTextField.setPromptText(configService.getString("itemDetailView", "releaseDatePrompt"));
        originalTitleLabel.setText(configService.getString("itemDetailView", "originalTitleLabel"));
        originalTitleTextField.setPromptText(configService.getString("itemDetailView", "originalTitlePrompt"));
        fetchReleaseDateButton.setText(configService.getString("itemDetailView", "fetchReleaseDateButton"));
        pathLabel.setText(configService.getString("itemDetailView", "pathLabel"));
        openSubtitleButton.setText(configService.getString("itemDetailView", "openButtonSubtitle"));
        tagsLabel.setText(configService.getString("itemDetailView", "tagsLabel"));
        addTagButton.setText(configService.getString("itemDetailView", "addButton"));
        cloneTagButton.setText(configService.getString("itemDetailView", "cloneButton"));
        genresLabelText.setText(configService.getString("itemDetailView", "genresLabel"));
        addGenreButton.setText(configService.getString("itemDetailView", "addButton"));
        cloneGenreButton.setText(configService.getString("itemDetailView", "cloneButton"));
        studiosLabel.setText(configService.getString("itemDetailView", "studiosLabel"));
        addStudioButton.setText(configService.getString("itemDetailView", "addButton"));
        cloneStudioButton.setText(configService.getString("itemDetailView", "cloneButton"));
        peopleLabel.setText(configService.getString("itemDetailView", "peopleLabel"));
        addPeopleButton.setText(configService.getString("itemDetailView", "addButton"));
        clonePeopleButton.setText(configService.getString("itemDetailView", "cloneButton"));
        overviewTextArea.setPromptText(configService.getString("itemDetailView", "overviewPrompt"));
        backdropGalleryLabel.setText(configService.getString("itemDetailView", "backdropGalleryLabel"));
        addBackdropButton.setText(configService.getString("itemDetailView", "addButton"));
        saveButton.setText(configService.getString("itemDetailView", "saveButton"));
        importButton.setText(configService.getString("itemDetailView", "importButton"));
        exportButton.setText(configService.getString("itemDetailView", "exportButton"));

        // (MỚI) I18n cho các nút Review (✓/✗)
        String acceptText = configService.getString("itemDetailView", "acceptButton");
        String rejectText = configService.getString("itemDetailView", "rejectButton");
        acceptTitleButton.setText(acceptText);
        rejectTitleButton.setText(rejectText);
        acceptCriticRatingButton.setText(acceptText);
        rejectCriticRatingButton.setText(rejectText);
        acceptOverviewButton.setText(acceptText);
        rejectOverviewButton.setText(rejectText);
        acceptReleaseDateButton.setText(acceptText);
        rejectReleaseDateButton.setText(rejectText);
        acceptOriginalTitleButton.setText(acceptText);
        rejectOriginalTitleButton.setText(rejectText);
        acceptTagsButton.setText(acceptText);
        rejectTagsButton.setText(rejectText);
        acceptGenresButton.setText(acceptText);
        rejectGenresButton.setText(rejectText);
        acceptStudiosButton.setText(acceptText);
        rejectStudiosButton.setText(rejectText);
        acceptPeopleButton.setText(acceptText);
        rejectPeopleButton.setText(rejectText);
    }

    /**
     * Binding các thuộc tính UI cơ bản.
     */
    private void bindUIState() {
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        loadingIndicator.visibleProperty().bind(viewModel.loadingProperty());
        statusLabel.visibleProperty().bind(viewModel.loadingProperty().or(viewModel.showStatusMessageProperty()));

        // Ẩn/hiện nội dung chính
        mainScrollPane.visibleProperty().bind(viewModel.loadingProperty().not().and(viewModel.showStatusMessageProperty().not()));
        mainScrollPane.managedProperty().bind(mainScrollPane.visibleProperty());
        bottomButtonBar.visibleProperty().bind(mainScrollPane.visibleProperty());
        bottomButtonBar.managedProperty().bind(mainScrollPane.visibleProperty());
        imageGalleryScrollPane.visibleProperty().bind(mainScrollPane.visibleProperty());
        imageGalleryScrollPane.managedProperty().bind(mainScrollPane.visibleProperty());
        backdropHeaderHBox.visibleProperty().bind(mainScrollPane.visibleProperty());
        backdropHeaderHBox.managedProperty().bind(mainScrollPane.visibleProperty());

        // Binding các trường dữ liệu
        titleTextField.textProperty().bindBidirectional(viewModel.titleProperty());
        originalTitleTextField.textProperty().bindBidirectional(viewModel.originalTitleProperty());
        overviewTextArea.textProperty().bindBidirectional(viewModel.overviewProperty());
        releaseDateTextField.textProperty().bindBidirectional(viewModel.releaseDateProperty());
        primaryImageView.imageProperty().bind(viewModel.primaryImageProperty());

        // Binding hiển thị đường dẫn (UR-39)
        pathTextField.textProperty().bind(viewModel.itemPathProperty());
        pathContainer.visibleProperty().bind(
                viewModel.itemPathProperty().isNotEmpty()
                        .and(viewModel.itemPathProperty().isNotEqualTo(configService.getString("itemDetailLoader", "noPath")))
        );
        pathContainer.managedProperty().bind(pathContainer.visibleProperty());

        // Nút Mở (UR-39)
        openButton.textProperty().bind(
                Bindings.when(viewModel.isFolderProperty())
                        .then(configService.getString("itemDetailView", "openButtonFolder"))
                        .otherwise(configService.getString("itemDetailView", "openButtonFile"))
        );

        // Nút Subtitle (UR-40)
        openSubtitleButton.visibleProperty().bind(
                pathContainer.visibleProperty().and(viewModel.isFolderProperty().not())
        );
        openSubtitleButton.managedProperty().bind(openSubtitleButton.visibleProperty());

        // Ẩn/hiện các trường theo loại (Folder/File)
        releaseDateContainer.visibleProperty().bind(viewModel.isFolderProperty().not());
        releaseDateContainer.managedProperty().bind(viewModel.isFolderProperty().not());

        // Binding nút Save (UR-48)
        saveButton.disableProperty().bind(viewModel.isDirtyProperty().not());

        // Binding nút Lưu Ảnh (UR-42)
        savePrimaryImageButton.visibleProperty().bind(viewModel.primaryImageDirtyProperty());
        savePrimaryImageButton.managedProperty().bind(viewModel.primaryImageDirtyProperty());
    }

    /**
     * (MỚI) Binding các nút (✓/✗) cho Import/Export (UR-46).
     */
    private void bindReviewButtons() {
        bindReviewContainer(reviewTitleContainer, viewModel.showTitleReviewProperty());
        bindReviewContainer(reviewCriticRatingContainer, viewModel.showCriticRatingReviewProperty());
        bindReviewContainer(reviewOverviewContainer, viewModel.showOverviewReviewProperty());
        bindReviewContainer(reviewReleaseDateContainer, viewModel.showReleaseDateReviewProperty());
        bindReviewContainer(reviewOriginalTitleContainer, viewModel.showOriginalTitleReviewProperty());
        bindReviewContainer(reviewStudiosContainer, viewModel.showStudiosReviewProperty());
        bindReviewContainer(reviewPeopleContainer, viewModel.showPeopleReviewProperty());
        bindReviewContainer(reviewGenresContainer, viewModel.showGenresReviewProperty());
        bindReviewContainer(reviewTagsContainer, viewModel.showTagsReviewProperty());
    }

    /**
     * (MỚI) Helper để binding visibility của HBox chứa nút (✓/✗).
     */
    private void bindReviewContainer(HBox container, ReadOnlyBooleanProperty visibilityProperty) {
        if (container != null && visibilityProperty != null) {
            container.visibleProperty().bind(visibilityProperty);
            container.managedProperty().bind(visibilityProperty);
        }
    }


    /**
     * Cài đặt binding cho các FlowPane.
     */
    private void bindFlowPanes() {
        // (UR-34: Tags)
        viewModel.getTagItems().addListener((ListChangeListener<Tag>) c -> updateFlowPane(tagsFlowPane, viewModel.getTagItems(), "TAG"));

        // (UR-34: Studios)
        viewModel.getStudioItems().addListener((ListChangeListener<Tag>) c -> updateFlowPane(studiosFlowPane, viewModel.getStudioItems(), "STUDIO"));

        // (UR-34: People)
        viewModel.getPeopleItems().addListener((ListChangeListener<Tag>) c -> updateFlowPane(peopleFlowPane, viewModel.getPeopleItems(), "PEOPLE"));

        // (UR-34: Genres)
        viewModel.getGenreItems().addListener((ListChangeListener<Tag>) c -> updateFlowPane(genresFlowPane, viewModel.getGenreItems(), "GENRE"));

        // (UR-41: Backdrops)
        viewModel.getBackdropImages().addListener((ListChangeListener<ImageInfo>) c -> updateImageGallery());
    }

    /**
     * Helper cập nhật FlowPane cho Tags/Studios/People/Genres.
     */
    private void updateFlowPane(FlowPane pane, ObservableList<Tag> list, String chipType) {
        Platform.runLater(() -> {
            pane.getChildren().clear();
            for (Tag tag : list) {
                TagChip chip = new TagChip(tag,
                        (model) -> { // OnDelete
                            switch (chipType) {
                                case "TAG": viewModel.removeTag(model); break;
                                case "STUDIO": viewModel.removeStudio(model); break;
                                case "PEOPLE": viewModel.removePeople(model); break;
                                case "GENRE": viewModel.removeGenre(model); break;
                            }
                        },
                        (model) -> { // OnClick (UR-36)
                            viewModel.fireChipClickEvent(model, chipType);
                        }
                );
                pane.getChildren().add(chip);
            }
        });
    }

    /**
     * Helper cập nhật Gallery ảnh nền (Backdrops).
     */
    private void updateImageGallery() {
        Platform.runLater(() -> {
            imageGalleryPane.getChildren().clear();
            if (viewModel == null) return;

            for (ImageInfo imageInfo : viewModel.getBackdropImages()) {
                String imageUrl = viewModel.getBackdropUrl(imageInfo);
                BackdropChip chip = new BackdropChip(imageInfo, imageUrl,
                        (img) -> viewModel.deleteBackdropCommand(img) // OnDelete
                );
                imageGalleryPane.getChildren().add(chip);
            }
        });
    }

    /**
     * Cài đặt 10 nút Rating.
     * (UR-32, UR-33).
     */
    private void setupCriticRatingButtons() {
        criticRatingPane.getChildren().clear();
        for (int i = 1; i <= 10; i++) {
            final int ratingValue = i;
            Button ratingButton = new Button(String.valueOf(ratingValue));
            ratingButton.getStyleClass().add("rating-button");
            ratingButton.setUserData(ratingValue);

            ratingButton.setOnAction(e -> {
                Float newRating = (float) ratingValue;
                if (Objects.equals(viewModel.criticRatingProperty().get(), newRating)) {
                    newRating = null;
                }
                viewModel.criticRatingProperty().set(newRating);
                viewModel.saveCriticRatingImmediately(newRating);
            });
            criticRatingPane.getChildren().add(ratingButton);
        }

        viewModel.criticRatingProperty().addListener((obs, oldVal, newVal) -> updateRatingButtonSelection(newVal));
        updateRatingButtonSelection(viewModel.criticRatingProperty().get());
    }

    /**
     * Helper cập nhật CSS cho nút Rating.
     */
    private void updateRatingButtonSelection(Float currentRating) {
        Integer selectedValue = (currentRating != null) ? Math.round(currentRating) : null;
        for (Node node : criticRatingPane.getChildren()) {
            if (node instanceof Button) {
                int buttonValue = (Integer) node.getUserData();
                if (selectedValue != null && buttonValue == selectedValue) {
                    if (!node.getStyleClass().contains("selected")) {
                        node.getStyleClass().add("selected");
                    }
                } else {
                    node.getStyleClass().remove("selected");
                }
            }
        }
    }

    /**
     * Gán tất cả các hành động (Command) cho nút.
     */
    private void setupButtonActions() {
        // Commands chính
        saveButton.setOnAction(e -> viewModel.saveChangesCommand());
        fetchReleaseDateButton.setOnAction(e -> viewModel.fetchReleaseDateCommand());

        // Commands Tương tác Local
        openButton.setOnAction(e -> viewModel.openFileOrFolderCommand());
        openSubtitleButton.setOnAction(e -> viewModel.openSubtitleCommand());
        primaryImageContainer.setOnMouseClicked(e -> viewModel.openScreenshotFolderCommand()); // (UR-43)

        // Commands Ảnh
        savePrimaryImageButton.setOnAction(e -> viewModel.saveNewPrimaryImageCommand());
        addBackdropButton.setOnAction(e -> {
            // (Tạm thời dùng FileChooser, GĐ 12 sẽ thêm Drag-Drop)
            File file = showImageFileChooser(true);
            if (file != null) {
                viewModel.uploadDroppedBackdropFiles(List.of(file));
            }
        });

        // Commands Add Chip (UR-35) - (Sẽ được thay thế ở GĐ 11)
        addTagButton.setOnAction(e -> viewModel.addTagCommand());
        addStudioButton.setOnAction(e -> viewModel.addStudioCommand());
        addGenreButton.setOnAction(e -> viewModel.addGenreCommand());
        addPeopleButton.setOnAction(e -> viewModel.addPeopleCommand());

        // Commands Clone (UR-37)
        cloneTagButton.setOnAction(e -> viewModel.clonePropertiesCommand("Tags"));
        cloneStudioButton.setOnAction(e -> viewModel.clonePropertiesCommand("Studios"));
        cloneGenreButton.setOnAction(e -> viewModel.clonePropertiesCommand("Genres"));
        clonePeopleButton.setOnAction(e -> viewModel.clonePropertiesCommand("People"));

        // (MỚI) Commands Import/Export (UR-44, UR-45)
        importButton.setOnAction(e -> {
            File file = jsonFileHandler.showOpenJsonDialog(getStage());
            if (file != null) viewModel.importAndPreview(file);
        });
        exportButton.setOnAction(e -> {
            String rawName = viewModel.getExportFileName();
            // Xử lý tên file (thay thế các ký tự không hợp lệ)
            String initialFileName = (rawName != null ? rawName.replaceAll("[^a-zA-Z0-9.-]", "_") : "item") + ".json";

            File file = jsonFileHandler.showSaveJsonDialog(getStage(), initialFileName);
            if (file != null) {
                viewModel.exportCommand(file);
            }
        });

        // (MỚI) Commands Accept/Reject (UR-47)
        acceptTitleButton.setOnAction(e -> viewModel.acceptImportField("title"));
        rejectTitleButton.setOnAction(e -> viewModel.rejectImportField("title"));
        acceptCriticRatingButton.setOnAction(e -> viewModel.acceptImportField("criticRating"));
        rejectCriticRatingButton.setOnAction(e -> viewModel.rejectImportField("criticRating"));
        acceptOverviewButton.setOnAction(e -> viewModel.acceptImportField("overview"));
        rejectOverviewButton.setOnAction(e -> viewModel.rejectImportField("overview"));
        acceptReleaseDateButton.setOnAction(e -> viewModel.acceptImportField("releaseDate"));
        rejectReleaseDateButton.setOnAction(e -> viewModel.rejectImportField("releaseDate"));
        acceptOriginalTitleButton.setOnAction(e -> viewModel.acceptImportField("originalTitle"));
        rejectOriginalTitleButton.setOnAction(e -> viewModel.rejectImportField("originalTitle"));
        acceptTagsButton.setOnAction(e -> viewModel.acceptImportField("tags"));
        rejectTagsButton.setOnAction(e -> viewModel.rejectImportField("tags"));
        acceptGenresButton.setOnAction(e -> viewModel.acceptImportField("genres"));
        rejectGenresButton.setOnAction(e -> viewModel.rejectImportField("genres"));
        acceptStudiosButton.setOnAction(e -> viewModel.acceptImportField("studios"));
        rejectStudiosButton.setOnAction(e -> viewModel.rejectImportField("studios"));
        acceptPeopleButton.setOnAction(e -> viewModel.acceptImportField("people"));
        rejectPeopleButton.setOnAction(e -> viewModel.rejectImportField("people"));
    }

    /**
     * Cài đặt Drag-and-Drop cho ảnh.
     */
    private void setupDragAndDrop() {
        // (UR-42: Ảnh Primary)
        primaryImageContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != primaryImageContainer && event.getDragboard().hasFiles()) {
                if (getFirstImageFileFromDragboard(event.getDragboard()).isPresent()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
            event.consume();
        });
        primaryImageContainer.setOnDragDropped(event -> {
            getFirstImageFileFromDragboard(event.getDragboard()).ifPresent(file -> {
                viewModel.setDroppedPrimaryImage(file);
                event.setDropCompleted(true);
            });
            event.consume();
        });

        // (UR-41: Ảnh Backdrop)
        Node[] dropTargets = {imageGalleryPane, imageGalleryScrollPane};
        for (Node target : dropTargets) {
            target.setOnDragOver(event -> {
                if (event.getGestureSource() != target && event.getDragboard().hasFiles()) {
                    if (!getImageFilesFromDragboard(event.getDragboard()).isEmpty()) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }
                }
                event.consume();
            });
            target.setOnDragDropped(event -> {
                List<File> files = getImageFilesFromDragboard(event.getDragboard());
                if (!files.isEmpty()) {
                    viewModel.uploadDroppedBackdropFiles(files);
                    event.setDropCompleted(true);
                }
                event.consume();
            });
        }
    }

    // --- Helpers (Drag-and-Drop) ---
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp");
    }
    private List<File> getImageFilesFromDragboard(Dragboard db) {
        if (db.hasFiles()) {
            return db.getFiles().stream().filter(this::isImageFile).collect(Collectors.toList());
        }
        return List.of();
    }
    private Optional<File> getFirstImageFileFromDragboard(Dragboard db) {
        if (db.hasFiles()) {
            return db.getFiles().stream().filter(this::isImageFile).findFirst();
        }
        return Optional.empty();
    }

    // --- Helpers (File Chooser) ---
    private File showImageFileChooser(boolean allowMultiple) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(allowMultiple ? configService.getString("itemImageUpdater", "selectBackdropsTitle") : configService.getString("itemImageUpdater", "selectPrimaryTitle"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );
        if (allowMultiple) {
            List<File> files = fileChooser.showOpenMultipleDialog(getStage());
            return (files != null && !files.isEmpty()) ? files.get(0) : null; // (Tạm thời vẫn chỉ 1 file)
        } else {
            return fileChooser.showOpenDialog(getStage());
        }
    }

    /**
     * (MỚI) Helper dùng JsonFileHandler đã được inject.
     */
    private File showJsonFileChooser(boolean isOpen) {
        if (isOpen) {
            return jsonFileHandler.showOpenJsonDialog(getStage());
        } else {
            // (Cần cải thiện tên file động)
            return jsonFileHandler.showSaveJsonDialog(getStage(), "item.json");
        }
    }

    private Stage getStage() {
        return (Stage) rootPane.getScene().getWindow();
    }
}