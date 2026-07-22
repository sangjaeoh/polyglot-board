package com.board.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.board.common.core.error.BaseException;
import com.board.common.core.error.ErrorCode;
import com.board.common.jpa.entity.BaseTimeEntity;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모듈 경계·네이밍 불변식을 빌드가 강제한다(리뷰가 아니라).
 *
 * <p>검증 대상 패키지는 하드코딩하지 않고 클래스패스에서 파생한다 — 클래스패스는 build.gradle.kts가 settings의
 * 모듈 목록에서 구성하므로 새 모듈은 자동 편입된다. 앱 루트는 {@code @SpringBootApplication} 소재 패키지,
 * 도메인 루트는 도메인 마커 서브패키지(entity·info·repository·service·port·event·exception)를 가진
 * {@code com.board} 직하 패키지다.
 */
@AnalyzeClasses(packages = "com.board", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final Set<String> BASE_FINDER_NAMES = Set.of("findById", "findAll", "count");

    private static final Set<String> DOMAIN_MARKERS =
            Set.of("entity", "info", "repository", "service", "port", "event", "exception");

    /**
     * base finder 금지의 예외("deletedAt 없는 엔티티는 그대로 사용")를 구현한다 — 소프트삭제 엔티티의
     * 리포지토리에 대한 호출만 위반이다. 엔티티는 직접 {@code JpaRepository<E, ID>} 파라미터화에서만 해석하고,
     * 해석 불가(raw 타입·중간 인터페이스 경유)는 우회 차단을 위해 위반으로 처리한다(fail-closed).
     */
    private static final DescribedPredicate<JavaMethodCall> BASE_JPA_FINDER_ON_SOFT_DELETE_REPOSITORY =
            new DescribedPredicate<>("base JpaRepository finder (findById·findAll·count) on soft-delete repository") {
                @Override
                public boolean test(JavaMethodCall call) {
                    if (!BASE_FINDER_NAMES.contains(call.getTarget().getName())) {
                        return false;
                    }
                    if (!call.getTargetOwner().isAssignableTo(JpaRepository.class)) {
                        return false;
                    }
                    return resolveEntity(call.getTargetOwner())
                            .map(entity -> entity.getAllFields().stream()
                                    .anyMatch(field -> field.getName().equals("deletedAt")))
                            .orElse(true);
                }
            };

    /**
     * 리포지토리의 직접 {@code JpaRepository<E, ID>} 파라미터화에서 엔티티 타입을 해석한다. 실인자가 구체
     * 클래스가 아니면(타입 변수 {@code E} 등) 해석 실패로 처리해 fail-closed에 합류시킨다.
     */
    private static Optional<JavaClass> resolveEntity(JavaClass repository) {
        return repository.getInterfaces().stream()
                .filter(JavaParameterizedType.class::isInstance)
                .map(JavaParameterizedType.class::cast)
                .filter(parameterized -> parameterized.toErasure().isEquivalentTo(JpaRepository.class))
                .findFirst()
                .map(parameterized -> parameterized.getActualTypeArguments().get(0))
                .filter(JavaClass.class::isInstance)
                .map(JavaClass.class::cast);
    }

    /** 앱 루트 패키지를 클래스패스에서 파생한다. */
    private static SortedSet<String> appRoots(JavaClasses allClasses) {
        SortedSet<String> roots = new TreeSet<>();
        for (JavaClass clazz : allClasses) {
            if (clazz.isAnnotatedWith(SpringBootApplication.class)) {
                roots.add(clazz.getPackageName());
            }
        }
        if (roots.isEmpty()) {
            throw new AssertionError("앱 루트 파생 실패 — @SpringBootApplication 클래스가 클래스패스에 없다");
        }
        return roots;
    }

    /** 도메인 루트 패키지를 클래스패스에서 파생한다(앱 루트와 겹치면 앱으로 분류). */
    private static SortedSet<String> domainRoots(JavaClasses allClasses) {
        SortedSet<String> apps = appRoots(allClasses);
        SortedSet<String> roots = new TreeSet<>();
        for (JavaClass clazz : allClasses) {
            String[] segments = clazz.getPackageName().split("\\.");
            if (segments.length >= 4
                    && segments[0].equals("com")
                    && segments[1].equals("board")
                    && !segments[2].equals("common")
                    && DOMAIN_MARKERS.contains(segments[3])) {
                String root = "com.board." + segments[2];
                if (!apps.contains(root)) {
                    roots.add(root);
                }
            }
        }
        if (roots.isEmpty()) {
            throw new AssertionError("도메인 루트 파생 실패 — 도메인 마커 서브패키지를 가진 패키지가 없다");
        }
        return roots;
    }

    private static String[] withSuffix(SortedSet<String> roots, String suffix) {
        return roots.stream().map(root -> root + suffix).toArray(String[]::new);
    }

    private static boolean underAny(String packageName, SortedSet<String> roots) {
        return roots.stream().anyMatch(root -> packageName.equals(root) || packageName.startsWith(root + "."));
    }

    /** 파생 무결성: 모든 클래스가 앱·도메인·common 루트 중 하나로 분류돼야 한다(파생 실패 fail-closed). */
    @ArchTest
    static void all_packages_derive_to_known_roots(JavaClasses allClasses) {
        SortedSet<String> apps = appRoots(allClasses);
        SortedSet<String> domains = domainRoots(allClasses);
        for (JavaClass clazz : allClasses) {
            String packageName = clazz.getPackageName();
            boolean known = packageName.equals("com.board.common")
                    || packageName.startsWith("com.board.common.")
                    || underAny(packageName, apps)
                    || underAny(packageName, domains);
            if (!known) {
                throw new AssertionError("미분류 패키지 " + packageName + " — 모듈 루트 파생 규칙이 이 클래스를 놓친다: " + clazz.getName());
            }
        }
    }

    /** 앱은 리포지토리에 직접 접근하지 않는다(도메인 서비스·파사드 경유). */
    @ArchTest
    static void apps_do_not_access_repositories(JavaClasses allClasses) {
        noClasses()
                .that()
                .resideInAnyPackage(withSuffix(appRoots(allClasses), ".."))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(withSuffix(domainRoots(allClasses), ".repository.."))
                .check(allClasses);
    }

    /** 엔티티는 도메인 모듈 밖(앱)에 노출되지 않는다. 경계는 Info로 넘는다. */
    @ArchTest
    static void entities_do_not_leak_to_apps(JavaClasses allClasses) {
        noClasses()
                .that()
                .resideInAnyPackage(withSuffix(appRoots(allClasses), ".."))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(withSuffix(domainRoots(allClasses), ".entity.."))
                .check(allClasses);
    }

    /** 도메인은 앱·web을 의존하지 않는다(의존은 한 방향). */
    @ArchTest
    static void domain_does_not_depend_on_apps_or_web(JavaClasses allClasses) {
        String[] appPackages = withSuffix(appRoots(allClasses), "..");
        String[] forbidden = new String[appPackages.length + 1];
        System.arraycopy(appPackages, 0, forbidden, 0, appPackages.length);
        forbidden[appPackages.length] = "com.board.common.web..";
        noClasses()
                .that()
                .resideInAnyPackage(withSuffix(domainRoots(allClasses), ".."))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(forbidden)
                .check(allClasses);
    }

    /** 도메인 서비스는 고정 역할 접미사만 쓴다. */
    @ArchTest
    static final ArchRule services_use_role_suffixes = classes()
            .that()
            .areAnnotatedWith(Service.class)
            .should()
            .haveNameMatching(".*(Reader|Appender|Modifier|Remover|Processor|Validator)");

    /** 컨트롤러는 web 계층에만 둔다(web/v{n}은 버전 바깥 축·리소스 안쪽 축으로 URL을 미러링). */
    @ArchTest
    static final ArchRule controllers_reside_in_web =
            classes().that().haveSimpleNameEndingWith("Controller").should().resideInAPackage("..web..");

    /** 소프트삭제 엔티티는 base JpaRepository finder(findById·findAll·count) 직접 호출을 금지한다(삭제분 노출). */
    @ArchTest
    static final ArchRule soft_delete_repositories_do_not_serve_base_finders = noClasses()
            .should()
            .callMethodWhere(BASE_JPA_FINDER_ON_SOFT_DELETE_REPOSITORY)
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
