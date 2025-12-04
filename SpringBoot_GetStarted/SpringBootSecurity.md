# Spring Security In-Depth Guide

Spring Boot integrates [Spring Security](https://spring.io/projects/spring-security), a powerful and customizable authentication and access-control framework. It provides robust security for web applications, REST APIs, and microservices.

---

## Core Concepts

### Authentication vs Authorization
Authentication = Identifies the user ("Who are you?").
Authorization = Decides what resources the user can access or actions they can perform ("What are you allowed to do?").

**Example Table:**
| Operation         | Type             |
|-------------------|------------------|
| Login form checks | Authentication   |
| Access /admin     | Authorization    |

---

### Principal, Authentication, SecurityContext
- **Principal:** the currently logged-in user, represented by `UserDetails` (for custom users) or another object.
- **Authentication:** contains principal, credentials, and authorities. Represents a successful authentication.
- **SecurityContext:** holds the `Authentication` object. Managed by `SecurityContextHolder` (thread local by default).

**Get the username of current user:**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

---

### The Filter Chain
Spring Security's heart is the filter chain—a set of servlet filters that intercept every HTTP request.

- Filters handle: Authentication, Authorization, CSRF, CORS, Session Mgmt, JWT, Remember-Me, etc.
- Filters execute in order (e.g. `UsernamePasswordAuthenticationFilter`, `JwtAuthenticationFilter`)

**Example (custom filter):**
```java
public class JwtFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Extract and validate JWT token from Authorization header
        filterChain.doFilter(request, response);
    }
}
```

---

## Getting Started: Secure Endpoints
Spring Security auto-configures and secures all endpoints by default.

### Minimal config: HTTP Basic authentication
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
          .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
          .httpBasic();
        return http.build();
    }
}
```
- All endpoints require authentication
- Uses browser popup for username/password

### Add In-Memory Users
```java
@Bean
public UserDetailsService users() {
    UserDetails user = User.withDefaultPasswordEncoder()
            .username("user")
            .password("password")
            .roles("USER")
            .build();
    return new InMemoryUserDetailsManager(user);
}
```
- Good for demos. For production, use encoded passwords and connect to a database (see below).

**Next: Customizing security for real apps (roles, database users, JWT, etc.)**

## Customizing Security

### HttpSecurity and SecurityFilterChain
- Central place to define how your app is secured.
- Use `authorizeHttpRequests()` to set up role-based access rules.
- Apply additional settings (form login, logout, CSRF, etc.)

**Restricting endpoints by role:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
        .anyRequest().authenticated()
      )
      .formLogin(Customizer.withDefaults()) // Standard login page
      .logout(Customizer.withDefaults())    // Standard logout
      .csrf(Customizer.withDefaults());     // CSRF enabled (MVC)
    return http.build();
  }
}
```

### Custom UserDetailsService (DB-backed users)
- Implement your own `UserDetailsService` to load users from a database.

**Sample User, Repo, Service:**
```java
@Entity
public class AppUser {
  @Id
  @GeneratedValue
  private Long id;
  private String username;
  private String password;
  private String role;
  // getters/setters
}

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByUsername(String username);
}

@Service
public class CustomUserDetailsService implements UserDetailsService {
  @Autowired
  private AppUserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username)
      throws UsernameNotFoundException {
    AppUser user = userRepository.findByUsername(username)
      .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return User.withUsername(user.getUsername())
               .password(user.getPassword())
               .roles(user.getRole())
               .build();
  }
}
```

---

### Password Encoding and Storage
- **Never** store passwords in plaintext.
- Use `PasswordEncoder` (BCrypt is recommended)

**Config and encoding example:**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

