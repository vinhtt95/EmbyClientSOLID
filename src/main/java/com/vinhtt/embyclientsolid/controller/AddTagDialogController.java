package com.vinhtt.embyclientsolid.controller;

import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.IAddTagViewModel;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Controller cho AddTagDialog.fxml (View).
 * (Cập nhật: Sửa lỗi binding, lắng nghe closeDialogProperty).
 */
public class AddTagDialogController {

    // --- FXML Fields ---
    @FXML private VBox rootPane;
    @FXML private Label titleLabel;
    @FXML private RadioButton simpleTagRadio;
    @FXML private RadioButton jsonTagRadio;
    @FXML private GridPane simpleTagPane;
    @FXML private Label contentLabel;
    @FXML private TextField simpleNameField;
    @FXML private GridPane jsonTagPane;
    @FXML private Label keyLabel;
    @FXML private TextField keyField;
    @FXML private Label valueLabel;
    @FXML private TextField valueField;
    @FXML private VBox suggestionJsonContainer;
    @FXML private Label suggestionKeyLabel;
    @FXML private FlowPane suggestionKeysPane;
    @FXML private Label suggestionValueLabel;
    @FXML private FlowPane suggestionValuesPane;
    @FXML private VBox suggestionSimpleContainer;
    @FXML private Label suggestionSimpleLabel;
    @FXML private FlowPane suggestionSimplePane;
    @FXML private Label copyIdLabel;
    @FXML private TextField copyIdField;
    @FXML private Button copyButton;
    @FXML private Label copyStatusLabel;
    @FXML private Button cancelButton;
    @FXML private Button okButton;

    private IAddTagViewModel viewModel;
    private Stage dialogStage;

    private static final String FOCUSED_CHIP_STYLE_CLASS = "focused-chip";

    /**
     * Khởi tạo Controller.
     */
    public AddTagDialogController() {
    }

    @FXML
    public void initialize() {
        // Logic binding được chuyển sang setViewModel()
    }

