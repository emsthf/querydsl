package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);  // 필드로 빼도 된다. 그럼 여러 멀티 쓰레드에서 접근하면 동시성 문제가 생기지 않을까? -> 스프링에서 주입해주는 em 자체가 동시성에 문제 없도록 설계가 되어 있다. 여러 멀티 쓰레드에서 들어와도 트랜잭션이 어디에 걸려있는지에 따라서 트랜잭션에 단위로 분배를 해줌.
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() throws Exception {
        // given
        String qlString =
                "select m from Member m " +
                "where m.username = :username";

        // when
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() throws Exception {
        // given

        // when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))  // parameter binding
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() throws Exception {
        // given

        // when
        Member findMember = queryFactory
                .selectFrom(member)  // selec와 from을 합친 메서드
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() throws Exception {
        // given

        // when
        Member findMember = queryFactory
                .selectFrom(member)  // selec와 from을 합친 메서드
                .where(
                        member.username.eq("member1"),  // ,로 and 조건을 이어서 사용할 수 있다.
                        member.age.eq(10)
                )
                .fetchOne();

        // then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() throws Exception {
        // given

        // when
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();// 리스트로 조회

        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();  // 단 건 조회

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();// limit(1).fetchOne()과 같다.

        // then

    }

    @Test
    void count() throws Exception {
        // given

        // when
        Long totalCount = queryFactory
                .select(member.count())  // select count(member.id)와 같다.
                .from(member)
                .fetchOne();

        // then
        assertThat(totalCount).isEqualTo(4);
    }

    @Test
    void totalCount() throws Exception {
        // given
        Member member5 = new Member("member5", 10);
        Member member6 = new Member("member6", 10);
        em.persist(member5);
        em.persist(member6);

        // when
        int total = queryFactory
                .select(member)
                .from(member)
                .fetch().size();  // fetchResult()가 deprecated 되었기 때문에 totalCount를 계산할 때에는 fetch().size()를 사용한다.

        // then
        assertThat(total).isEqualTo(6);
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    void sort() throws Exception {
        // given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        // when
        List<Member> results = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())  // nullsFirst()도 있다.
                .fetch();

        // then
        Member member5 = results.get(0);
        Member member6 = results.get(1);
        Member memberNull = results.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    void paging1() throws Exception {
        // given

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)  // offset은 앞의 몇개를 끊어서 사용할 건지. 0부터 시작이므로 1개를 스킵한다는 말.
                .limit(2)
                .fetch();

        // then
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2() throws Exception {
        // given

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)  // offset은 앞의 몇개를 끊어서 사용할 건지. 0부터 시작이므로 1개를 스킵한다는 말.
                .limit(2)
                .fetch();

        int totalCount = queryFactory
                .selectFrom(member)
                .fetch().size();

        // then
        assertThat(result.size()).isEqualTo(2);
        assertThat(totalCount).isEqualTo(4);
    }

    @Test
    void aggregation() throws Exception {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        // then
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    void groupBy() throws Exception {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)  // member.team이 team과 조인이 된다.
                .groupBy(team.name)
                .fetch();

        // then
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    void join() throws Exception {
        // given

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)  // member.team이 team과 조인이 된다.
                .where(team.name.eq("teamA"))
                .fetch();

        // then
        assertThat(result)
                .extracting("username")  // username이
                .containsExactly("member1", "member2");  // member1, member2가 나올 것이다.
    }

    /**
     * 세타 조인 (연관관계가 없는 조인)
     * 회원의 이름이 팀 이름과 같은 회원을 조회
     */
    @Test
    void thetaJoin() throws Exception {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)  // from 절에 여러 엔티티를 나열해서 세타 조인을 할 수 있다.
                .where(member.username.eq(team.name))
                .fetch();  // 모든 회원을 가져오고, 모든 팀을 가져와서 다 조인을 해버리는 것. 그리고 where절에서 필터링. DB가 알아서 성능 최적화를 해주긴 한다.

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인하고 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    void join_on_filtering() throws Exception {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))  // on절을 통해서 조인 대상을 필터링 할 수 있다.
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // then

    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 회원을 조회
     */
    @Test
    void join_on_on_relation() throws Exception {
        // given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(team).on(member.username.eq(team.name))  // 보통 join은 join(member.team, team)이렇게 사용하는데 이 연관관계가 없는 외부 조인은 leftJoin(team)으로 사용한다.
                .fetch();// 모든 회원을 가져오고, 모든 팀을 가져와서 다 조인을 해버리는 것. 그리고 where절에서 필터링. DB가 알아서 성능 최적화를 해주긴 한다.

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // then
        assertThat(result)
                .extracting(tuple -> tuple.get(0, Member.class).getUsername())
                .containsExactly("teamA", "teamB");
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNo() throws Exception {
        // given
        // 페치 조인할 때 영속성 컨텍스트에 있는 것을 안지워주면 제대로 된 결과를 보기 힘들기 때문에 비워준다.
        em.flush();
        em.clear();
        
        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        // Member에 있는 연관관계 team은 LAZY로 세팅했기 때문에 딱 member만 조회되고 team은 조회되지 않는다.


        // then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 이미 로딩된 엔티티인지, 초기화되지 않은 엔티티인지 판별
        assertThat(loaded).as("페치 조인 미적용").isFalse();  // LAZY 로딩 세팅으로 team은 아직 초기화가 안되어 있기 때문에 false가 나온다.
    }

    @Test
    void fetchJoinUse() throws Exception {
        // given
        // 페치 조인할 때 영속성 컨텍스트에 있는 것을 안지워주면 제대로 된 결과를 보기 힘들기 때문에 비워준다.
        em.flush();
        em.clear();

        // when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()  // 페치 조인을 사용하면 LAZY 로딩 세팅을 하더라도 member와 연관관계가 있는 team도 같이 조회가 된다.
                .where(member.username.eq("member1"))
                .fetchOne();
        // Member에 있는 연관관계 team은 LAZY로 세팅했기 때문에 딱 member만 조회되고 team은 조회되지 않는다.


        // then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// 이미 로딩된 엔티티인지, 초기화되지 않은 엔티티인지 판별
        assertThat(loaded).as("페치 조인 적용").isTrue();  // 페치 조인을 사용했기 때문에 team도 한 쿼리로 같이 조회가 되서 true가 나온다.
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() throws Exception {
        // given
        // 서브쿼리이기 때문에 밖에 있는 메인쿼리의 member와 겹치면 안된다. 그래서 서브쿼리용 member의 alias를 따로 만들어준다.
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        // then
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    void subQueryGoe() throws Exception {
        // given
        // 서브쿼리이기 때문에 밖에 있는 메인쿼리의 member와 겹치면 안된다. 그래서 서브쿼리용 member의 alias를 따로 만들어준다.
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        // then
        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 서브쿼리 in절로 나이가 10 이상인 회원 조회
     */
    @Test
    void subQueryIn() throws Exception {
        // given
        // 서브쿼리이기 때문에 밖에 있는 메인쿼리의 member와 겹치면 안된다. 그래서 서브쿼리용 member의 alias를 따로 만들어준다.
        QMember memberSub = new QMember("memberSub");

        // when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        // then
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQuery() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // then

    }

    @Test
    void basicCase() throws Exception {
        // given

        // when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

        // then

    }

    @Test
    void complexCase() throws Exception {
        // given

        // when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

        // then

    }

    @Test
    void constant() throws Exception {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // then

    }

    @Test
    void concat() throws Exception {
        // given

        // when
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))  // stringValue()를 사용하면 문자열로 변환해서 concat을 사용할 수 있다.
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

        // then

    }

    @Test
    void simpleProjection() throws Exception {
        // given

        // when
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("프로젝션 대상이 1개일 때 = " + s);
        }

        // then

    }

    @Test
    void tupleProjection() throws Exception {
        // given

        // when
        List<Tuple> result = queryFactory
                .select(member.username, member.age)  // 조회할 타입이 String도 있고, int도 있기 때문에 querydsl이 tuple 타입으로 반환
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }

        // then

    }

    @Test
    void findDtoByJPQL() throws Exception {
        // given
        String qlString = "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m";
        // JPQL은 이렇게 new 오퍼레이션을 사용해야 dto에 받을 것만 뽑아서 조회할 수 있다.
        List<MemberDto> resultList = em.createQuery(qlString, MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }

        // when

        // then

    }

    @Test
    void findDtoBySetter() throws Exception {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))  // Projections.bean()을 사용하면 setter를 통해서 값을 넣어준다.
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        // then

    }

    @Test
    void findDtoByField() throws Exception {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))  // Projections.fields()를 사용하면 필드에 값을 넣어준다.
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        // then

    }

    @Test
    void findDtoByConstructor() throws Exception {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))  // Projections.constructor()를 사용하면 생성자를 통해서 값을 넣어준다.
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        // then

    }

    @Test
    void findUserDto() throws Exception {
        // given
        QMember memberSub = new QMember("memberSub");

        // when
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                                member.username.as("name"),  // 필드명이 다르면 as를 사용해서 매칭시켜준다.
                                ExpressionUtils.as(JPAExpressions
                                        .select(memberSub.age.max())
                                        .from(memberSub), "age")  // 서브쿼리를 사용할 때는 ExpressionUtils.as()를 사용해서 별칭을 붙여준다.
                        )
                )
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }

        // then

    }

    @Test
    void findDtoByQueryProjection() throws Exception {
        // given

        // when
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))  // 생성자를 그대로 가져가기 때문에 타입도 다 맞춰준다.
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

        // then

    }

    @Test
    void dynamicQuery_BooleanBuilder() throws Exception {
        // given
        String usernameParam = "member1";
        Integer ageParam = null;

        // when
        List<Member> result = searchMember1(usernameParam, ageParam);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();  // BooleanBuilder를 사용하면 조건을 동적으로 넣을 수 있다.
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));  // BooleanBuilder에 and와 or를 사용해서 조립할 수 있다.
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)  // where절에 조립한 BooleanBuilder를 넣어주면 된다
                .fetch();
    }

    @Test
    void dynamicQuery_WhereParam() throws Exception {
        // given
        String usernameParam = "member1";
        Integer ageParam = null;

        // when
        List<Member> result = searchMember2(usernameParam, ageParam);

        // then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))  // 메서드를 직접 만들어서 where문 안에서 바로 해결해버릴 수 있다.
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;  // where에 null이 들어가면 기본적으로 무시하게 된다.
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));  // and를 사용해서 조립할 수 있다.(조립하려면 위 메서드 반환 타입을 BooleanExpression을 사용해야 한다.)
    }
}
