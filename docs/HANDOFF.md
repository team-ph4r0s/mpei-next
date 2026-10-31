# Где мы остановились (заметка для следующей сессии)

Обновлено 31.10.2026. Репозиторий: `ghostoftheeleven/mpei-next`, работа идёт прямо в `main` (правило владельца: без веток и PR,
пушить каждый законченный срез сразу). Коммиты заканчиваются строкой `Co-Authored-By: Syberia <noreply@syberia.invalid>`.
План iOS и статус по этапам — задача [#5](https://github.com/ghostoftheeleven/mpei-next/issues/5) (чекбоксы в ней и в дочерних
отстают, ориентироваться на этот файл). Рабочая папка: `C:\dev\MPEI Hub\mpei-next` (путь без кириллицы, это важно).

## 1. На чём остановились

- CI по `66551a8a` зелёный (Android и iOS). Последняя работа: Главная iOS доведена до Android (см. раздел 2, пункт 2).
  Всё закоммичено и запушено, незакоммиченных изменений нет.
- **На момент записки CI по `66551a8a`: Android зелёный, iOS ещё идёт** (~25 мин). Первым делом в новой сессии:
  `gh run list --limit 4`. Если iOS упал, смотреть `gh run view <id> --log-failed`: это почти наверняка ошибка компиляции Kotlin/Native
  в общем коде (локально проверяются только `compileCommonMainKotlinMetadata` и `compileIosMainKotlinMetadata`, они не ловят всё).
- Последняя работа: **сравнение iOS-версии с Android через Appetize** и правки по нему. Что нашли и что сделано:
  - сделано: отступы системных панелей применяются один раз (`App.kt`, `WindowInsets.safeDrawing`, верх и бока); нижняя панель из
    `ui_kit_navigationbar` с иконками `ui_icons`; пары — карточка `ClassCard`; расписание по одному дню с `DaySelector`;
    тап по группе на Главной открывает поиск, выбранное расписание не стирается (`ScheduleController.startSearch/cancelSearch`, «Отмена»).
  - **проверено глазами в Appetize** (сборка `a49d225c`, 31.10.2026, светлая и тёмная тема): Главная, поиск группы, расписание с выбором дня,
    Заметки, Карта, БАРС (форма входа), Настройки с выбором темы. Список найденных мелочей — в разделе 2, пункт 1.
- Минут Appetize на 31.10.2026 потрачено около 15–16 из 30 в месяц (счётчик сбрасывается раз в месяц); оставшиеся ~14 минут беречь.
  Вкладка Chrome с Appetize оставлена на главной странице `https://appetize.io/` (сессия симулятора закрыта).
  Прокрутка в Appetize: колесо мыши не работает, нужен `left_click_drag` снизу вверх (первый жест в пачке иногда теряется, делать по два).

## 2. Что делать дальше (по приоритету)

1. **Найдено в Appetize 31.10.2026 (сборка `a49d225c`); пункты про Главную, дату, пустые состояния, «9-я неделя» и кнопку «Настройки» исправлены следующим коммитом, сверить глазами при ближайшем проходе:**
   - Главная: «Ближайшие события» показывает до 12 пар на 7 дней, поэтому «Заметки» и «Избранные расписания» оказываются в самом низу (нужно ~8 жестов прокрутки).
     На Android показывается только первый день с парами: сделать так же (оставить пары первого дня, остальное убрать).
   - Главная: дата («Суббота, 31 октября») переносится на две строки из-за кнопки «Настройки»; вынести «Настройки» в отдельную строку или иконкой.
   - Главная: пустое состояние «Нет ни одной актуальной заметки» центрировано только по заголовку, подпись выровнена влево (нужен `TextAlign.Center`);
     то же в «Заметках» (`EmptyNotes`).
   - Расписание: заголовок недели «9 неделя» без «-я»; у субботы без пар надпись «Занятий нет» мелкая и без оформления.
   - Главная: первый тап по кнопке «Настройки» не сработал, второй сработал (кнопка маленькая; возможно, нужна область нажатия побольше).
   - Не проверено глазами: блок «Избранные расписания» (нужно добавить избранное), баннер обеда (только 12:45–13:45 по Москве), нажатие на заметку,
     светлая тема внизу Главной, вход в БАРС.
