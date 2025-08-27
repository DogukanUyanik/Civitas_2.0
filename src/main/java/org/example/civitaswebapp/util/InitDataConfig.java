package org.example.civitaswebapp;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
public class InitDataConfig implements CommandLineRunner {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MyUserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // Seed MyUsers (board/admins)
        MyUser admin1 = MyUser.builder()
                .username("adminFatima")
                .password(passwordEncoder.encode("adminpass1"))
                .role(MyUserRole.ADMIN)
                .build();

        MyUser admin2 = MyUser.builder()
                .username("boardTom")
                .password(passwordEncoder.encode("adminpass2"))
                .role(MyUserRole.ADMIN)
                .build();

        userRepository.saveAll(Arrays.asList(admin1, admin2));

        // Seed Members
        Member member1 = Member.builder()
                .firstName("Ali")
                .lastName("Khan")
                .email("ali@example.com")
                .phoneNumber("123456789")
                .address("Street 1, City")
                .dateOfBirth(LocalDate.of(1990, 5, 10))
                .dateOfLastPayment(LocalDate.now())
                .memberStatus(MemberStatus.ACTIVE)
                .build();

        Member member2 = Member.builder()
                .firstName("Fatima")
                .lastName("Yildiz")
                .email("fatima@example.com")
                .phoneNumber("987654321")
                .address("Street 2, City")
                .dateOfBirth(LocalDate.of(1995, 8, 20))
                .dateOfLastPayment(LocalDate.now().minusMonths(2))
                .memberStatus(MemberStatus.INACTIVE)
                .build();

        Member member3 = Member.builder()
                .firstName("Doğukan")
                .lastName("Demir")
                .email("dogukan@example.com")
                .phoneNumber("555123456")
                .address("Street 3, City")
                .dateOfBirth(LocalDate.of(1998, 2, 15))
                .dateOfLastPayment(LocalDate.now())
                .memberStatus(MemberStatus.ACTIVE)
                .build();

        memberRepository.saveAll(Arrays.asList(member1, member2, member3));
    }
}
