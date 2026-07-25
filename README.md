# 미르옷장 — Spring Boot 의류 쇼핑몰

Spring Boot + Thymeleaf 기반의 의류 쇼핑몰. 기능 수보다 인증·보안 영역의 완성도를 우선한 1인 개발 프로젝트다.

## 기술 스택

| 영역 | 선택 | 비고 |
|---|---|---|
| Language | Java 17 | |
| Framework | Spring Boot 3.3 (Web, Data JPA, Security, OAuth2 Client, Validation, Thymeleaf) | |
| View | Thymeleaf + thymeleaf-layout-dialect | SSR, 프래그먼트 레이아웃 |
| DB | MySQL 8.0 | schema.sql로 스키마 관리, local-seed-data.sql로 로컬 전용 시드 데이터 관리 |
| 외부 연동 | Google OAuth2, 도로명주소 API(juso.go.kr), 스마트택배 배송조회 API | 키는 전부 application-local.yml(gitignore)에서만 관리 |
| Build | Gradle | |
| 배포 | EC2 1대 + Docker Compose (nginx / app / mysql) | |

## 로컬 실행

```bash
# 1) MySQL 준비 (docker로 간단히)
docker run -d --name shop-mysql-local -p 3306:3306 \
  -e MYSQL_DATABASE=shopping_mall \
  -e MYSQL_USER=shop_user -e MYSQL_PASSWORD=shop_pass1234! \
  -e MYSQL_ROOT_PASSWORD=root1234! mysql:8.0

# 2) 로컬 전용 시크릿 파일 작성 (git에는 안 올라감)
#    src/main/resources/application-local.yml
#    spring.security.oauth2.client.registration.google.client-id / client-secret
#    app.juso.confirm-key, app.delivery-tracking.api-key
#    (구글 로그인 없이 로컬에서 로그인만 테스트하려면 이 파일 자체를 생략해도 기동은 된다)

# 3) 애플리케이션 실행 (local 프로필 기본 활성화)
gradle bootRun
# 또는 IDE에서 gradle wrapper 생성 후 ./gradlew bootRun
```

앱 기동 시 `schema.sql`이 자동 실행되어 테이블이 생성되고, `local` 프로필에서는 `local-seed-data.sql`도 함께 실행되어 샘플 상품 12종과 데모 계정이 세팅된다.
`prod` 프로필에서는 시드 데이터를 절대 적재하지 않는다(알려진 비밀번호의 데모 admin 계정이 운영 DB에 생기는 것을 방지).

- 일반 회원가입: `/members/register` (또는 로그인 화면에서 Google로 로그인)
- 관리자 데모 계정: `admin@mirocloset.com` / `admin1234!`

## 배포 (EC2)

CI/CD는 아직 없다. 지금은 EC2에 직접 붙어서 수동으로 배포한다.

1. EC2에 Docker, Docker Compose 설치 (스왑 없는 프리티어급 인스턴스면 빌드 중 메모리 부족이 날 수 있어 스왑 2G 정도 잡아두는 걸 권장)
2. 저장소를 인스턴스에 clone
3. `.env` 작성 (DB_USER/DB_PASSWORD/DB_ROOT_PASSWORD 등 — `docker-compose.yml`이 참조하는 값)
4. `docker compose up -d --build` (docker-compose.yml의 app 이미지는 GHCR pull이 아니라 로컬 Dockerfile 빌드로 띄운다 — 아직 이미지 레지스트리 파이프라인이 없어서)
5. 도메인 없이 IP로만 서비스 중이라 HTTPS는 아직 미적용 (nginx.conf에 443 블록은 만들어 두었고 주석 처리만 되어 있음 — 도메인 생기면 그때 켠다)

## 폴더 구조

