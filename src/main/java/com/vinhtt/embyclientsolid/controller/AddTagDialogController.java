package com.vinhtt.embyclientsolid.controller;

import com.vinhtt.embyclientsolid.core.IConfigurationService;
import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.IAddTagViewModel;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Controller cho AddTagDialog.fxml (View).
 * (Cập nhật: Sửa lỗi NullPointer trên copyStatusLabel và lỗi 'isJson()').
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
    private IConfigurationService configService;

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
     * Được gọi bởi AppNavigator để tiêm ViewModel, Stage và ConfigService.
     */
    public void setViewModel(IAddTagViewModel viewModel, Stage dialogStage, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;
        this.configService = configService;
        bindViewModel();
    }

    /**
     * Kết nối tất cả UI components với ViewModel.
     */
    private void bindViewModel() {
        // --- Binding I18n Labels (Lấy từ VM) ---
        titleLabel.textProperty().bind(viewModel.titleProperty());
        contentLabel.textProperty().bind(viewModel.simpleLabelProperty());
        suggestionSimpleLabel.textProperty().bind(viewModel.simpleSuggestionLabelProperty());
        simpleTagRadio.textProperty().bind(viewModel.simpleLabelProperty());

        // (Lấy I18n từ ConfigService)
        keyLabel.setText(configService.getString("addTagDialog", "keyLabel"));
        valueLabel.setText(configService.getString("addTagDialog", "valueLabel"));
        suggestionKeyLabel.setText(configService.getString("addTagDialog", "suggestionKeyLabel"));
        suggestionValueLabel.setText(configService.getString("addTagDialog", "suggestionValueLabel"));
        jsonTagRadio.setText(configService.getString("addTagDialog", "labelJson"));
        copyIdLabel.setText(configService.getString("addTagDialog", "quickCopyLabel"));
        copyButton.setText(configService.getString("addTagDialog", "copyButton"));
        cancelButton.setText(configService.getString("addTagDialog", "cancelButton"));
        okButton.setText(configService.getString("addTagDialog", "okButton"));
        simpleNameField.setPromptText(configService.getString("addTagDialog", "contentPrompt"));
        keyField.setPromptText(configService.getString("addTagDialog", "keyPrompt"));
        valueField.setPromptText(configService.getString("addTagDialog", "valuePrompt"));
        copyIdField.setPromptText(configService.getString("addTagDialog", "copyIdPrompt"));

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

        // (SỬA LỖI 1: Dòng này giờ đã hợp lệ)
        copyStatusLabel.textProperty().bind(viewModel.copyStatusProperty());

        // --- Binding FlowPanes Gợi ý ---
        viewModel.getSuggestionKeys().addListener((ListChangeListener<String>) c ->
                populateKeySuggestions(suggestionKeysPane, viewModel.getSuggestionKeys()));
        viewModel.getSuggestionValues().addListener((ListChangeListener<Tag>) c ->
                populateValueSuggestions(suggestionValuesPane, viewModel.getSuggestionValues()));
        viewModel.getSuggestionSimple().addListener((ListChangeListener<SuggestionItem>) c ->
                populateSimpleSuggestions(suggestionSimplePane, viewModel.getSuggestionSimple()));

        // --- Binding Focus (Điều hướng phím) ---
        bindChipFocus(suggestionKeysPane, viewModel.focusedKeyIndexProperty());
        bindChipFocus(suggestionValuesPane, viewModel.focusedValueIndexProperty());
        bindChipFocus(suggestionSimplePane, viewModel.focusedSimpleIndexProperty());

        // --- Chuyển tiếp (Delegate) Sự kiện Phím ---
        simpleNameField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            viewModel.handleFieldKeyEvent(e, "simple");
            if (e.getCode() == KeyCode.TAB && !e.isShiftDown()) {
                valueField.requestFocus();
            }
        });
        keyField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            viewModel.handleFieldKeyEvent(e, "key");
            if (e.getCode() == KeyCode.TAB && !e.isShiftDown()) {
                valueField.requestFocus();
            }
            if (e.getCode() == KeyCode.ENTER && viewModel.simpleModeProperty().get()) {
                simpleNameField.requestFocus();
                simpleNameField.selectAll();
            }
        });
        valueField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            viewModel.handleFieldKeyEvent(e, "value");
            if (e.getCode() == KeyCode.TAB && !e.isShiftDown()) {
                okButton.requestFocus();
            }
        });
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                viewModel.cancelCommand();
                e.consume();
            }
        });

        // --- Chuyển tiếp (Delegate) Sự kiện Mất Focus ---
        keyField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) { // Focus Lost
                viewModel.handleFocusLost("key");
            }
        });
        valueField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) { // Focus Lost
                viewModel.handleFocusLost("value");
            }
        });
        simpleNameField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) { // Focus Lost
                viewModel.handleFocusLost("simple");
            }
        });

        // --- Chuyển tiếp (Delegate) Sự kiện Nút ---
        copyButton.setOnAction(e -> viewModel.copyCommand());
        // (okButton và cancelButton được gán qua FXML onAction)

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
            chip.setOnAction(e -> {
                ensureSingleSelection(pane, chip);
                viewModel.selectKeySuggestion(key);
                valueField.requestFocus();
            });
            pane.getChildren().add(chip);
        }
    }

    private void populateValueSuggestions(FlowPane pane, ObservableList<Tag> tags) {
        pane.getChildren().clear();
        for (Tag tag : tags) {
            ToggleButton chip = new ToggleButton(tag.getValue());
            chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");
            chip.setUserData(tag);
            chip.setOnAction(e -> {
                ensureSingleSelection(pane, chip);
                viewModel.selectValueSuggestion(tag);
                okButton.requestFocus();
            });
            pane.getChildren().add(chip);
        }
    }

    private void populateSimpleSuggestions(FlowPane pane, ObservableList<SuggestionItem> items) {
        pane.getChildren().clear();
        for (SuggestionItem item : items) {
            ToggleButton chip = new ToggleButton(item.getName());
            chip.getStyleClass().add("suggested-tag-button");

            // (SỬA LỖI 2: Xóa 'item.isJson()' và đơn giản hóa logic)
            // Vì đây là populateSimpleSuggestions, chúng luôn là 'simple'.
            // Chúng ta chỉ cần áp dụng style 'tag-view-simple' (màu hồng)
            // cho các context nhất định, giống project cũ.
            String type = item.getType();
            if (Objects.equals(type, "GENRE") || Objects.equals(type, "TAG")) {
                chip.getStyleClass().add("tag-view-simple");
            }
            // (Context STUDIO và PEOPLE sẽ dùng style .suggested-tag-button mặc định)

            chip.setUserData(item);
            chip.setOnAction(e -> {
                ensureSingleSelection(pane, chip);
                viewModel.selectSimpleSuggestion(item);
                okButton.requestFocus();
            });
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

    /**
     * Helper đảm bảo chỉ có một ToggleButton trong FlowPane được chọn (setSelected(true)).
     */
    private void ensureSingleSelection(FlowPane pane, ToggleButton selectedButton) {
        Platform.runLater(() -> {
            for (Node node : pane.getChildren()) {
                if (node instanceof ToggleButton && node != selectedButton) {
                    // Buộc tất cả các nút khác phải bị bỏ chọn
                    ((ToggleButton) node).setSelected(false);
                }
            }
            // Đảm bảo nút hiện tại được chọn (phòng trường hợp người dùng click để bỏ chọn)
            selectedButton.setSelected(true);
        });
    }
}