package amgn.amu.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String userId;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String email;

    @Column(length = 40)
    private String nickName;

    @Column(nullable = false, length = 1)
    private String gender;

    @Column(nullable = false, length = 40)
    private String phoneNumber;

    @Column(nullable = false , length = 10)
    private Integer birthYear;  // 생년
    @Column(nullable = false, length = 10)
    private Integer birthMonth;  // 생월
    @Column(nullable = false, length = 10)
    private Integer birthDay;  // 생일

    @Column(nullable = false, length = 40)
    private String province;  // 시/도
    @Column(nullable = false, length = 40)
    private String city;  // 시/군/구
    @Column(nullable = false)
    private String detailAddress;  // 상세주소

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
