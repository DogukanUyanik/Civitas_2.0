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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        List<Member> members = new ArrayList<>();

        String[][] names = {
                {"Ali", "Khan"}, {"Fatima", "Yildiz"}, {"Doğukan", "Demir"}, {"Leyla", "Öztürk"},
                {"Ahmet", "Yılmaz"}, {"Ayşe", "Kaya"}, {"Mehmet", "Şahin"}, {"Elif", "Demirtaş"},
                {"Can", "Çelik"}, {"Zeynep", "Aksoy"}, {"Murat", "Koç"}, {"Seda", "Aydın"},
                {"Emre", "Polat"}, {"Derya", "Kurt"}, {"Burak", "Erdem"}, {"Aslı", "Özdemir"},
                {"Ozan", "Kara"}, {"Selin", "Çetin"}, {"Hakan", "Güneş"}, {"Deniz", "Kılıç"},
                {"Cem", "Arslan"}, {"Ebru", "Taş"}, {"Serkan", "Yavuz"}, {"Melis", "Özcan"},
                {"Tunahan", "Karaca"}, {"Gamze", "Demirci"}, {"Furkan", "Koçak"}, {"Merve", "Bulut"},
                {"Yusuf", "Doğan"}, {"İrem", "Öztürk"}
        };

        for (int i = 0; i < names.length; i++) {
            Member member = Member.builder()
                    .firstName(names[i][0])
                    .lastName(names[i][1])
                    .email(names[i][0].toLowerCase() + "." + names[i][1].toLowerCase() + "@example.com")
                    .phoneNumber("555000" + (100 + i))
                    .address("Street " + (i + 1) + ", City")
                    .dateOfBirth(LocalDate.of(1990 + (i % 10), (i % 12) + 1, (i % 28) + 1))
                    .dateOfLastPayment(LocalDate.now().minusMonths(i % 6))
                    .memberStatus(i % 3 == 0 ? MemberStatus.INACTIVE : MemberStatus.ACTIVE)
                    .build();
            members.add(member);
        }

        memberRepository.saveAll(members);
    }
}
