# Java 21 기반 이미지 사용
FROM openjdk:21-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# Maven 래퍼와 pom.xml 복사
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 소스 코드 복사
COPY src src

# 실행 권한 부여
RUN chmod +x mvnw

# 애플리케이션 빌드
RUN ./mvnw clean package -DskipTests

# JAR 파일을 app.jar로 복사
RUN cp target/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"] 