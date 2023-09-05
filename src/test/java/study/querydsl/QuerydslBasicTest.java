package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

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
                .fetchOne();// 단 건 조회

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
}
