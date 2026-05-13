package com.sqldpass.controller.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sqldpass.controller.admin.dto.LoginRequest;
import com.sqldpass.controller.admin.dto.LoginResponse;
import com.sqldpass.service.admin.JwtProvider;
import com.sqldpass.service.common.ErrorCode;
import com.sqldpass.service.common.SqldpassException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("login returns a JWT token for valid admin credentials")
    void login() {
        String hash = new BCryptPasswordEncoder().encode("secret123");
        AdminAuthController controller = new AdminAuthController(jwtProvider, "admin", hash);
        given(jwtProvider.createToken("admin")).willReturn("admin-jwt");

        LoginResponse response = controller.login(new LoginRequest("admin", "secret123"));

        assertThat(response.token()).isEqualTo("admin-jwt");
    }

    @Test
    @DisplayName("login throws when the password is invalid")
    void login_invalidPassword() {
        String hash = new BCryptPasswordEncoder().encode("secret123");
        AdminAuthController controller = new AdminAuthController(jwtProvider, "admin", hash);

        assertThatThrownBy(() -> controller.login(new LoginRequest("admin", "wrong-password")))
                .isInstanceOf(SqldpassException.class)
                .extracting(ex -> ((SqldpassException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_LOGIN_FAILED);
    }
}
