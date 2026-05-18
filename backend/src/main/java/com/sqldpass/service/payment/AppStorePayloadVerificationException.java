package com.sqldpass.service.payment;

/**
 * Apple App Store Server Notifications V2 JWS 검증 실패.
 *
 * <p>{@link AppStorePayloadVerifier} 가 서명/체인/bundleId/환경 검증 실패 또는
 * 디코딩 중 예외를 만난 경우 throw 한다. webhook 컨트롤러는 이 예외를 catch 하여 HTTP 401 로 응답.
 */
public class AppStorePayloadVerificationException extends RuntimeException {

    public AppStorePayloadVerificationException(String message) {
        super(message);
    }

    public AppStorePayloadVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
