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
 * (Cập nhật: Sửa lỗi biên dịch SuggestionItem constructor).
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

    // --- Properties Điều hướng Phím (Binding) ---
    private final ReadOnlyIntegerWrapper focusedKeyIndex = new ReadOnlyIntegerWrapper(-1);
    private final ReadOnlyIntegerWrapper focusedValueIndex = new ReadOnlyIntegerWrapper(-1);
    private final ReadOnlyIntegerWrapper focusedSimpleIndex = new ReadOnlyIntegerWrapper(-1);

    // --- (MỚI) Property Báo đóng Dialog ---
    private final ReadOnlyBooleanWrapper closeDialog = new ReadOnlyBooleanWrapper(false);

    // --- ObservableLists (Binding) ---
    private final ObservableList<String> suggestionKeys = FXCollections.observableArrayList();
    private final ObservableList<Tag> suggestionValues = FXCollections.observableArrayList();
    private final ObservableList<SuggestionItem> suggestionSimple = FXCollections.observableArrayList();

    public AddTagViewModel(IStaticDataRepository staticDataRepository, IItemRepository itemRepository) {
        this.staticDataRepository = staticDataRepository;
        this.itemRepository = itemRepository;
        setupListeners();
    }

    /**
     * Cài đặt các listener nội bộ (giống logic trong initialize() của controller cũ).
     */
    private void setupListeners() {
        // Chuyển đổi giữa JSON và Simple mode
        simpleMode.addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedKeyIndex.set(-1);
            focusedValueIndex.set(-1);
            focusedSimpleIndex.set(-1);
        });

        // Lọc danh sách gợi ý khi gõ
        keyProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingProgrammatically) return;
            focusedKeyIndex.set(-1);
            populateKeys(newVal);
            populateSimpleTags(newVal); // Quick search simple
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

    /**
     * Cập nhật các nhãn (label) I18n dựa trên context.
     */
    private void updateLabels(SuggestionContext context) {
        // (Sử dụng IConfigurationService sẽ tốt hơn, nhưng tạm thời hardcode)
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

    /**
     * Tải dữ liệu gợi ý từ Repository (chạy nền).
     */
    private void loadSuggestedTags() {
        new Thread(() -> {
            try {
                // 1. Tải dữ liệu thô
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

                // 2. Xử lý dữ liệu thô
                Map<String, List<Tag>> tempJsonGroups = allRawTags.stream()
                        .filter(Tag::isJson)
                        .collect(Collectors.groupingBy(Tag::getKey));

                // (SỬA LỖI BIÊN DỊCH TẠI ĐÂY)
                if (currentContext == SuggestionContext.TAG) {
                    List<SuggestionItem> simpleTagsFromJson = allRawTags.stream()
                            .filter(t -> !t.isJson())
                            // (SỬA LỖI: Gọi constructor mới của SuggestionItem)
                            .map(t -> new SuggestionItem(t.getDisplayName(), t.getId(), "Tag"))
                            .collect(Collectors.toList());
                    simpleSuggestions.addAll(simpleTagsFromJson);
                }


                // 3. Cập nhật UI (trên luồng JavaFX)
                Platform.runLater(() -> {
                    this.jsonGroups = tempJsonGroups;
                    this.allSimpleSuggestions = simpleSuggestions;

                    // 4. Populate lần đầu
                    isUpdatingProgrammatically = true;
                    populateKeys(key.get());
                    populateSimpleTags(simpleName.get());
                    isUpdatingProgrammatically = false;
                });

            } catch (Exception e) {
                System.err.println("Lỗi khi tải gợi ý: " + e.getMessage());
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
        // (SỬA LỖI: Đảm bảo danh sách không bị trùng lặp)
        suggestionSimple.setAll(filtered.stream().distinct().collect(Collectors.toList()));
    }

    // --- Logic Điều hướng Phím (UR-35) ---

    @Override
    public void handleFieldKeyEvent(KeyEvent event) {
        // (Sẽ được triển khai đầy đủ ở Giai đoạn 12)
        if (event.getCode() == KeyCode.ENTER) {
            okCommand();
        }
    }

    @Override
    public void selectKeySuggestion(String key) {
        isUpdatingProgrammatically = true;
        this.key.set(key);
        isUpdatingProgrammatically = false;
        populateValues(key, "");
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
                    // (Tạm thời không lấy full DTO, chỉ cần ID)
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