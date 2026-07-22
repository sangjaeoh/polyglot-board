package com.board.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.board.common.core.error.BaseException;
import com.board.common.core.error.ErrorCode;
import com.board.common.jpa.entity.BaseTimeEntity;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.Entity;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    private static final List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = List.of(
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class,
            RequestMapping.class);

    /**
     * web 핸들러와 request/response는 {@code @Operation}·{@code @ApiResponse}·{@code @Parameter(description)}·
     * {@code @Schema}로 명시한다. 검사 대상 DTO는 패키지가 아니라 핸들러 시그니처(@RequestBody·반환 타입, 제네릭
     * 언랩)에서 파생한다 — 응답 봉투가 다른 패키지로 이동해도 강제가 유지된다. 에러 상태 집합의 정확성은 리뷰가
     * 소유하고(에러 응답은 OpenApiConfig OperationCustomizer 소유), 여기서는 존재만 강제한다.
     */
    @ArchTest
    static void web_api_surface_is_documented(JavaClasses allClasses) {
        List<String> problems = new ArrayList<>();
        SortedSet<JavaClass> dtoTypes = new TreeSet<>(Comparator.comparing(JavaClass::getFullName));
        for (JavaClass candidate : allClasses) {
            if (!candidate.isAnnotatedWith(RestController.class)) {
                continue;
            }
            for (JavaMethod method : candidate.getMethods()) {
                if (MAPPING_ANNOTATIONS.stream().noneMatch(method::isAnnotatedWith)) {
                    continue;
                }
                if (!method.isAnnotatedWith(Operation.class)) {
                    problems.add(method.getFullName() + ": @Operation 부재");
                }
                if (!declaresApiResponse(method)) {
                    problems.add(method.getFullName() + ": @ApiResponse 부재");
                }
                for (JavaParameter parameter : method.getParameters()) {
                    if (parameter.isAnnotatedWith(RequestParam.class)
                            || parameter.isAnnotatedWith(PathVariable.class)) {
                        boolean documented = parameter
                                .tryGetAnnotationOfType(io.swagger.v3.oas.annotations.Parameter.class)
                                .map(doc -> !doc.description().isBlank())
                                .orElse(false);
                        if (!documented) {
                            problems.add(method.getFullName() + " 파라미터 " + parameter.getIndex()
                                    + ": @Parameter(description) 부재");
                        }
                    }
                    if (parameter.isAnnotatedWith(RequestBody.class)
                            || parameter.isAnnotatedWith(ParameterObject.class)) {
                        collectBoardTypes(parameter.getType(), dtoTypes);
                    }
                }
                collectBoardTypes(method.getReturnType(), dtoTypes);
            }
        }
        for (JavaClass dto : dtoTypes) {
            if (!dto.isAnnotatedWith(Schema.class)) {
                problems.add(dto.getName() + ": 타입 @Schema 부재");
            }
            for (JavaField field : dto.getFields()) {
                if (field.getModifiers().contains(JavaModifier.STATIC)) {
                    continue;
                }
                if (!field.isAnnotatedWith(Schema.class)) {
                    problems.add(field.getFullName() + ": 컴포넌트 @Schema 부재");
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new AssertionError("web 계약 표면 문서화 위반:\n" + String.join("\n", problems));
        }
    }

    private static boolean declaresApiResponse(JavaMethod method) {
        if (method.isAnnotatedWith(ApiResponse.class) || method.isAnnotatedWith(ApiResponses.class)) {
            return true;
        }
        return method.tryGetAnnotationOfType(Operation.class)
                .map(operation -> operation.responses().length > 0)
                .orElse(false);
    }

    /** 핸들러 시그니처의 com.board 타입을 제네릭 실인자까지 재귀 수집한다(request/response DTO 파생). */
    private static void collectBoardTypes(JavaType type, SortedSet<JavaClass> into) {
        JavaClass erasure = type.toErasure();
        if (erasure.getPackageName().startsWith("com.board")) {
            into.add(erasure);
        }
        if (type instanceof JavaParameterizedType parameterized) {
            for (JavaType argument : parameterized.getActualTypeArguments()) {
                collectBoardTypes(argument, into);
            }
        }
    }

    /**
     * web 컨트롤러 핸들러는 int·Integer {@code @RequestParam}을 직접 선언하지 않는다 — 페이징 GET은 common-web
     * {@code PaginationRequest}({@code @Valid @ParameterObject})를 사용한다. 무예외 전면 금지.
     */
    @ArchTest
    static final ArchRule web_handlers_do_not_declare_int_request_params = methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .should(notDeclareIntRequestParams())
            .because("int 페이징 파라미터는 PaginationRequest로 대체한다 — 계약과 검증을 한 곳(common-web)이 소유한다");

    private static ArchCondition<JavaMethod> notDeclareIntRequestParams() {
        return new ArchCondition<>("not declare int/Integer @RequestParam directly") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaParameter> parameters = method.getParameters();
                for (int index = 0; index < parameters.size(); index++) {
                    JavaParameter parameter = parameters.get(index);
                    if (!parameter.isAnnotatedWith(RequestParam.class)) {
                        continue;
                    }
                    JavaClass raw = parameter.getRawType();
                    if (raw.isEquivalentTo(int.class) || raw.isEquivalentTo(Integer.class)) {
                        events.add(SimpleConditionEvent.violated(
                                method,
                                method.getFullName() + " 파라미터 " + index + "가 " + raw.getSimpleName()
                                        + " @RequestParam을 직접 선언한다"));
                    }
                }
            }
        };
    }
}
