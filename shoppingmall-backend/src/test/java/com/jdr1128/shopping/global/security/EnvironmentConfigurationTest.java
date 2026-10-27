package com.jdr1128.shopping.global.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentConfigurationTest {
    @TempDir Path directory;

    @Test
    void importsDotEnvWithoutAdditionalLibrary() throws Exception {
        Path env = directory.resolve(".env");
        Files.writeString(env, "DB_URL=jdbc:h2:mem:env-probe\nDB_USERNAME=sa\nDB_PASSWORD=fixture-password\n"
                + "JWT_SECRET=test-only-dotenv-probe-secret-at-least-48-bytes-long\n");
        Path config = writeConfig(env.toUri() + "[.properties]");
        runner(config).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:env-probe");
            assertThat(context.getEnvironment().getProperty("spring.datasource.password")).isEqualTo("fixture-password");
            assertThat(context.getEnvironment().getProperty("jwt.secret")).startsWith("test-only-dotenv-probe");
        });
    }

    @Test
    void testProfileRunsWithoutAnyLocalEnvFile() throws Exception {
        writeConfig("optional:" + directory.resolve("absent.env").toUri() + "[.properties]");
        Files.copy(Path.of("src/main/resources/application-test.properties"), directory.resolve("application-test.properties"));
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.location=" + directory.toUri(), "spring.profiles.active=test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
                    assertThat(context.getEnvironment().getProperty("jwt.secret")).startsWith("test-only-");
                });
    }

    private Path writeConfig(String envImport) throws Exception {
        String source = Files.readString(Path.of("src/main/resources/application.properties"));
        Path config = directory.resolve("application.properties");
        Files.writeString(config, source.replaceFirst("(?m)^spring.config.import=.*$", Matcher.quoteReplacement("spring.config.import=" + envImport)));
        return config;
    }

    private ApplicationContextRunner runner(Path config) {
        return new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.location=" + config.toUri());
    }
}
