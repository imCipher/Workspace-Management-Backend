package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.InviteResponse;
import com.cipher.workspace.DTOs.InviteWorkerRequest;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.DTOs.TeamMemberResponse;
import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.UserStatus;
import com.cipher.workspace.Services.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Team Management", description = "Endpoints for managing workspace workers and invitations (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/api/team", "/team"})
@PreAuthorize("hasRole('ADMIN')")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "Invite team member / worker", description = "Creates an invitation link and emails registration instructions to the prospective team member.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Active team member with this email already exists")
    })
    @PostMapping({"/invite", "/add-member"})
    public ResponseEntity<InviteResponse> inviteMember(
            @AuthenticationPrincipal User adminUser,
            @Valid @RequestBody InviteWorkerRequest request
    ) {
        InviteResponse response = teamService.inviteWorker(adminUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get team members", description = "Retrieves a paginated list of team members in the workspace.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of team members returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping({"/members", "/active-members"})
    public ResponseEntity<PagedResponse<TeamMemberResponse>> getMembers(
            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<TeamMemberResponse> members = teamService.getTeamMembers(page, size);
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Search team members", description = "Searches team members by role, status, and name/email/username query.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered list of team members returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping("/members/search")
    public ResponseEntity<PagedResponse<TeamMemberResponse>> searchMembers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false, name = "query") String query,
            @RequestParam(required = false, name = "search") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String searchTerm = (query != null && !query.isBlank()) ? query : search;
        PagedResponse<TeamMemberResponse> result = teamService.searchTeamMembers(role, status, searchTerm, page, size);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get team invitations", description = "Retrieves a paginated list of issued invitation links and status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of invitations returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role")
    })
    @GetMapping({"/invites", "/all-members"})
    public ResponseEntity<PagedResponse<InviteResponse>> getInvites(
            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<InviteResponse> invites = teamService.getInvites(page, size);
        return ResponseEntity.ok(invites);
    }

    @Operation(summary = "Remove team member or invite", description = "Deactivates/deletes a team member or revokes an invite by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed or invite revoked successfully"),
            @ApiResponse(responseCode = "400", description = "Admin cannot remove their own account"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Member or invite not found")
    })
    @DeleteMapping({"/{id}", "/remove-member/{id}"})
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal User adminUser,
            @Parameter(description = "UUID of team member or invite")
            @PathVariable UUID id
    ) {
        teamService.removeWorkerOrInvite(adminUser, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke invitation", description = "Revokes a pending invitation link by invite ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invite revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Invite not found")
    })
    @DeleteMapping("/invites/{id}")
    public ResponseEntity<Void> revokeInvite(
            @AuthenticationPrincipal User adminUser,
            @Parameter(description = "UUID of invite")
            @PathVariable UUID id
    ) {
        teamService.revokeInvite(adminUser, id);
        return ResponseEntity.noContent().build();
    }
}
