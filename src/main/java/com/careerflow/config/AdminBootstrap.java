package com.careerflow.config;
import com.careerflow.entity.User;
import com.careerflow.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
@Component @ConditionalOnProperty(name="app.bootstrap.enabled",havingValue="true")
public class AdminBootstrap implements ApplicationRunner {
 private final UserRepository users;private final RoleRepository roles;private final PasswordEncoder passwords;
 private final String email;private final String password;
 public AdminBootstrap(UserRepository users,RoleRepository roles,PasswordEncoder passwords,
  @Value("${app.bootstrap.email:}") String email,@Value("${app.bootstrap.password:}") String password){this.users=users;this.roles=roles;this.passwords=passwords;this.email=email.strip().toLowerCase(java.util.Locale.ROOT);this.password=password;}
 @Override @Transactional public void run(ApplicationArguments arguments){
  if(!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || email.length()>255 || password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
   throw new IllegalStateException("Admin bootstrap needs a valid email and a 12-to-72-byte password");
  var existing=users.findByEmail(email);
  if(existing.isPresent()){
   if(!"ADMIN".equals(existing.get().getRole().getName()))throw new IllegalStateException("Bootstrap email belongs to a non-admin account");
   return;
  }
  var user=new User();user.setEmail(email);user.setPassword(passwords.encode(password));user.setRole(roles.findByName("ADMIN").orElseThrow());users.save(user);
 }
}