2. **Внешний вид iOS до Android — сделано** (карточки пар, Настройки по разделам с выбором темы, Заметки и поиск карточками, тёмная тема работает).
   Карточка пары теперь копия настоящей карточки Android (`ClassesItemCard`: номер кружком, время, плашка типа справа, палитра приложения); БАРС: вкладки на `SegmentedControl`, карточки одной формы. Остаётся: поля ввода БАРС и карта на Material3 (в `ui_kit_*` для них компонентов нет).
3. **Проверка на настоящем iPhone** (нужен владелец): напоминания (разрешение, срабатывание), фоновое обновление и уведомления о заменах
   (`BGAppRefreshTask`, как запустить вручную — в `docs/IOS.md`), вход в БАРС с настоящим 2FA.
4. **Мелкая очистка Android — сделано 31.10.2026**: мёртвая аналитика удалена (модули `lib_analytics_*`, вызовы в 17 экранах), заголовок `X-Device-Id`
   больше нигде не отправляется (`ClientIdentity` без `deviceId`, `DeviceIdProvider` удалён). Осталось: на карте на Android не проверены пинч и двойной тап (через `adb` не воспроизвести).
5. **Задачи из списка** (состояние на 31.10.2026):
   - #17 (карта): все чекбоксы отмечены, задачу можно закрывать (владельцу). #18: отмечены уведомления, Firebase (не делаем) и безопасные зоны; открыты виджет,
     подпись/TestFlight и идентификатор. #15 и #16: открытые чекбоксы настоящие (Room KMP, `SharedPreferences` → `lib_settings`, экраны Android на Compose, «О приложении»), не закрывать.
   - #19 (проверка iOS в Appetize): сделана 31.10.2026 по сборке `a49d225c`, итоги в разделе 2, пункт 1; для закрытия осталась проверка на настоящем iPhone.
   - #13 (ресурсы Compose): на iOS в Appetize видны шрифт Roboto и иконки нижней панели (Compose Resources работают); строки и plural в общем коде не используются,
     на Android не сверялось отдельно.
   - #11 (кнопка «назад»): на iOS в `App.kt` стоят `BackHandler` (жест от края: закрывает «Настройки», выходит из поиска расписания, из других вкладок возвращает на Главную); в Appetize жест не проверялся. #12 (шиммер): код есть,
     в Appetize не наблюдался (сеть быстрая). #14 (Material3 1.4.0-beta01): решение владельца.
   - #20 (единый сетевой клиент) — сделано 31.10.2026 (проверено мной на эмуляторе: релиз с R8 запускается, расписание грузится, вход в БАРС с неверными данными даёт «Неверный логин или пароль»; с настоящим логином и 2FA не проверялось), см. раздел 4: на Android один Ktor-клиент над движком OkHttp, Retrofit, `ext_okhttp` и вендоренный `lib_bars` (с `third_party/`) удалены.
     На эмуляторе не проверены: вход в БАРС с настоящими логином и кодом (нет учётных данных), выбор студента, оценки, переживание перезапуска сессии в Keystore. Проверено: форма входа доходит до bars.mpei.ru и показывает «Неверный логин или пароль» (debug и release с R8), расписание, поиск, офлайн, Chucker.
6. **Решено не делать** (не возвращаться без новой просьбы владельца): замена Android-моделей расписания (`feature_schedule_api`, `ScheduleDto`) на общие
   (77 файлов, `java.time`, Bundle, виджет, будильники — пользы для iOS нет); Firebase на iOS и на Android (убран из обеих версий).
7. **Отложено до решения владельца**: виджет WidgetKit (Swift, отдельное расширение; на бесплатном Apple ID App Groups ограничены, можно сделать вариант,
   где виджет сам ходит в сеть по названию группы из своих настроек, но без устройства его нельзя проверить); TestFlight (нужен Apple Developer Program).

