package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.data.IStaticDataRepository;
import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.AddTagResult;
import com.vinhtt.embyclientsolid.viewmodel.IAddTagViewModel;
import com.vinhtt.embyclientsolid.core.IConfigurationService;
import embyclient.model.BaseItemDto;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Triển khai của {@link IAddTagViewModel}.
 * Quản lý trạng thái và logic nghiệp vụ cho dialog "Add Tag", bao gồm
 * quản lý chế độ (Simple/JSON), tải và lọc gợi ý (UR-35),
 * và xử lý điều hướng bằng phím phức tạp.
 */
public class AddTagViewModel implements IAddTagViewModel {

    // --- Services (DI) ---
    private final IStaticDataRepository staticDataRepository;
    private final IItemRepository itemRepository; // Dùng cho chức năng "Copy by ID"
    private final IConfigurationService configService;

    // --- Trạng thái nội bộ ---
    private SuggestionContext currentContext = SuggestionContext.TAG;
    private AddTagResult result = null; // Kết quả trả về khi dialog đóng
    // Cờ (flag) để ngăn các listener kích hoạt khi code đang tự cập nhật text field
    private boolean isUpdatingProgrammatically = false;

    // --- Dữ liệu Gợi ý (Nguồn) ---
    // Map chứa các gợi ý JSON (Key -> List<Tag>)
    private Map<String, List<Tag>> jsonGroups = new HashMap<>();
    // List chứa tất cả gợi ý Simple
    private List<SuggestionItem> allSimpleSuggestions = new ArrayList<>();

    // --- Properties (Binding) ---
    private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper simpleLabel = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper simpleSuggestionLabel = new ReadOnlyStringWrapper();
    private final BooleanProperty simpleMode = new SimpleBooleanProperty(false);
    private final StringProperty simpleName = new SimpleStringProperty("");
    private final StringProperty key = new SimpleStringProperty("");
    private final StringProperty value = new SimpleStringProperty("");
    private final StringProperty copyId = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper copyStatus = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper closeDialog = new ReadOnlyBooleanWrapper(false);

    // --- Properties Điều hướng Phím (Binding) ---
    private final ReadOnlyIntegerWrapper focusedKeyIndex = new ReadOnlyIntegerWrapper(-1);
    private final ReadOnlyIntegerWrapper focusedValueIndex = new ReadOnlyIntegerWrapper(-1);
    private final ReadOnlyIntegerWrapper focusedSimpleIndex = new ReadOnlyIntegerWrapper(-1);

    // --- ObservableLists (Binding) ---
    private final ObservableList<String> suggestionKeys = FXCollections.observableArrayList();
    private final ObservableList<Tag> suggestionValues = FXCollections.observableArrayList();
    private final ObservableList<SuggestionItem> suggestionSimple = FXCollections.observableArrayList();

    public AddTagViewModel(IStaticDataRepository staticDataRepository, IItemRepository itemRepository, IConfigurationService configService) {
        this.staticDataRepository = staticDataRepository;
        this.itemRepository = itemRepository;
        this.configService = configService;
        setupListeners();
    }

