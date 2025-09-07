package org.example.civitaswebapp;

import org.example.civitaswebapp.domain.*;
import org.example.civitaswebapp.repository.EventRepository;
import org.example.civitaswebapp.repository.MemberRepository;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class InitDataConfig implements CommandLineRunner {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MyUserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Inside your InitDataConfig class
    @Autowired
    private EventRepository eventRepository;

    @Override
    public void run(String... args) throws Exception {

        List<Event> events = new ArrayList<>();

        events.add(new Event(
                null,
                "Board Meeting",
                "Monthly board meeting to discuss union activities.",
                LocalDateTime.now().plusDays(2).withHour(18).withMinute(0),
                LocalDateTime.now().plusDays(2).withHour(20).withMinute(0),
                "Union Office - Conference Room A",
                EventType.MEETING,
                new HashSet<>()
        ));

        events.add(new Event(
                null,
                "Community Clean-up",
                "Join us to clean up the local park!",
                LocalDateTime.now().plusDays(5).withHour(10).withMinute(0),
                LocalDateTime.now().plusDays(5).withHour(13).withMinute(0),
                "Central Park - Main Entrance",
                EventType.SOCIAL,
                new HashSet<>()
        ));

        events.add(new Event(
                null,
                "Annual General Meeting",
                "Annual meeting for all members to review the year.",
                LocalDateTime.now().plusWeeks(1).withHour(19).withMinute(0),
                LocalDateTime.now().plusWeeks(1).withHour(21).withMinute(0),
                "Community Center - Main Hall",
                EventType.MEETING,
                new HashSet<>()
        ));

        events.add(new Event(
                null,
                "Leadership Workshop",
                "Workshop on developing leadership skills for union representatives.",
                LocalDateTime.now().plusDays(10).withHour(14).withMinute(0),
                LocalDateTime.now().plusDays(10).withHour(17).withMinute(0),
                "Training Center - Room 201",
                EventType.WORKSHOP,
                new HashSet<>()
        ));

        events.add(new Event(
                null,
                "Member Welcome Event",
                "Welcome event for new members to meet the community.",
                LocalDateTime.now().plusDays(7).withHour(16).withMinute(0),
                LocalDateTime.now().plusDays(7).withHour(19).withMinute(0),
                "Union Hall - Main Floor",
                EventType.SOCIAL,
                new HashSet<>()
        ));

        events.add(new Event(
                null,
                "Monthly Newsletter Planning",
                "Planning session for next month's newsletter content.",
                LocalDateTime.now().plusDays(14).withHour(15).withMinute(0),
                LocalDateTime.now().plusDays(14).withHour(16).withMinute(30),
                "Union Office - Meeting Room B",
                EventType.GENERAL,
                new HashSet<>()
        ));

        eventRepository.saveAll(events);


        // Seed MyUsers (board/admins)
        MyUser admin1 = MyUser.builder()
                .username("apo")
                .password(passwordEncoder.encode("apo"))
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
