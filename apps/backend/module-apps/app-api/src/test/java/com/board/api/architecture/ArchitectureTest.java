package com.board.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.board.common.core.error.BaseException;
import com.board.common.core.error.ErrorCode;
import com.board.common.jpa.entity.BaseTimeEntity;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 모듈 경계·네이밍 불변식을 빌드가 강제한다(리뷰가 아니라). */
@AnalyzeClasses(packages = "com.board", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final Set<String> BASE_FINDER_NAMES = Set.of("findById", "findAll", "count");

    private static final DescribedPredicate<JavaMethodCall> BASE_JPA_FINDER =
            new DescribedPredicate<>("base JpaRepository finder (findById·findAll·count)") {
                @Override
                public boolean test(JavaMethodCall call) {
                    return BASE_FINDER_NAMES.contains(call.getTarget().getName())
                            && call.getTargetOwner().isAssignableTo(JpaRepository.class);
                }
            };

    /** 앱은 리포지토리에 직접 접근하지 않는다(도메인 서비스·파사드 경유). */
    @ArchTest
    static final ArchRule apps_do_not_access_repositories = noClasses()
            .that()
            .resideInAPackage("com.board.api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.board.board.repository..");

    /** 엔티티는 도메인 모듈 밖(앱)에 노출되지 않는다. 경계는 Info로 넘는다. */
    @ArchTest
    static final ArchRule entities_do_not_leak_to_apps = noClasses()
            .that()
            .resideInAPackage("com.board.api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.board.board.entity..");

    /** 도메인은 앱·web을 의존하지 않는다(의존은 한 방향). */
    @ArchTest
    static final ArchRule domain_does_not_depend_on_apps_or_web = noClasses()
            .that()
            .resideInAPackage("com.board.board..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.board.api..", "com.board.common.web..");

    /** 도메인 서비스는 고정 역할 접미사만 쓴다. */
    @ArchTest
    static final ArchRule services_use_role_suffixes = classes()
            .that()
            .areAnnotatedWith(Service.class)
            .should()
            .haveNameMatching(".*(Reader|Appender|Modifier|Remover|Processor|Validator)");

    /** 컨트롤러는 presentation 계층에만 둔다. */
    @ArchTest
    static final ArchRule controllers_reside_in_presentation = classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAPackage("com.board.api.presentation..");

    /** 소프트삭제 엔티티는 base JpaRepository finder(findById·findAll·count) 직접 호출을 금지한다(삭제분 노출). */
    @ArchTest
    static final ArchRule services_do_not_call_base_jpa_finders = noClasses()
            .that()
            .resideInAPackage("com.board.board.service..")
            .should()
            .callMethodWhere(BASE_JPA_FINDER)
            .because("소프트삭제 엔티티의 base finder는 삭제분을 반환한다 — 활성 필터 파생 쿼리를 쓴다");

    /** 파사드는 트랜잭션을 열지 않는다(트랜잭션 경계는 도메인 서비스의 쓰기 메서드가 소유). */
    @ArchTest
    static final ArchRule facade_classes_do_not_open_transactions = noClasses()
            .that()
            .resideInAPackage("..facade..")
            .should()
            .beAnnotatedWith(Transactional.class)
            .because("facade 트랜잭션은 한 트랜잭션이 여러 애그리거트를 바꾼다 — 조율은 도메인 이벤트로 한다");

    /** 파사드는 메서드 단위로도 트랜잭션을 열지 않는다. */
    @ArchTest
    static final ArchRule facade_methods_do_not_open_transactions = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..facade..")
            .should()
            .beAnnotatedWith(Transactional.class)
            .because("facade 트랜잭션은 한 트랜잭션이 여러 애그리거트를 바꾼다 — 조율은 도메인 이벤트로 한다");

    /** 커스텀 예외는 발생 위치와 무관하게 해당 모듈의 exception 패키지에 모은다(공통 상위 BaseException은 common-core error 소유). */
    @ArchTest
    static final ArchRule exceptions_reside_in_exception_package = classes()
            .that()
            .haveSimpleNameEndingWith("Exception")
            .and()
            .doNotHaveFullyQualifiedName(BaseException.class.getName())
            .should()
            .resideInAPackage("..exception..");

    /** ErrorCode 구현 enum은 해당 모듈의 exception 패키지에 모은다(계약 인터페이스는 common-core error 소유). */
    @ArchTest
    static final ArchRule error_codes_reside_in_exception_package =
            classes().that().implement(ErrorCode.class).should().resideInAPackage("..exception..");

    /** 엔티티는 BaseTimeEntity를 상속한다(감사 시각·Persistable·식별자 동등성). */
    @ArchTest
    static final ArchRule entities_extend_base_time_entity =
            classes().that().areAnnotatedWith(Entity.class).should().beAssignableTo(BaseTimeEntity.class);
}
