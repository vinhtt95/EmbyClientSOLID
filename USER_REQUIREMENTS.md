# Phân tích Yêu cầu Người dùng (User Requirements)

Dưới đây là danh sách 50 yêu cầu chức năng (UR) chi tiết được trích xuất từ mã nguồn của dự án EmbyClientJavaFX gốc.

## UR-AUTH: Xác thực & Phiên làm việc (Authentication & Session)
* **UR-1 (Đăng nhập):** Người dùng phải có khả năng đăng nhập vào một máy chủ Emby bằng Server URL, Tên đăng nhập và Mật khẩu.
* **UR-2 (Ghi nhớ Server):** Ứng dụng phải ghi nhớ Server URL cuối cùng đã được sử dụng.
* **UR-3 (Phản hồi Đăng nhập):** Ứng dụng phải cung cấp phản hồi trực quan (thành công/lỗi) trong quá trình đăng nhập.
* **UR-4 (Tự động Đăng nhập):** Ứng dụng phải cố gắng khôi phục phiên làm việc (tự động đăng nhập) khi khởi động nếu có token hợp lệ.
* **UR-5 (Đăng xuất):** Người dùng phải có khả năng đăng xuất, việc này sẽ xóa phiên làm việc và quay lại màn hình đăng nhập.

## UR-MAIN: Cửa sổ chính & Điều hướng (Main Window & Navigation)
* **UR-6 (Ghi nhớ Cửa sổ):** Ứng dụng phải ghi nhớ vị trí và kích thước cửa sổ chính giữa các lần khởi động.
* **UR-7 (Bố cục 3 cột):** Giao diện chính phải là bố cục 3 cột (Cây thư viện, Lưới Item, Chi tiết Item).
* **UR-8 (Ghi nhớ Bố cục):** Vị trí các thanh chia (divider) của bố cục 3 cột phải được lưu và khôi phục.
* **UR-9 (Trạng thái Toàn cục):** Ứng dụng phải hiển thị một thanh trạng thái (status bar) liên tục để thông báo các hành động (ví dụ: "Đang tải...", "Sẵn sàng").
* **UR-10 (Nút Home):** Người dùng có thể nhấp vào nút "Home" để xóa lựa chọn thư mục và hiển thị tất cả các item ở thư mục gốc.
* **UR-11 (Quốc tế hóa - I18n):** Tất cả các chuỗi văn bản trên UI phải được tải từ tệp `config.json` bên ngoài.

## UR-HOTKEY: Phím tắt (Hotkeys & Shortcuts)
* **UR-13 (Phím tắt Trong ứng dụng):** Ứng dụng phải hỗ trợ các phím tắt khi đang focus:
    * `Cmd+S`: Lưu thay đổi ở cột Chi tiết.
    * `Cmd+N` / `Cmd+P`: Chọn item tiếp theo/trước đó trong lưới.
    * `Cmd+Enter`: Phát item đang chọn.
    * `Enter` (trong cột Chi tiết): Mở lại dialog "Add Tag" cuối cùng.
    * `Alt+Left/Right` (hoặc `Cmd+[ / ]`): Điều hướng Lùi/Tiến trong lịch sử lưới.
* **UR-14 (Điều hướng Chuột):** Ứng dụng phải hỗ trợ các nút Lùi/Tiến (Back/Forward) trên chuột để điều hướng lịch sử lưới.

## UR-TREE: Cột Thư viện (Cột 1 - Library Tree)
* **UR-15 (Hiển thị Thư mục):** Cột 1 phải hiển thị danh sách thư viện (root views) dưới dạng cây.
* **UR-16 (Chỉ Thư mục):** Cây chỉ được hiển thị các item là thư mục (ví dụ: Collection, Folder), không hiển thị các file media.
* **UR-17 (Lazy Loading):** Các thư mục con chỉ được tải khi người dùng mở rộng (expand) một mục trong cây.
* **UR-18 (Copy ID Cây):** Người dùng có thể nhấp chuột phải vào một mục trong cây để sao chép ID của nó.

