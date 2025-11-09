# AwardsDownloadService (сервис для загрузки наград)

Микросервис для загрузки, обработки и сохранении наград сотрудников из файлов (CSV, Excel) через REST API.

## Технологический стек:

* Java 21
* Spring Boot 3.2.0
* Gradle 8.14.3
* Spring Data JPA
* H2 Database
* Spring Web - REST API
* Lombok
* Apache POI - работа с Excel файлами
* OpenCSV - работа с CSV файлами
* SpringDoc OpenAPI 3
* Flyway
* JUnit 5
* Mockito

## База данных
### Миграции Flyway:
* V1__Create_employees_table.sql - таблица сотрудников
* V2__Create_rewards_table.sql - таблица наград
* V3__Insert_test_employees.sql - добавление тестовые данные

## Полезные endpoints для отладки:
* H2DB Console: http://localhost:8081/h2-console
* Swagger UI: http://localhost:8081/swagger-ui.html