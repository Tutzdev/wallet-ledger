package dev.tutz.ledger.auth;

import dev.tutz.ledger.common.InvalidCredentialsException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final OperatorRepository operators;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(OperatorRepository operators, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
            SecurityProperties properties, Clock clock) {
        this.operators = operators;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("timing-equalizer");
    }

    @Transactional(readOnly = true)
    public TokenResponse issueToken(TokenRequest request) {
        Operator operator = operators.findByEmailIgnoreCase(request.email().strip()).orElse(null);
        // BCrypt runs even for unknown e-mails, so response time does not reveal which accounts exist.
        String hash = operator != null ? operator.getPasswordHash() : dummyPasswordHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (operator == null || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return tokenFor(operator);
    }

    private TokenResponse tokenFor(Operator operator) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConfiguration.ISSUER)
                .subject(operator.getId().toString())
                .claim("email", operator.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(properties.tokenTtl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", properties.tokenTtl().toSeconds());
    }
}
