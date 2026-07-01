package org.example.civitaswebapp.util;

import org.example.civitaswebapp.domain.*;
import org.example.civitaswebapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class InitDataConfig implements CommandLineRunner {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MyUserRepository userRepository;

    @Autowired
    private UnionRepository unionRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 🛡️ VEILIGHEIDSCHECK: Voorkomt 'Duplicate Entry' crashes bij herstarten
        if (unionRepository.count() > 0) {
            System.out.println("✅ Database bevat al data. InitDataConfig wordt overgeslagen.");
            return;
        }

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
}