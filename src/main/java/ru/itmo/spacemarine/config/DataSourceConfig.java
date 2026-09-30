package ru.itmo.spacemarine.config;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Пул соединений с PostgreSQL (pg / studs).
 * Логин и пароль берутся из переменных окружения DB_USER / DB_PASSWORD
 * (синтаксис ${ENV=...} понимает Payara), чтобы не хранить их в коде.
 * Для локального запуска через SSH-туннель: DB_URL=jdbc:postgresql://localhost:5432/studs
 *
 * Если DataSource у вас уже настроен иначе (glassfish-resources.xml, asadmin),
 * этот класс можно удалить — главное, чтобы JNDI-имя совпало с persistence.xml.
 */
@DataSourceDefinition(
        name = "java:app/jdbc/studs",
        className = "org.postgresql.ds.PGSimpleDataSource",
        url = "${ENV=DB_URL}",
        user = "${ENV=DB_USER}",
        password = "${ENV=DB_PASSWORD}",
        minPoolSize = 1,
        maxPoolSize = 5
)
@ApplicationScoped
public class DataSourceConfig {
}
