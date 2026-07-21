# LAB211 — NHÓM 1

 **Employee Payroll Management Simulation**
Hệ thống mô phỏng quản lý bảng lương nhân viên | FPT University • OOP with Java • Kiến trúc MVC

---

##  Research Question

> "Khi nhiều luồng (HR Threads) cùng cập nhật ngày nghỉ phép và ghi nhận lương vào file CSV — cơ chế đồng bộ hóa nào đảm bảo không xảy ra **'trả lương hai lần'** hoặc **'trừ sai ngày phép'** mà vẫn hoàn thành bảng lương toàn công ty trong thời gian ngắn nhất?"

Nhóm implement và so sánh 4 cơ chế đồng bộ hóa (`NO_LOCK`, `SYNCHRONIZED`, `OPTIMISTIC`, `FILE_LOCK`) bằng một Simulator Tool nội bộ, đo `success / doublePayment / wrongLeave / elapsedMs / TPS`, rồi trả lời câu hỏi bằng số liệu thực nghiệm chứ không phỏng đoán.

---

##  Thành viên

| Tên thành viên | MSSV |
|---|---|
| Đinh Đỗ Gia Huy | QE200022 |
| Nguyễn Đức Duy | QE200053 |
| Nguyễn Quốc Vương | QE200066 |
| Nguyễn Nhật Tường Vy | QE200093 |

---

## 🗂 Tóm tắt phân công theo tuần

| Tuần | Huy | Duy | Vương | Vy |
|---|---|---|---|---|
| **T1–2** | File CSV | Use Case Diagram | UML Class Diagram | Bảng schema CSV |
| **T3** | SalaryCalculator | BaseEntity, Employee, Department | Payroll & Attendance (Model) | Leave Management (Model) |
| **T4** | Csv + PayrollRun + LeaveRequest Repo | PayrollEntryRepository + OptimisticLock + DuplicatePayment Exception | Employee + Attendance + Department Repo | LeaveBalanceRepository + InsufficientLeave + CsvParse Exception |
| **T5** | PayrollController (NO_LOCK) | LeaveController (submit/approve/reject) | Fix LeaveBalanceRepository  blocker, ưu tiên số 1 | EmployeeController (wrap CRUD) |
| **T6** | MainView + tích hợp + code review | PayrollView (bảng lương ASCII) | Làm CRUD | EmployeeView + ReportView |
| **T7** | FILE_LOCK mechanism | OPTIMISTIC mechanism + retry backoff | SYNCHRONIZED per-employee | NO_LOCK baseline + SynchronizationTest.java |
| **T8** | SimulatorController (lõi, CountDownLatch) | Detector (detectDoublePayments/WrongLeave) | SimulatorView + bảng so sánh 4 cơ chế | Tích hợp vào MainView + chuẩn bị demo |
| **T9** | Chạy full experiment (20×4×3) | Vẽ biểu đồ & phân tích | Viết report.docx (≥15 trang) | Làm slide.pptx |
| **T10** | AI Log + Reflection cá nhân | AI Log + Reflection cá nhân | AI Log + Reflection cá nhân | AI Log + Reflection cá nhân |



---

## 🏗 Kiến trúc

Dự án tuân thủ nghiêm ngặt **MVC (Model – View – Controller)**:

| Lớp | Trách nhiệm | Không được làm |
|---|---|---|
| **Model** | Entity + `SalaryCalculator` + business rules HR | — |
| **Repository** | Đọc/ghi CSV, quản lý `version` (Optimistic Lock), xử lý conflict, các cơ chế đồng bộ | Không chứa UI |
| **Controller** | Điều phối flow, chọn cơ chế lock, gọi đúng Repository/Model | ❌ Không được chứa công thức tính lương, không ghi CSV trực tiếp |
| **View** | Console UI, nhận input, in bảng ASCII | ❌ Không được tính lương, không truy cập file trực tiếp |


---

##  Cấu trúc thư mục