    /**
     * Cài đặt các listener để tự động lọc danh sách gợi ý
     * khi người dùng gõ vào các trường text.
     */
    private void setupListeners() {
        // Khi chuyển chế độ (Simple/JSON), reset focus
        simpleMode.addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedKeyIndex.set(-1);
            focusedValueIndex.set(-1);
            focusedSimpleIndex.set(-1);
        });

        // Khi gõ vào trường Key (JSON)
        keyProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedKeyIndex.set(-1);
            populateKeys(newVal);
            populateSimpleTags(newVal); // Cũng tìm kiếm nhanh ở chế độ Simple
        });

        // Khi gõ vào trường Value (JSON)
        valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedValueIndex.set(-1);
            populateValues(key.get(), newVal);
        });

        // Khi gõ vào trường Simple Name
        simpleNameProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedSimpleIndex.set(-1);
            populateSimpleTags(newVal);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContext(SuggestionContext context) {
        this.currentContext = context;
        loadSuggestedTags(); // Bắt đầu tải dữ liệu gợi ý
        updateLabels(context); // Cập nhật tiêu đề, nhãn
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AddTagResult getResult() {
        return result;
    }

    /**
     * Cập nhật các chuỗi I18n (Tiêu đề, Nhãn) dựa trên context (UR-11).
     */
    private void updateLabels(SuggestionContext context) {
        switch (context) {
            case STUDIO:
                title.set(configService.getString("addTagDialog", "addStudioTitle"));
                simpleLabel.set(configService.getString("addTagDialog", "labelSimpleStudio"));
                simpleSuggestionLabel.set(configService.getString("addTagDialog", "suggestionSimpleStudioLabel"));
                break;
            case PEOPLE:
                title.set(configService.getString("addTagDialog", "addPeopleTitle"));
                simpleLabel.set(configService.getString("addTagDialog", "labelSimplePeople"));
                simpleSuggestionLabel.set(configService.getString("addTagDialog", "suggestionSimplePeopleLabel"));
                break;
            case GENRE:
                title.set(configService.getString("addTagDialog", "addGenreTitle"));
                simpleLabel.set(configService.getString("addTagDialog", "labelSimpleGenre"));
                simpleSuggestionLabel.set(configService.getString("addTagDialog", "suggestionSimpleGenreLabel"));
                break;
            case TAG:
            default:
                title.set(configService.getString("addTagDialog", "addTagTitle"));
                simpleLabel.set(configService.getString("addTagDialog", "labelSimple"));
                simpleSuggestionLabel.set(configService.getString("addTagDialog", "suggestionSimpleLabel"));
                break;
        }
    }

    /**
     * Tải dữ liệu gợi ý (Tags, Studios, People, Genres) từ Repository
     * trên một luồng nền (background thread).
     */
    private void loadSuggestedTags() {
        new Thread(() -> {
            try {
                // 1. Tải dữ liệu thô (List<Tag>)
                List<Tag> allRawTags = new ArrayList<>();

                // Gọi đúng hàm repository dựa trên context
                switch (currentContext) {
                    case TAG:
                        allRawTags.addAll(staticDataRepository.getAllUsedTags());
                        break;
                    case STUDIO:
                        allRawTags.addAll(staticDataRepository.getStudioSuggestions());
                        break;
                    case PEOPLE:
                        allRawTags.addAll(staticDataRepository.getPeopleSuggestions());
                        break;
                    case GENRE:
                        allRawTags.addAll(staticDataRepository.getGenreSuggestions());
                        break;
                }

                // 2. Xử lý dữ liệu thô: Tách thành 2 nhóm (JSON và Simple)
                Map<String, List<Tag>> tempJsonGroups = allRawTags.stream()
                        .filter(Tag::isJson)
                        .collect(Collectors.groupingBy(Tag::getKey));

                List<SuggestionItem> simpleSuggestions = allRawTags.stream()
                        .filter(t -> !t.isJson())
                        // Chuyển đổi Tag (model) sang SuggestionItem (model cho UI)
                        .map(t -> new SuggestionItem(t.getDisplayName(), t.getId(), currentContext.name()))
                        .collect(Collectors.toList());

                // 3. Cập nhật UI (phải chạy trên luồng JavaFX)
                Platform.runLater(() -> {
                    this.jsonGroups = tempJsonGroups;
                    this.allSimpleSuggestions = simpleSuggestions.stream().distinct().collect(Collectors.toList());

                    // 4. Populate (điền) danh sách gợi ý lần đầu
                    isUpdatingProgrammatically = true;
                    populateKeys(key.get());
                    populateSimpleTags(simpleName.get());
                    isUpdatingProgrammatically = false;
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- Logic Lọc Gợi ý (chạy trên luồng JavaFX) ---

    /**
     * Lọc và hiển thị danh sách "Key" (JSON) dựa trên text tìm kiếm.
     */
    private void populateKeys(String searchText) {
        List<String> filteredKeys;
        if (searchText == null || searchText.trim().isEmpty()) {
            // Nếu không tìm kiếm -> hiển thị tất cả
            filteredKeys = new ArrayList<>(jsonGroups.keySet());
        } else {
            // Nếu có tìm kiếm -> lọc
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredKeys = jsonGroups.keySet().stream()
                    .filter(k -> k.toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }
        Collections.sort(filteredKeys, String.CASE_INSENSITIVE_ORDER);
        suggestionKeys.setAll(filteredKeys); // Cập nhật ObservableList
        populateValues(key.get(), value.get()); // Tự động cập nhật danh sách Value
    }

    /**
     * Lọc và hiển thị danh sách "Value" (JSON) dựa trên Key đang chọn và text tìm kiếm.
     */
    private void populateValues(String currentKey, String searchValue) {
        List<Tag> tagsInGroup = jsonGroups.get(currentKey);
        if (tagsInGroup == null) {
            suggestionValues.clear(); // Không có Key nào khớp -> danh sách Value rỗng
            return;
        }
        final List<Tag> filteredTags;
        if (searchValue == null || searchValue.trim().isEmpty()) {
            filteredTags = tagsInGroup;
        } else {
            String lowerCaseSearchText = searchValue.trim().toLowerCase();
            filteredTags = tagsInGroup.stream()
                    .filter(pt -> pt.getValue().toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }
        suggestionValues.setAll(filteredTags); // Cập nhật ObservableList
    }

    /**
     * Lọc và hiển thị danh sách "Simple" dựa trên text tìm kiếm.
     */
    private void populateSimpleTags(String searchText) {
        final List<SuggestionItem> filtered;
        if (searchText == null || searchText.trim().isEmpty()) {
            filtered = allSimpleSuggestions;
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filtered = allSimpleSuggestions.stream()
                    .filter(item -> item.getName().toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }
        suggestionSimple.setAll(filtered.stream().distinct().collect(Collectors.toList()));
    }

    // --- Logic Điều hướng Phím (UR-35) ---

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleFieldKeyEvent(KeyEvent event, String sourceField) {
        KeyCode code = event.getCode();

        if (code == KeyCode.UP || code == KeyCode.DOWN) {
            event.consume(); // Ngăn con trỏ di chuyển trong text field
            handleArrowKeys(code, sourceField);
        } else if (code == KeyCode.TAB && !event.isShiftDown()) {
            event.consume(); // Xử lý Tab thủ công
            handleTabKey(sourceField);
        } else if (code == KeyCode.ENTER) {
            event.consume(); // Xử lý Enter thủ công
            handleEnterKey(sourceField);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleFocusLost(String sourceField) {
        if (isUpdatingProgrammatically) return;

        // Xử lý auto-complete khi focus rời khỏi trường "Key" (JSON)
        if ("key".equals(sourceField) && focusedKeyIndex.get() == -1) {
            String currentKeyText = key.get().trim();
            if (!currentKeyText.isEmpty()) {
                // Tìm key khớp (không phân biệt hoa thường)
                String correctCaseKey = null;
                int matchIndex = -1;
                for (int i = 0; i < suggestionKeys.size(); i++) {
                    String toggleKey = suggestionKeys.get(i);
                    if (toggleKey.equalsIgnoreCase(currentKeyText)) {
                        correctCaseKey = toggleKey;
                        matchIndex = i;
                        break;
                    }
                }

                if (correctCaseKey != null) {
                    // Tự động sửa hoa/thường và cập nhật UI
                    isUpdatingProgrammatically = true;
                    key.set(correctCaseKey); // Auto-complete
                    isUpdatingProgrammatically = false;

                    populateValues(correctCaseKey, ""); // Populate values
                    focusedKeyIndex.set(matchIndex); // Set focus index
                }
            }
        }
    }

    /**
     * Xử lý phím mũi tên LÊN/XUỐNG (UR-35).
     */
    private void handleArrowKeys(KeyCode code, String sourceField) {
        if (simpleMode.get()) {
            // Chế độ Simple
            int nextIndex = navigateChipList(focusedSimpleIndex.get(), suggestionSimple.size(), code);
            focusedSimpleIndex.set(nextIndex); // Cập nhật index -> View sẽ highlight chip
            if (nextIndex != -1) {
                // Tự động điền text field với chip được focus
                SuggestionItem item = suggestionSimple.get(nextIndex);
                isUpdatingProgrammatically = true;
                simpleName.set(item.getName());
                isUpdatingProgrammatically = false;
            }
        } else {
            // Chế độ JSON
            if ("key".equals(sourceField)) {
                int nextIndex = navigateChipList(focusedKeyIndex.get(), suggestionKeys.size(), code);
                focusedKeyIndex.set(nextIndex);
                if (nextIndex != -1) {
                    String keyText = suggestionKeys.get(nextIndex);
                    isUpdatingProgrammatically = true;
                    key.set(keyText);
                    isUpdatingProgrammatically = false;
                    populateValues(keyText, ""); // Tải lại Values khi Key thay đổi
                    focusedValueIndex.set(-1);
                }
            } else if ("value".equals(sourceField)) {
                int nextIndex = navigateChipList(focusedValueIndex.get(), suggestionValues.size(), code);
                focusedValueIndex.set(nextIndex);
                if (nextIndex != -1) {
                    Tag tag = suggestionValues.get(nextIndex);
                    isUpdatingProgrammatically = true;
                    value.set(tag.getValue());
                    isUpdatingProgrammatically = false;
                }
            }
        }
    }

    /**
     * Helper tính toán index tiếp theo (có vòng lặp) khi nhấn Lên/Xuống.
     */
    private int navigateChipList(int currentIndex, int listSize, KeyCode code) {
        if (listSize == 0) return -1;
        int nextIndex = currentIndex;
        if (code == KeyCode.DOWN) {
            nextIndex++;
            if (nextIndex >= listSize) nextIndex = 0; // Vòng lại đầu
        } else { // KeyCode.UP
            nextIndex--;
            if (nextIndex < 0) nextIndex = listSize - 1; // Vòng lại cuối
        }
        return nextIndex;
    }

    /**
     * Xử lý phím TAB (UR-35).
     */
    private void handleTabKey(String sourceField) {
        if ("key".equals(sourceField)) {
            // Nếu đang focus 1 chip Key -> chọn nó
            if (focusedKeyIndex.get() != -1) {
                selectKeySuggestion(suggestionKeys.get(focusedKeyIndex.get()));
            } else {
                // Nếu không -> kích hoạt logic auto-complete (giống như blur)
                handleFocusLost("key");
            }
            // (Controller sẽ chuyển focus sang trường Value)
        } else if ("value".equals(sourceField)) {
            // Nếu đang focus 1 chip Value -> chọn nó
            if (focusedValueIndex.get() != -1) {
                selectValueSuggestion(suggestionValues.get(focusedValueIndex.get()));
            }
            // (Controller sẽ chuyển focus sang nút OK)
        } else if ("simple".equals(sourceField)) {
            // Nhấn TAB ở chế độ Simple -> chuyển sang chế độ JSON
            String currentSimpleText = simpleName.get();
            isUpdatingProgrammatically = true;
            simpleMode.set(false);
            key.set(currentSimpleText); // Chuyển text từ Simple -> Key
            simpleName.set("");
            value.set("");
            isUpdatingProgrammatically = false;

            populateKeys(currentSimpleText);
            populateSimpleTags(currentSimpleText);
            handleFocusLost("key"); // Kích hoạt auto-complete cho trường Key
        }
    }

    /**
     * Xử lý phím ENTER (UR-35).
     */
    private void handleEnterKey(String sourceField) {
        if (simpleMode.get()) {
            // Chế độ Simple
            if (focusedSimpleIndex.get() != -1) {
                // Nếu đang focus 1 chip Simple -> chọn nó
                selectSimpleSuggestion(suggestionSimple.get(focusedSimpleIndex.get()));
            }
            okCommand(); // Nhấn OK
        } else {
            // Chế độ JSON
            if ("key".equals(sourceField)) {
                if (value.get().trim().isEmpty()) {
                    // Nếu trường Value rỗng -> chuyển sang chế độ Simple
                    String keyText = key.get().trim();
                    if (focusedKeyIndex.get() != -1) {
                        keyText = suggestionKeys.get(focusedKeyIndex.get());
                    }
                    if (!keyText.isEmpty()) {
                        isUpdatingProgrammatically = true;
                        simpleMode.set(true);
                        simpleName.set(keyText); // Chuyển text từ Key -> Simple
                        key.set("");
                        value.set("");
                        isUpdatingProgrammatically = false;
                        populateSimpleTags(keyText);
                    } else {
                        cancelCommand(); // Nếu rỗng cả 2 -> Hủy
                    }
                } else {
                    okCommand(); // Nếu cả Key và Value đều có text -> Nhấn OK
                }
            } else if ("value".equals(sourceField)) {
                if (focusedValueIndex.get() != -1) {
                    // Nếu đang focus 1 chip Value -> chọn nó
                    selectValueSuggestion(suggestionValues.get(focusedValueIndex.get()));
                }
                okCommand(); // Nhấn OK
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectKeySuggestion(String key) {
        isUpdatingProgrammatically = true;
        this.key.set(key);
        isUpdatingProgrammatically = false;
        populateValues(key, ""); // Tải lại Values
        focusedValueIndex.set(-1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectValueSuggestion(Tag tag) {
        isUpdatingProgrammatically = true;
        this.simpleMode.set(false); // Đảm bảo đang ở chế độ JSON
        this.key.set(tag.getKey());
        this.value.set(tag.getValue());
        isUpdatingProgrammatically = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectSimpleSuggestion(SuggestionItem item) {
        isUpdatingProgrammatically = true;
        this.simpleMode.set(true); // Đảm bảo đang ở chế độ Simple
        this.simpleName.set(item.getName());
        isUpdatingProgrammatically = false;
    }

    // --- Commands (Nút bấm) ---

    /**
     * {@inheritDoc}
     */
    @Override
    public void okCommand() {
        this.result = null; // Reset kết quả
        if (simpleMode.get()) {
            // Chế độ Simple
            String name = simpleName.get();
            if (name != null && !name.trim().isEmpty()) {
                // Tìm xem tag này có ID không (nếu người dùng chọn từ gợi ý)
                // Bước 1: Tìm SuggestionItem trước (nếu có)
                java.util.Optional<SuggestionItem> matchingItem = allSimpleSuggestions.stream()
                        .filter(item -> item.getName().equalsIgnoreCase(name.trim()))
                        .findFirst();

                // Bước 2: Map sang ID một cách an toàn
                // Nếu matchingItem tồn tại, gọi getId() (có thể trả về null)
                // Nếu không, trả về null
                String id = matchingItem.map(SuggestionItem::getId).orElse(null);

                this.result = new AddTagResult(new Tag(name.trim(), id));
            }
        } else {
            // Chế độ JSON
            String k = key.get();
            String v = value.get();
            if (k != null && !k.trim().isEmpty() && v != null && !v.trim().isEmpty()) {
                // Khi tạo JSON thủ công, ID luôn là null
                this.result = new AddTagResult(new Tag(k.trim(), v.trim(), null));
            }
        }

        if (this.result != null) {
            closeDialog.set(true); // Bắn tín hiệu đóng dialog
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelCommand() {
        this.result = null;
        closeDialog.set(true); // Bắn tín hiệu đóng dialog
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyCommand() {
        String id = copyId.get();
        if (id != null && !id.trim().isEmpty()) {
            copyStatus.set(configService.getString("addTagDialog", "copyStatusLoading", id.trim()));
            // (Chạy nền để không block UI, mặc dù logic đã bị đơn giản hóa)
            new Thread(() -> {
                try {
                    // (Logic cũ phức tạp bị loại bỏ, giờ chỉ cần trả về ID)
                    Platform.runLater(() -> {
                        copyStatus.set(configService.getString("addTagDialog", "copyStatusAccepted"));
                        // Tạo kết quả dạng "Copy"
                        this.result = new AddTagResult(id.trim());
                        closeDialog.set(true); // Bắn tín hiệu đóng dialog
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> copyStatus.set(configService.getString("addTagDialog", "copyErrorNotFound")));
                }
            }).start();
        } else {
            copyStatus.set(configService.getString("addTagDialog", "copyErrorIdRequired"));
        }
    }

    // --- Getters cho Properties (Binding) ---
    @Override public ReadOnlyStringProperty titleProperty() { return title.getReadOnlyProperty(); }
    @Override public ReadOnlyStringProperty simpleLabelProperty() { return simpleLabel.getReadOnlyProperty(); }
    @Override public ReadOnlyStringProperty simpleSuggestionLabelProperty() { return simpleSuggestionLabel.getReadOnlyProperty(); }
    @Override public BooleanProperty simpleModeProperty() { return simpleMode; }
    @Override public StringProperty simpleNameProperty() { return simpleName; }
    @Override public StringProperty keyProperty() { return key; }
    @Override public StringProperty valueProperty() { return value; }
    @Override public StringProperty copyIdProperty() { return copyId; }
    @Override public ReadOnlyStringProperty copyStatusProperty() { return copyStatus.getReadOnlyProperty(); }
    @Override public ReadOnlyIntegerProperty focusedKeyIndexProperty() { return focusedKeyIndex.getReadOnlyProperty(); }
    @Override public ReadOnlyIntegerProperty focusedValueIndexProperty() { return focusedValueIndex.getReadOnlyProperty(); }
    @Override public ReadOnlyIntegerProperty focusedSimpleIndexProperty() { return focusedSimpleIndex.getReadOnlyProperty(); }
    @Override public ObservableList<String> getSuggestionKeys() { return suggestionKeys; }
    @Override public ObservableList<Tag> getSuggestionValues() { return suggestionValues; }
    @Override public ObservableList<SuggestionItem> getSuggestionSimple() { return suggestionSimple; }
    @Override public ReadOnlyBooleanProperty closeDialogProperty() { return closeDialog.getReadOnlyProperty(); }
}