String encoded = passwordEncoder.encode("mypassword");
```

- Store only encoded passwords in your DB! Plain `{noop}` is for testing only.

---

### Stateless vs Stateful (JWT, Sessions)
- **Stateful** (default): session stored on server; Spring MVC and most web apps by default.
- **Stateless:** for REST APIs using JWT tokens. No server-side session; JWT is validated on each request.

**Session-based:**
- Works with browser cookies; user stays logged in as long as session is alive on server.
- Suited for web apps.

**JWT Stateless:**
- Suited for REST APIs, SPAs, mobile apps.
- Example login & filter:

```java
@RestController
public class AuthController {
  @PostMapping("/login")
  public String login(@RequestBody AuthRequest req) {
    // Validate and authenticate
    return jwtService.generateToken(req.getUsername());
  }
}

public class JwtFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String token = req.getHeader("Authorization");
    // Validate and set authentication
    chain.doFilter(req, res);
  }
}
```

**You must register JWT filters in the `SecurityFilterChain` and disable sessions**

```java
http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

---

### CSRF Protection
- **CSRF (Cross-Site Request Forgery):** Attacker tricks user’s browser into making unwanted authenticated request
- Enabled by default for web apps. For pure REST APIs (no browser clients), you usually **disable** CSRF.

```java
http.csrf(csrf -> csrf.disable()); // For stateless APIs only!
```

---

### CORS Configuration
- **CORS (Cross-Origin Resource Sharing):** controls which domains can call your API from browsers

**Basic CORS setup:**
```java
@Bean
public WebMvcConfigurer corsConfigurer() {
  return new WebMvcConfigurer() {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
      registry.addMapping("/api/**")
        .allowedOrigins("https://your-frontend.com")
        .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
  };
}
```

---

### REST API Security
Checklist for REST API security with Spring:
- Use stateless sessions (`SessionCreationPolicy.STATELESS`)
- Use JWT tokens, not sessions/cookies
- Disable CSRF
- Tighten CORS
- Validate input
- Return proper HTTP error codes

---

### Role-Based Access Control (RBAC)
- Restrict endpoints based on roles/authorities (see above config)
- Roles are prefixed with `ROLE_` ("ADMIN" → "ROLE_ADMIN")

**Method-level annotation:**
```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/dashboard")
public String adminDashboard() {
  return "Admin area";
}
```

---

### Method-Level Security
- Secure methods directly using annotations
- Enable by adding `@EnableMethodSecurity`

```java
@EnableMethodSecurity
@Configuration
public class MethodSecurityConfig {}
```

**Common annotations:**
- `@PreAuthorize("expression")`: runs before method
- `@PostAuthorize("expression")`: after
- `@Secured({"ROLE_ADMIN"})`: basic role check

---

### Exception Handling
Handle authentication/authorization errors gracefully.

