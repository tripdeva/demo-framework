# Demo Framework 상세 분석서

- **UUID:** f8d2a3c1
- **작성일:** 2026-03-20
- **최종 갱신:** 2026-03-20 (멀티모듈 통합 반영)
- **저장소:** https://github.com/tripdeva/demo-framework
- **목적:** BACK_END_STD2 통합을 위한 레퍼런스 문서 (지속 업데이트)

---

## 목차

1. [프로젝트 구조 및 빌드 설정](#1-프로젝트-구조-및-빌드-설정)
2. [의존 관계도](#2-의존-관계도)
3. [demo-core](#3-demo-core)
4. [demo-spring-boot-starter](#4-demo-spring-boot-starter)
5. [demo-storage-jpa](#5-demo-storage-jpa)
6. [demo-storage-jpa-processor](#6-demo-storage-jpa-processor)
7. [demo-storage-mybatis](#7-demo-storage-mybatis)
8. [demo-storage-mybatis-processor](#8-demo-storage-mybatis-processor)
9. [demo-storage-jdbc](#9-demo-storage-jdbc)
10. [BACK_END_STD2 통합 가이드](#10-back_end_std2-통합-가이드)

---

## 1. 프로젝트 구조 및 빌드 설정

### 디렉토리 구조

```
demo-framework/
├── build.gradle                    ← 루트: 공통 설정 (Java 17, BOM, publishing)
├── settings.gradle                 ← 7개 모듈 include
├── gradle.properties               ← 버전 통합 관리
│
├── demo-core/                      ← 순수 Java (의존성 없음)
│   └── src/main/java/kr/co/demo/core/
│
├── demo-spring-boot-starter/       ← Spring Boot AutoConfig + 어노테이션
│   └── src/main/java/kr/co/demo/spring/boot/starter/
│
├── demo-storage-jpa/               ← JPA 어댑터 (런타임)
│   └── src/main/java/kr/co/demo/client/jpa/
│
├── demo-storage-jpa-processor/     ← JPA 코드 생성 (컴파일 타임)
│   └── src/main/java/kr/co/demo/client/jpa/processor/
│
├── demo-storage-mybatis/           ← MyBatis 어댑터 (런타임)
│   └── src/main/java/kr/co/demo/client/mybatis/
│
├── demo-storage-mybatis-processor/ ← MyBatis 코드 생성 (컴파일 타임)
│   └── src/main/java/kr/co/demo/client/processor/
│
└── demo-storage-jdbc/              ← JDBC 어댑터 (스켈레톤)
    └── src/main/resources/
```

### gradle.properties (버전 통합)

| 속성 | 값 | 용도 |
|------|---|------|
| `group` | `kr.co.demo` | 전 모듈 공통 groupId |
| `version` | `1.0.0` | 전 모듈 동일 버전 |
| `springBootVersion` | `4.0.1` | Spring Boot BOM |
| `dependencyManagementVersion` | `1.1.7` | Spring 의존성 관리 |
| `querydslVersion` | `7.0` | QueryDSL (JPA 모듈) |
| `javapoetVersion` | `1.13.0` | 코드 생성 (Processor 모듈) |
| `autoServiceVersion` | `1.1.1` | Processor 자동 등록 |
| `mybatisStarterVersion` | `3.0.4` | MyBatis 스타터 |

### 루트 build.gradle 핵심 구성

- **전 서브프로젝트:** `java-library` 플러그인, Java 17 toolchain, JUnit 5
- **Spring Boot BOM 적용 대상:** demo-core와 *-processor를 **제외**한 모듈
  - demo-core: 외부 의존성 없음
  - *-processor: 컴파일 타임 전용, Spring 불필요
- **Publishing:** 전 모듈 GitHub Packages 일괄 배포 (`tripdeva/demo-framework`)
- **환경변수:** `.env` 파일 자동 탐색 (GITHUB_USER, GITHUB_TOKEN)

### 모듈별 의존성 요약

| 모듈 | demo-core | Spring Boot | 추가 의존성 |
|------|-----------|-------------|------------|
| demo-core | - | X | 없음 |
| demo-spring-boot-starter | `api` | spring-boot-starter | - |
| demo-storage-jpa | `implementation` | spring-boot-starter-data-jpa (`api`) | QueryDSL 7.0, Lombok |
| demo-storage-jpa-processor | `implementation` | X | JavaPoet, AutoService |
| demo-storage-mybatis | `implementation` | mybatis-spring-boot-starter (`api`) | - |
| demo-storage-mybatis-processor | `implementation` | X | JavaPoet, AutoService |
| demo-storage-jdbc | `implementation` | spring-boot-starter-data-jdbc (`api`) | - |

> **통합 전 vs 후:** JitPack `com.github.tripdeva:demo-core:v1.0.0` → `project(':demo-core')` 로컬 의존으로 전환. 빌드 안정성 향상.

---

## 2. 의존 관계도

### 모듈 간 의존 방향

```
demo-core  ← 순수 Java 17, 외부 의존성 없음
    ▲
    │ project(':demo-core')
    ├── demo-spring-boot-starter
    ├── demo-storage-jpa
    ├── demo-storage-jpa-processor
    ├── demo-storage-mybatis
    ├── demo-storage-mybatis-processor
    └── demo-storage-jdbc
```

### 테스트 시 Processor 연결

```
demo-storage-jpa
    └── testAnnotationProcessor → project(':demo-storage-jpa-processor')

demo-storage-mybatis
    └── testAnnotationProcessor → project(':demo-storage-mybatis-processor')
```

### 전체 아키텍처 흐름

```
[도메인 객체 + @Storage 어노테이션]
            │
            ▼
┌──────────────────────────┐
│  demo-core               │  어노테이션 정의 + Port + Mapper + 예외
└──────────┬───────────────┘
           │
     ┌─────┴──────┐
     ▼            ▼
[Processor]    [Processor]
  JPA용          MyBatis용
     │            │
     ▼            ▼
 Entity.java   BaseMapper.java
 Repository    (9개 CRUD)
 Mapper
     │            │
     ▼            ▼
┌─────────┐  ┌──────────┐
│ storage  │  │ storage  │
│  -jpa    │  │ -mybatis │   ← 런타임 어댑터
└─────────┘  └──────────┘
     │            │
     └─────┬──────┘
           ▼
   [demo-spring-boot-starter]  ← @UseCase, @Adapter, AutoConfiguration
           │
           ▼
     [BACK_END_STD2]  ← 최종 애플리케이션
```

---

## 3. demo-core

> **경로:** `demo-framework/demo-core/`
> **역할:** 프레임워크의 핵심. 영속성 프레임워크에 독립적인 어노테이션, 포트, 예외, 매퍼를 정의한다.
> **외부 의존성:** 없음 (순수 Java 17)

### 패키지 구조

```
kr.co.demo.core
├── exception/
│   ├── DomainException.java        ← 도메인 예외 기본 클래스
│   └── StorageException.java       ← 영속성 예외 (팩토리 메서드 제공)
├── mapper/
│   └── DomainMapper.java           ← 도메인 ↔ 영속성 모델 변환 인터페이스
├── model/
│   ├── Command.java                ← 요청 마커 인터페이스
│   └── Result.java                 ← 응답 마커 인터페이스
├── port/
│   ├── Port.java                   ← 포트 루트 마커
│   ├── InboundPort.java            ← 유스케이스 진입 포트
│   └── OutboundPort.java           ← 저장소/외부 시스템 포트
└── storage/
    ├── annotation/
    │   ├── StorageTable.java        ← 클래스 → 테이블 매핑
    │   ├── StorageId.java           ← PK 필드 지정
    │   ├── StorageColumn.java       ← 필드 → 컬럼 매핑
    │   ├── StorageRelation.java     ← 연관관계 매핑
    │   ├── StorageEnum.java         ← Enum 저장 전략
    │   ├── StorageTransient.java    ← 영속성 제외 필드
    │   └── Patch.java               ← 부분 수정 메서드 마커
    └── enums/
        ├── DialectType.java         ← DB 방언 (MYSQL, POSTGRES, ORACLE, H2)
        ├── RelationType.java        ← 관계 유형 (1:N, N:1, 1:1, N:M)
        └── EnumType.java            ← Enum 저장 방식 (STRING, ORDINAL)
```

### 어노테이션 7종 상세

| 어노테이션 | 대상 | 속성 | 기본값 | 용도 |
|-----------|------|------|--------|------|
| `@StorageTable` | 클래스 | `value` (테이블명) | 클래스명 → snake_case | 도메인 클래스를 DB 테이블에 매핑 |
| `@StorageId` | 필드 | `autoGenerated` | `true` | PK 필드 지정, auto-increment 여부 |
| `@StorageColumn` | 필드 | `value` (컬럼명), `nullable`, `unique` | snake_case, `true`, `false` | 필드를 DB 컬럼에 매핑 |
| `@StorageRelation` | 필드 | `type` (RelationType), `mappedBy`, `targetField` | 필수, `""`, `""` | 엔티티 간 연관관계 정의 |
| `@StorageEnum` | 필드 | `value` (EnumType) | `STRING` | Enum을 문자열/숫자로 저장 |
| `@StorageTransient` | 필드 | 없음 | - | 해당 필드를 영속성에서 제외 |
| `@Patch` | 메서드 | 없음 | - | 부분 수정 메서드 마커 |

### Port 인터페이스 (헥사고날 아키텍처)

```
Port (마커)
├── InboundPort   → Controller → Service 방향 (유스케이스)
└── OutboundPort  → Service → Repository/외부 방향 (어댑터)
```

- **InboundPort:** 비즈니스 로직 진입점. `@UseCase` 구현체가 이를 구현.
- **OutboundPort:** 데이터 접근 추상화. `@Adapter` 구현체가 이를 구현.

### 예외 계층

```
DomainException (code, message, cause)
└── StorageException
    ├── saveFailed(entityName, cause)      → code: "SAVE_FAILED"
    ├── notFound(entityName, id)           → code: "NOT_FOUND"
    ├── deleteFailed(entityName, cause)    → code: "DELETE_FAILED"
    ├── duplicate(entityName, field, val)  → code: "DUPLICATE"
    └── of(message) / of(message, cause)   → code: "GENERAL"
```

### DomainMapper<D, P>

```java
public interface DomainMapper<D, P> {
    P toStorage(D domain);     // 도메인 → 영속성 모델
    D toDomain(P persistence); // 영속성 모델 → 도메인
}
```

- Processor가 자동 생성하는 StorageMapper가 이 인터페이스를 구현
- `@StorageTransient`, `@StorageRelation` 필드는 매핑 제외

### 모델 마커 인터페이스

- **Command:** 요청/명령 객체 (CQRS)
- **Result:** 응답/결과 객체 (CQRS)
- 흐름: `Command → InboundPort → OutboundPort → Result`

### Enum 3종

| Enum | 값 | 사용처 |
|------|---|--------|
| `DialectType` | MYSQL, POSTGRES, ORACLE, H2 | UpsertSqlProvider 방언 분기 |
| `RelationType` | MANY_TO_ONE, ONE_TO_MANY, ONE_TO_ONE, MANY_TO_MANY | @StorageRelation |
| `EnumType` | STRING, ORDINAL | @StorageEnum |

---

## 4. demo-spring-boot-starter

> **경로:** `demo-framework/demo-spring-boot-starter/`
> **역할:** Spring Boot AutoConfiguration 진입점. @UseCase/@Adapter 메타 어노테이션을 제공.
> **의존:** `project(':demo-core')` (api), `spring-boot-starter`

### 패키지 구조

```
kr.co.demo.spring.boot.starter
├── annotation/
│   ├── UseCase.java              ← @Service 래핑 (유스케이스 구현)
│   └── Adapter.java              ← @Component 래핑 (어댑터 구현)
└── config/
    └── DemoAutoConfiguration.java ← 빈 AutoConfiguration (확장 포인트)
```

### @UseCase

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface UseCase {
    @AliasFor(annotation = Service.class)
    String value() default "";
}
```

- `@Service` 메타 어노테이션 → Spring이 서비스 빈으로 등록
- InboundPort 구현체에 사용

### @Adapter

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Adapter {
    @AliasFor(annotation = Component.class)
    String value() default "";
}
```

- `@Component` 메타 어노테이션 → Spring이 컴포넌트 빈으로 등록
- OutboundPort 구현체(Repository 어댑터)에 사용

### DemoAutoConfiguration

- 현재 **빈 클래스** (향후 공통 빈 등록 확장 포인트)
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`로 자동 로드
- Spring Boot 2.7+ / 4.0 호환 (spring.factories 대체)

---

## 5. demo-storage-jpa

> **경로:** `demo-framework/demo-storage-jpa/`
> **역할:** JPA 기반 영속성 어댑터. 3가지 전략(Auto/Manual/QueryDsl)을 제공.
> **의존:** `project(':demo-core')`, `spring-boot-starter-data-jpa`, QueryDSL 7.0

### 패키지 구조

```
kr.co.demo.client.jpa
├── adapter/
│   ├── AutoJpaAdapter.java       ← ApplicationContext에서 빈 자동 탐색
│   ├── ManualJpaAdapter.java     ← 타입 안전 명시적 주입
│   └── QueryDslAdapter.java      ← QueryDSL 동적 쿼리
└── config/
    └── JpaStorageAutoConfiguration.java ← JPAQueryFactory 빈 자동 등록
```

### AutoJpaAdapter<D, ID>

- **핵심:** `ApplicationContext.getBean()`으로 Repository/Mapper 자동 탐색
- **빈 네이밍 규칙:**
  - Repository: `{domainName}EntityRepository` (첫글자 소문자)
  - Mapper: `{domainName}StorageMapper`
- **메서드 (예외 변환 O/X 2벌):**

| 메서드 | 예외 변환 | 반환 |
|--------|----------|------|
| `save(D)` / `saveWithException(D)` | X / O | D |
| `saveAll(List<D>)` / `saveAllWithException(List<D>)` | X / O | List\<D> |
| `findById(ID)` / `findByIdWithException(ID)` | X / O | Optional\<D> |
| `findByIdOrThrow(ID)` | O (없으면 StorageException) | D |
| `findAll()` / `findAllWithException()` | X / O | List\<D> |
| `delete(D)` / `deleteWithException(D)` | X / O | void |
| `deleteById(ID)` / `deleteByIdWithException(ID)` | X / O | void |
| `existsById(ID)` | X | boolean |
| `count()` | X | long |

- `DataAccessException` → `StorageException` 자동 변환

### ManualJpaAdapter<D, E, ID, R>

- **두 가지 모드:**
  1. **DomainMapper 모드:** 생성자에 `DomainMapper<D, E>` 주입
  2. **Function 모드:** 메서드마다 `Function<D, E>`, `Function<E, D>` 전달
- AutoJpaAdapter와 동일한 CRUD 메서드 제공
- 테스트 용이 (명시적 의존 주입)

### QueryDslAdapter

- `JPAQueryFactory` 기반 동적 쿼리
- `fetchWithPaging(JPAQuery<T>, offset, limit)` 헬퍼
- 복잡한 조건 검색, 정렬, 페이징에 사용

### JpaStorageAutoConfiguration

- `@ConditionalOnClass(JpaRepository.class)` 조건 활성화
- QueryDSL 클래스패스 감지 시 `JPAQueryFactory` 빈 자동 등록
- `@ConditionalOnMissingBean`으로 기존 빈과 충돌 방지
- `AutoConfiguration.imports`로 자동 로드

### 도메인 → JPA 매핑 흐름

```
도메인 객체 (@StorageTable)
      │
      │ [컴파일 타임: demo-storage-jpa-processor]
      ▼
┌─────────────────────────┐
│ {Name}Entity.java       │  JPA @Entity + @Table + @Column + 관계
│ {Name}StorageMapper.java │  @Component, DomainMapper<D, E> 구현
│ {Name}EntityRepository   │  JpaRepository<E, ID> 인터페이스
└─────────────────────────┘
      │
      │ [런타임: demo-storage-jpa]
      ▼
┌─────────────────────────┐
│ AutoJpaAdapter          │  빈 자동 탐색 → CRUD 위임
│ ManualJpaAdapter        │  명시적 주입 → CRUD 위임
│ QueryDslAdapter         │  동적 쿼리
└─────────────────────────┘
```

### 테스트 사용 예시

```java
// 도메인 객체 정의 (demo-storage-jpa/src/test/)
@StorageTable("orders")
public class Order {
    @StorageId
    private Long id;

    @StorageColumn(value = "order_no", nullable = false, unique = true)
    private String orderNumber;

    @StorageColumn(nullable = false)
    private String customerName;

    @StorageEnum(EnumType.STRING)
    private OrderStatus status;

    @StorageColumn(nullable = false)
    private BigDecimal totalAmount;

    private LocalDateTime orderedAt;

    @StorageRelation(type = RelationType.ONE_TO_MANY, mappedBy = "order")
    private List<OrderItem> items;

    @StorageTransient
    private String tempCalculation;
}

// 어댑터 사용
public class TestOrderAdapter extends AutoJpaAdapter<Order, Long> {
    public TestOrderAdapter(ApplicationContext ctx) {
        super(Order.class, ctx);
    }
}

// 자동 등록 확인 (4개 빈)
// orderEntityRepository, orderStorageMapper,
// orderItemEntityRepository, orderItemStorageMapper
```

---

## 6. demo-storage-jpa-processor

> **경로:** `demo-framework/demo-storage-jpa-processor/`
> **역할:** 컴파일 타임 어노테이션 프로세서. @StorageTable 도메인에서 JPA Entity/Repository/Mapper를 자동 생성.
> **의존:** `project(':demo-core')`, JavaPoet 1.13.0, AutoService 1.1.1

### 패키지 구조

```
kr.co.demo.client.jpa.processor
├── StorageJpaProcessor.java          ← 메인 프로세서 (진입점)
├── generator/
│   ├── EntityGenerator.java          ← JPA Entity 생성
│   ├── RepositoryGenerator.java      ← JpaRepository 인터페이스 생성
│   └── MapperGenerator.java          ← DomainMapper 구현체 생성
└── util/
    └── NamingUtils.java              ← camelCase ↔ snake_case 변환
```

### StorageJpaProcessor

- `@AutoService(Processor.class)` — 서비스 로더 자동 등록
- `@SupportedAnnotationTypes("kr.co.demo.core.storage.annotation.StorageTable")`
- Java 17 지원
- 흐름: `@StorageTable` 탐지 → 클래스 검증 → EntityGenerator → MapperGenerator → RepositoryGenerator

### EntityGenerator — 코드 생성 규칙

| 도메인 어노테이션 | 생성되는 JPA 어노테이션 | 비고 |
|-----------------|----------------------|------|
| `@StorageTable("orders")` | `@Entity @Table(name="orders")` | 값 없으면 클래스명 snake_case |
| `@StorageId` | `@Id @GeneratedValue(IDENTITY)` | `autoGenerated=false`면 @GeneratedValue 생략 |
| `@StorageColumn(v, n, u)` | `@Column(name, nullable, unique)` | 값 없으면 필드명 snake_case |
| `@StorageEnum(STRING)` | `@Enumerated(EnumType.STRING)` | STRING 또는 ORDINAL |
| `@StorageRelation(MANY_TO_ONE)` | `@ManyToOne` | mappedBy 포함 가능 |
| `@StorageRelation(ONE_TO_MANY)` | `@OneToMany(mappedBy=...)` | 컬렉션 → List\<XxxEntity> |
| `@StorageTransient` | 필드 제외 | Entity에서 생략됨 |

생성 위치: `{패키지}.entity.{Name}Entity.java`
- 기본 생성자 포함 (JPA 요구사항)
- getter/setter 자동 생성
- 관계 필드의 타입을 Entity 타입으로 변환 (`Order` → `OrderEntity`)

### RepositoryGenerator

- `{Name}EntityRepository extends JpaRepository<{Name}Entity, {IdType}>` 생성
- ID 타입: `@StorageId` 필드에서 추출 (기본: Long)
- primitive → boxed 자동 변환 (long → Long)
- 생성 위치: `{패키지}.repository.{Name}EntityRepository.java`

### MapperGenerator

- `{Name}StorageMapper implements DomainMapper<{Name}, {Name}Entity>` 생성
- `@Component` 포함 (Spring 빈 자동 등록)
- `toStorage()` / `toDomain()` 양방향 변환
- `@StorageTransient`와 `@StorageRelation` 필드는 매핑 **제외**
- null 체크 포함 (null 입력 → null 반환)
- 생성 위치: `{패키지}.mapper.{Name}StorageMapper.java`

### NamingUtils

| 메서드 | 입력 | 출력 | 용도 |
|--------|------|------|------|
| `toSnakeCase()` | `orderNumber` | `order_number` | 테이블/컬럼명 자동 변환 |
| `capitalize()` | `order` | `Order` | getter/setter 생성 |
| `uncapitalize()` | `Order` | `order` | 변수명 생성 |

---

## 7. demo-storage-mybatis

> **경로:** `demo-framework/demo-storage-mybatis/`
> **역할:** MyBatis 기반 영속성 어댑터. CRUD 위임 + Patch(부분 수정) + Upsert(4개 DB 방언).
> **의존:** `project(':demo-core')`, `mybatis-spring-boot-starter:3.0.4`

### 패키지 구조

```
kr.co.demo.client.mybatis
├── adapter/
│   └── MybatisAdapter.java            ← CRUD 위임 추상 기본 클래스
├── config/
│   ├── DataSourceConfig.java          ← DataSource 홀더 + 방언 감지
│   └── MybatisStorageAutoConfiguration.java ← AutoConfiguration
└── util/
    ├── Patch.java                     ← 부분 수정 컨테이너 (불변)
    ├── PatchValue.java                ← 필드-값 쌍
    ├── PatchSqlProvider.java          ← UPDATE SQL 동적 생성
    ├── UpsertSqlProvider.java         ← UPSERT SQL (4개 방언)
    └── Resolver.java                  ← @Storage 어노테이션 해석 (리플렉션)
```

### MybatisAdapter<D, ID, M>

- 추상 클래스. 하위 클래스가 구체적 Mapper 메서드를 연결.
- **추상 메서드 (하위에서 구현):**
  - `doFindById()`, `doFindAll()`, `doInsert()`, `doUpdate()`
  - `doDeleteById()`, `doCount()`, `doExistsById()`
- **두 벌 메서드:**
  - 일반: `findById()`, `insert()` ... (예외 변환 없음)
  - WithException: `findByIdWithException()` ... (`DataAccessException` → `StorageException`)
- `findByIdOrThrow(ID)`: 없으면 `StorageException.notFound()` 발생

### Patch / PatchValue — 부분 수정

```java
// 사용법
Patch<Order> patch = Patch.create(
    Order.class,
    orderId,
    PatchValue.of("orderNumber", "NEW-001"),
    PatchValue.of("customerName", "홍길동"),
    PatchValue.of("orderedAt", null)  // NULL로 설정
);
int affected = orderBaseMapper.patch(patch);
```

- `Patch<T>`: domainType, id, values (불변, 최소 1개 PatchValue 필수)
- `PatchValue<T>`: fieldName, value, isNull()

### PatchSqlProvider — UPDATE SQL 동적 생성

```sql
-- 생성 예시
UPDATE orders
SET order_no = #{values[0].value},
    customer_name = #{values[1].value},
    ordered_at = NULL
WHERE id = #{id}
```

- `Resolver`로 테이블명/컬럼명 해석
- `@StorageColumn(nullable=false)` 필드에 null → `IllegalStateException`
- nullable 필드에 null → `columnName = NULL` 리터럴

### UpsertSqlProvider — 4개 DB 방언

| 방언 | SQL 패턴 |
|------|---------|
| **MySQL** | `INSERT ... ON DUPLICATE KEY UPDATE ...` |
| **PostgreSQL** | `INSERT ... ON CONFLICT (id) DO UPDATE SET ...` |
| **Oracle** | `MERGE INTO ... USING (SELECT ... FROM dual) ON (...) WHEN MATCHED/NOT MATCHED` |
| **H2** | `MERGE INTO ... (...) KEY(id) VALUES (...)` |

- `DataSourceConfig.resolve()`로 DB 방언 자동 감지
- `@StorageColumn` 있는 필드만 포함
- ID 필드는 UPDATE SET 절에서 제외

### Resolver — 어노테이션 해석 (런타임 리플렉션)

| 메서드 | 용도 |
|--------|------|
| `resolveTable(Class)` | `@StorageTable` → 테이블명 |
| `resolveIdField(Class)` | `@StorageId` → ID 필드 |
| `resolveField(Class, name)` | 리플렉션으로 필드 조회 |
| `resolveColumn(Field)` | `@StorageColumn` → 컬럼명 (없으면 snake_case) |
| `camelToSnake(String)` | camelCase → snake_case |

### DataSourceConfig

- 싱글톤 `@Component`. DataSource를 static 필드에 캐싱.
- `resolve(DataSource)`: JDBC 메타데이터 `getDatabaseProductName()` → DialectType
- 지원: mysql, postgres, oracle, h2

### MybatisStorageAutoConfiguration

- `@AutoConfiguration(after = MybatisAutoConfiguration.class)`
- `@ConditionalOnClass(name = "org.apache.ibatis.session.SqlSessionFactory")`
- 현재 빈 클래스 (확장 포인트)

### 테스트 사용 예시

```java
// BaseMapper (demo-storage-mybatis-processor가 자동 생성)
@Mapper
public interface OrderBaseMapper {
    @Select("SELECT id, order_no AS orderNumber, ... FROM orders WHERE id = #{id}")
    Order findById(Long id);
    // findAll, insert, update, patch, save, deleteById, count, existsById
}

// 사용자 정의 확장
public interface OrderMapper extends OrderBaseMapper {
    @Select("SELECT ... FROM orders WHERE status = #{status}")
    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCondition(OrderSearchCondition cond); // XML 매퍼
}
```

XML 매퍼: `src/test/resources/kr/co/demo/domain/mapper/OrderMapper.xml`
- `<where>` + `<if test="...">` 동적 조건
- status, customerName (LIKE), minAmount, maxAmount 조합

---

## 8. demo-storage-mybatis-processor

> **경로:** `demo-framework/demo-storage-mybatis-processor/`
> **역할:** 컴파일 타임 어노테이션 프로세서. @StorageTable 도메인에서 MyBatis BaseMapper 자동 생성.
> **의존:** `project(':demo-core')`, JavaPoet 1.13.0, AutoService 1.1.1

### 패키지 구조

```
kr.co.demo.client.processor
├── StorageMybatisProcessor.java       ← 메인 프로세서
├── generator/
│   └── BaseMapperGenerator.java       ← BaseMapper 인터페이스 생성
└── util/
    ├── NamingUtils.java               ← camelCase ↔ snake_case
    └── PatchMethodProcessor.java      ← @Patch 메서드 검증
```

### StorageMybatisProcessor

- `@AutoService(Processor.class)` 자동 등록
- `@StorageTable` 처리 → `BaseMapperGenerator.generate()` 호출
- 클래스에만 적용 가능 (인터페이스/Enum 불가)

### BaseMapperGenerator — 9개 CRUD 메서드 생성

| # | 메서드 | SQL 유형 | 특이사항 |
|---|--------|---------|---------|
| 1 | `findById(ID)` | @Select | 컬럼 AS 필드명 별칭 |
| 2 | `findAll()` | @Select | 전체 조회 |
| 3 | `insert(D)` | @Insert | autoGenerated ID 컬럼 제외, @Options(useGeneratedKeys) |
| 4 | `update(D)` | @Update | SET 절에서 ID 제외 |
| 5 | `patch(Patch<D>)` | @UpdateProvider | PatchSqlProvider 위임 |
| 6 | `save(D)` | @InsertProvider | UpsertSqlProvider 위임 (UPSERT) |
| 7 | `deleteById(ID)` | @Delete | PK 기반 삭제 |
| 8 | `count()` | @Select | COUNT(*) |
| 9 | `existsById(ID)` | @Select | boolean 반환 |

- `@StorageTransient`, `@StorageRelation` 필드 제외
- SELECT: `column_name AS fieldName` 별칭
- `@Mapper` 어노테이션 포함
- 생성 위치: `{패키지}.mapper.{Name}BaseMapper.java`

### PatchMethodProcessor

- `@Patch` 메서드 컴파일 타임 검증 전용
- 규칙: 메서드에만 사용, 파라미터 1개, 반환 타입 int
- 위반 시 컴파일 에러 발생

---

## 9. demo-storage-jdbc

> **경로:** `demo-framework/demo-storage-jdbc/`
> **역할:** JDBC 기반 영속성 어댑터. 현재 **스켈레톤** 상태.
> **의존:** `project(':demo-core')`, `spring-boot-starter-data-jdbc`

### 현재 상태

- **소스 코드 없음.** `build.gradle`과 `application.properties`만 존재.

### 향후 구현 방향 제안

JPA/MyBatis와 동일한 패턴:

1. **JdbcAdapter<D, ID>** — CRUD 추상 기본 클래스 (`JdbcTemplate` 기반)
2. **JdbcStorageAutoConfiguration** — DataSource/JdbcTemplate 자동 설정
3. **demo-storage-jdbc-processor** 모듈 추가 — SQL/RowMapper 코드 생성

---

## 10. BACK_END_STD2 통합 가이드

### 현재 BACK_END_STD2 구조 vs demo-framework

| BACK_END_STD2 모듈 | 대응 demo-framework 모듈 | 비고 |
|-------------------|------------------------|------|
| `core/core-app` | demo-core + demo-spring-boot-starter | 진입점 + 어노테이션 |
| `core/core-domain` | demo-core (Port, Command, Result) | 도메인 모델/포트 |
| `storage/storage-jpa` | demo-storage-jpa + demo-storage-jpa-processor | JPA 영속성 |
| `client/client-sap` | - | 대응 없음 |
| `client/client-firebase` | - | 대응 없음 |
| `support/*` | - | 대응 없음 |

### 소비자 프로젝트에서의 사용법

```groovy
// JPA만 쓸 때
implementation 'kr.co.demo:demo-core:1.0.0'
implementation 'kr.co.demo:demo-spring-boot-starter:1.0.0'
implementation 'kr.co.demo:demo-storage-jpa:1.0.0'
annotationProcessor 'kr.co.demo:demo-storage-jpa-processor:1.0.0'

// MyBatis만 쓸 때
implementation 'kr.co.demo:demo-core:1.0.0'
implementation 'kr.co.demo:demo-spring-boot-starter:1.0.0'
implementation 'kr.co.demo:demo-storage-mybatis:1.0.0'
annotationProcessor 'kr.co.demo:demo-storage-mybatis-processor:1.0.0'

// 둘 다 쓸 때도 가능 (도메인 어노테이션이 프레임워크 독립적)
```

### 마이그레이션 순서 (권장)

```
Step 1: demo-core를 라이브러리로 import
Step 2: 도메인 객체에 @StorageTable 어노테이션 추가
Step 3: demo-storage-jpa-processor를 annotationProcessor로 추가
Step 4: 자동 생성된 Entity/Repository/Mapper 확인
Step 5: demo-storage-jpa의 Adapter로 기존 Repository 접근 래핑
Step 6: demo-spring-boot-starter 도입 (@UseCase, @Adapter)
Step 7: (선택) demo-storage-mybatis 추가
```

### 주의사항

- `@StorageRelation` 필드는 자동 매핑에서 **제외** → 관계 매핑은 수동 처리
- 도메인 객체에 **public getter/setter 필수** (생성된 Mapper가 사용)
- Lombok `@Data` 금지 (BACK_END_STD2 lombok.config) → `@Getter`/`@Setter` 개별 사용
- 생성된 Repository 패키지를 `@EnableJpaRepositories`에 등록 필요
- QueryDSL Q-class는 Entity 생성 후 별도 빌드 단계에서 생성

---

> **이 문서는 지속적으로 업데이트됩니다.** demo-framework 수정 시 해당 섹션을 갱신하세요.
