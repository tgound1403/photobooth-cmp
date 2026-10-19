# Device checks bổ sung (emulator-5554)

| Mục | Kết quả | Bằng chứng |
|---|---|---|
| P0-1 PhotoBooth "Chụp Lại" | Chưa xác nhận dứt khoát. Bấm Chụp Lại mở lại màn ảnh xem trước (log: CameraView trong PhotoBoothScreen được khởi tạo lại), nhưng màn vẫn hiện 3 thumbnail cũ và dump UI không có nút chụp. Nghi ngờ mạnh: `capturedImages` không được xóa khi retake, khớp với code PhotoBoothResultScreen.kt:147. | run4/r1_after_retake.png, run4/r2_recheck.png |
| P0-2 Back-stack từ Result | Xác nhận: back từ Photo booth result thoát app ra launcher (không quay về Home hay Selection). | run4/r4_back2.png |
| Quyền camera bị từ chối | Xác nhận: hiện màn "Ứng dụng cần quyền truy cập camera để chụp ảnh" với nút "Mở cài đặt". | run4/p2_denied.png |
| Cấp lại quyền | Xác nhận: sau pm grant, app về Home bình thường, CAMERA đã granted. | run4/p3_regranted.png |
| Paywall | Chưa kiểm: chưa tìm được đường vào Paywall trên máy. | — |

Ghi chú: đã xác nhận P0-4 (chọn máy ở picker không áp dụng khi về Home), cold start mở Home, và "Chỉnh ảnh từ máy" mở system photo picker (trước đó).
Không có lỗi E/ từ app trong logcat ở các bước trên.