```
NHOM_01_LAB211_Payroll/
├── src/
│   ├── model/          # Entity, Enum, BaseEntity, SalaryCalculator
│   ├── repository/     # CsvRepository<T> + các Repository cụ thể
│   ├── controller/      # PayrollController, LeaveController, SimulatorController...
│   └── view/            # MainView, PayrollView, LeaveView, SimulatorView...
├── data/
│   ├── employees.csv          (≥ 1,000 dòng)
│   ├── departments.csv        (≥ 20 dòng)
│   ├── leave_balances.csv     (≥ 2,000 dòng)
│   ├── leave_requests.csv     (≥ 4,000 dòng)
│   ├── attendance.csv         (≥ 12,000 dòng)
│   ├── payroll_entries.csv    (≥ 12,000 dòng)
│   └── payroll_runs.csv       (kết quả simulation)
├── docs/
│   ├── report.docx            # Báo cáo Word ≥ 15 trang
│   ├── slide.pptx              # Slide trình bày
│   ├── class_diagram.png       # UML Class Diagram
│   └── flowcharts/             # Payroll / Leave / RaceCondition / Simulator / DataGen
├── ai_logs/
│   ├── member1_ai_log.md       # AI Log cá nhân — raw, có timestamp
│   ├── member2_ai_log.md
│   ├── member3_ai_log.md
│   └── member4_ai_log.md
└── README.md
```

---

##  Cơ chế đồng bộ hóa (4 mechanisms)

| Cơ chế | Ý tưởng | Kỳ vọng |
|---|---|---|
| `NO_LOCK` | Baseline, không khóa gì | Phải **tái hiện được lỗi** double payment / wrong leave khi chạy 10+ threads |
| `SYNCHRONIZED` | `synchronized(empId.intern())` per-employee | 0 lỗi, các nhân viên khác nhau vẫn chạy song song |
| `OPTIMISTIC` | So sánh `version` khi ghi, retry với exponential backoff (tối đa 3 lần) | 0 lỗi hoặc retry/fail có kiểm soát |
| `FILE_LOCK` | `java.nio.channels.FileLock` trên `payroll_entries.csv` / `leave_balances.csv` | 0 lỗi ở cấp file |

Simulator dùng `ExecutorService` + `CountDownLatch` để đảm bảo N HR-thread chạy **thực sự đồng thời**, sau đó `detectDoublePayments()` / `detectWrongLeaveDeductions()` đọc lại **trực tiếp file CSV** (không đếm trong RAM) để xác nhận kết quả.

---

##  Compile & Run

**Yêu cầu:** Java 17+

```bash
# Compile
javac -d out $(find src -name "*.java")

# Chạy DataGenerator (tạo dữ liệu mẫu ≥ 10,000 dòng)
java -cp out generator.DataGenerator

# Chạy chương trình chính
java -cp out view.MainView

# Chạy Simulator với từng cơ chế (từ menu MainView → Simulator)
# Chọn số thread, số dept x số emp (mặc định 20 x 50 = 1000 nhân viên), cơ chế đồng bộ
```

---

##  Phân công công việc — 10 tuần

### Tuần 1–2 — Phân tích & Dữ liệu
| Thành viên | Task |
|---|---|
| Huy | File CSV |
| Duy | Sơ đồ Use Case |
| Vương | Sơ đồ UML Class Diagram |
| Vy | Bảng schema CSV |

### Tuần 3 — Model Layer
| Thành viên | Task |
|---|---|
| Huy | `SalaryCalculator` |
| Duy | `BaseEntity` & `Employee` & `Department` |
| Vương | `Payroll` & `Attendance` |
| Vy | Leave Management |

### Tuần 4 — Repository Layer
| Thành viên | Task |
|---|---|
| Huy | `Csv` + `PayrollRun` + `LeaveRequest` Repository |
| Duy | `PayrollEntryRepository` & `OptimisticLock` + `DuplicatePayment` Exception |
| Vương | `Employee` + `Attendance` + `Department` Repository |
| Vy | `LeaveBalanceRepository` & `InsufficientLeave` + `CsvParse` Exception |


### Tuần 5 — Controller Layer
| Thành viên | Task |
|---|---|
| Huy | `PayrollController.runPayroll(month, year)`: load employee → load attendance → tạo `SalaryCalculator` → tạo `PayrollEntry` PENDING → `save()` → cộng dồn `totalNetPay` → mark `PayrollRun` COMPLETED. Dùng `NO_LOCK` trước. Test: đếm entry CSV = số employee. |
| Duy | `LeaveController`: `submitLeave()`, `approve()` (gọi repo approve + trừ balance), `reject()`. Test: submit → approve → kiểm tra balance giảm đúng số ngày. |
| Vương | **Fix `LeaveBalanceRepository`** (blocker của Duy — ưu tiên số 1, làm đầu tuần): rewrite kế thừa `CsvRepository<LeaveBalance>`, thêm `deductWithOptimisticLock(empId, year, type, days, expectedVersion)`, `findByEmpIdAndYear(empId, year)`. |
| Vy | `EmployeeController`: wrap `findById`, `findByDeptId`, `add`, `update`, `delete` từ `EmployeeRepository`. Làm xong sớm để hỗ trợ Vương test. |

