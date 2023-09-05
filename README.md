# Querydsl
## 1. Querydsl이란?
Java를 위한 type-safe SQL-like 쿼리 생성 라이브러리.
이 라이브러리를 사용하면 쿼리의 구성 및 실행 중에 타입 안전성을 유지하면서 SQL 또는 JPQL과 같은 쿼리를 생성할 수 있다.

## 2. Querydsl의 특징
- 타입 안정성 : 쿼리 작성 중에 컴파일 타임에 오류를 감지한다. 이를 통해 실행 시간에 발생할 수 있는 많은 오류를 미리 방지할 수 있다.
- 체이닝 방식의 쿼리 작성 : 쿼리를 체이닝 방식으로 작성하므로 가독성이 좋고, 유동적인 쿼리 조건을 쉽게 조합할 수 있다.
- 다양한 백엔드 지원 : JPA 뿐만 아니라 SQL, JDO, Lucene, Hibernate Search, MongoDB 등 다양한 백엔에 대한 지원을 제공한다.
- 코드 생성도구 포함 : JPA 엔터티나 SQL 스키마 등을 기반으로 메타모델 코드(Q-타입)를 자동 생성하는 도구가 포함되어 있다.

### Q파일
QueryDSL의 특징으로는 Q파일이 있다. JAVA compiler가 Q파일을 생성하고, Q파일을 통해 type-safe한 쿼리를 생성할 수 있다.
Q파일은 QueryDSL의 중심적인 부분이며, 타입 안전성을 보장하면서 쿼리를 쉽게 작성하게 해준다.



# Spring Boot 3.x.x 에서 Querydsl 사용하기
이번 프로젝트는 Spring Boot 3.1.3을 사용.
Spring Boot 2.x.x 버전과는 사용법이 많이 달라졌다.

```groovy
// Spring Boot 3.x.x Querydsl 설정
implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
annotationProcessor "com.querydsl:querydsl-apt:${dependencyManagement.importedProperties['querydsl.version']}:jakarta"
annotationProcessor "jakarta.annotation:jakarta.annotation-api"
annotationProcessor "jakarta.persistence:jakarta.persistence-api"
```

2.x.x 버전에서는 querydsl-apt를 사용했지만, 3.x.x 버전에서는 querydsl-apt가 아닌 querydsl-apt:jakarta를 사용해야 한다.
부트 3.x.x 이상에서는 2.x.x 처럼 복잡한 설정이 필요 없다.(오라클이 Java EE에 대한 권리를 포기하고 Eclipse 재단으로 이관되면서 상표권 문제 때문에 Java EE의 이름이 Jakarta EE로 변경되었다. 스프링부터 3.x 부터는 Java 17이상부터 가능하므로 Jakarta를 사용해야 한다.) 

위 디펜던시 4줄을 build.gradle의 dependencies에 추가 해주면 끝.
그리고 Gradle > Complie QueryDSL을 할 필요 없이, 그냥 main을 실행하면 Q파일이 자동으로 생성된다.(만약 main을 실행해도 생성되지 않는다면 Gradle > build를 한번 해주면 된다.)

```
이렇게 만들어진 Q파일은 깃으로 관리하면 안된다. 시스템이 만들어주는 것이기 때문에 버전이 달라지면 내용도 달라질 수 있다.
```

## Q파일의 심볼을 처리할 수 없을 때
IDE 설정 → 빌드,실행,배포 → Gradle → 다음을 설정하여 빌드 및 실행 → Gradle로 설정