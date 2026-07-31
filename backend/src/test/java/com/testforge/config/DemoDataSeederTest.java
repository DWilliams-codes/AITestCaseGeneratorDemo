package com.testforge.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.testforge.auth.application.AuthService;
import com.testforge.project.application.ProjectService;
import com.testforge.project.domain.ProjectEntity;
import com.testforge.project.repository.ProjectRepository;
import com.testforge.requirement.application.RequirementService;
import com.testforge.requirement.dto.RequirementDtos.CreateRequirementRequest;
import com.testforge.requirement.repository.RequirementRepository;
import com.testforge.user.domain.UserEntity;
import com.testforge.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;

class DemoDataSeederTest {
  /** Verifies the demo seeds varied enterprise stories while leaving generated output empty. */
  @Test
  void seedsOnlyProfessionalSourceStories() {
    UserRepository users = mock(UserRepository.class);
    AuthService authService = mock(AuthService.class);
    ProjectService projectService = mock(ProjectService.class);
    ProjectRepository projects = mock(ProjectRepository.class);
    RequirementService requirementService = mock(RequirementService.class);
    RequirementRepository requirements = mock(RequirementRepository.class);
    UserEntity user = mock(UserEntity.class);
    ProjectEntity project = mock(ProjectEntity.class);
    UUID userId = UUID.randomUUID();
    UUID projectId = UUID.randomUUID();
    when(user.getId()).thenReturn(userId);
    when(project.getId()).thenReturn(projectId);
    when(users.findByEmailNormalized(DemoDataSeeder.DEMO_EMAIL)).thenReturn(Optional.of(user));
    when(projects.findFirstByOwnerIdAndNameOrderByCreatedAtAsc(userId, "Commerce Returns Platform"))
        .thenReturn(Optional.of(project));

    new DemoDataSeeder(
            users, authService, projectService, projects, requirementService, requirements)
        .run(mock(ApplicationArguments.class));

    ArgumentCaptor<CreateRequirementRequest> stories =
        ArgumentCaptor.forClass(CreateRequirementRequest.class);
    verify(requirementService, times(4)).create(eq(userId), eq(projectId), stories.capture());
    assertThat(stories.getAllValues())
        .allSatisfy(
            story -> {
              assertThat(story.userStory()).startsWith("As a").contains(" so that ");
              assertThat(story.businessRequirements()).hasSizeGreaterThan(500);
              assertThat(story.assumptions()).isNotBlank();
              assertThat(story.sourceReference()).startsWith("ADO-RET-");
              assertThat(story.acceptanceCriteria()).hasSize(6).allSatisfy(this::assertCriterion);
            });
    long uniqueReferences =
        stories.getAllValues().stream()
            .map(CreateRequirementRequest::sourceReference)
            .distinct()
            .count();
    assertThat(uniqueReferences).isEqualTo(4);
  }

  /** Requires each seeded criterion to state context, action, and an observable outcome. */
  private void assertCriterion(String criterion) {
    assertThat(criterion).startsWith("Given ").contains(", when ").contains(", then ");
  }
}
