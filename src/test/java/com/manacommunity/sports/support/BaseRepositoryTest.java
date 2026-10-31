package com.manacommunity.sports.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all @DataJpaTest repository tests.
 *
 * Uses H2 (PostgreSQL-mode) configured in application-test.yaml.
 * Replace.NONE tells Spring to use our configured H2 instead of
 * auto-replacing with its own embedded DB.
 *
 * Schema is created by src/test/resources/sql/init-schema.sql
 * before Hibernate runs DDL.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.manacommunity")
public abstract class BaseRepositoryTest {
}
