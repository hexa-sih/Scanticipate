package com.example.demo;

import java.util.List;

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
            if (userRepository.count() > 0) {
                return;
            }

            userRepository.saveAll(List.of(
                    new User("inspector1", "password", "inspector", 19.0760, 72.8777, "AVAILABLE"),
                    new User("inspector2", "password", "inspector", 19.0850, 72.8900, "AVAILABLE"),
                    new User("inspector_south1", "password", "inspector", 18.9220, 72.8310, "AVAILABLE"),
                    new User("inspector_south2", "password", "inspector", 18.9950, 72.8240, "AVAILABLE"),
                    new User("inspector_dadar", "password", "inspector", 19.0180, 72.8420, "AVAILABLE"),
                    new User("inspector_bandra", "password", "inspector", 19.0596, 72.8295, "AVAILABLE"),
                    new User("inspector_andheri1", "password", "inspector", 19.1170, 72.8360, "ON_TASK"),
                    new User("inspector_andheri2", "password", "inspector", 19.1412, 72.8310, "AVAILABLE"),
                    new User("inspector_malad", "password", "inspector", 19.1842, 72.8344, "AVAILABLE"),
                    new User("inspector_borivali", "password", "inspector", 19.2290, 72.8560, "AVAILABLE"),
                    new User("inspector_kurla", "password", "inspector", 19.0865, 72.8890, "AVAILABLE"),
                    new User("inspector_ghatkopar", "password", "inspector", 19.0996, 72.9162, "AVAILABLE"),
                    new User("inspector_mulund", "password", "inspector", 19.1760, 72.9520, "AVAILABLE"),
                    new User("inspector_thane1", "password", "inspector", 19.2090, 72.9730, "AVAILABLE"),
                    new User("inspector_thane2", "password", "inspector", 19.2450, 72.9720, "AVAILABLE"),
                    new User("inspector_vashi", "password", "inspector", 19.0750, 73.0090, "AVAILABLE"),
                    new User("inspector_kharghar", "password", "inspector", 19.0450, 73.0670, "AVAILABLE"),
                    new User("admin", "password", "admin", null, null, null)
            ));

            System.out.println("User database seeded successfully!");
        };
    }

    @Bean
    CommandLineRunner initStoreDatabase(StoreRepository storeRepository) {
        return args -> {
            if (storeRepository.count() > 0) {
                System.out.println("Store database already seeded. Skipping initialization.");
                return;
            }

            storeRepository.saveAll(List.of(
                    new Store("Phoenix Marketcity Mall, Kurla", "Shopping Mall / Hypermarket", 19.0865, 72.8890, 0.92),
                    new Store("Urban Platter Store, BKC", "Supermarket Hub", 19.0650, 72.8680, 0.88),
                    new Store("Reliance Fresh, Ghatkopar East", "Local Supermarket", 19.0810, 72.9080, 0.60),
                    new Store("Quality Kirana & Provisions, Bandra", "Local Retailer", 19.0550, 72.8350, 0.76),
                    new Store("Jio World Drive, BKC", "Shopping Mall / Hypermarket", 19.0628, 72.8653, 0.88),
                    new Store("Nature Basket, Bandra West", "Supermarket Hub", 19.0596, 72.8295, 0.72),
                    new Store("Link Square Mall, Bandra West", "Shopping Mall", 19.0610, 72.8335, 0.65),
                    new Store("Suburbia Supermarket, Bandra", "Local Supermarket", 19.0565, 72.8389, 0.48),
                    new Store("Khar Food Hall, Khar West", "Supermarket Hub", 19.0700, 72.8340, 0.58),
                    new Store("Santacruz Central Market", "Wholesale Market", 19.0830, 72.8410, 0.85),
                    new Store("Hi-Life Mall, Santacruz West", "Shopping Mall", 19.0815, 72.8390, 0.62),
                    new Store("D-Mart, Vile Parle East", "Hypermarket", 19.0965, 72.8520, 1.00),
                    new Store("Alfa Market No 1, Vile Parle West", "Retail Hub", 19.1025, 72.8375, 0.89),
                    new Store("Alfa Market No 3, Irla", "Retail Hub", 19.1032, 72.8380, 0.94),
                    new Store("Shoppers Stop, Andheri West", "Shopping Mall", 19.1170, 72.8360, 0.70),
                    new Store("Infinity Mall, Andheri West", "Shopping Mall / Hypermarket", 19.1412, 72.8310, 0.90),
                    new Store("Chitrakoot Ground Commercial Market", "Wholesale Market", 19.1350, 72.8330, 0.82),
                    new Store("Star Bazaar, Andheri West", "Hypermarket", 19.1310, 72.8365, 0.87),
                    new Store("Lallubhai Park Kirana Hub", "Local Retailer", 19.1190, 72.8420, 0.72),
                    new Store("DN Nagar Provisions, Andheri", "Local Retailer", 19.1250, 72.8315, 0.38),
                    new Store("Seven Bungalows Food Mart", "Local Supermarket", 19.1290, 72.8190, 0.52),
                    new Store("Versova Supermarket, Yari Road", "Local Supermarket", 19.1380, 72.8130, 0.45),
                    new Store("Hub Mall, Goregaon East", "Shopping Mall", 19.1560, 72.8560, 0.93),
                    new Store("City Centre Mall, Goregaon West", "Shopping Mall", 19.1610, 72.8420, 0.68),
                    new Store("Inorbit Mall, Malad West", "Shopping Mall / Hypermarket", 19.1730, 72.8360, 0.95),
                    new Store("Infiniti Mall, Malad West", "Shopping Mall / Hypermarket", 19.1842, 72.8344, 0.93),
                    new Store("Natraj Market, Malad West", "Wholesale Market", 19.1860, 72.8480, 0.88),
                    new Store("D-Mart, Malad West", "Hypermarket", 19.1920, 72.8310, 0.89),
                    new Store("Growel 101 Mall, Kandivali East", "Shopping Mall", 19.2050, 72.8680, 0.99),
                    new Store("Thakur Mall, Kandivali East", "Shopping Mall", 19.2120, 72.8630, 0.60),
                    new Store("Charkop Supermarket, Kandivali West", "Local Supermarket", 19.2150, 72.8320, 0.50),
                    new Store("Borivali Wholesale Market", "Wholesale Market", 19.2300, 72.8570, 0.91),
                    new Store("Indraprastha Shopping Centre, Borivali", "Shopping Mall", 19.2290, 72.8560, 0.74),
                    new Store("D-Mart, Borivali West", "Hypermarket", 19.2380, 72.8420, 0.92),
                    new Store("Shimpoli Provisions, Borivali West", "Local Retailer", 19.2320, 72.8380, 0.35),
                    new Store("Dahisar Supermarket, Dahisar East", "Local Supermarket", 19.2550, 72.8680, 0.46),
                    new Store("Sion Circle Wholesale Provisions", "Wholesale Market", 19.0380, 72.8610, 0.79),
                    new Store("Kamgar Nagar Supermarket, Kurla East", "Local Supermarket", 19.0680, 72.8840, 0.53),
                    new Store("Phoenix Marketcity Mall, Kurla West", "Shopping Mall / Hypermarket", 19.0865, 72.8890, 0.92),
                    new Store("Kohinoor City Mall, Kurla", "Shopping Mall", 19.0750, 72.8820, 0.81),
                    new Store("Vidyavihar Station Provisions Hub", "Local Retailer", 19.0800, 72.8970, 0.40),
                    new Store("R-City Mall, Ghatkopar West", "Shopping Mall / Hypermarket", 19.0996, 72.9162, 0.96),
                    new Store("Ghatkopar Wholesale Grocery Market", "Wholesale Market", 19.0860, 72.9080, 0.86),
                    new Store("Garodia Nagar Supermarket", "Local Supermarket", 19.0810, 72.9120, 0.44),
                    new Store("Vikhroli Parksite Provisions", "Local Retailer", 19.1100, 72.9230, 0.39),
                    new Store("Kannamwar Nagar Food Hub, Vikhroli", "Local Retailer", 19.1020, 72.9380, 0.41),
                    new Store("D-Mart, Kanjurmarg East", "Hypermarket", 19.1312, 72.9356, 0.88),
                    new Store("Bhandup Station Market Hub", "Wholesale Market", 19.1450, 72.9380, 0.82),
                    new Store("Dreams Mall, Bhandup West", "Shopping Mall", 19.1480, 72.9340, 0.55),
                    new Store("R Mall, Mulund West", "Shopping Mall", 19.1760, 72.9520, 0.81),
                    new Store("Mulund Station Wholesale Market", "Wholesale Market", 19.1720, 72.9560, 0.87),
                    new Store("Zaver Road Supermarket, Mulund West", "Local Supermarket", 19.1740, 72.9480, 0.49),
                    new Store("Colaba Causeway Retail Market", "Retail Hub", 18.9150, 72.8250, 0.84),
                    new Store("Regal Circle Supermarket, Colaba", "Local Supermarket", 18.9220, 72.8310, 0.51),
                    new Store("Fort Wholesale Packaging House", "Wholesale Market", 18.9340, 72.8350, 0.89),
                    new Store("Flora Fountain Food Mart", "Local Supermarket", 18.9320, 72.8320, 0.56),
                    new Store("Crawford Wholesale Market", "Wholesale Market", 18.9472, 72.8347, 0.98),
                    new Store("Manish Market, Crawford", "Retail Hub", 18.9460, 72.8355, 0.91),
                    new Store("Kalbadevi Wholesale Trading Hub", "Wholesale Market", 18.9510, 72.8290, 0.93),
                    new Store("Charni Road Station Provisions", "Local Retailer", 18.9520, 72.8180, 0.37),
                    new Store("Girgaon Chowpatty Supermarket", "Local Supermarket", 18.9560, 72.8140, 0.48),
                    new Store("Atria Mall, Worli", "Shopping Mall", 18.9890, 72.8150, 0.73),
                    new Store("Worli Seaface Kirana Hub", "Local Retailer", 19.0010, 72.8170, 0.36),
                    new Store("Phoenix Palladium Mall, Lower Parel", "Shopping Mall / Hypermarket", 18.9950, 72.8240, 0.97),
                    new Store("High Street Phoenix, Lower Parel", "Shopping Mall", 18.9960, 72.8250, 0.94),
                    new Store("Lower Parel Supermarket Hub", "Supermarket Hub", 18.9980, 72.8300, 0.77),
                    new Store("Dadar Ranade Road Wholesale Market", "Wholesale Market", 19.0190, 72.8430, 0.92),
                    new Store("Dadar Vegetable & Grain Market", "Wholesale Market", 19.0180, 72.8420, 0.95),
                    new Store("Star Bazaar, Dadar West", "Hypermarket", 19.0220, 72.8390, 0.85),
                    new Store("Prabhadevi Local Provisions", "Local Retailer", 19.0150, 72.8280, 0.40),
                    new Store("Mahim West Grocery Mart", "Local Supermarket", 19.0350, 72.8400, 0.47),
                    new Store("Korum Mall, Thane West", "Shopping Mall / Hypermarket", 19.2010, 72.9660, 0.89),
                    new Store("Viviana Mall, Thane West", "Shopping Mall / Hypermarket", 19.2090, 72.9730, 0.96),
                    new Store("R-Mall, Thane West", "Shopping Mall", 19.2320, 72.9780, 0.80),
                    new Store("Hypercity, Ghodbunder Road", "Hypermarket", 19.2450, 72.9720, 0.87),
                    new Store("D-Mart, Kasarvadavali Thane", "Hypermarket", 19.2680, 72.9650, 0.90),
                    new Store("Naupada Station Wholesale Hub", "Wholesale Market", 19.1880, 72.9720, 0.83),
                    new Store("Gokhale Road Supermarket, Thane", "Local Supermarket", 19.1920, 72.9700, 0.52),
                    new Store("Majiwada Provisions Store", "Local Retailer", 19.2150, 72.9810, 0.42),
                    new Store("Wagle Estate Commercial Mart", "Supermarket Hub", 19.1980, 72.9520, 0.67),
                    new Store("Kalwa Station Market Hub", "Local Supermarket", 19.1910, 72.9960, 0.59),
                    new Store("Inorbit Mall, Vashi", "Shopping Mall / Hypermarket", 19.0640, 73.0010, 0.91),
                    new Store("Center One Mall, Vashi", "Shopping Mall", 19.0630, 73.0020, 0.72),
                    new Store("APMC Wholesale Grain Market, Vashi", "Wholesale Market", 19.0750, 73.0090, 0.99),
                    new Store("APMC Fruit & Vegetable Market, Vashi", "Wholesale Market", 19.0780, 73.0110, 0.98),
                    new Store("Vashi Sector 17 Shopping Center", "Retail Hub", 19.0710, 73.0000, 0.78),
                    new Store("Sanpada Station Supermarket", "Local Supermarket", 19.0660, 73.0180, 0.49),
                    new Store("D-Mart, Koparkhairane", "Hypermarket", 19.0920, 73.0080, 0.88),
                    new Store("Ghansoili Wholesale Mart", "Local Supermarket", 19.1210, 73.0030, 0.55),
                    new Store("Airoli Sector 5 Retail Hub", "Local Retailer", 19.1550, 72.9950, 0.43),
                    new Store("D-Mart, Airoli", "Hypermarket", 19.1580, 72.9980, 0.86),
                    new Store("Nerul LP Wholesale Grocery", "Wholesale Market", 19.0320, 73.0170, 0.79),
                    new Store("Seawoods Grand Central Mall", "Shopping Mall / Hypermarket", 19.0210, 73.0180, 0.94),
                    new Store("D-Mart, Nerul Sector 15", "Hypermarket", 19.0380, 73.0220, 0.87),
                    new Store("Belapur CBD Sector 11 Mart", "Local Supermarket", 19.0180, 73.0410, 0.51),
                    new Store("Kharghar Little World Mall", "Shopping Mall", 19.0450, 73.0670, 0.76),
                    new Store("D-Mart, Kharghar Sector 15", "Hypermarket", 19.0520, 73.0680, 0.89),
                    new Store("Kharghar Sector 20 Kirana", "Local Retailer", 19.0600, 73.0750, 0.38),
                    new Store("Kamothe Sector 7 Grocery", "Local Retailer", 19.0300, 73.0910, 0.45),
                    new Store("Panvel Station Wholesale Market", "Wholesale Market", 18.9890, 73.1180, 0.88),
                    new Store("K-Mall, Panvel", "Shopping Mall", 18.9920, 73.1150, 0.69),
                    new Store("D-Mart, New Panvel", "Hypermarket", 18.9980, 73.1280, 0.87),
                    new Store("Chhatrapati Shivaji Terminal 2 Duty Free Hub", "Supermarket Hub", 19.0880, 72.8680, 0.85),
                    new Store("Sahar Cargo Packaging Hub", "Wholesale Market", 19.0950, 72.8620, 0.91),
                    new Store("Domestic Airport Terminal 1 Retail Mart", "Retail Hub", 19.0920, 72.8530, 1.00),
                    new Store("Kurla West LBS Marg Wholesale Hub", "Wholesale Market", 19.0880, 72.8820, 0.92),
                    new Store("Ghatkopar LBS Marg Supermarket Cluster", "Supermarket Hub", 19.0980, 72.9110, 0.79)
            ));

            System.out.println("Store database seeded successfully!");
        };
    }
}