**Custom Access Denied Handler:**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  http
    .exceptionHandling(ex -> ex
       .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
       .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
    );
  // ... other config ...
  return http.build();
}
```

---

### Custom Security Filters
Add custom authentication/authorization logic using filters.

```java
@Component
public class CustomLoggingFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    // Custom logic
    chain.doFilter(req, res);
  }
}
```
Register in your config:
```java
.addFilterBefore(customLoggingFilter, UsernamePasswordAuthenticationFilter.class)
```

---

### Remember-Me
Useful for "keep me logged in" browser functionality.

```java
http.rememberMe(rm -> rm
    .key("unique-and-secret")
    .tokenValiditySeconds(7 * 24 * 60 * 60) // 1 week
);
```

---

### Logout
Spring Security supports secure logout by default.

```java
http.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
);
```

---

## Advanced Topics

### OAuth2/OpenID Connect Login
- Enable single sign-on (Google, Facebook, GitHub, etc.)
- Spring Boot makes this easy with `spring-boot-starter-oauth2-client` dependency

```java
http.oauth2Login(Customizer.withDefaults());
```
- Configuration is via `application.properties`/`application.yml`

---

### Custom Authentication Providers
You can plug your own authentication logic for special cases.

```java
@Component
public class CustomAuthProvider implements AuthenticationProvider {
  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    // verify credentials, return new UsernamePasswordAuthenticationToken(...)
  }
  @Override
  public boolean supports(Class<?> auth) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(auth);
  }
}
```
Register in your configuration:
```java
http.authenticationProvider(customAuthProvider);
```

---

### SecurityContextHolder Strategies
By default, security context uses ThreadLocal storage (`MODE_THREADLOCAL`). For async and reactive apps, you may need to use `MODE_INHERITABLETHREADLOCAL` or pass context explicitly.

```java
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
```
---

## Best Practices and Pitfalls
- Always encode/hay passwords (`BCryptPasswordEncoder`)
- Never use `{noop}` outside of tests
- Prefer role/authority checks at method or endpoint level
- Never return detailed auth failures in production
- Limit session duration and use stateless for APIs
- Secure actuator endpoints separately
- Sanitize user data and validate all input
- Test with different roles/users

**Pitfall:** Accidentally exposing `/h2-console` or actuator in production!  
**Pitfall:** Forgetting to disable CSRF for REST APIs (causing POSTs to break)
**Pitfall:** Leaving CORS open (use only for trusted origins)

---

## References
- [Official Spring Security Docs](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Guides](https://spring.io/guides)
- [Baeldung Spring Security](https://www.baeldung.com/security-spring)
- [JWT Spring Tutorial](https://www.javainuse.com/spring/boot-jwt)

---
**This guide is designed to provide both practical code and conceptual depth. Adapt and extend these examples as your apps grow!**

## Form Login with Custom Login Page

By default, Spring Security provides a generic login form. For a custom user experience, you can plug your own login page easily. This is essential for most web apps needing a branded or multi-field login interface.

**Example:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
      .formLogin(form -> form
         .loginPage("/login") // custom login page at /login
         .permitAll()
      );
    return http.build();
  }
}
```
- Your controller should map `/login` (GET for page, POST for submit).

**When to use?**
- Any web app with a custom look or additional login fields.

---

## Multiple Roles and Authorities Per User

A single user can have multiple roles/authorities (e.g., both USER and ADMIN). Implement this in your custom UserDetails.

```java
public class CustomUserDetails implements UserDetails {
    private String username;
    private String password;
    private List<GrantedAuthority> authorities;
    // other fields ...

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    // ... other UserDetails methods ...
}
```
**Tip:** Use roles for broad access, authorities for fine-grained permissions.

---

## JWT: Advanced Concepts

JSON Web Tokens should be signed and carry claims (like username, roles, expiration).

**JWT Generation (with claims & expiration):**
```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public String generateToken(String username) {
   String secret = "YOUR_SECRET_KEY";
   long expirationMs = 86400000; // 1 day
   return Jwts.builder()
      .setSubject(username)
      .claim("role", "ADMIN")
      .setIssuedAt(new Date())
      .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
      .signWith(SignatureAlgorithm.HS512, secret)
      .compact();
}
```
**JWT Validation:**
```java
public Claims extractClaims(String token) {
   String secret = "YOUR_SECRET_KEY";
   return Jwts.parser()
      .setSigningKey(secret)
      .parseClaimsJws(token)
      .getBody();
}
```
**Pro-tip:**
- Always validate signature, check expiration, and check claims!
- Never expose your secret and always use strong algorithm (HS512 or RS256 with keypair)

---

## Account Locking & User State

Spring Security can protect accounts by status (locked, enabled, credentials expired, etc.), which is crucial for security.

**UserDetails implementation:**
```java
public class CustomUserDetails implements UserDetails {
    // ...
    private boolean accountNonLocked;
    private boolean enabled;
    private boolean credentialsNonExpired;
    private boolean accountNonExpired;

    @Override
    public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override
    public boolean isEnabled() { return enabled; }
    @Override
    public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
    @Override
    public boolean isAccountNonExpired() { return accountNonExpired; }
}
```
**Tip:**
- Lock on repeated failed authentications (see event listeners below)

---

## Security Events & Auditing

Spring Security can publish authentication events, which are useful to lock accounts after failed attempts or audit logins.

