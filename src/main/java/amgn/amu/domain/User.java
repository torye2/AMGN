package amgn.amu.domain;

import amgn.amu.entity.Listing;
import amgn.amu.entity.Order;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(unique = true, nullable = false, length = 50)
    private String loginId;

    @Column(nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String userName;

    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String emailNormalized;

    @Column(length = 40)
    private String nickName;

    @Column(nullable = false, length = 1)
    private String gender;

    @Column(nullable = false, length = 40)
    private String phoneNumber;
    @Column(name = "phone_E164", nullable = false, length = 20)
    private String phoneE164;
    @Column(nullable = false, length = 1)
    private Integer phoneVerified;

    @Column(nullable = false, length = 1)
    private Integer profileCompleted;
    @Column(nullable = false)
    private LocalDateTime profileCompletedAt;

    @Column(nullable = false , length = 10)
    private Integer birthYear;  // 생년
    @Column(nullable = false, length = 10)
    private Integer birthMonth;  // 생월
    @Column(nullable = false, length = 10)
    private Integer birthDay;  // 생일

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Listing> listings = new ArrayList<>();

    @OneToMany(mappedBy = "buyer", fetch = FetchType.LAZY)
    private List<Order> ordersAsBuyer = new ArrayList<>();

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Order> ordersAsSeller = new ArrayList<>();
}
