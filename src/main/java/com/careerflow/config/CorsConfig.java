package com.careerflow.config;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.*;
import java.util.*;
@Configuration
public class CorsConfig {
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:http://localhost:4200}") String origins){
  var config=new CorsConfiguration();
  config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).filter(s->!s.isEmpty()).toList());
  config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
  config.setAllowedHeaders(List.of("Authorization","Content-Type"));
  config.setExposedHeaders(List.of("Content-Disposition"));config.setAllowCredentials(false);config.setMaxAge(3600L);
  var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",config);return source;
 }
}