## UR-GRID: Lưới Item (Cột 2 - Item Grid)
* **UR-19 (Hiển thị Item):** Khi chọn một thư mục (UR-15) hoặc "Home" (UR-10), cột 2 phải hiển thị các item con trong một lưới (grid).
* **UR-20 (Tìm kiếm):** Người dùng có thể tìm kiếm item bằng từ khóa. Kết quả tìm kiếm sẽ thay thế nội dung lưới hiện tại.
* **UR-21 (Sắp xếp):** Người dùng có thể sắp xếp lưới theo: Tên (A-Z), Ngày phát hành (ProductionYear/PremiereDate), Ngày tạo (DateCreated).
* **UR-22 (Thứ tự Sắp xếp):** Người dùng có thể chuyển đổi thứ tự sắp xếp (Tăng dần/Giảm dần).
* **UR-23 (Phân trang):** Lưới phải tải item theo trang (ví dụ: 50 item/trang).
* **UR-24 (Tải trang khi cuộn):** Khi cuộn đến cuối/đầu lưới, ứng dụng phải hỏi người dùng xác nhận trước khi tải trang tiếp theo/trước đó.
* **UR-25 (Thumbnail & Badge):** Mỗi item trong lưới phải hiển thị ảnh thumbnail (Primary Image) và một "badge" (huy hiệu) cho Critic Rating (nếu có).
* **UR-26 (Chọn Item):** Nhấp chuột (đơn) vào một item trong lưới sẽ chọn nó, highlight nó, và hiển thị chi tiết của nó trong Cột 3.
* **UR-27 (Phát Item):** Nhấp đúp chuột vào một item trong lưới sẽ mở/phát tệp media đó bằng trình phát mặc định của hệ thống.
* **UR-28 (Copy ID Lưới):** Người dùng có thể nhấp chuột phải vào một item trong lưới để sao chép ID của nó.
* **UR-29 (Lịch sử Điều hướng):** Lưới phải duy trì một lịch sử điều hướng (cho các hành động: chọn thư mục, tìm kiếm, nhấp vào chip). Người dùng có thể điều hướng Lùi/Tiến (UR-13, UR-14) qua lịch sử này.

## UR-DETAIL: Chi tiết Item (Cột 3 - Item Detail)
* **UR-30 (Hiển thị Chi tiết):** Cột 3 phải hiển thị thông tin chi tiết đầy đủ của item được chọn (UR-26).
* **UR-31 (Chỉnh sửa Metadata):** Người dùng có thể chỉnh sửa các trường: Tiêu đề (Title), Tiêu đề gốc (Original Title), Mô tả (Overview), và Ngày phát hành (Release Date).
* **UR-32 (Chỉnh sửa Rating):** Người dùng có thể xem/chỉnh sửa Critic Rating bằng cách nhấp vào 10 nút (1-10). Nhấp vào nút đã chọn sẽ bỏ chọn (set null).
* **UR-33 (Lưu Rating Ngay lập tức):** Thay đổi Critic Rating (UR-32) phải được lưu ngay lập tức (một lệnh gọi API riêng) mà không cần nhấn nút "Lưu" chính.
* **UR-34 (Quản lý "Chip"):** Người dùng có thể xem, thêm, và xóa các mục sau, được hiển thị dưới dạng "chip":
    * Tags (Hỗ trợ cả chuỗi đơn giản và JSON key-value).
    * Studios
    * People
    * Genres
* **UR-35 (Dialog Thêm Chip):** Khi thêm một "chip" (UR-34), một dialog (cửa sổ) phải xuất hiện:
    * Dialog phải hỗ trợ cả hai chế độ: "Đơn giản" (chỉ tên) và "JSON" (Key-Value).
    * Dialog phải cung cấp các "suggestion chips" (chip gợi ý) cho các Keys và Values đã tồn tại trong thư viện.
    * Dialog phải hỗ trợ điều hướng bàn phím (Lên/Xuống/Tab/Enter) để chọn các gợi ý.
    * Dialog phải cho phép người dùng "Sao chép" (copy) tất cả các chip (Tags/Studios/...) từ một item khác bằng cách nhập ID của item nguồn.
