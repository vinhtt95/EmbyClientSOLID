package com.vinhtt.embyclientsolid.viewmodel.impl;

import com.vinhtt.embyclientsolid.data.IItemRepository;
import com.vinhtt.embyclientsolid.data.IStaticDataRepository;
import com.vinhtt.embyclientsolid.model.SuggestionContext;
import com.vinhtt.embyclientsolid.model.SuggestionItem;
import com.vinhtt.embyclientsolid.model.Tag;
import com.vinhtt.embyclientsolid.viewmodel.AddTagResult;
import com.vinhtt.embyclientsolid.viewmodel.IAddTagViewModel;
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
 * Triển khai (Implementation) của IAddTagViewModel.
 * (Cập nhật: Sửa lỗi .clear() VÀ port logic auto-complete (focus lost)).
 */
public class AddTagViewModel implements IAddTagViewModel {

    // --- Services (DI) ---
    private final IStaticDataRepository staticDataRepository;
    private final IItemRepository itemRepository; // Dùng cho Copy by ID

    // --- Trạng thái nội bộ ---
    private SuggestionContext currentContext = SuggestionContext.TAG;
    private AddTagResult result = null;
    private boolean isUpdatingProgrammatically = false;

    // --- Dữ liệu Gợi ý (Nguồn) ---
    private Map<String, List<Tag>> jsonGroups = new HashMap<>();
    private List<SuggestionItem> allSimpleSuggestions = new ArrayList<>();

    // --- Properties (Binding) ---
    private final ReadOnlyStringWrapper title = new ReadOnlyStringWrapper("Thêm Tag");
    private final ReadOnlyStringWrapper simpleLabel = new ReadOnlyStringWrapper("Tag Đơn giản");
    private final ReadOnlyStringWrapper simpleSuggestionLabel = new ReadOnlyStringWrapper("Tag Đơn giản");
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

    public AddTagViewModel(IStaticDataRepository staticDataRepository, IItemRepository itemRepository) {
        this.staticDataRepository = staticDataRepository;
        this.itemRepository = itemRepository;
        setupListeners();
    }

