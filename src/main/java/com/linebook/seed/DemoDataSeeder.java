package com.linebook.seed;

import com.linebook.billing.service.BillingService;
import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.domain.Customer;
import com.linebook.customer.domain.PackagePlan;
import com.linebook.customer.repository.CustomerRepository;
import com.linebook.customer.repository.PackagePlanRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Optional demo data (start with SPRING_PROFILES_ACTIVE=demo): the same 36 customers as the front-end demo seed,
 * plus this month's bills for the billable ones, loaded only when the customers table is empty. Phones/STB numbers use a seeded Java Random, so they are
 * stable between runs but not identical to the browser demo's numbers.
 * Demo customers have billingDay 10, matching the due date of the front end's demo bills.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final List<String> NAMES = List.of(
            "Lakshmi Devi", "Mohammed Irfan", "Srinivas Rao", "Padmavathi G", "Venkatesh Reddy", "Anjali Sharma",
            "Raju K", "Fatima Begum", "Narasimha Rao", "Sneha Patel", "Krishna Murthy", "Ayesha Siddiqui",
            "Ramesh Babu", "Divya Sri", "Prakash Goud", "Meena Kumari", "Suresh Kumar", "Bhavani D", "Imran Ali",
            "Kavitha Reddy", "Chandra Shekar", "Swathi N", "Yadagiri", "Pooja Jain", "Satyanarayana", "Harika Ch",
            "Naveen Kumar", "Shahina", "Murali Krishna", "Tejaswi Rao", "Gopal Reddy", "Sunitha", "Arjun Varma",
            "Rekha Rani", "Vamsi Krishna", "Noorjahan");

    private static final List<String> AREAS =
            List.of("Ameerpet", "Kukatpally", "Mehdipatnam", "LB Nagar", "Secunderabad", "Uppal");

    private final CustomerRepository customers;
    private final PackagePlanRepository packages;
    private final BillingService billing;
    private final Clock clock;

    public DemoDataSeeder(CustomerRepository customers, PackagePlanRepository packages, BillingService billing,
                          Clock clock) {
        this.customers = customers;
        this.packages = packages;
        this.billing = billing;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customers.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        List<PackagePlan> plans = packages.findAllByOrderBySortOrderAsc();
        Random rng = new Random(20260826L);

        for (int i = 0; i < NAMES.size(); i++) {
            String area = AREAS.get(i % AREAS.size());
            PackagePlan plan = plans.get(i % plans.size());
            int house = 10 + rng.nextInt(80);
            int lane = 1 + rng.nextInt(9);
            String phone = "9" + String.format("%09d", 800_000_000L + (long) (rng.nextDouble() * 90_000_000));
            String stb = String.format("%012d", 120_000_000_000L + i * 137L + (long) (rng.nextDouble() * 900));

            ConnectionStatus status = i == 33 ? ConnectionStatus.DISCONNECTED
                    : i == 27 ? ConnectionStatus.PENDING
                    : (i == 21 || i == 14) ? ConnectionStatus.SUSPENDED
                    : ConnectionStatus.ACTIVE;

            customers.save(new Customer(
                    String.format("cu-%02d", i + 1),
                    "SCN-" + (1001 + i),
                    NAMES.get(i),
                    phone,
                    house + "-" + lane + "-" + (10 + i) + ", Lane " + lane + ", " + area,
                    area,
                    stb,
                    "VC" + String.format("%08d", 440_000 + i * 17),
                    plan.getId(),
                    plan.getAmount(),
                    status,
                    i != 33 && i != 27,
                    10,
                    agentForArea(area),
                    today.minusMonths(8 + (i % 18)).plusDays(i)));
        }
        customers.flush();
        int bills = billing.generateCycle(null).created();
        log.info("Loaded {} demo customers and {} bills for the current cycle", NAMES.size(), bills);
    }

    private static String agentForArea(String area) {
        return switch (area) {
            case "Ameerpet" -> "ag-01";
            case "Kukatpally" -> "ag-02";
            case "Mehdipatnam" -> "ag-03";
            case "LB Nagar" -> "ag-04";
            default -> "ag-05";
        };
    }
}
