package com.testforge.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.testforge.user.domain.UserEntity;
import com.testforge.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class DemoInfoControllerTest {
  /**
   * Reveals only verified public credentials; disabled, missing, or changed accounts stay opaque.
   */
  @Test
  void exposesOnlyReadyKnownPublicAccount() {
    var policy = mock(DemoModePolicy.class);
    var users = mock(UserRepository.class);
    var encoder = mock(PasswordEncoder.class);
    var controller = new DemoInfoController(policy, users, encoder);
    assertThat(controller.get()).containsExactlyEntriesOf(java.util.Map.of("enabled", false));
    verifyNoInteractions(users, encoder);
    when(policy.isReady()).thenReturn(true);
    assertThat(controller.get()).containsExactlyEntriesOf(java.util.Map.of("enabled", false));
    var user = mock(UserEntity.class);
    when(users.findByEmailNormalized(DemoDataSeeder.DEMO_EMAIL)).thenReturn(Optional.of(user));
    when(user.isEnabled()).thenReturn(true);
    when(user.getPasswordHash()).thenReturn("hash");
    assertThat(controller.get()).containsExactlyEntriesOf(java.util.Map.of("enabled", false));
    when(encoder.matches(DemoDataSeeder.DEMO_PASSWORD, "hash")).thenReturn(true);
    assertThat(controller.get())
        .containsEntry("enabled", true)
        .containsEntry("password", DemoDataSeeder.DEMO_PASSWORD)
        .hasSize(4);
  }
}