    private void setupListeners() {
        simpleMode.addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedKeyIndex.set(-1);
            focusedValueIndex.set(-1);
            focusedSimpleIndex.set(-1);
        });

        keyProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedKeyIndex.set(-1);
            populateKeys(newVal);
            populateSimpleTags(newVal); // Quick search
        });

        valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedValueIndex.set(-1);
            populateValues(key.get(), newVal);
        });

        simpleNameProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedSimpleIndex.set(-1);
            populateSimpleTags(newVal);
        });
    }

    @Override
    public void setContext(SuggestionContext context) {
        this.currentContext = context;
        loadSuggestedTags();
        updateLabels(context);
    }

    @Override
    public AddTagResult getResult() {
        return result;
    }

    private void updateLabels(SuggestionContext context) {
        // (Tạm thời hardcode, GĐ 13 sẽ dùng IConfigurationService)
        switch (context) {
            case STUDIO:
                title.set("Thêm Studio Mới");
                simpleLabel.set("Tên Studio");
                simpleSuggestionLabel.set("Studios");
                break;
            case PEOPLE:
                title.set("Thêm Người Mới");
                simpleLabel.set("Tên Người");
                simpleSuggestionLabel.set("People");
                break;
            case GENRE:
                title.set("Thêm Thể Loại Mới");
                simpleLabel.set("Tên Thể Loại");
                simpleSuggestionLabel.set("Genres");
                break;
            case TAG:
            default:
                title.set("Thêm Tag Mới");
                simpleLabel.set("Tag Đơn giản");
                simpleSuggestionLabel.set("Tag Đơn giản");
                break;
        }
    }

    private void loadSuggestedTags() {
        new Thread(() -> {
            try {
                List<Tag> allRawTags = new ArrayList<>();
                List<SuggestionItem> simpleSuggestions = new ArrayList<>();

                switch (currentContext) {
                    case TAG:
                        allRawTags.addAll(staticDataRepository.getAllUsedTags());
                        break;
                    case STUDIO:
                        simpleSuggestions.addAll(staticDataRepository.getStudioSuggestions());
                        break;
                    case PEOPLE:
                        simpleSuggestions.addAll(staticDataRepository.getPeopleSuggestions());
                        break;
                    case GENRE:
                        simpleSuggestions.addAll(staticDataRepository.getGenreSuggestions());
                        break;
                }

                Map<String, List<Tag>> tempJsonGroups = allRawTags.stream()
                        .filter(Tag::isJson)
                        .collect(Collectors.groupingBy(Tag::getKey));

                if (currentContext == SuggestionContext.TAG) {
                    List<SuggestionItem> simpleTagsFromJson = allRawTags.stream()
                            .filter(t -> !t.isJson())
                            .map(t -> new SuggestionItem(t.getDisplayName(), t.getId(), "Tag"))
                            .collect(Collectors.toList());
                    simpleSuggestions.addAll(simpleTagsFromJson);
                }

                Platform.runLater(() -> {
                    this.jsonGroups = tempJsonGroups;
                    this.allSimpleSuggestions = simpleSuggestions;
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

    // --- Logic Lọc Gợi ý ---

    private void populateKeys(String searchText) {
        List<String> filteredKeys;
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredKeys = new ArrayList<>(jsonGroups.keySet());
        } else {
            String lowerCaseSearchText = searchText.trim().toLowerCase();
            filteredKeys = jsonGroups.keySet().stream()
                    .filter(k -> k.toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
        }
        Collections.sort(filteredKeys, String.CASE_INSENSITIVE_ORDER);
        suggestionKeys.setAll(filteredKeys);
        // Tự động cập nhật value suggestions dựa trên key hiện tại (ngay cả khi chưa chọn)
        populateValues(key.get(), value.get());
    }

    private void populateValues(String currentKey, String searchValue) {
        List<Tag> tagsInGroup = jsonGroups.get(currentKey);
        if (tagsInGroup == null) {
            suggestionValues.clear();
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
        suggestionValues.setAll(filteredTags);
    }

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

    @Override
    public void handleFieldKeyEvent(KeyEvent event, String sourceField) {
        KeyCode code = event.getCode();

        if (code == KeyCode.UP || code == KeyCode.DOWN) {
            event.consume();
            handleArrowKeys(code, sourceField);
        } else if (code == KeyCode.TAB && !event.isShiftDown()) {
            event.consume();
            handleTabKey(sourceField);
        } else if (code == KeyCode.ENTER) {
            event.consume();
            handleEnterKey(sourceField);
        }
    }

    /**
     * (MỚI - SỬA LỖI) Xử lý logic auto-complete khi mất focus (blur).
     */
    @Override
    public void handleFocusLost(String sourceField) {
        if (isUpdatingProgrammatically) return;

        // Đây là logic auto-complete mà bạn đã chỉ ra
        if ("key".equals(sourceField) && focusedKeyIndex.get() == -1) {
            String currentKeyText = key.get().trim();
            if (!currentKeyText.isEmpty()) {
                String correctCaseKey = null;
                int matchIndex = -1;
                // Tìm trong danh sách gợi ý *hiện tại*
                for (int i = 0; i < suggestionKeys.size(); i++) {
                    String toggleKey = suggestionKeys.get(i);
                    if (toggleKey.equalsIgnoreCase(currentKeyText)) {
                        correctCaseKey = toggleKey;
                        matchIndex = i;
                        break;
                    }
                }

                if (correctCaseKey != null) {
                    isUpdatingProgrammatically = true;
                    key.set(correctCaseKey); // Auto-complete
                    isUpdatingProgrammatically = false;

                    populateValues(correctCaseKey, ""); // Populate values
                    focusedKeyIndex.set(matchIndex); // Set focus index
                }
            }
        }
    }


    private void handleArrowKeys(KeyCode code, String sourceField) {
        if (simpleMode.get()) {
            int nextIndex = navigateChipList(focusedSimpleIndex.get(), suggestionSimple.size(), code);
            focusedSimpleIndex.set(nextIndex);
            if (nextIndex != -1) {
                SuggestionItem item = suggestionSimple.get(nextIndex);
                isUpdatingProgrammatically = true;
                simpleName.set(item.getName());
                isUpdatingProgrammatically = false;
            }
        } else {
            if ("key".equals(sourceField)) {
                int nextIndex = navigateChipList(focusedKeyIndex.get(), suggestionKeys.size(), code);
                focusedKeyIndex.set(nextIndex);
                if (nextIndex != -1) {
                    String keyText = suggestionKeys.get(nextIndex);
                    isUpdatingProgrammatically = true;
                    key.set(keyText);
                    isUpdatingProgrammatically = false;
                    populateValues(keyText, "");
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

    private int navigateChipList(int currentIndex, int listSize, KeyCode code) {
        if (listSize == 0) return -1;
        int nextIndex = currentIndex;
        if (code == KeyCode.DOWN) {
            nextIndex++;
            if (nextIndex >= listSize) nextIndex = 0; // Vòng
        } else { // KeyCode.UP
            nextIndex--;
            if (nextIndex < 0) nextIndex = listSize - 1; // Vòng
        }
        return nextIndex;
    }

    private void handleTabKey(String sourceField) {
        if ("key".equals(sourceField)) {
            // Auto-chọn key nếu đang focus chip
            if (focusedKeyIndex.get() != -1) {
                selectKeySuggestion(suggestionKeys.get(focusedKeyIndex.get()));
            } else {
                // (LOGIC MỚI) Nếu không focus chip, chạy logic auto-complete y như focus lost
                handleFocusLost("key");
            }
            // (Controller sẽ chuyển focus qua valueField)
        } else if ("value".equals(sourceField)) {
            // Auto-chọn value nếu đang focus chip
            if (focusedValueIndex.get() != -1) {
                selectValueSuggestion(suggestionValues.get(focusedValueIndex.get()));
            }
            // (Controller sẽ chuyển focus qua okButton)
        } else if ("simple".equals(sourceField)) {
            // Chuyển từ Simple -> JSON
            String currentSimpleText = simpleName.get();
            isUpdatingProgrammatically = true;
            simpleMode.set(false); // Chuyển radio
            key.set(currentSimpleText);

            // --- SỬA LỖI 1 ---
            simpleName.set(""); // Dùng .set("")
            value.set("");      // Dùng .set("")
            // --- KẾT THÚC SỬA LỖI 1 ---

            isUpdatingProgrammatically = false;

            populateKeys(currentSimpleText);
            populateSimpleTags(currentSimpleText);
            // (Controller sẽ chuyển focus qua keyField)
        }
    }

    private void handleEnterKey(String sourceField) {
        if (simpleMode.get()) {
            if (focusedSimpleIndex.get() != -1) {
                selectSimpleSuggestion(suggestionSimple.get(focusedSimpleIndex.get()));
            }
            okCommand();
        } else {
            if ("key".equals(sourceField)) {
                if (value.get().trim().isEmpty()) {
                    String keyText = key.get().trim();
                    if (focusedKeyIndex.get() != -1) {
                        keyText = suggestionKeys.get(focusedKeyIndex.get());
                    }
                    if (!keyText.isEmpty()) {
                        isUpdatingProgrammatically = true;
                        simpleMode.set(true);
                        simpleName.set(keyText);

                        // --- SỬA LỖI 2 ---
                        key.set("");   // Dùng .set("")
                        value.set(""); // Dùng .set("")
                        // --- KẾT THÚC SỬA LỖI 2 ---

                        isUpdatingProgrammatically = false;

                        populateSimpleTags(keyText);
                        // (Controller sẽ chuyển focus)
                    } else {
                        cancelCommand();
                    }
                } else {
                    okCommand();
                }
            } else if ("value".equals(sourceField)) {
                if (focusedValueIndex.get() != -1) {
                    selectValueSuggestion(suggestionValues.get(focusedValueIndex.get()));
                }
                okCommand();
            }
        }
    }


    @Override
    public void selectKeySuggestion(String key) {
        isUpdatingProgrammatically = true;
        this.key.set(key);
        isUpdatingProgrammatically = false;
        populateValues(key, "");
        focusedValueIndex.set(-1);
    }

    @Override
    public void selectValueSuggestion(Tag tag) {
        isUpdatingProgrammatically = true;
        this.key.set(tag.getKey());
        this.value.set(tag.getValue());
        isUpdatingProgrammatically = false;
    }

    @Override
    public void selectSimpleSuggestion(SuggestionItem item) {
        isUpdatingProgrammatically = true;
        this.simpleName.set(item.getName());
        isUpdatingProgrammatically = false;
    }

    // --- Commands ---

    @Override
    public void okCommand() {
        this.result = null; // Reset
        if (simpleMode.get()) {
            String name = simpleName.get();
            if (name != null && !name.trim().isEmpty()) {
                String id = allSimpleSuggestions.stream()
                        .filter(item -> item.getName().equalsIgnoreCase(name.trim()))
                        .map(SuggestionItem::getId)
                        .findFirst().orElse(null);
                this.result = new AddTagResult(new Tag(name.trim(), id));
            }
        } else { // JSON mode
            String k = key.get();
            String v = value.get();
            if (k != null && !k.trim().isEmpty() && v != null && !v.trim().isEmpty()) {
                this.result = new AddTagResult(new Tag(k.trim(), v.trim(), null));
            }
        }

        if (this.result != null) {
            closeDialog.set(true);
        }
    }

    @Override
    public void cancelCommand() {
        this.result = null;
        closeDialog.set(true);
    }

    @Override
    public void copyCommand() {
        String id = copyId.get();
        if (id != null && !id.trim().isEmpty()) {
            copyStatus.set("Đang kiểm tra ID...");
            new Thread(() -> {
                try {
                    // (Tạm thời bỏ qua kiểm tra, chỉ cần ID)
                    Platform.runLater(() -> {
                        copyStatus.set("Đã chấp nhận ID.");
                        this.result = new AddTagResult(id.trim());
                        closeDialog.set(true);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> copyStatus.set("Lỗi: Không tìm thấy ID."));
                }
            }).start();
        } else {
            copyStatus.set("Vui lòng nhập ID.");
        }
    }

    // --- Getters cho Properties ---
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