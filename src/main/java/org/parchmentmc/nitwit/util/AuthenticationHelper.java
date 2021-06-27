package org.parchmentmc.nitwit.util;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

public final class AuthenticationHelper {
    public static final long DEFAULT_TIME_TO_LIVE = 1000 * 60 * 60; // one hour

    private AuthenticationHelper() { // Prevent instantiation
    }

    static PrivateKey readPrivateKey(Path privateKey) throws Exception {
        byte[] keyBytes = Files.readAllBytes(privateKey);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static String createJWT(Path privateKey, String githubAppId) throws Exception {
        return createJWT(privateKey, githubAppId, DEFAULT_TIME_TO_LIVE);
    }

    public static String createJWT(Path privateKey, String githubAppId, long ttlMillis) throws Exception {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        Key signingKey = readPrivateKey(privateKey);

        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .setIssuer(githubAppId)
                .signWith(signingKey, signatureAlgorithm);

        if (ttlMillis > 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        return builder.compact();
    }
}
