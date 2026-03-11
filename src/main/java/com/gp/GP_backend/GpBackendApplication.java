package com.gp.GP_backend;

// import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling 
public class GpBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GpBackendApplication.class, args);
	}

	// @Bean
    // public ModelMapper modelMapper() {
    //     return new ModelMapper();
    // }
}

// package com.gp.GP_backend;

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
// import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
// import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;

// // This tells Spring Boot to completely ignore database setup on startup
// @SpringBootApplication(exclude = {
// 		DataSourceAutoConfiguration.class,
// 		DataSourceTransactionManagerAutoConfiguration.class,
// 		HibernateJpaAutoConfiguration.class
// })
// public class GpBackendApplication {

// 	public static void main(String[] args) {
// 		SpringApplication.run(GpBackendApplication.class, args);
// 	}

// }