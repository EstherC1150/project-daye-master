# Spring Security Taglibs + MariaDB 예제

이 프로젝트는 최신 기술 스택을 사용하여 Spring Security와 Thymeleaf를 통합한 웹 애플리케이션입니다.

## 기술 스택

- **Java**: 21 (LTS)
- **Spring Boot**: 3.2.0
- **Spring Security**: 6.x
- **Thymeleaf**: 3.x + Spring Security taglibs
- **MariaDB**: 11.5
- **JPA/Hibernate**: 최신 버전
- **Maven**: 3.9.5
- **Docker**: 컨테이너화 및 배포

## 주요 기능

### 1. 인증 상태 확인
```html
<!-- 로그인 상태 확인 -->
<p sec:authorize="isAuthenticated()">로그인되어 있습니다.</p>
<p sec:authorize="!isAuthenticated()">로그인되지 않았습니다.</p>
```

### 2. 사용자 정보 표시
```html
<!-- 사용자명 표시 -->
<span sec:authentication="name"></span>

<!-- 권한 표시 -->
<span sec:authentication="principal.authorities"></span>
```

### 3. 역할별 접근 제어
```html
<!-- 특정 역할 확인 -->
<div sec:authorize="hasRole('ADMIN')">관리자 전용 섹션</div>
<div sec:authorize="hasRole('USER')">사용자 전용 섹션</div>

<!-- 여러 역할 중 하나라도 있으면 표시 -->
<div sec:authorize="hasAnyRole('ADMIN', 'USER')">공통 섹션</div>
```

### 4. 권한별 접근 제어
```html
<!-- 특정 권한 확인 -->
<div sec:authorize="hasAuthority('READ')">읽기 권한이 있습니다.</div>
<div sec:authorize="hasAuthority('WRITE')">쓰기 권한이 있습니다.</div>

<!-- 여러 권한 중 하나라도 있으면 표시 -->
<div sec:authorize="hasAnyAuthority('READ', 'WRITE')">읽기 또는 쓰기 권한이 있습니다.</div>
```

## 설정된 사용자 계정

### 관리자 계정
- **사용자명**: admin
- **비밀번호**: admin123
- **역할**: ADMIN

### 일반 사용자 계정
- **사용자명**: user
- **비밀번호**: user123
- **역할**: USER

### 매니저 계정
- **사용자명**: manager
- **비밀번호**: manager123
- **역할**: ADMIN, USER

## 주요 페이지

1. **홈 페이지 (/)**: Spring Security taglibs의 모든 기능을 보여주는 메인 페이지
2. **관리자 페이지 (/admin)**: ADMIN 역할을 가진 사용자만 접근 가능
3. **사용자 페이지 (/user)**: USER 역할을 가진 사용자만 접근 가능
4. **프로필 페이지 (/profile)**: 로그인한 모든 사용자가 접근 가능
5. **Actuator (/actuator)**: 애플리케이션 모니터링 엔드포인트

## Spring Security Taglibs 주요 속성

### sec:authorize
- `isAuthenticated()`: 인증된 사용자
- `!isAuthenticated()`: 인증되지 않은 사용자
- `hasRole('ROLE_NAME')`: 특정 역할 확인
- `hasAnyRole('ROLE1', 'ROLE2')`: 여러 역할 중 하나라도 있는지 확인
- `hasAuthority('AUTHORITY')`: 특정 권한 확인
- `hasAnyAuthority('AUTH1', 'AUTH2')`: 여러 권한 중 하나라도 있는지 확인

### sec:authentication
- `name`: 사용자명
- `principal.authorities`: 사용자의 권한 목록
- `principal.username`: 사용자명 (name과 동일)
- `principal.password`: 비밀번호 (보안상 권장하지 않음)

## 로컬 개발 환경 실행

### 1. MariaDB 설치 및 설정

#### Docker를 사용하는 경우:
```bash
# MariaDB 컨테이너 실행
docker run --name demo_mariadb \
  -e MYSQL_ROOT_PASSWORD=root_password \
  -e MYSQL_DATABASE=demo_db \
  -e MYSQL_USER=demo_user \
  -e MYSQL_PASSWORD=demo_password \
  -p 3306:3306 \
  -d mariadb:11.5
```

#### 로컬 MariaDB 설치:
1. MariaDB 11.5 설치
2. 데이터베이스 생성:
   ```sql
   CREATE DATABASE demo_db;
   CREATE USER 'demo_user'@'localhost' IDENTIFIED BY 'demo_password';
   GRANT ALL PRIVILEGES ON demo_db.* TO 'demo_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

### 2. 애플리케이션 실행

```bash
# 프로젝트 디렉토리로 이동
cd project/demo

