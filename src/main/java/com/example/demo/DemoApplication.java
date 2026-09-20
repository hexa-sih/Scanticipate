package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername("inspector1").isEmpty()) {
                userRepository.save(new User("inspector1", "password", "inspector", 19.0760, 72.8777, "AVAILABLE"));
                userRepository.save(new User("inspector2", "password", "inspector", 19.0850, 72.8900, "AVAILABLE"));
                userRepository.save(new User("admin", "password", "admin", null, null, null));
                System.out.println("Mock data seeded successfully!");
            }
        };
    }

    @Bean
    CommandLineRunner initStoreDatabase(StoreRepository storeRepository, UserRepository userRepository) {
        return args -> {
            if (storeRepository.count() == 0) {
                storeRepository.save(new Store("Phoenix Marketcity Mall, Kurla", "Shopping Mall / Hypermarket", 19.0865, 72.8890, 0.92));
                storeRepository.save(new Store("Urban Platter Store, BKC", "Supermarket Hub", 19.0650, 72.8680, 0.88));
                storeRepository.save(new Store("Reliance Fresh, Ghatkopar East", "Local Supermarket", 19.0810, 72.9080, 0.45));
                storeRepository.save(new Store("Quality Kirana & Provisions, Bandra", "Local Retailer", 19.0550, 72.8350, 0.76));
            }
        };
    }
}