### Tuần 6 — View Layer + MVC Wiring
| Thành viên | Task |
|---|---|
| Huy | `MainView` + integration: menu console điều hướng Payroll/Leave/Employee/Report, kết nối toàn bộ View ↔ Controller ↔ Repository. Code review cuối tuần: không có công thức lương trong View/Controller (tránh −5%). |
| Duy | `PayrollView`: nhập tháng/năm → gọi `PayrollController` → in bảng lương ASCII theo phòng ban; xem chi tiết lương 1 nhân viên. |
| Vương | `EmployeeView`: giao diện console cho CRUD nhân viên; thêm, xem danh sách, cập nhật và xóa nhân viên thông qua `EmployeeController`. |
| Vy | `EmployeeView` + `ReportView`: CRUD employee từ console; `ReportView` tổng net pay tháng, số nhân viên xử lý, số đơn APPROVED. |

### Tuần 7 — Synchronization Mechanisms
*Cả nhóm cùng làm trên `PayrollController` và `LeaveController`, thêm 3 cơ chế lock:*

| Thành viên | Cơ chế | Task |
|---|---|---|
| Huy | `FILE_LOCK` | `FileLock` trên `payroll_entries.csv` và `leave_balances.csv`; implement `PayrollController.runPayrollWithFileLock()`. Test: 5 threads cùng chạy → 0 double payment. |
| Duy | `OPTIMISTIC` | Wire `PayrollEntryRepository.processWithOptimistic()` (đã có) vào `runPayrollWithOptimistic()`; thêm retry loop exponential backoff (tối đa 3 lần) khi gặp `OptimisticLockException`. Test: 5 threads cùng duyệt lương E001 → chỉ 1 thành công. |
| Vương | `SYNCHRONIZED` | `synchronized(empId.intern())` trong `LeaveController.approve()`, đảm bảo per-employee lock (2 nhân viên khác nhau vẫn chạy song song). Test: 5 threads cùng approve leave của EMP001 → balance chỉ bị trừ đúng 1 lần. |
| Vy | `NO_LOCK` baseline + test suite | Đảm bảo `NO_LOCK` **có thể tái hiện** double payment khi chạy 10+ threads (nếu không lỗi: −5%). Viết `SynchronizationTest.java`: 3 test method cho 3 cơ chế, dùng `CountDownLatch` để threads thực sự đồng thời. |

### Tuần 8 — Simulator Tool
*(vai trò A/B/C/D theo phân công nhóm; mặc định theo thứ tự Huy → Duy → Vương → Vy)*

| Vai trò | Task |
|---|---|
| **A — SimulatorController (lõi)** | Viết `SimulatorController` dùng `ExecutorService` + `CountDownLatch` để N thread HR chạy **thực sự đồng thời** (bắt buộc, nếu không sẽ bị trừ 8%). Tham số đầu vào: số thread, số dept × số emp (test với 20 dept × 50 emp = 1000 nhân viên), cơ chế đồng bộ (`NO_LOCK`/`SYNCHRONIZED`/`OPTIMISTIC`/`FILE_LOCK`). Đo elapsedMs, tính `TPS = success / elapsedMs`. |
| **B — Detector** | `detectDoublePayments()`: đọc lại **trực tiếp file CSV** sau khi simulation xong (không đếm trong RAM — trừ 3% nếu sai) → tìm `entryId` trùng hoặc version bất thường. `detectWrongLeaveDeductions()`: so khớp `LeaveBalance.annualUsed/sickUsed` với tổng `sumApprovedDays()` từ `LeaveRequestRepository` → lệch là lỗi. Viết unit test riêng cho 2 hàm detect này với dữ liệu giả có lỗi cố ý. |
| **C — SimulatorView + bảng so sánh** | `SimulatorView`: menu chọn cơ chế, nhập số thread, chạy và in bảng ASCII so sánh 4 cơ chế: `success \| doublePayment \| wrongLeave \| elapsedMs \| TPS`. Đảm bảo `NO_LOCK` phải ra lỗi thật (nếu không sẽ bị trừ 5%) — review lại `PayrollEntryRepository.save()/update()` xem có race condition thật khi 20 thread ghi cùng lúc không. |
| **D — Tích hợp & Demo** | Nối `SimulatorController` vào `MainView` (thêm menu mới). Chuẩn bị data: chạy `DataGenerator` cho đủ 20 dept, 1000 emp. Chuẩn bị kịch bản demo: `NO_LOCK` 20 threads → double payment > 0; `SYNCHRONIZED`/`OPTIMISTIC` → 0 lỗi. |


