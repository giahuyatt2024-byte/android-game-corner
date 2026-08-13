# GAME CORNER — ROG Style Android Native

Đây là bộ khung Android Native Kotlin độc lập, không dùng AndroidX và không
phụ thuộc file video ngoài. Hiệu ứng boot được vẽ bằng `Canvas`, màn Home
được dựng theo bố cục video tham chiếu, còn HUD được render bằng `WindowManager`
với `TYPE_APPLICATION_OVERLAY`. Palette cyan–tím được dùng riêng để khác menu
nâu/cam trong video.

## Chạy bằng Android Studio

1. Mở thư mục `android-game-corner` bằng Android Studio.
2. Chờ Gradle sync, dùng JDK 17 và Android SDK 35.
3. Build/install app lên thiết bị Android 8.0 trở lên.
4. Mở **GAME CORNER**, chọn `ALLOW OVERLAY PERMISSION`.
5. Quay lại app, chọn một app thật trong danh sách rồi kéo thanh
   `Slide to Open` hoặc bấm `LAUNCH`.

## Luồng hoạt động

- `MainActivity.kt`: kiểm tra `Settings.canDrawOverlays()` và mở
  `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` khi chưa có quyền. Đồng thời
  dùng `PackageManager.queryIntentActivities()` để đọc danh sách app launcher
  thật trên thiết bị và ưu tiên Free Fire nếu package đã cài. Màn hình gồm
  banner, recently played, game library, launch mode và loading dialog.
- `BootAnimationActivity.kt` + `RogFireEyeView.kt`: chạy intro sci-fi ngang
  kiểu flare/logo/vortex bằng Canvas native, lấy cảm hứng từ video tham chiếu
  khoảng 4,2 giây, sau đó mở đúng package thật mà người dùng chọn.
- `GameOverlayService.kt`: foreground service tạo overlay điều khiển giới hạn
  trong vùng HUD để không chặn cảm ứng của game.
- `RogOverlayIconView.kt`: biểu tượng ROG nổi có thể kéo.
- `HudOverlayView.kt`: menu lục giác; chạm từng ô để chuyển mức X-Mode, FPS,
  RAM và nhiệt độ hiển thị với màu cyan–tím khác video tham chiếu.

## Ghi chú Android

Android sẽ luôn hiển thị notification cho foreground service. Nếu Free Fire
chưa được cài, app sẽ hiển thị các app launcher thật khác trên thiết bị.
Danh sách app không dùng dữ liệu mẫu; project dùng `<queries>` cho launcher
intent thay vì yêu cầu quyền nhạy cảm `QUERY_ALL_PACKAGES`.

## Dữ liệu hiệu năng thật

- RAM được đọc từ `ActivityManager.MemoryInfo`.
- Nhiệt độ là nhiệt độ pin Android công khai qua `BatteryManager`.
- Ô `FPS/Hz` hiển thị tần số làm tươi màn hình thật qua `DisplayManager`;
  Android không cho app thông thường đọc FPS render nội bộ của một game khác.
- X-Mode là profile hiển thị/thao tác của GAME CORNER. Việc ép xung, sửa
  governor, thay đổi FPS nội bộ hoặc can thiệp bộ nhớ game cần quyền hệ thống
  hoặc API OEM, nên app không giả vờ thực hiện các thao tác đó.