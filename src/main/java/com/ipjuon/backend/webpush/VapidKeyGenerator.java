package com.ipjuon.backend.webpush;

import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;

/**
 * VAPID 키 페어 생성 utility — 한 번만 실행하여 결과를 환경변수에 저장.
 *
 * 실행:
 *   ./gradlew run -PmainClass=com.ipjuon.backend.webpush.VapidKeyGenerator
 * 또는 IDE에서 직접 main 실행
 *
 * 출력된 두 값을 환경변수로 설정:
 *   WEBPUSH_PUBLIC_KEY=...
 *   WEBPUSH_PRIVATE_KEY=...
 */
public class VapidKeyGenerator {
    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        ECParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256r1");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH", "BC");
        kpg.initialize(params);
        KeyPair pair = kpg.generateKeyPair();

        ECPublicKey pub = (ECPublicKey) pair.getPublic();
        ECPrivateKey priv = (ECPrivateKey) pair.getPrivate();

        String publicKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Utils.encode(pub));
        String privateKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Utils.encode(priv));

        System.out.println("===== VAPID Keys generated =====");
        System.out.println("WEBPUSH_PUBLIC_KEY=" + publicKey);
        System.out.println("WEBPUSH_PRIVATE_KEY=" + privateKey);
        System.out.println("================================");
        System.out.println();
        System.out.println("application.yml 또는 환경변수에 위 두 값을 설정하세요.");
        System.out.println("FE: VITE_VAPID_PUBLIC_KEY 에 동일한 PUBLIC_KEY 사용");
    }
}