### Tuần 9 — Research & Báo cáo
*(vai trò A/B/C/D theo phân công nhóm — mặc định theo thứ tự Huy → Duy → Vương → Vy)*

| Vai trò | Task |
|---|---|
| **A — Chạy thực nghiệm** | Chạy full experiment: 20 threads × 4 cơ chế × 3 lần, lấy trung bình. Xuất kết quả ra file (CSV/JSON) để vẽ biểu đồ. |
| **B — Biểu đồ & phân tích** | Vẽ biểu đồ so sánh doublePayment/wrongLeave rate vs. throughput (emp/giây) — Excel hoặc Python/matplotlib. Trả lời rõ Research Question: cơ chế nào đạt **0% lỗi VÀ nhanh nhất** — cần số liệu thật, không phỏng đoán. |
| **C — Viết báo cáo Word (≥15 trang)** | Dùng skill docx để dựng `report.docx`: giới thiệu, kiến trúc MVC, class diagram, flowchart, kết quả thực nghiệm, kết luận. |
| **D — Slide trình bày** | Dựng `slide.pptx` tóm tắt: vấn đề, kiến trúc, demo simulator, kết quả, kết luận. |

### Tuần 10 — AI Reflection & Nộp bài (30% điểm cá nhân!)
*Phần này mỗi người tự làm riêng, không thể chia sẻ:*

- Mỗi thành viên tự tổng hợp **AI Log cá nhân** (lịch sử chat thật, có timestamp) → `ai_logs/memberX_ai_log.md`
- Mỗi người viết **AI Reflection ≥ 500 từ**: AI giúp gì tốt (CRUD, UI...), AI sai gì — đặc biệt về **concurrent logic** (synchronized, race condition, optimistic lock) — cách phát hiện lỗi đó và cách sửa thực tế. Đây là tiêu chí quyết định điểm cá nhân; viết chung chung/hời hợt sẽ mất điểm nặng (0–14%).

**Việc chung cả nhóm** (một người làm trưởng nhóm điều phối):
- Code review toàn bộ: `grep` tìm công thức lương có lọt vào View/Controller không (mỗi lần phát hiện: −5%)
- Fix bug, polish UI
- Đóng gói đúng cấu trúc ZIP: `src/`, `data/` (đủ số dòng: employees ≥1000, attendance/payroll_entries ≥12000...), `docs/`, `ai_logs/`, `README.md`
- Đặt tên file: `NHOM_XX_LAB211_Payroll.zip`

---

##  Checklist trước khi nộp

- [ ] `DataGenerator` sinh đủ ≥ 10,000 dòng tổng
- [ ] `NO_LOCK` **có** double payment / wrong leave khi chạy 10+ threads
- [ ] `SYNCHRONIZED` / `OPTIMISTIC` / `FILE_LOCK` → 0 lỗi
- [ ] `detectDoublePayments()` / `detectWrongLeaveDeductions()` đọc trực tiếp CSV, không đếm trong RAM
- [ ] Không có công thức lương trong View/Controller (grep kiểm tra)
- [ ] Simulator dùng `CountDownLatch` để đảm bảo threads thực sự đồng thời
- [ ] Class Diagram + 5 Flowchart (Payroll/Leave/RaceCondition/Simulator/DataGen)
- [ ] `report.docx` ≥ 15 trang, `slide.pptx`
- [ ] AI Log + AI Reflection ≥ 500 từ cho **mỗi** thành viên
- [ ] ZIP đúng cấu trúc, đặt tên `NHOM_XX_LAB211_Payroll.zip`

---

##  Deadline

Nộp qua FPT University Learning trước deadline được thông báo trên lớp.
Trễ 1 ngày: −10% | Trễ 2 ngày: −30% | Trễ ≥ 3 ngày: 0 điểm