    /**
     * Được gọi bởi AppNavigator để tiêm ViewModel và Stage.
     */
    public void setViewModel(IAddTagViewModel viewModel, Stage dialogStage) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;
        bindViewModel();
    }

    /**
     * Kết nối tất cả UI components với ViewModel.
     */
    private void bindViewModel() {
        // --- Binding I18n Labels ---
        titleLabel.textProperty().bind(viewModel.titleProperty());
        contentLabel.textProperty().bind(viewModel.simpleLabelProperty());
        suggestionSimpleLabel.textProperty().bind(viewModel.simpleSuggestionLabelProperty());

        // (Hardcode các label khác, GĐ 13 sẽ sửa)
        keyLabel.setText("Key:");
        valueLabel.setText("Value:");
        suggestionKeyLabel.setText("Key");
        suggestionValueLabel.setText("Value");
        jsonTagRadio.setText("Tag Key-Value (JSON)");
        copyIdLabel.setText("Sao chép nhanh từ ID:");
        copyButton.setText("Sao chép");
        cancelButton.setText("Hủy");
        okButton.setText("Đồng ý");

        // --- Binding Radio Buttons (2 chiều) ---
        simpleTagRadio.selectedProperty().bindBidirectional(viewModel.simpleModeProperty());

        // --- Binding Chế độ Hiển thị (Pane) ---
        simpleTagPane.visibleProperty().bind(viewModel.simpleModeProperty());
        simpleTagPane.managedProperty().bind(viewModel.simpleModeProperty());
        jsonTagPane.visibleProperty().bind(viewModel.simpleModeProperty().not());
        jsonTagPane.managedProperty().bind(viewModel.simpleModeProperty().not());

        // --- Binding Trường Text (2 chiều) ---
        simpleNameField.textProperty().bindBidirectional(viewModel.simpleNameProperty());
        keyField.textProperty().bindBidirectional(viewModel.keyProperty());
        valueField.textProperty().bindBidirectional(viewModel.valueProperty());
        copyIdField.textProperty().bindBidirectional(viewModel.copyIdProperty());
        copyStatusLabel.textProperty().bind(viewModel.copyStatusProperty());

        // --- Binding FlowPanes Gợi ý ---
        viewModel.getSuggestionKeys().addListener((javafx.collections.ListChangeListener.Change<? extends String> c) ->
                populateKeySuggestions(suggestionKeysPane, viewModel.getSuggestionKeys()));

        viewModel.getSuggestionValues().addListener((javafx.collections.ListChangeListener.Change<? extends Tag> c) ->
                populateValueSuggestions(suggestionValuesPane, viewModel.getSuggestionValues()));

        viewModel.getSuggestionSimple().addListener((javafx.collections.ListChangeListener.Change<? extends SuggestionItem> c) ->
                populateSimpleSuggestions(suggestionSimplePane, viewModel.getSuggestionSimple()));

        // --- Binding Focus (Điều hướng phím) ---
        bindChipFocus(suggestionKeysPane, viewModel.focusedKeyIndexProperty());
        bindChipFocus(suggestionValuesPane, viewModel.focusedValueIndexProperty());
        bindChipFocus(suggestionSimplePane, viewModel.focusedSimpleIndexProperty());

        // --- Chuyển tiếp (Delegate) Sự kiện Phím ---
        simpleNameField.addEventFilter(KeyEvent.KEY_PRESSED, e -> viewModel.handleFieldKeyEvent(e));
        keyField.addEventFilter(KeyEvent.KEY_PRESSED, e -> viewModel.handleFieldKeyEvent(e));
        valueField.addEventFilter(KeyEvent.KEY_PRESSED, e -> viewModel.handleFieldKeyEvent(e));
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                viewModel.cancelCommand();
                e.consume();
            }
        });

        // --- Chuyển tiếp (Delegate) Sự kiện Nút ---
        copyButton.setOnAction(e -> viewModel.copyCommand());

        // --- Lắng nghe Kết quả từ VM ---
        viewModel.closeDialogProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> dialogStage.close());
            }
        });

        // --- Focus ban đầu ---
        Platform.runLater(() -> {
            if (viewModel.simpleModeProperty().get()) {
                simpleNameField.requestFocus();
            } else {
                keyField.requestFocus();
            }
        });
    }

    // --- Helpers tạo Chip Gợi ý ---

    private void populateKeySuggestions(FlowPane pane, ObservableList<String> keys) {
        pane.getChildren().clear();
        for (String key : keys) {
            ToggleButton chip = new ToggleButton(key);
            chip.getStyleClass().add("suggestion-key-button");
            chip.setUserData(key);
            chip.setOnAction(e -> viewModel.selectKeySuggestion(key));
            pane.getChildren().add(chip);
        }
    }

    private void populateValueSuggestions(FlowPane pane, ObservableList<Tag> tags) {
        pane.getChildren().clear();
        for (Tag tag : tags) {
            ToggleButton chip = new ToggleButton(tag.getValue());
            chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");
            chip.setUserData(tag);
            chip.setOnAction(e -> viewModel.selectValueSuggestion(tag));
            pane.getChildren().add(chip);
        }
    }

    private void populateSimpleSuggestions(FlowPane pane, ObservableList<SuggestionItem> items) {
        pane.getChildren().clear();
        for (SuggestionItem item : items) {
            ToggleButton chip = new ToggleButton(item.getName());
            chip.getStyleClass().add("suggested-tag-button");
            if (Objects.equals(item.getType(), "Genre")) {
                chip.getStyleClass().add("tag-view-simple");
            } else if (Objects.equals(item.getType(), "Tag")) {
                chip.getStyleClass().add("tag-view-simple"); // Thêm style cho tag đơn giản
            }
            chip.setUserData(item);
            chip.setOnAction(e -> viewModel.selectSimpleSuggestion(item));
            pane.getChildren().add(chip);
        }
    }

    /**
     * Binding style focus (CSS) cho các chip.
     */
    private void bindChipFocus(FlowPane pane, ReadOnlyIntegerProperty focusIndex) {
        focusIndex.addListener((obs, oldIndex, newIndex) -> {
            Platform.runLater(() -> {
                int idx = newIndex.intValue();
                for (int i = 0; i < pane.getChildren().size(); i++) {
                    Node child = pane.getChildren().get(i);
                    if (i == idx) {
                        if (!child.getStyleClass().contains(FOCUSED_CHIP_STYLE_CLASS)) {
                            child.getStyleClass().add(FOCUSED_CHIP_STYLE_CLASS);
                        }
                    } else {
                        child.getStyleClass().remove(FOCUSED_CHIP_STYLE_CLASS);
                    }
                }
            });
        });
    }

    /**
     * Highlight các trường nhập liệu nếu VM báo lỗi.
     */
    private void highlightInvalidField() {
        TextField fieldToHighlight = null;
        if (viewModel.simpleModeProperty().get()) {
            if (viewModel.simpleNameProperty().get().trim().isEmpty()) {
                fieldToHighlight = simpleNameField;
            }
        } else {
            if (viewModel.keyProperty().get().trim().isEmpty()) {
                fieldToHighlight = keyField;
            } else if (viewModel.valueProperty().get().trim().isEmpty()) {
                fieldToHighlight = valueField;
            }
        }

        if (fieldToHighlight != null) {
            final TextField finalField = fieldToHighlight;
            Platform.runLater(() -> {
                finalField.requestFocus();
                finalField.getStyleClass().add("validation-error");
                // (Giai đoạn 12 sẽ thêm PauseTransition để xóa style)
            });
        }
    }

    /**
     * Được gọi từ FXML.
     */
    @FXML
    private void handleOk() {
        viewModel.okCommand();
        // Kiểm tra xem VM đã tạo result chưa
        if (viewModel.getResult() == null) {
            highlightInvalidField();
        }
    }

    /**
     * Được gọi từ FXML.
     */
    @FXML
    private void handleCancel() {
        viewModel.cancelCommand();
    }
}