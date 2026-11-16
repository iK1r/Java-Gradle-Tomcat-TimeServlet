# Time Servlet — модуль 10

У цьому домашньому завданні я зробив невеликий вебзастосунок на Java для Apache Tomcat 9. Він показує поточний час на HTML-сторінці. Часовий пояс можна передати в адресі, а неправильні значення перевіряє окремий вебфільтр.

## Що реалізовано

- **`TimeServlet`** — обробляє `GET /time` та повертає HTML-сторінку з поточним часом до секунд. Якщо параметра `timezone` немає, використовується `UTC`.
- **`TimezoneValidateFilter`** — перехоплює запити на `/time` і перевіряє часовий пояс. Якщо значення неправильне, повертає HTTP `400` та повідомлення `Invalid timezone`.
- **`TimezoneResolver`** — допоміжний клас для розбору часових поясів через `java.time`. Виніс його окремо, щоб не змішувати перевірку параметра з формуванням HTML.

Програма підтримує, наприклад, `UTC`, `UTC+2`, `UTC-3`, `UTC+05:30` та `Europe/Kyiv`. Символ `+` у query-параметрах краще передавати як `%2B`. Також обробляється випадок, коли він декодувався як пробіл.

## Технології

- Java 17+ (у мене перевірено на JDK 26.0.1);
- Gradle 9.4.1 та плагін `war`;
- Servlet API `javax.servlet:javax.servlet-api:4.0.1`;
- Apache Tomcat 9.x.

Використовується саме `javax.servlet`, тому для запуску потрібен Tomcat 9. У Tomcat 10 і новіших версіях API перейменовано на `jakarta.servlet`.

## Як зібрати проєкт

У репозиторії вже є **Gradle Wrapper**, тому окремо встановлювати Gradle або копіювати його файли з попередніх модулів не потрібно.

Відкрий папку проєкту в IntelliJ IDEA або запусти PowerShell у її корені:

```powershell
.\gradlew.bat clean check war
```

Команда компілює код, запускає локальну перевірку `TimeServletSelfTest` та створює WAR-файл:

```text
build\libs\ROOT.war
```

`TimeServletSelfTest` перевіряє час UTC, інші часові пояси, обробку символу `+` та відповідь HTTP `400` на неправильні значення. Ці перевірки не потребують запущеного Tomcat.

Назва `ROOT.war` потрібна для того, щоб після розгортання сторінка відкривалася за адресою `/time`, без назви проєкту в URL.

## Запуск через Tomcat 9 на Windows

1. Завантажити **Core → 64-bit Windows zip** з [офіційної сторінки Tomcat 9](https://tomcat.apache.org/download-90.cgi) і розпакувати архів у зручну папку.
2. Якщо Tomcat уже працює, спочатку зупинити його. Якщо у папці `webapps` є стандартний каталог `ROOT`, перенести його за межі `webapps` як резервну копію.
3. Скопіювати зібраний `ROOT.war` до папки `webapps` у Tomcat.
4. Вказати правильний шлях до встановленого JDK та запустити Tomcat.

Наприклад, для моєї локальної конфігурації після виконання збірки використовую такі команди в PowerShell:

```powershell
$tomcat = "F:\ProjectsTEST\apache-tomcat-9.0.124"

# Виконується лише один раз, якщо ще є стандартна папка ROOT
if ((Test-Path "$tomcat\webapps\ROOT") -and
    -not (Test-Path "$tomcat\ROOT-original-backup")) {
    Move-Item "$tomcat\webapps\ROOT" "$tomcat\ROOT-original-backup"
}

Copy-Item ".\build\libs\ROOT.war" "$tomcat\webapps\ROOT.war" -Force

$env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.1"
$env:JRE_HOME = $env:JAVA_HOME
$env:CATALINA_HOME = $tomcat
$env:CATALINA_BASE = $tomcat

& "$tomcat\bin\catalina.bat" run
```

Шляхи до Tomcat та JDK у прикладі — з мого комп'ютера. На іншому ПК потрібно підставити свої. Якщо Tomcat запущено командою `catalina.bat run`, його логи залишаються у вікні PowerShell. Зупинити сервер можна через `Ctrl + C`.

### Чому я використовую порт 8081

Стандартний порт Tomcat — **8080**, але на моєму комп'ютері він був зайнятий процесом Steam (`steamwebhelper.exe`). Тому в файлі `conf/server.xml` я змінив порт HTTP Connector із `8080` на **8081**:

В існуючому елементі `Connector` я замінив лише значення `port="8080"` на `port="8081"`. Інші атрибути у `server.xml` залишилися без змін.

Якщо порт 8080 вільний, його можна не змінювати. У такому разі всі посилання нижче потрібно відкривати з портом `8080` замість `8081`.

## Перевірка в браузері

На моєму комп'ютері застосунок працює за адресами:

| Адреса | Очікуваний результат |
| --- | --- |
| `http://localhost:8081/time` | Поточний час UTC, HTTP 200 |
| `http://localhost:8081/time?timezone=UTC%2B2` | Час UTC+2, HTTP 200 |
| `http://localhost:8081/time?timezone=UTC-3` | Час UTC-3, HTTP 200 |
| `http://localhost:8081/time?timezone=Europe/Kyiv` | Час за часовим поясом Києва, HTTP 200 |
| `http://localhost:8081/time?timezone=UTC%2B99` | `Invalid timezone`, HTTP 400 |
| `http://localhost:8081/time?timezone=Mars/Olympus` | `Invalid timezone`, HTTP 400 |

Сторінка формує поточний час під час кожного GET-запиту, тому після оновлення сторінки час змінюється.

HTTP-статус помилки можна перевірити в PowerShell:

```powershell
curl.exe -i "http://localhost:8081/time?timezone=UTC%2B99"
```

У відповіді має бути статус `HTTP/1.1 400` та HTML із текстом `Invalid timezone`. Я також перевірив роботу звичайного `/time` і `UTC+2` безпосередньо в браузері.

## Що є в репозиторії

На GitHub завантажені вихідні Java-класи, `build.gradle`, `settings.gradle`, `web.xml`, локальні тести, README та Gradle Wrapper. Папки `.idea`, `.gradle` та `build` виключені через `.gitignore`. Зібраний `ROOT.war` міститься всередині `build`, тому також не потрапляє до репозиторію.