## 3. Чем проверять

### Android (локально, до пуша — Android Studio у владельца установлен, но всё делается и из консоли)
- Сборка и тесты: `./gradlew :app:assembleDebug` (~3 мин с нуля), `./gradlew testDebugUnitTest` (~2,5 мин), `./gradlew :<модуль>:jvmTest` для общих модулей,
  релиз с R8: `./gradlew :app:assembleRelease` (R8 с Ktor и ресурсами Compose проходит; релиз тоже запускали на эмуляторе).
- Эмулятор без окна: `emulator.exe -avd Medium_Phone_API_37.0 -no-window -no-audio -no-snapshot -gpu swiftshader_indirect` (SDK:
  `C:\Users\rrust\AppData\Local\Android\Sdk`), загрузка ~40 с, дальше `adb install -r modules/app/build/outputs/apk/debug/MPEI-Hub-v0.5.2-debug.apk`,
  пакет `ru.mpei.hub.dev` (релиз — `ru.mpei.hub`), скриншот `adb exec-out screencap -p > shot.png`, холодный старт ~10–25 с (сначала чёрный экран).
  Нижняя панель на экране 1080×2400: тапы по `y=2250`, `x=108/322/540/756/972` (Главная, Расписание, БАРС, Карта, Профиль).
  `adb input text` не умеет кириллицу: группу искать цифрами (`03-25`) и выбирать подсказку. Группа А-03-25 имеет пары на этой неделе; у А-01-22 пар нет.
  При диплинках на эмуляторе указывать пакет: `adb shell am start -p ru.mpei.hub.dev -a android.intent.action.VIEW -d 'mpeix://map?placeUid=…'`.
- Демо UI-кита `ui_kit_demo`: `MapDemoActivity` показывает карту без остального приложения (`am start -n kekmech.ru.mpeiapp.demo/.MapDemoActivity`);
  главный экран демо (`navigation3`) падает из-за конфликта `activity 1.12` и Compose UI 1.9 (было и до этих работ), в демо закреплён `activity 1.11`.

### iOS
- **Локально собирается только общий код**: `./gradlew :shared:compileCommonMainKotlinMetadata :shared:compileIosMainKotlinMetadata`. Тесты общих модулей
  идут на JVM (`:feature_schedule_shared:jvmTest`, `:feature_map_shared:jvmTest`, `:feature_notes_shared:jvmTest`, `:lib_settings:jvmTest` и т.д.); на iOS-симуляторе их гоняет CI.
- **CI**: workflow `iOS` (macOS-раннер, 15–30 мин) собирает фреймворк и неподписанный `.ipa` и гоняет тесты на симуляторе; `Android` — ~2–7 мин.
  Раннер macOS иногда не находится 15 минут и прогон отменяется без шагов — это не ошибка кода, `gh run rerun`.
- **Посмотреть интерфейс iOS — через Appetize** (облачный симулятор; токен — секрет репозитория `APPETIZE_API_TOKEN`, я его не вижу, он нужен только workflow):
  1. `gh workflow run "iOS Simulator"` (собирает `.app` под симулятор, ~25–31 мин, минуты Appetize не тратит); дождаться: `gh run watch <id>`.
  2. `gh workflow run "Appetize Upload" -f run_id=<id запуска iOS Simulator>` (~10 с–9 мин). Публичный ключ приложения:
     `gh run view <id загрузки> --log | grep -E "Appetize public key: [A-Za-z0-9_-]+" | grep -v '\${'`. Страница: `https://appetize.io/app/<ключ>`
     (приложение приватное, открывается только владельцу аккаунта; ключ не секретный, `privateKey` в логи не печатать).
  3. Открыть страницу в Chrome (плагин Claude in Chrome, своя вкладка через `navigate`; вход в Appetize у владельца уже есть). По умолчанию там iPhone 14 Pro, iOS 17.2.
  4. **Лимит владельца: 30 минут симулятора в месяц.** Время идёт, пока открыта сессия («Tap to Start»). Заранее расписать маршрут, делать **один короткий проход
     через `browser_batch`** (клик, пауза, `zoom` по области телефона), потом **сразу уйти на другую страницу** (навигация на `https://appetize.io/` закрывает сессию).
     Рекомендуемый проход (6–7 мин): Главная без группы → Расписание (поле поиска, ввод `03-25`, подождать ~6 с, тап по «А-03-25») → Главная → Заметки → Карта → БАРС → «Настройки»
     (кнопка на Главной). Экран телефона в окне ~230×460 px, поэтому снимать `zoom` по региону телефона; координаты вкладок нижней панели считать от первого снимка.
     Ввод с клавиатуры хозяина проходит в симулятор (кириллицу не проверяли).
  5. Эталон Android снимать на эмуляторе тех же экранов (см. выше) и сравнивать по списку отличий из раздела 2.
