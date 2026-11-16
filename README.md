# Time Servlet — модуль 10

У цьому домашньому завданні я зробив вебзастосунок для Tomcat 9. Він показує поточний час через сервлет, а фільтр перевіряє часовий пояс із query-параметра.

## Що зроблено

- `TimeServlet` — обробляє `GET /time` і повертає HTML із часом до секунд та часовим поясом.
- `TimezoneValidateFilter` — перевіряє параметр `timezone` перед передачею запиту в сервлет. Для неправильного значення повертає HTTP `400` і сторінку `Invalid timezone`.
- `TimezoneResolver` — окремо розбирає часові пояси, щоб не змішувати перевірку та виведення HTML. Використовую `java.time.ZoneId`, який не замінює невідомі пояси на GMT.

За відсутності параметра використовується `UTC`. Працюють значення на кшталт `UTC+2`, `UTC-3`, `UTC+05:30`, `Europe/Kyiv` та `America/New_York`.

У query-параметрах символ `+` може декодуватись як пробіл. Тому фільтр також розпізнає `UTC 2` як `UTC+2`. Якщо записувати URL вручну, правильніше кодувати плюс через `%2B`.

## Технології

- Java 17+;
- Gradle (плагін `war`);
- `javax.servlet:javax.servlet-api:4.0.1` (`compileOnly`);
- Apache Tomcat **9.x**.

Для Tomcat 10/11 цей код не підходить без переходу на `jakarta.servlet`. У матеріалі GoIT використовується саме `javax.servlet`, тому я залишив його.

## Як зібрати

Відкрити папку в IntelliJ IDEA та дочекатись Gradle Sync. В архіві немає файлів Gradle Wrapper, щоб не дублювати двійкові файли попередніх завдань. Їх можна скопіювати з дев'ятого модуля:

```powershell
cd "F:\ProjectsTEST\Java-Gradle-Tomcat-TimeServlet"
Copy-Item "..\Java-Gradle-HTTP-Status-Cats\gradlew" . -Force
Copy-Item "..\Java-Gradle-HTTP-Status-Cats\gradlew.bat" . -Force
New-Item -ItemType Directory -Force ".\gradle\wrapper" | Out-Null
Copy-Item "..\Java-Gradle-HTTP-Status-Cats\gradle\wrapper\*" ".\gradle\wrapper\" -Force
```

У PowerShell із кореня проєкту:

```powershell
.\gradlew.bat clean check war
```

`check` запускає `TimeServletSelfTest`: перевіряє UTC, різні часові пояси, декодування `+`, HTML та відповідь `400` на неправильні значення. Ці тести працюють локально без Tomcat.

Готовий файл: `build/libs/ROOT.war`. Назва `ROOT.war` потрібна, щоб додаток відкривався без префікса — прямо за `/time`.

## Запуск на Tomcat 9 (Windows)

1. Завантажити Core → 64-bit Windows zip зі сторінки https://tomcat.apache.org/download-90.cgi та розпакувати Tomcat в окрему папку, наприклад `F:\ProjectsTEST\apache-tomcat-9.0.xxx`.
2. Переконатись, що встановлена Java, і зупинити Tomcat, якщо він уже працює.
3. Якщо в `webapps` є стандартний каталог `ROOT`, зробити його резервну копію **за межами `webapps`**, щоб він не заважав новому застосунку.
4. Скопіювати `build\libs\ROOT.war` у `webapps\ROOT.war` каталогу Tomcat.
5. Запустити `<TOMCAT_HOME>\bin\startup.bat`.
6. Відкрити в браузері `http://localhost:8080/time`.

Якщо порт 8080 уже використовується, слід зупинити інший процес або змінити порт у `conf/server.xml` Tomcat.

## Приклади для браузера

| URL | Очікування |
| --- | --- |
| `http://localhost:8080/time` | HTML, поточний час UTC, HTTP 200 |
| `http://localhost:8080/time?timezone=UTC%2B2` | HTML, час UTC+2, HTTP 200 |
| `http://localhost:8080/time?timezone=UTC+2` | Також працює, з поправкою на декодування `+` |
| `http://localhost:8080/time?timezone=Europe/Kyiv` | HTML, місцевий час Києва, HTTP 200 |
| `http://localhost:8080/time?timezone=UTC%2B99` | HTML `Invalid timezone`, HTTP 400 |
| `http://localhost:8080/time?timezone=Mars/Olympus` | HTML `Invalid timezone`, HTTP 400 |

Вебзастосунок формує час щоразу при GET-запиті, тому сторінка показує актуальний час при оновленні браузера.

## Файли на GitHub

В репозиторій додаються Java-класи, `build.gradle`, `settings.gradle`, `src/main/webapp/WEB-INF/web.xml`, тести, README, `.gitignore` та файли Gradle Wrapper. Сам Tomcat, локальні налаштування IntelliJ і створений WAR не потрібно додавати — їх виключено через `.gitignore`.
