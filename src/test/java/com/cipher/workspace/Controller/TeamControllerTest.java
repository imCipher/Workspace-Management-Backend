package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.InviteResponse;
import com.cipher.workspace.DTOs.InviteWorkerRequest;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.DTOs.TeamMemberResponse;
import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.UserStatus;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.GlobalExceptionHandler;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Services.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void inviteMember_returns201() throws Exception {
        InviteWorkerRequest request = new InviteWorkerRequest("Worker Name", "worker@example.com", Role.WORKER);
        InviteResponse response = new InviteResponse(
                UUID.randomUUID(),
                "worker@example.com",
                "Worker Name",
                Role.WORKER,
                "admin",
                Instant.now().plusSeconds(86400),
                true,
                "https://workspace.local/invite?token=xyz"
        );

        when(teamService.inviteWorker(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/team/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("worker@example.com"))
                .andExpect(jsonPath("$.fullname").value("Worker Name"))
                .andExpect(jsonPath("$.role").value("WORKER"));
    }

    @Test
    void getMembers_returns200() throws Exception {
        TeamMemberResponse member = new TeamMemberResponse(
                UUID.randomUUID(),
                "Worker One",
                "worker_1",
                "worker@example.com",
                Role.WORKER,
                UserStatus.ACTIVE,
                UUID.randomUUID(),
                "admin",
                LocalDateTime.now()
        );

        PagedResponse<TeamMemberResponse> paged = PagedResponse.of(List.of(member), 0, 10, 1);
        when(teamService.getTeamMembers(0, 10)).thenReturn(paged);

        mockMvc.perform(get("/api/team/members?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("worker_1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchMembers_returns200() throws Exception {
        PagedResponse<TeamMemberResponse> paged = PagedResponse.of(List.of(), 0, 10, 0);
        when(teamService.searchTeamMembers(any(), any(), any(), anyInt(), anyInt())).thenReturn(paged);

        mockMvc.perform(get("/api/team/members/search?query=test&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getInvites_returns200() throws Exception {
        InviteResponse invite = new InviteResponse(
                UUID.randomUUID(), "pending@test.com", "Pending User", Role.WORKER, "admin", Instant.now().plusSeconds(3600), true
        );
        PagedResponse<InviteResponse> paged = PagedResponse.of(List.of(invite), 0, 10, 1);
        when(teamService.getInvites(0, 10)).thenReturn(paged);

        mockMvc.perform(get("/api/team/invites?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("pending@test.com"));
    }

    @Test
    void removeMember_success_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(teamService).removeWorkerOrInvite(any(), eq(id));

        mockMvc.perform(delete("/api/team/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeMember_adminSelfRemoval_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BadRequestException("Admin cannot remove their own account"))
                .when(teamService).removeWorkerOrInvite(any(), eq(id));

        mockMvc.perform(delete("/api/team/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Admin cannot remove their own account"));
    }

    @Test
    void revokeInvite_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Invite not found"))
                .when(teamService).revokeInvite(any(), eq(id));

        mockMvc.perform(delete("/api/team/invites/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
