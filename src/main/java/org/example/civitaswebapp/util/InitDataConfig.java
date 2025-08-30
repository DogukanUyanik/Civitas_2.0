package org.example.civitaswebapp;

import org.example.civitaswebapp.domain.*;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

        Random random = new Random();

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

            // Create a few mock transactions per member
            List<Transaction> transactions = new ArrayList<>();
            int numTransactions = 2 + random.nextInt(3); // 2-4 transactions
            TransactionType[] types = TransactionType.values();
            for (int j = 0; j < numTransactions; j++) {
                Transaction tx = Transaction.builder()
                        .member(member)
                        .amount(5.0 + random.nextInt(20)) // Random amount €5-€24
                        .currency("EUR")
                        .status(TransactionStatus.values()[random.nextInt(TransactionStatus.values().length)])
                        .type(types[random.nextInt(types.length)]) // Random TransactionType
                        .createdAt(LocalDateTime.now().minusDays(random.nextInt(60)))
                        .updatedAt(LocalDateTime.now())
                        .build();
                transactions.add(tx);
            }
            member.setTransactions(transactions);


            members.add(member);
        }

        memberRepository.saveAll(members);
    }
}
