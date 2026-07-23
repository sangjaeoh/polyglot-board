package com.board.test.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.board.test.architecture.fixture.GenericBaseCaller;
import com.board.test.architecture.fixture.GenericBaseRepository;
import com.board.test.architecture.fixture.PlainNote;
import com.board.test.architecture.fixture.PlainNoteCaller;
import com.board.test.architecture.fixture.PlainNoteRepository;
import com.board.test.architecture.fixture.SoftDeletedNote;
import com.board.test.architecture.fixture.SoftDeletedNoteCaller;
import com.board.test.architecture.fixture.SoftDeletedNoteRepository;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/**
 * base finder 금지 규칙의 위반·예외 경로를 픽스처로 상시 검증한다.
 *
 * <p>제네릭 엔티티 해석과 fail-closed 분기가 비자명해 규칙 자체를 단위 테스트한다. 픽스처는 테스트 소스라
 * 본 {@code @AnalyzeClasses} 분석({@code DoNotIncludeTests})에는 포함되지 않는다.
 */
class BaseFinderRuleTest {

    @Test
    void baseFinderOnSoftDeleteRepositoryIsViolation() {
        JavaClasses fixture = new ClassFileImporter()
                .importClasses(SoftDeletedNote.class, SoftDeletedNoteRepository.class, SoftDeletedNoteCaller.class);

        boolean violated = ArchitectureTest.soft_delete_repositories_do_not_serve_base_finders
                .evaluate(fixture)
                .hasViolation();

        assertThat(violated).isTrue();
    }

    @Test
    void baseFinderViaUnresolvableRepositoryIsViolation() {
        JavaClasses fixture = new ClassFileImporter()
                .importClasses(PlainNote.class, GenericBaseRepository.class, GenericBaseCaller.class);

        boolean violated = ArchitectureTest.soft_delete_repositories_do_not_serve_base_finders
                .evaluate(fixture)
                .hasViolation();

        assertThat(violated).isTrue();
    }

    @Test
    void baseFinderOnEntityWithoutDeletedAtIsAllowed() {
        JavaClasses fixture = new ClassFileImporter()
                .importClasses(PlainNote.class, PlainNoteRepository.class, PlainNoteCaller.class);

        boolean violated = ArchitectureTest.soft_delete_repositories_do_not_serve_base_finders
                .evaluate(fixture)
                .hasViolation();

        assertThat(violated).isFalse();
    }
}
