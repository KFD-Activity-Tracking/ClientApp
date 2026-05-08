# KFD Activity Tracker — ClientApp

Kotlin JVM-приложение для сбора и отправки статистики активности пользователя на сервер.

## Запуск

```bash
# Требуется Java 21
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew run

# Сбросить локальную БД при запуске:
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew run --args="--reset-db"
```

Открывается окно с полями логин/пароль. После успешной авторизации начинается отслеживание.

## Что собирается

| Тип | Данные |
|-----|--------|
| Мышь (движение) | delta_x, delta_y |
| Мышь (клик) | кнопка, позиция (нормализована 0–1) |
| Клавиатура | код клавиши |
| Смена приложения | имя окна/приложения |

Данные сохраняются в локальную SQLite БД (`activity.db`) и отправляются батчами каждые 30 секунд.

## Системные метрики

Параллельно с отправкой батчей накапливается скользящее среднее:
- **CPU** — через `/proc/stat` (два чтения с интервалом 200 мс)
- **RAM** — через `/proc/meminfo`
- **GPU** — NVIDIA через `nvidia-smi`, AMD через `/sys/class/drm/card0/device/gpu_busy_percent`

При завершении сессии средние значения отправляются на сервер в `SessionMetricsDto`.

## Поддерживаемые платформы

| Платформа | Трекер |
|-----------|--------|
| Linux | `LinuxEvdevTracker` (evdev) или `DesktopTracker` (jnativehook) |
| Windows | `WindowsTracker` (jnativehook) |
| macOS | `MacTracker` (jnativehook) |

На Linux требуется доступ к `/dev/input`. Если недоступен:
```bash
sudo usermod -aG input $USER  # перелогиниться после
```

## Структура

```
database/   — DatabaseFactory (SQLite/Exposed), репозитории, модели таблиц
tracker/    — InputTracker (интерфейс), InputTrackerFactory, платформенные реализации
network/    — ServerClient (Ktor), BatchSender (daemon thread)
metrics/    — SystemMetrics, SystemMetricsCollector
ui/         — TrackerWindow (Swing)
Main.kt     — точка входа
```