- Скриншоты настоящего iPhone (тёмная тема) владелец присылает из CI-артефакта `ios-ipa-unsigned` через Sideloadly (инструкция в `docs/IOS.md`).

## 4. Что есть в коде (кратко, по модулям)

Общий код живёт в KMP-модулях (цели jvm + iosArm64 + iosSimulatorArm64, конвенция `mpeix.lib.kmp`; с Android-таргетом — `mpeix.lib.kmp.android`).
- **Фундамент (M1):** `ext_kotlin`, `ext_koin`, `lib_coroutines`, `lib_elm`, `ext_datetime`, `lib_settings` (`KeyValueStore`: `NSUserDefaults` на iOS, адаптер
  `SharedPreferences` в `ext_shared_preferences`), `lib_http` (Ktor: `createHttpClient` для JSON API, `createPageHttpClient` для страниц чужих сайтов; каталог `BackendServiceUrl`). На Android `lib_network` собирает единственный клиент над движком OkHttp (заголовки приложения = `ClientIdentity`, проверка соединения `NoConnectionInterceptor`, Chucker в debug/qa — перехватчики движка, лог Ktor вне PROD),
  `lib_mpei_api`, `lib_navigator`, `lib_navigator_compose`, `lib_elm_ui`.
- **Дизайн (M2):** `ui_theme` (палитра, шрифты Roboto), `ui_kit_*` (навигация, кнопки, диалоги, списки, переключатели…), `ui_icons`, `ui_shimmer`, `ui_map` (см. ниже).
- **Расписание (M3), `feature_schedule_shared`:** модели (`@Serializable`), `MpeiScheduleMapper`, `ScheduleService` (кэш id под `Mutex`, студенты отфильтрованы),
  `ScheduleCache`, `FavoriteSchedules`, `ScheduleController`, `WeekLoader` (неделя из сети или из копии), `DashboardController` + `upcomingClasses`,
  `ClassReminderPlanner`/`RemindersController`, `ScheduleChangeDetector`/`ScheduleChangeText`/`ScheduleSnapshots`/`ScheduleChangeChecker`/`ScheduleBackgroundSync`.
  Android ходит в сеть через `KtorScheduleService` (обёртка над общим сервисом, отдаёт старые `ScheduleDto`; `ResponseException` Ktor остаётся как есть, `HttpRequestTimeoutException` переводится в `SocketTimeoutException`).
- **Заметки (M4):** `feature_notes_shared` (JSON в `KeyValueStore`; Android остаётся на Room).
- **Карта (M5):** `feature_map_shared` (`MapMarkersRepository`, `MapController`, `WebMercator`, `MapCamera`) и `ui_map` (`MpeiMap`, `MapScreen`, `TileLoader`, `MarkerIcons`).
  **Android `MapFragment` тоже на `MapScreen`** (`osmdroid`, ELM-стор карты, шторка, `lib_emoji` удалены); поиск мест (`feature_search`) всё ещё берёт метки из старого `MapRepository`.
  Тайлы грузит отдельный клиент `tileHttpClient` (на обеих платформах).
