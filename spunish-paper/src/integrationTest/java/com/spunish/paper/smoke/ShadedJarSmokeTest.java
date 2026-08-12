package com.spunish.paper.smoke;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class ShadedJarSmokeTest {

    private static final String RELOCATED_DRIVER = "com.spunish.libs.mysql.cj.jdbc.Driver";
    private static final String ORIGINAL_DRIVER = "com.mysql.cj.jdbc.Driver";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Test
    void relocatedDriverOpensARealConnectionAndTheOriginalClassNameIsGone() throws Exception {
        Path jarPath = Path.of(System.getProperty("spunish.shadedJar"));
        assertThat(jarPath).exists();

        try (URLClassLoader isolated = new URLClassLoader(
                new URL[] {jarPath.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Driver driver = (Driver) isolated.loadClass(RELOCATED_DRIVER).getDeclaredConstructor().newInstance();

            Properties credentials = new Properties();
            credentials.setProperty("user", MYSQL.getUsername());
            credentials.setProperty("password", MYSQL.getPassword());
            try (Connection connection = driver.connect(MYSQL.getJdbcUrl(), credentials)) {
                assertThat(connection.isValid(2)).isTrue();
            }

            assertThatThrownBy(() -> isolated.loadClass(ORIGINAL_DRIVER)).isInstanceOf(ClassNotFoundException.class);
        }
    }
}
