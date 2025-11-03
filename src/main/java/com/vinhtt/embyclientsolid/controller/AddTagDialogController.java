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
 * Controller (View) cho AddTagDialog.fxml.
 * Lớp này chỉ chịu trách nhiệm binding UI components với ViewModel và
 * ủy thác (delegate) các hành động của người dùng (như click, gõ phím) cho ViewModel xử lý.
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

    // Định danh CSS cho chip đang được focus (điều hướng bằng phím)
    private static final String FOCUSED_CHIP_STYLE_CLASS = "focused-chip";

    /**
     * Khởi tạo Controller (mặc định).
     */
    public AddTagDialogController() {
    }

    @FXML
    public void initialize() {
        // Logic binding được chuyển sang setViewModel() để đảm bảo ViewModel đã được tiêm (inject).
    }

    /**
     * Tiêm (inject) ViewModel, Stage và ConfigService vào Controller.
     * Đây là phương thức khởi tạo chính cho Controller này, được gọi bởi AppNavigator.
     *
     * @param viewModel   ViewModel chứa logic và trạng thái của dialog.
     * @param dialogStage Stage (cửa sổ) của dialog này.
     * @param configService Service để lấy chuỗi I18n.
     */
    public void setViewModel(IAddTagViewModel viewModel, Stage dialogStage, IConfigurationService configService) {
        this.viewModel = viewModel;
        this.dialogStage = dialogStage;
        this.configService = configService;
        bindViewModel();
    }

    /**
     * Kết nối tất cả các UI components với các thuộc tính (Properties) trong ViewModel.
     * Cài đặt lắng nghe sự kiện và ủy thác cho ViewModel.
     */
    private void bindViewModel() {
        // --- Binding I18n Labels (Lấy từ VM) ---
        // Các label này thay đổi dựa trên Context (Tag, Studio, People...)
        titleLabel.textProperty().bind(viewModel.titleProperty());
        contentLabel.textProperty().bind(viewModel.simpleLabelProperty());
        suggestionSimpleLabel.textProperty().bind(viewModel.simpleSuggestionLabelProperty());
        simpleTagRadio.textProperty().bind(viewModel.simpleLabelProperty());

        // --- Binding I18n Labels (Lấy từ ConfigService) ---
        // Các label này cố định
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
        // Liên kết nút radio "Simple" với thuộc tính simpleModeProperty của VM
        simpleTagRadio.selectedProperty().bindBidirectional(viewModel.simpleModeProperty());

        // --- Binding Chế độ Hiển thị (Pane) ---
        // Hiển thị pane "Simple" nếu simpleMode là true
        simpleTagPane.visibleProperty().bind(viewModel.simpleModeProperty());
        simpleTagPane.managedProperty().bind(viewModel.simpleModeProperty());
        // Hiển thị pane "JSON" nếu simpleMode là false
        jsonTagPane.visibleProperty().bind(viewModel.simpleModeProperty().not());
        jsonTagPane.managedProperty().bind(viewModel.simpleModeProperty().not());

        // --- Binding Trường Text (2 chiều) ---
        simpleNameField.textProperty().bindBidirectional(viewModel.simpleNameProperty());
        keyField.textProperty().bindBidirectional(viewModel.keyProperty());
        valueField.textProperty().bindBidirectional(viewModel.valueProperty());
        copyIdField.textProperty().bindBidirectional(viewModel.copyIdProperty());

        // Liên kết label trạng thái copy
        // copyStatusLabel.textProperty().bind(viewModel.copyStatusProperty());

        // --- Binding FlowPanes Gợi ý ---
        // Tự động cập nhật UI khi danh sách gợi ý trong VM thay đổi
        viewModel.getSuggestionKeys().addListener((ListChangeListener<String>) c ->
                populateKeySuggestions(suggestionKeysPane, viewModel.getSuggestionKeys()));
        viewModel.getSuggestionValues().addListener((ListChangeListener<Tag>) c ->
                populateValueSuggestions(suggestionValuesPane, viewModel.getSuggestionValues()));
        viewModel.getSuggestionSimple().addListener((ListChangeListener<SuggestionItem>) c ->
                populateSimpleSuggestions(suggestionSimplePane, viewModel.getSuggestionSimple()));

        // --- Binding Focus (Điều hướng phím) ---
        // Lắng nghe thuộc tính index focus của VM để highlight chip tương ứng
        bindChipFocus(suggestionKeysPane, viewModel.focusedKeyIndexProperty());
        bindChipFocus(suggestionValuesPane, viewModel.focusedValueIndexProperty());
        bindChipFocus(suggestionSimplePane, viewModel.focusedSimpleIndexProperty());

        // --- Chuyển tiếp (Delegate) Sự kiện Phím ---
        // Gửi sự kiện gõ phím (UP, DOWN, ENTER, TAB) từ các trường text đến VM
        simpleNameField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            viewModel.handleFieldKeyEvent(e, "simple");
            // Xử lý TAB thủ công để chuyển focus
            if (e.getCode() == KeyCode.TAB && !e.isShiftDown()) {
                valueField.requestFocus();
            }
        });
        keyField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            viewModel.handleFieldKeyEvent(e, "key");
            if (e.getCode() == KeyCode.TAB && !e.isShiftDown()) {
                valueField.requestFocus();
            }
            // Nếu nhấn Enter ở chế độ Simple (đã bị ẩn), chuyển focus về simpleNameField
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
        // Bắt phím ESCAPE để đóng dialog
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                viewModel.cancelCommand();
                e.consume();
            }
        });

        // --- Chuyển tiếp (Delegate) Sự kiện Mất Focus ---
        // Báo cho VM biết khi người dùng click ra ngoài một trường text
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
        // Tự động đóng dialog khi VM ra lệnh
        viewModel.closeDialogProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> dialogStage.close());
            }
        });

        // --- Focus ban đầu ---
        // Tự động focus vào trường text thích hợp khi mở dialog
        Platform.runLater(() -> {
            if (viewModel.simpleModeProperty().get()) {
                simpleNameField.requestFocus();
            } else {
                keyField.requestFocus();
            }
        });
    }

    // --- Helpers tạo Chip Gợi ý ---

    /**
     * Tạo các chip (ToggleButton) cho danh sách Key gợi ý (JSON).
     *
     * @param pane FlowPane để chứa các chip.
     * @param keys Danh sách các Key (String).
     */
    private void populateKeySuggestions(FlowPane pane, ObservableList<String> keys) {
        pane.getChildren().clear();
        for (String key : keys) {
            ToggleButton chip = new ToggleButton(key);
            chip.getStyleClass().add("suggestion-key-button");
            chip.setUserData(key);
            // Khi click, báo cho VM biết key được chọn
            chip.setOnAction(e -> {
                ensureSingleSelection(pane, chip);
                viewModel.selectKeySuggestion(key);
                valueField.requestFocus(); // Chuyển focus sang trường Value
            });
            pane.getChildren().add(chip);
        }
    }

    /**
     * Tạo các chip (ToggleButton) cho danh sách Value gợi ý (JSON).
     *
     * @param pane FlowPane để chứa các chip.
     * @param tags Danh sách các Tag (chứa Key-Value).
     */
    private void populateValueSuggestions(FlowPane pane, ObservableList<Tag> tags) {
        pane.getChildren().clear();
        for (Tag tag : tags) {
            ToggleButton chip = new ToggleButton(tag.getValue());
            chip.getStyleClass().addAll("suggested-tag-button", "tag-view-json");
            chip.setUserData(tag);
            // Khi click, báo cho VM biết Tag (Value) được chọn
            chip.setOnAction(e -> {
                ensureSingleSelection(pane, chip);
                viewModel.selectValueSuggestion(tag);
                okButton.requestFocus(); // Chuyển focus sang nút OK
            });
            pane.getChildren().add(chip);
        }
    }

    /**
     * Tạo các chip (ToggleButton) cho danh sách gợi ý Đơn giản (Simple).
     *
     * @param pane FlowPane để chứa các chip.
     * @param items Danh sách các SuggestionItem.
     */
    private void populateSimpleSuggestions(FlowPane pane, ObservableList<SuggestionItem> items) {
        pane.getChildren().clear();
        for (SuggestionItem item : items) {
            ToggleButton chip = new ToggleButton(item.getName());
            chip.getStyleClass().add("suggested-tag-button");

            // Áp dụng style màu hồng cho Tag và Genre
            String type = item.getType();
            if (Objects.equals(type, "GENRE") || Objects.equals(type, "TAG")) {
                chip.getStyleClass().add("tag-view-simple");
            }
            // (Context STUDIO và PEOPLE sẽ dùng style .suggested-tag-button mặc định)

            chip.setUserData(item);
            // Khi click, báo cho VM biết item được chọn
            chip.setOnAction(e -> {
                ensureSingleSelection(pane, chip);
                viewModel.selectSimpleSuggestion(item);
                okButton.requestFocus(); // Chuyển focus sang nút OK
            });
            pane.getChildren().add(chip);
        }
    }

    /**
     * Lắng nghe VM và áp dụng/xóa style CSS 'focused-chip' cho chip
     * dựa trên chỉ số (index) focus.
     *
     * @param pane FlowPane chứa các chip.
     * @param focusIndex Thuộc tính (ReadOnlyIntegerProperty) chứa chỉ số chip đang được focus.
     */
    private void bindChipFocus(FlowPane pane, ReadOnlyIntegerProperty focusIndex) {
        focusIndex.addListener((obs, oldIndex, newIndex) -> {
            Platform.runLater(() -> {
                int idx = newIndex.intValue();
                // Duyệt qua tất cả các chip con
                for (int i = 0; i < pane.getChildren().size(); i++) {
                    Node child = pane.getChildren().get(i);
                    if (i == idx) {
                        // Thêm style nếu là chip được focus
                        if (!child.getStyleClass().contains(FOCUSED_CHIP_STYLE_CLASS)) {
                            child.getStyleClass().add(FOCUSED_CHIP_STYLE_CLASS);
                        }
                    } else {
                        // Xóa style khỏi các chip khác
                        child.getStyleClass().remove(FOCUSED_CHIP_STYLE_CLASS);
                    }
                }
            });
        });
    }

    /**
     * Đánh dấu (highlight) trường text không hợp lệ nếu người dùng nhấn OK
     * mà VM báo lỗi (chưa có kết quả).
     */
    private void highlightInvalidField() {
        TextField fieldToHighlight = null;

        // Xác định trường nào cần highlight dựa trên chế độ (mode)
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

        // Áp dụng style lỗi và focus vào trường đó
        if (fieldToHighlight != null) {
            final TextField finalField = fieldToHighlight;
            Platform.runLater(() -> {
                finalField.requestFocus();
                finalField.getStyleClass().add("validation-error");
                // (Có thể thêm PauseTransition để xóa style sau vài giây)
            });
        }
    }

    /**
     * Xử lý sự kiện onAction của nút OK (được gọi từ FXML).
     * Ủy thác cho ViewModel.
     */
    @FXML
    private void handleOk() {
        viewModel.okCommand();
        // Nếu VM báo rằng kết quả không hợp lệ (result == null), highlight trường lỗi
        if (viewModel.getResult() == null) {
            highlightInvalidField();
        }
    }

    /**
     * Xử lý sự kiện onAction của nút Cancel (được gọi từ FXML).
     * Ủy thác cho ViewModel.
     */
    @FXML
    private void handleCancel() {
        viewModel.cancelCommand();
    }

    /**
     * Đảm bảo chỉ có một ToggleButton trong FlowPane được chọn (setSelected(true)).
     * Ngăn người dùng bỏ chọn (deselect) chip.
     *
     * @param pane FlowPane chứa các chip.
     * @param selectedButton Chip vừa được click.
     */
    private void ensureSingleSelection(FlowPane pane, ToggleButton selectedButton) {
        Platform.runLater(() -> {
            // Bỏ chọn tất cả các nút khác
            for (Node node : pane.getChildren()) {
                if (node instanceof ToggleButton && node != selectedButton) {
                    ((ToggleButton) node).setSelected(false);
                }
            }
            // Đảm bảo nút hiện tại luôn được chọn
            selectedButton.setSelected(true);
        });
    }
}