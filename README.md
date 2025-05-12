# AI Voice Assistant

AI Voice Assistant là một ứng dụng Android cho phép người dùng tương tác với các mô hình AI mạnh mẽ thông qua giao diện chat và tương tác giọng nói.

## Tính năng chính

- Chat văn bản với nhiều mô hình AI khác nhau
- Tương tác bằng giọng nói (nhấn giữ để ghi âm)
- Chuyển đổi văn bản thành giọng nói
- Hỗ trợ nhiều mô hình AI:
  - OpenAI GPT-4o-mini
  - Google Gemini 2.0
  - Llama 3.2
  - Qwen 2.5

## Công nghệ sử dụng

- **Ngôn ngữ**: Java
- **Framework**: Android SDK
- **Thư viện kết nối API**:
  - Retrofit2 và OkHttp cho các yêu cầu mạng
  - Gson cho xử lý JSON
- **API AI**:
  - OpenAI API (Chat Completion và Speech-to-Text)
  - Google Gemini API
  - Mô hình Llama và Qwen (qua API tùy chỉnh)
- **Thư viện giao diện**:
  - RecyclerView cho hiển thị tin nhắn
  - Material Design Components

## Giao diện ứng dụng

### Màn hình chính
![Giao diện chính](public/images/main_ui.png)

### Màn hình chat
![Giao diện chat](public/images/chat.png)

### Tương tác bằng giọng nói
![Giao diện ghi âm](public/images/main_voice_chat.png)

### Cài đặt
![Giao diện cài đặt](public/images/setting.png)

### Demo tương tác giọng nói
![Demo tương tác giọng nói](public/videos/voice_chat.gif)

## Cách sử dụng

1. **Chat văn bản**:
   - Nhập tin nhắn vào ô văn bản
   - Nhấn nút gửi để gửi tin nhắn đến AI
   - Xem phản hồi của AI hiển thị trong lịch sử cuộc trò chuyện

2. **Tương tác bằng giọng nói**:
   - Nhấn nút microphone
   - Giữ nút để ghi âm giọng nói của bạn
   - Thả nút để gửi âm thanh tới AI để xử lý
   - AI sẽ chuyển đổi giọng nói thành văn bản, xử lý và phản hồi bằng giọng nói

3. **Thay đổi mô hình AI**:
   - Nhấn nút cài đặt
   - Chọn mô hình AI mong muốn từ menu

4. **Tạo cuộc trò chuyện mới**:
   - Nhấn nút "Cuộc trò chuyện mới" để xóa lịch sử và bắt đầu cuộc trò chuyện mới

## Yêu cầu hệ thống

- Android 8.0 (API 24) trở lên
- Kết nối internet
- Quyền truy cập microphone (cho tính năng nhận dạng giọng nói)

## Phát triển

Dự án này sử dụng Gradle để quản lý các phụ thuộc. Để phát triển, bạn cần:

1. Clone repository
2. Mở dự án trong Android Studio
3. Đảm bảo bạn có các API key cần thiết trong file Constants.java
4. Build và chạy ứng dụng

## Giấy phép

© 2024 - Bản quyền thuộc về tác giả 