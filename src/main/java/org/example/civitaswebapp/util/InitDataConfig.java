package org.example.civitaswebapp.util;

import org.example.civitaswebapp.domain.*;
import org.example.civitaswebapp.repository.*;
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
    private EventRepository eventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UnionRepository unionRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {


        Union testUnion = Union.builder()
                .name("Civitas Demo Union")
                .address("Kerkstraat 1, 9140 Temse")
                .vatCode("BE0123.456.789")
                .build();

        unionRepository.save(testUnion);

        Union unionB = Union.builder()
                .name("Student Union Ghent")
                .address("Kouter 1, 9000 Gent")
                .vatCode("BE0999.888.777")
                .build();
        unionRepository.save(unionB);

        MyUser adminB = MyUser.builder()
                .username("admin_gent")
                .password(passwordEncoder.encode("gent123"))
                .role(MyUserRole.ADMIN)
                .union(unionB)
                .build();
        userRepository.save(adminB);


        List<Event> events = new ArrayList<>();


        events.add(new Event(null, "Board Meeting", "Monthly board meeting.", LocalDateTime.now().plusDays(2).withHour(18).withMinute(0), LocalDateTime.now().plusDays(2).withHour(20).withMinute(0), "Union Office", EventType.MEETING, new HashSet<>(), testUnion));
        events.add(new Event(null, "Community Clean-up", "Clean up the park!", LocalDateTime.now().plusDays(5).withHour(10).withMinute(0), LocalDateTime.now().plusDays(5).withHour(13).withMinute(0), "Central Park", EventType.SOCIAL, new HashSet<>(), testUnion));
        events.add(new Event(null, "Annual General Meeting", "Review the year.", LocalDateTime.now().plusWeeks(1).withHour(19).withMinute(0), LocalDateTime.now().plusWeeks(1).withHour(21).withMinute(0), "Community Center", EventType.MEETING, new HashSet<>(), testUnion));
        events.add(new Event(null, "Leadership Workshop", "Skills workshop.", LocalDateTime.now().plusDays(10).withHour(14).withMinute(0), LocalDateTime.now().plusDays(10).withHour(17).withMinute(0), "Training Center", EventType.WORKSHOP, new HashSet<>(), testUnion));
        events.add(new Event(null, "Member Welcome", "Welcome new members.", LocalDateTime.now().plusDays(7).withHour(16).withMinute(0), LocalDateTime.now().plusDays(7).withHour(19).withMinute(0), "Union Hall", EventType.SOCIAL, new HashSet<>(), testUnion));

        eventRepository.saveAll(events);

        // 3. CREATE USERS (Linked to Union) 👤
        MyUser admin1 = MyUser.builder()
                .username("apo")
                .password(passwordEncoder.encode("apo"))
                .role(MyUserRole.ADMIN)
                .union(testUnion) // <--- LINKED
                .build();

        MyUser admin2 = MyUser.builder()
                .username("boardTom")
                .password(passwordEncoder.encode("adminpass2"))
                .role(MyUserRole.ADMIN)
                .union(testUnion) // <--- LINKED
                .build();

        userRepository.saveAll(Arrays.asList(admin1, admin2));

        // 4. CREATE MEMBERS (Linked to Union) 👥
        List<Member> memberList = new ArrayList<>();
        String[][] names = {
                {"Ali", "Khan"}, {"Fatima", "Yildiz"}, {"Doğukan", "Demir"}, {"Leyla", "Öztürk"},
                {"Ahmet", "Yılmaz"}, {"Ayşe", "Kaya"}, {"Mehmet", "Şahin"}, {"Elif", "Demirtaş"},
                {"Can", "Çelik"}, {"Zeynep", "Aksoy"}, {"Murat", "Koç"}, {"Seda", "Aydın"},
                {"Emre", "Polat"}, {"Derya", "Kurt"}, {"Burak", "Erdem"}, {"Aslı", "Özdemir"},
                {"Ozan", "Kara"}, {"Selin", "Çetin"}, {"Hakan", "Güneş"}, {"Deniz", "Kılıç"}
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
                    .union(testUnion) // <--- LINKED
                    .build();

            // Create Transactions (Linked to Union) 💶
            List<Transaction> transactions = new ArrayList<>();
            int numTransactions = 2 + random.nextInt(3);
            TransactionType[] types = TransactionType.values();

            for (int j = 0; j < numTransactions; j++) {
                Transaction tx = Transaction.builder()
                        .member(member)
                        .amount(5.0 + random.nextInt(20))
                        .currency("EUR")
                        .status(TransactionStatus.values()[random.nextInt(TransactionStatus.values().length)])
                        .type(types[random.nextInt(types.length)])
                        .createdAt(LocalDateTime.now().minusDays(random.nextInt(60)))
                        .updatedAt(LocalDateTime.now())
                        .union(testUnion) // <--- LINKED (Don't forget this one!)
                        .build();
                transactions.add(tx);
            }
            member.setTransactions(transactions);
            memberList.add(member);
        }

        memberRepository.saveAll(memberList);


        List<Notification> notifications = new ArrayList<>();

        Notification n1 = Notification.builder().user(admin1).url("/members/view/1").title("Nieuw lid").message("Lid Ali Khan is toegevoegd.").type(NotificationType.MEMBER).status(NotificationStatus.UNREAD).createdAt(java.time.Instant.now()).build();
        Notification n2 = Notification.builder().user(admin2).url("/events").title("Nieuwe activiteit").message("Nieuw evenement: Board Meeting.").type(NotificationType.EVENT).status(NotificationStatus.UNREAD).createdAt(java.time.Instant.now()).build();
        Notification n3 = Notification.builder().user(admin1).url("/transactions").title("Betaling ontvangen").message("Fatima heeft betaald.").type(NotificationType.TRANSACTION).status(NotificationStatus.READ).createdAt(java.time.Instant.now().minusSeconds(3600)).build();

        notificationRepository.saveAll(Arrays.asList(n1, n2, n3));

        System.out.println("✅ Data Seeding Completed: Created Union 'Civitas Demo Union' with ID: " + testUnion.getId());
    }
}