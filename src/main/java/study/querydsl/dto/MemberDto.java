package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection  // 이 어노테이션이 있으면 컴파일할 때 DTO도 Q파일로 생성해준다. Querydsl에서 DTO로 바로 조회하기 위해 사용
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}