**Sample event listener:**
```java
@Component
public class AuthenticationEventListener implements ApplicationListener<AbstractAuthenticationEvent> {
    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (event instanceof AuthenticationFailureBadCredentialsEvent failure) {
            // Increment failure count, possibly lock user
        } else if (event instanceof AuthenticationSuccessEvent success) {
            // Log successful login
        }
    }
}
```
**When to use?**
- Implement brute force protection, audit trails, notification flows, etc.

---

## Security Testing Tips

It’s crucial to test endpoint protection and authentication logic.

**Example using MockMvc:**
```java
@Autowired
private MockMvc mockMvc;

@Test
@WithMockUser(username="admin",roles={"ADMIN"})
public void adminPage_Allowed() throws Exception {
    mockMvc.perform(get("/admin/dashboard"))
           .andExpect(status().isOk());
}

@Test
@WithMockUser(username="user",roles={"USER"})
public void adminPage_Forbidden() throws Exception {
    mockMvc.perform(get("/admin/dashboard"))
           .andExpect(status().isForbidden());
}
```
**Best Practice:**
- Cover all role combinations; test anonymous users; never rely on UI-only logic for security.

---

## Multi-Factor Authentication (MFA/2FA)

Spring Security supports two-factor and multi-factor authentication (2FA/MFA) for additional security. Typical flows use a TOTP app, email/SMS OTP, or hardware token.

- Use libraries like [Spring Security extensions](https://github.com/spring-projects/spring-security) or integrate with providers (Twilio, Authy, Google Authenticator).
- MFA is often implemented as an authentication filter after username/password, before issuing final authentication.

**When to use?**
- Required for sensitive data, finance, admin panels.

---

## Session Fixation Protection and Secure Session Settings

Session fixation is an attack where attacker sets or steals a user's session ID. Spring protects you by default, but you can adjust settings.

```java
http.sessionManagement(sm -> sm
    .sessionFixation().migrateSession()    // Regenerates session ID after login
    .maximumSessions(1)                    // Only 1 login per user
    .expiredUrl("/session-expired")       // Redirect if expired
);
```
**Tip:** Always use `migrateSession` and limit concurrent sessions for admins.

---

## Security for Static Resources

You can allow static content (JS, CSS, images) to be served without authentication using antMatchers or requestMatchers.

```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
    // ... other rules ...
);
```
**Pitfall:** Forgetting this can break your login forms or site layout!

---

## SAML & LDAP Authentication (Mention)

Spring Security supports SAML (enterprise SSO, e.g., Okta/ADFS) and LDAP (directory authentication, e.g., corporate AD).
- Use specialized starters: `spring-security-saml2-service-provider`, `spring-boot-starter-data-ldap`.
- Not covered in detail here. See the official guides.

---

## OAuth2 Advanced: Scopes, Refresh Tokens, Client Registration

When building OAuth2 clients/servers, you’ll deal with scope checks, refresh tokens, and client registrations.

**Config for protected resource with scopes:**
```java
@PreAuthorize("hasAuthority('SCOPE_read_data')")
@GetMapping("/data")
public String getData() { return "secret"; }
```
- Scopes start with `SCOPE_` in authorities.

**Configure authorized clients:**
Set up `application.yml`:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: YOUR_ID
            client-secret: YOUR_SECRET
            scope: "read:user"
```

**Refresh tokens and resource servers:**
- Use `spring-boot-starter-oauth2-resource-server` for JWT validation on APIs.
- Maintain refresh tokens securely for long-lived client sessions.

---

## Example: Plug-and-Play Security Module Structure

```
security/
├── config/                # SecurityConfig, custom filter/handlers
├── controller/            # Authentication, login/logout, 2FA endpoints
├── jwt/                   # JWT provider, filter, util classes
├── model/                 # User, Role, Permission entities
├── repository/            # User/Role/OTP repository
├── service/               # UserDetailsService, auth, role, OTP services
├── util/                  # Security utils, encoders, etc.
```
**Tip:** Keep security code isolated for easier upgrades and testing!

---