- **БАРС (M6):** `feature_bars_shared`, `lib_bars_client`, `lib_bars_parser`; на iOS сессия в Keychain, на Android в `KeystoreBarsSessionStore` (AES-GCM, ключ в Android Keystore). Экраны Android (`feature_bars_impl`) ходят в портал через тот же `BarsClient` (`BarsApi`), клиент страниц — `createPageHttpClient` из `lib_http` (без заголовков приложения). После обновления с версии на `lib_bars` БАРС просит войти заново (старая сессия не переносится).
- **iOS-оболочка:** `modules/shared` — `App`, `AppDependencies` (всё, из чего собрано приложение), `IosAppGraph`, экраны (`dashboard`, `schedule`, `notes`, `settings`, `bars`, `ClassCard`,
  `AppNavigationBar`), `IosReminderScheduler`, `IosChangeNotifier`, `IosBackground`; Swift: `iosApp/iosApp/` (`iOSApp.swift`, `ContentView.swift`, `BackgroundRefresh.swift`), проект генерирует XcodeGen из `iosApp/project.yml`.
- **Настройки без выпуска версии:** `statics/app/config.json` (принудительное обновление, тоггл снежинок; читает `RepoRemoteConfig` на Android). **Firebase нет нигде**, README это утверждает и это правда.

## 5. Подводные камни (не наступать снова)

- **kotlinx-datetime 0.7.1:** Ktor тянет 0.7.1, там `Clock`/`Instant` — typealias на `kotlin.time`; с объявленной 0.6.2 линковка iOS падала (`IrTypeAliasSymbolImpl is already bound`).
  В общем коде только `kotlin.time.Clock/Instant` с `@OptIn(ExperimentalTime)`. `LocalDate.until` возвращает `Long`.
- **Kotlin/Native: в именах тестов (в обратных кавычках) нельзя `( ) , . ; : / < >`.**
- **Тесты контроллеров:** `MockEngine` Ktor отвечает на реальном диспетчере — ждать через `settle` (`advanceUntilIdle` + реальная пауза); для корутин из `backgroundScope`
  без сети вместо `advanceUntilIdle()` нужен `runCurrent()` (иначе фоновые задачи не запускаются).
- **GitHub raw отдаёт `text/plain`:** Ktor-десериализатор не сработает, читать `bodyAsText()` и разбирать вручную.
- **Отступы iOS:** один раз в `App.kt` (`safeDrawing`, верх и бока). Не `safeContent`: он включает зоны жестов и даёт боковые поля. Экраны отступы сами не добавляют.
- **Compose Multiplatform 1.9.3** жёстко задаёт `material3:1.4.0-beta01` для всего Android-приложения; `material-icons-core` в 1.4 нужно добавлять явно.
- **`local.properties`** (в `.gitignore`) создавать с прямыми слэшами: `sdk.dir=C:/Users/rrust/AppData/Local/Android/Sdk`.
- **Кириллица в пути ломает JVM-тесты** (компиляция проходит, тесты падают с `ClassNotFoundException`).
- **Python в этой среде нет** (заглушка Microsoft Store): правки делать `sed`/`perl`/инструментами Edit и Write; длинные heredoc с кириллицей в bash иногда обрываются — писать файл инструментом Write.
- Перед правкой большого экрана читать его целиком: серия `sed`-вставок уже ломала файлы (метод оказывался не в том классе).

## 6. Ждём от владельца

- Проверка `.ipa` на iPhone/iPad (iOS 15+, режим разработчика, отдельный Apple ID, Sideloadly, iTunes/iCloud с сайта Apple): напоминания, фон, БАРС с 2FA, тёмная тема.
- Позиция ИВЦ МЭИ по доступу к БАРС из приложения (влияет на публикацию в сторах).
- Apple Developer Program для TestFlight и виджета (оплата из России проблематична).
- Решение: нужен ли виджет iOS (и в каком виде).
- Вне плана iOS: задачи #8 (подготовка Android к сторам; секрет `GOOGLE_SERVICES` в GitHub больше не нужен, его можно удалить) и #9 (ветка `master` нужна установленным v0.5.x).