```
src/main/java/com/mondaycloset/shop
├── domain          # 엔티티 (도메인별 패키지: member, category, product, cart, order, security)
├── repository       # Spring Data JPA 리포지토리
├── service           # 비즈니스 로직 (admin 하위 패키지: 관리자 전용 서비스)
├── security          # 폼 로그인 + 구글 OAuth2가 공유하는 인증 컴포넌트 (AppUserPrincipal로 로그인 방식 통일)
├── config            # SecurityConfig, RestTemplateConfig, PasswordEncoderConfig
├── web
│   ├── controller    # Controller (admin 하위 패키지: 관리자 컨트롤러)
│   └── dto           # 화면/요청 바인딩용 DTO (엔티티 미노출 원칙)
└── global
    └── exception     # BusinessException, ErrorCode, GlobalExceptionHandler
```

## 설계 문서

- `docs/erd/shopping_mall_erd.png` — ERD (정규화/비정규화 설계 포인트 포함)
- `docs/monday-closet-presentation.pptx` — 초기 기획 발표자료 (이름은 옛날 프로젝트명 그대로 남아있음)
- `src/main/resources/schema.sql` — 물리 스키마 (ERD 대비 추가된 기술 컬럼: product.version, member.provider, orders.carrier_code/tracking_number 등)

## 핵심 설계 포인트

1. **비정규화 설계**: `product.thumbnail_url`(목록 조회 성능), `order_item.product_name/price`(주문 이력 스냅샷), `orders.total_price`(집계값 저장) — 세 곳 모두 "왜 정규화를 깼는가"를 설명 가능하도록 주석으로 남겨두었다.
2. **동시성 제어**: `Product.version`(@Version) 기반 낙관적 락으로 동시 주문 시 재고 초과 판매를 방지한다.
3. **보안**: BCrypt 비밀번호 해시, 로그인 성공 시 세션ID 재발급(세션 고정 공격 방어), CSRF(Spring Security 기본 + Thymeleaf 자동 토큰 삽입), `login_history` 기반 로그인 실패 추적 + 실제 계정 잠금(10분 내 5회 실패 시), 외부 콜백(주소검색)만 예외적으로 CSRF 제외하되 origin 검증은 별도로 함.
4. **소프트 삭제**: 상품 삭제 대신 `HIDDEN` 상태 전환, 주문 취소 시 재고 자동 복원 — 장바구니/주문 이력의 참조 무결성을 깨지 않는다.
5. **로그인 방식 통일**: 폼 로그인과 구글 OAuth2 로그인이 서로 다른 Principal 타입을 쓰는 문제를 `AppUserPrincipal` 인터페이스로 통일해서 컨트롤러 코드가 로그인 방식을 몰라도 되게 함.
6. **외부 API는 전부 서버사이드 프록시**: 주소검색 승인키, 배송조회 API 키 모두 클라이언트에 노출하지 않고 서버에서만 사용. 키가 없어도 앱은 정상 기동하고 해당 기능만 조용히 비활성화됨.
7. **관리자 대시보드**: 매출/주문 집계는 매번 전체 조회 후 계산하는 대신 리포지토리 단에서 집계 쿼리(SUM/COUNT/GROUP BY)로 처리.
8. **계층 구조**: Controller → Service → Repository → Domain, DTO로 엔티티 비노출, `@ControllerAdvice` 기반 전역 예외 처리로 스택트레이스 미노출.

## 알려진 제약 / 다음 단계 (의도적으로 범위에서 제외)

- 실제 PG 결제 연동 없음 (주문 확정 = 결제 완료로 간주)
- 상품 이미지는 URL 텍스트 입력 방식 (S3 업로드는 로드맵)
- 배송지는 회원당 1개 (주소록 다중관리 미지원)
- 구글 로그인은 이메일이 같으면 기존 로컬 계정에 자동으로 연동됨 — 개인 프로젝트 스케일에서는 괜찮지만, 실서비스라면 이메일 인증 없이 계정을 이어붙이는 건 추가 검토가 필요한 지점
- CI/CD 파이프라인 없음, 배포는 수동 SSH
- HTTPS 미적용 (도메인 연결 전까지는 IP로만 서비스)
