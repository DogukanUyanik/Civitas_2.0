package org.example.civitaswebapp.util;

import org.example.civitaswebapp.domain.*;
import org.example.civitaswebapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Component
public class InitDataConfig implements CommandLineRunner {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MyUserRepository userRepository;

    @Autowired
    private UnionRepository unionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Fixed seed so the demo dataset looks the same on every restart (schema is recreated on every
    // boot via ddl-auto=create-drop) — handy for reproducible screenshots.
    private final Random random = new Random(20260825L);

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.findByUsername("admin_TemseDiyanet") == null) {
            seedTemseDiyanet();
        } else {
            System.out.println("✅ Temse Diyanet cami bestaat al. Seeding overgeslagen.");
        }

        if (userRepository.findByUsername("demo_admin") == null
                && !unionRepository.existsByName("Culturele Vereniging De Brug")) {
            seedCultureleVerenigingDeBrug();
        } else {
            System.out.println("✅ Culturele Vereniging De Brug bestaat al. Seeding overgeslagen.");
        }
    }

    private void seedTemseDiyanet() {
        // 1. CREATE UNION (Temse Diyanet)
        Union diyanetUnion = Union.builder()
                .name("Temse Diyanet cami")
                .address("Schoolstraat 139, 9140 Temse, Belgium")
                .vatCode("BE0000.000.000") // Dummy BTW, pas aan indien bekend
                .build();

        unionRepository.save(diyanetUnion);

        MyUser admin = MyUser.builder()
                .username("admin_TemseDiyanet")
                .password(passwordEncoder.encode("Bmit9140"))
                .role(MyUserRole.ADMIN)
                .union(diyanetUnion)
                .build();

        userRepository.save(admin);

        Member dogukan = Member.builder()
                .firstName("Dogukan")
                .lastName("Uyanik")
                .phoneNumber("+32486290585")
                .email("dogukanuyanik9140@gmail.com")
                .address("Roeland Lefevrestraat 37, 9140 Temse")
                .dateOfBirth(LocalDate.of(2003, 4, 25))
                .dateOfLastPayment(LocalDate.now()) // Zet op vandaag als default
                .memberStatus(MemberStatus.ACTIVE)
                .union(diyanetUnion)
                .build();

        memberRepository.save(dogukan);

        System.out.println("✅ Data Seeding Completed: Temse Diyanet cami, Admin & Dogukan succesvol toegevoegd!");
    }

    // ------------------------------------------------------------------------------------------
    // Demo showcase tenant: "Culturele Vereniging De Brug" — a populated, realistic association
    // used for screenshots/demos. Fully self-contained; does not touch any other tenant's data.
    // ------------------------------------------------------------------------------------------

    private static final String[][] NAME_POOL = {
            // Turkish members (paired with MemberLanguage.TR)
            {"Ayşe", "Yılmaz", "TR"}, {"Mehmet", "Kaya", "TR"}, {"Fatma", "Demir", "TR"},
            {"Ahmet", "Çelik", "TR"}, {"Zeynep", "Şahin", "TR"}, {"Mustafa", "Yıldız", "TR"},
            {"Elif", "Aydın", "TR"}, {"Hasan", "Öztürk", "TR"}, {"Emine", "Arslan", "TR"},
            {"Hüseyin", "Doğan", "TR"}, {"Meryem", "Kılıç", "TR"}, {"Ali", "Aslan", "TR"},
            {"Büşra", "Çetin", "TR"}, {"İbrahim", "Kurt", "TR"}, {"Selin", "Koç", "TR"},
            {"Yusuf", "Kara", "TR"}, {"Derya", "Şen", "TR"}, {"Emre", "Aksoy", "TR"},
            {"Gizem", "Yıldırım", "TR"}, {"Burak", "Özdemir", "TR"},

            // Flemish / Dutch-speaking members (paired with MemberLanguage.NL)
            {"Joris", "Peeters", "NL"}, {"Thomas", "De Smet", "NL"}, {"Sander", "Janssens", "NL"},
            {"Wouter", "Maes", "NL"}, {"Bart", "Jacobs", "NL"}, {"Kevin", "Mertens", "NL"},
            {"Tom", "Willems", "NL"}, {"Dries", "Claes", "NL"}, {"Wim", "Goossens", "NL"},
            {"Bram", "Wouters", "NL"}, {"Niels", "Vermeulen", "NL"}, {"An", "Segers", "NL"},
            {"Lotte", "Van Damme", "NL"}, {"Anke", "De Clercq", "NL"}, {"Els", "Verlinden", "NL"},
            {"Sarah", "Hermans", "NL"}, {"Charlotte", "De Backer", "NL"}, {"Femke", "Van Loon", "NL"},
            {"Julie", "Cools", "NL"}, {"Laura", "Van den Berghe", "NL"}, {"Nele", "Verstraete", "NL"},
            {"Kirsten", "De Vos", "NL"}, {"Ellen", "Van Acker", "NL"}, {"Katrien", "De Wilde", "NL"},
            {"Ilse", "Van Hecke", "NL"}, {"Griet", "De Ridder", "NL"}, {"Karen", "Vermeersch", "NL"},
            {"Evi", "De Coninck", "NL"}, {"Steven", "Van Dyck", "NL"}, {"Koen", "Wauters", "NL"},

            // French-speaking members (paired with MemberLanguage.EN — no FR option on the enum)
            {"Marie", "Dubois", "EN"}, {"Camille", "Lefevre", "EN"}, {"Julien", "Simon", "EN"},
            {"Antoine", "Laurent", "EN"}, {"Pierre", "Michel", "EN"}, {"Chloé", "Bernard", "EN"},
            {"Léa", "Rousseau", "EN"}, {"Manon", "Moreau", "EN"}, {"Hugo", "Petit", "EN"},
            {"Adrien", "Lambert", "EN"}, {"Sophie", "Dupont", "EN"}, {"Amandine", "Fontaine", "EN"},
            {"Baptiste", "Girard", "EN"}, {"Aurélie", "Mercier", "EN"}, {"Mathieu", "Roy", "EN"},
    };

    private static final String[] STREETS = {
            "Kerkstraat", "Stationsstraat", "Molenstraat", "Dorpsstraat", "Nieuwstraat",
            "Bruggestraat", "Kasteeldreef", "Lindenlaan", "Veldstraat", "Bergstraat"
    };

    private static final String[] CITIES = {
            "9140 Temse", "9100 Sint-Niklaas", "9000 Gent", "2000 Antwerpen",
            "9200 Dendermonde", "9120 Beveren", "9160 Lokeren", "9060 Zelzate"
    };

    private static final String[] EMAIL_DOMAINS = {"gmail.com", "hotmail.com", "outlook.com", "telenet.be"};

    private static final double[] FEE_AMOUNTS = {20.0, 25.0, 50.0};
    private static final double[] FEE_WEIGHTS = {0.45, 0.35, 0.20};

    private void seedCultureleVerenigingDeBrug() {
        Union deBrug = Union.builder()
                .name("Culturele Vereniging De Brug")
                .address("Grote Markt 5, 9100 Sint-Niklaas, Belgium")
                .vatCode("BE0741.234.567")
                .status(SubscriptionStatus.ACTIVE)
                .build();
        unionRepository.save(deBrug);

        MyUser demoAdmin = MyUser.builder()
                .username("demo_admin")
                .password(passwordEncoder.encode("demo123"))
                .role(MyUserRole.ADMIN)
                .union(deBrug)
                .build();
        userRepository.save(demoAdmin);

        int totalMembers = 172;
        int activeCount = (int) Math.round(totalMembers * 0.85);   // ~146
        int pendingCount = (int) Math.round(totalMembers * 0.10);  // ~17
        // remainder (~9) is INACTIVE/EXPIRED

        List<Member> members = new ArrayList<>(totalMembers);
        for (int i = 0; i < totalMembers; i++) {
            members.add(buildMember(i, deBrug, activeCount, pendingCount));
        }
        members = memberRepository.saveAll(members);

        List<Member> activeMembers = members.subList(0, activeCount);
        List<Member> pendingMembers = members.subList(activeCount, activeCount + pendingCount);

        seedMonthlyTransactions(deBrug, activeMembers, pendingMembers);
        seedEvents(deBrug, members);

        System.out.println("✅ Demo Seeding Completed: Culturele Vereniging De Brug (" + totalMembers
                + " leden) succesvol toegevoegd! Login: demo_admin / demo123");
    }

    private Member buildMember(int index, Union union, int activeCount, int pendingCount) {
        String[] entry = NAME_POOL[index % NAME_POOL.length];
        String firstName = entry[0];
        String lastName = entry[1];
        MemberLanguage language = MemberLanguage.valueOf(entry[2]);
        int cycle = index / NAME_POOL.length;

        String localPart = normalizeForEmail(firstName) + "." + normalizeForEmail(lastName)
                + (cycle > 0 ? String.valueOf(cycle + 1) : "");
        String email = localPart + "@" + EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];
        String phoneNumber = String.format("+32470%06d", index);

        String address = STREETS[random.nextInt(STREETS.length)] + " " + (1 + random.nextInt(150))
                + ", " + CITIES[random.nextInt(CITIES.length)];

        LocalDate dateOfBirth = LocalDate.now().minusYears(18 + random.nextInt(58)).minusDays(random.nextInt(365));

        LocalDate today = LocalDate.now();
        SubscriptionFrequency frequency = random.nextInt(10) < 8 ? SubscriptionFrequency.MONTHLY : SubscriptionFrequency.YEARLY;
        double subscriptionAmount = weightedFeeAmount();

        MemberStatus memberStatus;
        MemberSubscriptionStatus subscriptionStatus;
        LocalDate dateOfLastPayment;
        LocalDate nextBillingDate;

        if (index < activeCount) {
            // ~85% — paid up, in good standing
            memberStatus = MemberStatus.ACTIVE;
            subscriptionStatus = MemberSubscriptionStatus.ACTIVE;
            dateOfLastPayment = today.minusDays(random.nextInt(28));
            nextBillingDate = frequency.nextBillingDate(dateOfLastPayment);
        } else if (index < activeCount + pendingCount) {
            // ~10% — still an active member, but their next fee is due/overdue (payment pending)
            memberStatus = MemberStatus.ACTIVE;
            subscriptionStatus = MemberSubscriptionStatus.ACTIVE;
            dateOfLastPayment = today.minusMonths(1).minusDays(random.nextInt(10));
            nextBillingDate = today.minusDays(random.nextInt(6));
        } else {
            // ~5% — expired / no longer active
            memberStatus = MemberStatus.INACTIVE;
            subscriptionStatus = MemberSubscriptionStatus.PAUSED;
            dateOfLastPayment = today.minusMonths(3 + random.nextInt(6));
            nextBillingDate = null;
        }

        return Member.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .address(address)
                .dateOfBirth(dateOfBirth)
                .dateOfLastPayment(dateOfLastPayment)
                .memberStatus(memberStatus)
                .language(language)
                .subscriptionFrequency(frequency)
                .subscriptionAmount(BigDecimal.valueOf(subscriptionAmount))
                .nextBillingDate(nextBillingDate)
                .subscriptionStatus(subscriptionStatus)
                .union(union)
                .build();
    }

    /**
     * Spreads realistic membership-fee payments across days 1..today of the current month, landing
     * total revenue (SUCCEEDED + PAID_MANUALLY) between €3.000 and €4.000 so the dashboard revenue
     * chart shows an active, steady curve. Target is picked with enough headroom below €4.000 that
     * the greedy fill (min fee €20) can never overshoot €4.000 or undershoot €3.000.
     */
    private void seedMonthlyTransactions(Union union, List<Member> activeMembers, List<Member> pendingMembers) {
        List<Member> payingPool = new ArrayList<>(activeMembers);
        payingPool.addAll(pendingMembers);
        Collections.shuffle(payingPool, random);

        double target = 3050 + random.nextInt(901); // [3050, 3950]
        int today = LocalDate.now().getDayOfMonth();

        List<Transaction> transactions = new ArrayList<>();
        double running = 0;
        while (running + FEE_AMOUNTS[0] <= target) {
            double amount = weightedFeeAmount();
            if (running + amount > target) {
                continue;
            }
            running += amount;

            Member member = payingPool.get(random.nextInt(payingPool.size()));
            int day = 1 + random.nextInt(today);
            LocalDateTime createdAt = LocalDate.now().withDayOfMonth(day)
                    .atTime(9 + random.nextInt(11), random.nextInt(60));

            boolean cash = random.nextInt(100) < 12; // ~12% settled manually in cash
            TransactionStatus status = cash ? TransactionStatus.PAID_MANUALLY : TransactionStatus.SUCCEEDED;
            String note = cash ? "Cash payment (aan de balie)" : (random.nextBoolean() ? "Bancontact" : "iDEAL");
            String paymentId = cash ? null : "pi_" + Long.toHexString(random.nextLong() & Long.MAX_VALUE);
            TransactionType type = random.nextInt(10) < 9 ? TransactionType.MEMBERSHIP_FEE : TransactionType.DONATION;

            transactions.add(Transaction.builder()
                    .member(member)
                    .union(union)
                    .amount(amount)
                    .currency("EUR")
                    .status(status)
                    .type(type)
                    .note(note)
                    .paymentId(paymentId)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());
        }

        // A handful of still-outstanding charges for members in the "pending" segment — excluded
        // from revenue KPIs (PENDING is not in TransactionStatus.revenueStatuses()).
        int outstandingCount = Math.min(6, pendingMembers.size());
        for (int i = 0; i < outstandingCount; i++) {
            Member member = pendingMembers.get(i);
            int day = 1 + random.nextInt(today);
            LocalDateTime createdAt = LocalDate.now().withDayOfMonth(day).atTime(10 + random.nextInt(8), random.nextInt(60));
            transactions.add(Transaction.builder()
                    .member(member)
                    .union(union)
                    .amount(weightedFeeAmount())
                    .currency("EUR")
                    .status(TransactionStatus.PENDING)
                    .type(TransactionType.MEMBERSHIP_FEE)
                    .note("Betaallink verstuurd, nog niet betaald")
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build());
        }

        transactionRepository.saveAll(transactions);
    }

    private void seedEvents(Union union, List<Member> members) {
        List<Member> attendeePool = new ArrayList<>(members);
        Collections.shuffle(attendeePool, random);

        LocalDateTime ledenvergaderingStart = LocalDate.now().plusDays(14).atTime(19, 0);
        Event ledenvergadering = new Event();
        ledenvergadering.setTitle("Ledenvergadering & Ontmoetingsdag");
        ledenvergadering.setDescription("Jaarlijkse algemene ledenvergadering, gevolgd door een gezellige ontmoetingsdag voor alle leden.");
        ledenvergadering.setStart(ledenvergaderingStart);
        ledenvergadering.setEnd(ledenvergaderingStart.plusHours(3));
        ledenvergadering.setLocation("Ontmoetingscentrum De Brug, Sint-Niklaas");
        ledenvergadering.setEventType(EventType.MEETING);
        ledenvergadering.setUnion(union);
        ledenvergadering.setAttendees(new HashSet<>(attendeePool.subList(0, 38)));
        eventRepository.save(ledenvergadering);

        Collections.shuffle(attendeePool, random);
        LocalDateTime eetfestijnStart = LocalDate.now().plusDays(45).atTime(18, 0);
        Event eetfestijn = new Event();
        eetfestijn.setTitle("Jaarlijks Eetfestijn 2026");
        eetfestijn.setDescription("Ons traditionele eetfestijn met live muziek — steun onze vereniging en geniet van een heerlijke maaltijd.");
        eetfestijn.setStart(eetfestijnStart);
        eetfestijn.setEnd(eetfestijnStart.plusHours(4));
        eetfestijn.setLocation("Feestzaal Concordia, Sint-Niklaas");
        eetfestijn.setEventType(EventType.SOCIAL);
        eetfestijn.setUnion(union);
        eetfestijn.setAttendees(new HashSet<>(attendeePool.subList(0, 46)));
        eventRepository.save(eetfestijn);
    }

    private double weightedFeeAmount() {
        double r = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < FEE_AMOUNTS.length; i++) {
            cumulative += FEE_WEIGHTS[i];
            if (r <= cumulative) {
                return FEE_AMOUNTS[i];
            }
        }
        return FEE_AMOUNTS[FEE_AMOUNTS.length - 1];
    }

    /** Strips accents/diacritics (incl. Turkish ı/İ/ş/ğ/ç/ö/ü and spaces) for use in an email local part. */
    private String normalizeForEmail(String value) {
        String replaced = value
                .replace("ı", "i").replace("İ", "I")
                .replace("ş", "s").replace("Ş", "S")
                .replace("ğ", "g").replace("Ğ", "G")
                .replace("ç", "c").replace("Ç", "C")
                .replace("ö", "o").replace("Ö", "O")
                .replace("ü", "u").replace("Ü", "U");
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("[^a-zA-Z]", "").toLowerCase();
    }
}
