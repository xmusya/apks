# Android Apps

Лицензия: [MIT](LICENSE).

Репозиторий с двумя независимыми Android-приложениями. Каждое приложение лежит в отдельной папке и собирается как самостоятельный Gradle-проект.

## Структура

```text
.
├── groq-ai-chat/          # Groq AI Chat — исходники, APK и архив
└── transmify/             # Transmify — исходники, APK и архив
```

Внутри каждой папки:

```text
<app>/
├── app/                   # модуль приложения (Kotlin, Jetpack Compose)
├── gradle/                # версии зависимостей и настройки wrapper
├── build.gradle.kts
├── settings.gradle.kts
└── README.md              # README от AI Studio
```

Готовые APK не хранятся в git — они публикуются в разделе **Releases** этого репозитория. Локально файлы `<app>.apk` и `<app>.zip` могут лежать в папке приложения, но добавлены в `.gitignore`.

## Приложения

| Приложение | Описание | Package ID | Версия |
| --- | --- | --- | --- |
| **Groq AI Chat** (`groq-ai-chat/`) | Клиент Groq AI: Material 3 Expressive, статистика токенов и задержки, локальная история чатов, выбор моделей, адаптивный интерфейс для планшетов | `com.aistudio.groqchat.fxmpzq` | 1.0 (1) |
| **Transmify** (`transmify/`) | Удалённый клиент Transmission: статистика сидирования, уведомления, поиск по RuTracker, Material 3 Expressive | `com.aistudio.transmify.app` | 1.0 (1) |

Оба приложения: `minSdk 24`, `targetSdk 36`, `compileSdk 36`.

## Установка APK

Скачайте APK из раздела **Releases** или поставьте локальный файл на устройство:

```bash
adb install <путь>/groq-ai-chat.apk
adb install <путь>/transmify.apk
```

Либо скопировать APK на телефон и открыть его (потребуется разрешить установку из неизвестных источников).

## Сборка

Требования:

- Android Studio (актуальная версия)
- JDK 17+
- Android SDK 36

Так как в репозитории два независимых проекта, открывайте их в Android Studio **по отдельности**:

1. **File → Open** → выбрать папку `groq-ai-chat/` или `transmify/`.
2. Дождаться синхронизации Gradle.
3. Запустить конфигурацию `app` на эмуляторе или устройстве.

> В архивах нет скриптов `gradlew`/`gradlew.bat` и `gradle-wrapper.jar`. Android Studio создаст/подтянет wrapper сама. Для сборки из терминала можно выполнить `gradle wrapper` в папке приложения (при установленном Gradle 9.3.1), после чего использовать `./gradlew assembleDebug`.

## Ключи и настройки

- **Groq AI Chat** — API-ключ Groq задаётся в настройках приложения. При необходимости можно создать `.env` на основе `.env.example` (`GROQ_API_KEY=gsk_...`). Файл `.env` в репозиторий не добавляется.
- **Transmify** — адрес Transmission-сервера и учётные данные настраиваются в приложении. Для поиска по RuTracker могут потребоваться отдельные настройки доступа в соответствующем разделе.

## Git

```bash
git status
git add .
git commit -m "Add Groq AI Chat and Transmify apps"
```

В репозиторий не попадают `build/`, `.gradle/`, `.idea/`, `local.properties`, `.env` и файлы ключей — см. `.gitignore`.
