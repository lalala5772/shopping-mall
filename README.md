# 미르옷장 — Spring Boot 의류 쇼핑몰

Spring Boot + Thymeleaf 기반의 의류 쇼핑몰. 기능 수보다 인증·보안 영역의 완성도를 우선한 1인 개발 프로젝트다.

## 기술 스택

| 영역 | 선택 | 비고 |
|---|---|---|
| Language | Java 17 | |
| Framework | Spring Boot 3.3 (Web, Data JPA, Security, Validation, Thymeleaf) | |
| View | Thymeleaf + thymeleaf-layout-dialect | SSR, 프래그먼트 레이아웃 |
| DB | MySQL 8.0 | schema.sql로 스키마 관리, local-seed-data.sql로 로컬 전용 시드 데이터 관리 |
| Build | Gradle | |
| 배포 | EC2 1대 + Docker Compose (nginx / app / mysql) | |
| CI/CD | GitHub Actions → GHCR → EC2 SSH 배포 | `.github/workflows/ci-cd.yml` |

## 로컬 실행

```bash
# 1) MySQL 준비 (docker로 간단히)
docker run -d --name shop-mysql-local -p 3306:3306 \
  -e MYSQL_DATABASE=shopping_mall \
  -e MYSQL_USER=shop_user -e MYSQL_PASSWORD=shop_pass1234! \
  -e MYSQL_ROOT_PASSWORD=root1234! mysql:8.0

# 2) 애플리케이션 실행 (local 프로필 기본 활성화)
gradle bootRun
# 또는 IDE에서 gradle wrapper 생성 후 ./gradlew bootRun
```

앱 기동 시 `schema.sql`이 자동 실행되어 테이블이 생성되고, `local` 프로필에서는 `local-seed-data.sql`도 함께 실행되어 샘플 상품 12종과 데모 계정이 세팅된다.
`prod` 프로필에서는 시드 데이터를 절대 적재하지 않는다(알려진 비밀번호의 데모 admin 계정이 운영 DB에 생기는 것을 방지).

- 일반 회원가입: `/members/register`
- 관리자 데모 계정: `admin@mirocloset.com` / `admin1234!`

## 배포 (EC2)

1. EC2에 Docker, Docker Compose 설치
2. 리포지토리의 `docker-compose.yml`, `nginx/` 를 EC2로 복사
3. `.env.example`을 참고해 `.env` 작성 (DB 계정/비밀번호)
4. `docker compose up -d`
5. GitHub Actions 시크릿에 `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` 등록 → main 브랜치 push 시 자동 빌드·배포

## 폴더 구조

```
src/main/java/com/mondaycloset/shop
├── domain          # 엔티티 (도메인별 패키지: member, category, product, cart, order, security)
├── repository       # Spring Data JPA 리포지토리
├── service           # 비즈니스 로직 (admin 하위 패키지: 관리자 전용 서비스)
├── security          # Spring Security 인증 관련 구성요소
├── config            # SecurityConfig, WebConfig
├── web
│   ├── controller    # Controller (admin 하위 패키지: 관리자 컨트롤러)
│   └── dto           # 화면/요청 바인딩용 DTO (엔티티 미노출 원칙)
└── global
    └── exception     # BusinessException, ErrorCode, GlobalExceptionHandler
```

## 설계 문서

- `docs/erd/shopping_mall_erd.png` — ERD (정규화/비정규화 설계 포인트 포함)
- `docs/monday-closet-presentation.pptx` — 발표자료 (5분 PT용)
- `src/main/resources/schema.sql` — 물리 스키마 (ERD 대비 추가된 기술 컬럼: product.version, login_history.email_attempted)

## 핵심 설계 포인트 (발표 요약)

1. **비정규화 설계**: `product.thumbnail_url`(목록 조회 성능), `order_item.product_name/price`(주문 이력 스냅샷), `orders.total_price`(집계값 저장) — 세 곳 모두 "왜 정규화를 깼는가"를 설명 가능하도록 주석으로 남겨두었다.
2. **동시성 제어**: `Product.version`(@Version) 기반 낙관적 락으로 동시 주문 시 재고 초과 판매를 방지한다.
3. **보안**: BCrypt 비밀번호 해시, 로그인 성공 시 세션ID 재발급(세션 고정 공격 방어), CSRF(Spring Security 기본 + Thymeleaf 자동 토큰 삽입), `login_history` 기반 비정상 로그인 시도 추적.
4. **소프트 삭제**: 상품 삭제 대신 `HIDDEN` 상태 전환 — 장바구니/주문 이력의 참조 무결성을 깨지 않는다.
5. **계층 구조**: Controller → Service → Repository → Domain, DTO로 엔티티 비노출, `@ControllerAdvice` 기반 전역 예외 처리로 스택트레이스 미노출.

## 알려진 제약 / 다음 단계 (의도적으로 범위에서 제외)

- 실제 PG 결제 연동 없음 (주문 확정 = 결제 완료로 간주)
- 상품 이미지는 URL 텍스트 입력 방식 (S3 업로드는 로드맵)
- 배송지는 회원당 1개 (주소록 다중관리 미지원)