* **UR-36 (Nhấp vào Chip):** Nhấp vào một "chip" (UR-34) trong Cột 3 phải kích hoạt Cột 2 (UR-19) tải và hiển thị tất cả các item có chứa chip đó (và thêm vào lịch sử UR-29).
* **UR-37 (Clone Thuộc tính):** Người dùng có thể "Clone" (nhân bản) các thuộc tính (Tags, Studios, People, Genres) từ item *hiện tại* (Cột 3) sang *tất cả* các item con của thư mục *đã chọn* (Cột 1).
* **UR-38 (Fetch Ngày phát hành):** Người dùng có thể nhấp vào nút "Lấy ngày P.H" để gọi một API bên ngoài (`localhost:8081`). API này sử dụng "Original Title" làm mã (code):
    * Nó sẽ trả về và điền vào trường "Release Date".
    * Nó cũng sẽ trả về "actressName". Tên này phải được tìm kiếm trong danh sách "People" của thư viện.
    * Nếu tìm thấy "People" tương ứng, tất cả "Tags" từ người đó sẽ được sao chép (merge) vào item hiện tại.
    * Tên "actressName" cũng sẽ được thêm vào danh sách "People" của item hiện tại.
* **UR-39 (Đường dẫn & Mở File):** Hiển thị đường dẫn tệp cục bộ. Nút "Mở" sẽ phát tệp (nếu là media) hoặc mở thư mục (nếu là thư mục).
* **UR-40 (Mở Subtitle):** Nút "Mở Subtitle" (chỉ hiển thị cho file media):
    * Nếu tệp `[tên-file].srt` không tồn tại, tạo mới nó với mã hóa UTF-8 BOM và ghi Tiêu đề (UR-31) của item làm phụ đề đầu tiên.
    * Mở tệp `.srt` bằng trình chỉnh sửa mặc định.
    * Mở thư mục chứa tệp đó.
* **UR-41 (Quản lý Ảnh nền):** Hiển thị một gallery các ảnh nền (Backdrops). Người dùng có thể xóa ảnh nền hoặc thêm ảnh mới (bằng cách chọn tệp hoặc kéo-thả).
* **UR-42 (Quản lý Ảnh chính):** Hiển thị ảnh chính (Primary). Người dùng có thể thay thế ảnh này (bằng cách kéo-thả). Nút "Lưu ảnh" sẽ xuất hiện để xác nhận việc tải lên.
* **UR-43 (Mở Thư mục Screenshot):** Nhấp vào ảnh chính (Primary) sẽ mở một thư mục cục bộ theo đường dẫn: `[screenshotBasePath]` (từ `config.json`) + `[tên-file-của-item]`.
* **UR-44 (Export JSON):** Người dùng có thể xuất (export) metadata đầy đủ của item hiện tại ra một tệp `.json`.
* **UR-45 (Import JSON):** Người dùng có thể nhập (import) metadata từ một tệp `.json`.
* **UR-46 (Preview Import):** Sau khi nhập (UR-45), UI phải hiển thị các giá trị mới và các nút (✓/✗) bên cạnh mỗi trường đã thay đổi (Title, Overview, Rating, Tags, Studios, People, Genres, v.v.).
* **UR-47 (Accept/Reject Import):** Người dùng phải có khả năng chấp nhận (✓) hoặc từ chối (✗) từng thay đổi. Từ chối sẽ khôi phục giá trị ban đầu cho trường đó.
* **UR-48 (Trạng thái "Dirty"):** Nút "Lưu" (Save) chính phải bị vô hiệu hóa (disabled) cho đến khi người dùng thực hiện một thay đổi thủ công (ví dụ: gõ text) hoặc chấp nhận (✓) một thay đổi từ import.
* **UR-49 (Lưu Import):** Nhấn "Lưu" (UR-48) sau khi import sẽ chỉ lưu các trường đã được chấp nhận (✓).
* **UR-50 (Pop-out View):** Cột 3 (Chi tiết) phải có khả năng "pop-out" (tách ra) thành một cửa sổ riêng, có thể thay đổi kích thước và ghi nhớ vị trí/kích thước của nó.