# Maven을 사용하여 프로젝트 실행
mvn spring-boot:run
```

### 3. 브라우저에서 접속
- http://localhost:8080

## Docker를 사용한 배포

### 1. Docker Compose로 전체 스택 실행

```bash
# 프로젝트 디렉토리로 이동
cd project/demo

# Docker Compose로 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f
```

### 2. 개별 Docker 이미지 빌드 및 실행

```bash
# 애플리케이션 이미지 빌드
docker build -t demo-app .

# MariaDB 컨테이너 실행
docker run --name demo_mariadb \
  -e MYSQL_ROOT_PASSWORD=root_password \
  -e MYSQL_DATABASE=demo_db \
  -e MYSQL_USER=demo_user \
  -e MYSQL_PASSWORD=demo_password \
  -p 3306:3306 \
  -d mariadb:11.5

# 애플리케이션 컨테이너 실행
docker run --name demo_app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/demo_db \
  -e SPRING_DATASOURCE_USERNAME=demo_user \
  -e SPRING_DATASOURCE_PASSWORD=demo_password \
  -d demo-app
```

## 프로덕션 배포

### 1. 클라우드 플랫폼 배포

#### AWS ECS/Fargate:
```bash
# ECR에 이미지 푸시
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag demo-app:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/demo-app:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/demo-app:latest
```

#### Google Cloud Run:
```bash
# Cloud Build로 이미지 빌드
gcloud builds submit --tag gcr.io/PROJECT_ID/demo-app

# Cloud Run에 배포
gcloud run deploy demo-app --image gcr.io/PROJECT_ID/demo-app --platform managed
```

#### Azure Container Instances:
```bash
# Azure Container Registry에 푸시
az acr build --registry <registry-name> --image demo-app .
```

### 2. 쿠버네티스 배포

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: demo-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: demo-app
  template:
    metadata:
      labels:
        app: demo-app
    spec:
      containers:
      - name: demo-app
        image: demo-app:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:mariadb://mariadb-service:3306/demo_db
        - name: SPRING_DATASOURCE_USERNAME
          value: demo_user
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
```

## 모니터링 및 관리

### Actuator 엔드포인트
- **Health Check**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info
- **Metrics**: http://localhost:8080/actuator/metrics

### 데이터베이스 관리
```bash
# MariaDB 컨테이너에 접속
docker exec -it demo_mariadb mysql -u demo_user -p demo_db

# 사용자 조회
SELECT username, email, full_name FROM users;

# 역할 조회
SELECT u.username, ur.role FROM users u 
JOIN user_roles ur ON u.id = ur.user_id;
```

## 의존성

이 프로젝트는 다음 의존성을 사용합니다:

- `spring-boot-starter-security`: Spring Security 기본 기능
- `spring-boot-starter-thymeleaf`: Thymeleaf 템플릿 엔진
- `thymeleaf-extras-springsecurity6`: Spring Security 6용 Thymeleaf 확장
- `spring-boot-starter-data-jpa`: JPA/Hibernate
- `mariadb-java-client`: MariaDB JDBC 드라이버
- `spring-boot-starter-actuator`: 모니터링 및 관리

## 주의사항

- Spring Security taglibs를 사용하려면 `thymeleaf-extras-springsecurity6` 의존성이 필요합니다.
- Thymeleaf 템플릿에서 `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"` 네임스페이스를 선언해야 합니다.
- 보안 관련 정보는 서버 사이드에서도 검증해야 하며, 클라이언트 사이드 보안은 보조적인 역할만 합니다.
- 프로덕션 환경에서는 데이터베이스 비밀번호를 환경 변수나 시크릿으로 관리해야 합니다. 

## 나혼자만 정리
- 서버 부분: src/main/java/com/project/demo/ --> Java 소스코드
  - controller/: MVC컨트롤러(요청처리)
  - service/: 비즈니스 로직
  - repository/: 데이터 접근 계층
  - entity/: 데이터베이스 엔티티
  - config/: 설정 클래스들

- 프론트 부분: src/main/resources/templates/ --> Thymeleaf HTML 템플릿
  - index.html: 메인 페이지
  - admin.html: 관리자 페이지
  - user.html: 사용자 페이지
  - profile.html: 프로필 페이지
  