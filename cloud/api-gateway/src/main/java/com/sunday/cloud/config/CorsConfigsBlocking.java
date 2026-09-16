//package com.zosh.cloud.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.filter.CorsFilter;
//
//import java.util.List;
//
//@Configuration
//public class CorsConfigsBlocking {
//
//    @Bean
//    public CorsFilter corsFilter() {
//        CorsConfiguration config = new CorsConfiguration();
//
//        config.setAllowCredentials(true);
//        config.setAllowedOrigins(List.of(
//                "https://zosh-air.vercel.app",
//                "https://zosh-fly.vercel.app",
//                "http://localhost:5173",
//                "http://localhost:3000"
//        ));
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("*");
//        config.setExposedHeaders(List.of("Authorization"));
//        config.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//
//        return new CorsFilter(source);  // ✅ Servlet-based, works with Gateway MVC
//    }
//}