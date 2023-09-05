package study.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

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
}
