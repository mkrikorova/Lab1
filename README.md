# SpaceMarine — backend (Jakarta EE 10 + CDI + EclipseLink + Payara Micro)

## Структура
```
src/main/resources/schema.sql          таблицы, ограничения, 5 функций БД
src/main/resources/META-INF/persistence.xml
src/main/webapp/WEB-INF/beans.xml
ru.itmo.spacemarine
 ├─ config       DataSourceConfig (пул к pg/studs), RestConfig (/api), CorsFilter
 ├─ model        SpaceMarine, Coordinates, Chapter, MeleeWeapon, ZonedDateTimeConverter
 ├─ dto          DTO запросов/ответов, фильтр таблицы, страница, ошибка, DtoMapper
 ├─ repository   JPA-доступ к данным; SpecialOperationsRepository вызывает функции БД
 ├─ service      бизнес-логика, транзакции (@Transactional), исключения, CDI-события
 ├─ controller   REST (JAX-RS) + mapper'ы исключений в JSON
 └─ websocket    уведомления клиентов об изменениях (ws/changes)
```

## 1. База
```bash
psql -h pg -d studs -f src/main/resources/schema.sql
```
Скрипт можно запускать повторно — он всё пересоздаёт (данные удаляются!).

## 2. Сборка и запуск
```bash
mvn package
export DB_URL=jdbc:postgresql://pg:5432/studs DB_USER=s123456 DB_PASSWORD=...
java -jar payara-micro.jar --deploy target/spacemarine.war --port 8080
```
Локально через туннель: `ssh -L 5432:pg:5432 s123456@helios.cs.ifmo.ru -p 2222`
и `DB_URL=jdbc:postgresql://localhost:5432/studs`.

## 3. REST API (`/api`)
| Метод | Путь | Что делает |
|---|---|---|
| GET | /space-marines?page=0&size=10&name=&chapterName=&meleeWeapon=&sort=name&order=asc | таблица (пагинация, фильтр по полному совпадению, сортировка) |
| GET | /space-marines/all | все объекты без пагинации (для карты) |
| GET | /space-marines/{id} | объект + связанные coordinates и chapter |
| POST | /space-marines | создать |
| PUT | /space-marines/{id} | изменить |
| DELETE | /space-marines/{id} | удалить |
| GET/POST/PUT | /chapters, /coordinates | вспомогательные объекты (списки для выбора) |
| DELETE | /chapters/{id}?reassignTo={id2} | удалить орден; без reassignTo при связях → 409 |
| DELETE | /coordinates/{id}?reassignTo={id2} | то же для координат |
| GET | /operations/heart-count-sum | сумма heartCount — `sm_sum_heart_count()` |
| GET | /operations/group-by-melee-weapon | группы по оружию — `sm_group_by_melee_weapon()` |
| GET | /operations/name-contains?substring=an | поиск по подстроке — `sm_find_by_name_substring()` |
| POST | /operations/chapters | создать орден — `sm_create_chapter()` |
| DELETE | /operations/chapters/{id} | распустить орден — `sm_disband_chapter()` |

Пример создания (новые координаты + существующий орден):
```json
{ "name": "Titus", "coordinates": {"x": 1, "y": 2.5}, "chapterId": 1,
  "health": 100, "heartCount": 2, "height": 200, "meleeWeapon": "CHAIN_SWORD" }
```
Для координат и ордена передаётся ровно одно: `coordinatesId` **или** `coordinates`, `chapterId` **или** `chapter`.

Ошибки всегда приходят как
`{"status":400,"message":"Некорректные данные","details":["health: health должно быть больше 0"]}`.

## 4. WebSocket
`ws://<хост>:<порт>/<контекст>/ws/changes` — сервер присылает после каждого успешного изменения:
```json
{"entity":"spaceMarine","action":"CREATED","id":5}
```
`entity`: spaceMarine | chapter | coordinates; `action`: CREATED | UPDATED | DELETED;
`id: null` — изменилось несколько объектов (перепривязка, роспуск ордена).
Клиент на сообщение просто перезапрашивает текущую страницу таблицы.

Проверка: открыть `ws-test.html` (лежит рядом с `/api`) в двух вкладках и сделать POST через curl.

## 5. Фронтенд (React + Vite)
Исходники — `frontend/`, собранный результат — `src/main/webapp/app/` (уже собран и попадает в war).
Открывать: `http://localhost:61131/spacemarine/` (перенаправит на `app/`).

Если меняли фронтенд — пересобрать (нужен Node.js 18+), потом `mvn package`:
```bash
cd frontend
npm install        # один раз
npm run build      # -> ../src/main/webapp/app
```
Разработка с горячей перезагрузкой: открыть туннель к helios и `npm run dev`
(http://localhost:5173, запросы /api и /ws проксируются на localhost:61131/spacemarine).

```
frontend/src
 ├─ api.js            все запросы к REST, разбор ошибок
 ├─ live.js           одно WebSocket-соединение, useChanges(), индикатор связи
 ├─ validation.js     клиентская проверка полей (те же правила, что в БД)
 ├─ App.jsx           меню и маршруты
 ├─ components/       Modal, Field, MarineForm, MarineView, ReassignDialog, Pagination
 └─ pages/            MarinesPage, ChaptersPage, CoordinatesPage (через HelperPage), OperationsPage,
                      MapPage + mapMath.js (карта: масштаб, сетка, группировка фигурок)
```
