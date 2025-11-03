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
 * Triển khai (Implementation) của IAddTagViewModel.
 * (Cập nhật: Sửa lỗi logic tải gợi ý cho Studio/Genre/People).
 */
public class AddTagViewModel implements IAddTagViewModel {

    // --- Services (DI) ---
    private final IStaticDataRepository staticDataRepository;
    private final IItemRepository itemRepository;
    private final IConfigurationService configService;

    // --- Trạng thái nội bộ ---
    private SuggestionContext currentContext = SuggestionContext.TAG;
    private AddTagResult result = null;
    private boolean isUpdatingProgrammatically = false;

    // --- Dữ liệu Gợi ý (Nguồn) ---
    private Map<String, List<Tag>> jsonGroups = new HashMap<>();
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
     * Tải dữ liệu gợi ý từ Repository (chạy nền).
     * (SỬA LỖI LOGIC TẠI ĐÂY)
     */
    private void loadSuggestedTags() {
        new Thread(() -> {
            try {
                // 1. Tải dữ liệu thô (List<Tag>)
                List<Tag> allRawTags = new ArrayList<>();

                switch (currentContext) {
                    case TAG:
                        allRawTags.addAll(staticDataRepository.getAllUsedTags());
                        break;
                    case STUDIO:
                        // (SỬA LỖI: Gọi hàm trả về List<Tag>)
                        allRawTags.addAll(staticDataRepository.getStudioSuggestions());
                        break;
                    case PEOPLE:
                        // (SỬA LỖI: Gọi hàm trả về List<Tag>)
                        allRawTags.addAll(staticDataRepository.getPeopleSuggestions());
                        break;
                    case GENRE:
                        // (SỬA LỖI: Gọi hàm trả về List<Tag>)
                        allRawTags.addAll(staticDataRepository.getGenreSuggestions());
                        break;
                }

                // 2. Xử lý dữ liệu thô (Logic này giờ đúng cho TẤT CẢ context)
                Map<String, List<Tag>> tempJsonGroups = allRawTags.stream()
                        .filter(Tag::isJson)
                        .collect(Collectors.groupingBy(Tag::getKey));

                List<SuggestionItem> simpleSuggestions = allRawTags.stream()
                        .filter(t -> !t.isJson())
                        .map(t -> new SuggestionItem(t.getDisplayName(), t.getId(), currentContext.name())) // Dùng context.name() làm Type
                        .collect(Collectors.toList());


                // 3. Cập nhật UI (trên luồng JavaFX)
                Platform.runLater(() -> {
                    this.jsonGroups = tempJsonGroups;
                    this.allSimpleSuggestions = simpleSuggestions.stream().distinct().collect(Collectors.toList());

                    // 4. Populate lần đầu
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

    // --- Logic Lọc Gợi ý (Không cần thay đổi) ---

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
     * (MỚI) Xử lý logic auto-complete khi mất focus (blur).
     */
    @Override
    public void handleFocusLost(String sourceField) {
        if (isUpdatingProgrammatically) return;

        if ("key".equals(sourceField) && focusedKeyIndex.get() == -1) {
            String currentKeyText = key.get().trim();
            if (!currentKeyText.isEmpty()) {
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
                    isUpdatingProgrammatically = true;
                    key.set(correctCaseKey); // Auto-complete
                    isUpdatingProgrammatically = false;

                    populateValues(correctCaseKey, ""); // Populate values
                    focusedKeyIndex.set(matchIndex); // Set focus index
                }
            }
        }
        // (Không cần logic focus lost cho value/simple)
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
            if (focusedKeyIndex.get() != -1) {
                selectKeySuggestion(suggestionKeys.get(focusedKeyIndex.get()));
            } else {
                handleFocusLost("key");
            }
        } else if ("value".equals(sourceField)) {
            if (focusedValueIndex.get() != -1) {
                selectValueSuggestion(suggestionValues.get(focusedValueIndex.get()));
            }
        } else if ("simple".equals(sourceField)) {
            String currentSimpleText = simpleName.get();

            // 1. Chuyển chế độ và cập nhật text
            isUpdatingProgrammatically = true;
            simpleMode.set(false);
            key.set(currentSimpleText);
            simpleName.set("");
            value.set("");
            isUpdatingProgrammatically = false;

            // 2. Populate lại
            populateKeys(currentSimpleText);
            populateSimpleTags(currentSimpleText);

            // 3. (FIX) Gọi logic auto-complete của 'key'
            handleFocusLost("key");
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
                        key.set("");
                        value.set("");
                        isUpdatingProgrammatically = false;
                        populateSimpleTags(keyText);
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
        this.simpleMode.set(false);
        this.key.set(tag.getKey());
        this.value.set(tag.getValue());
        isUpdatingProgrammatically = false;
    }

    @Override
    public void selectSimpleSuggestion(SuggestionItem item) {
        isUpdatingProgrammatically = true;
        this.simpleMode.set(true);
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
                // (SỬA LỖI LOGIC: Khi tạo JSON thủ công, ID luôn là null)
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