package co.sena.iam.adapter.in.rest;

import co.sena.iam.application.port.in.CreateUserCommand;
import co.sena.iam.application.port.in.GetCurrentUserUseCase;
import co.sena.iam.application.port.in.GetUserUseCase;
import co.sena.iam.application.port.in.ListUsersQuery;
import co.sena.iam.application.port.in.ListUsersUseCase;
import co.sena.iam.application.port.in.UpdateUserCommand;
import co.sena.iam.application.port.in.UserCreateResult;
import co.sena.iam.contract.api.UsersApi;
import co.sena.iam.contract.model.ListUsers200Response;
import co.sena.iam.contract.model.ListUsers200ResponsePagination;
import co.sena.iam.contract.model.User;
import co.sena.iam.contract.model.UserCreate;
import co.sena.iam.contract.model.UserCreateResponse;
import co.sena.iam.contract.model.UserUpdate;
import co.sena.iam.domain.model.ActorType;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

/** HU-IAM-005 E1-E3 — user CRUD and session management endpoints. */
@RestController
@RequestMapping("/api/v1")
class UserController implements UsersApi {

    private final UserApplicationService userService;
    private final GetUserUseCase getUser;
    private final ListUsersUseCase listUsers;
    private final GetCurrentUserUseCase getCurrentUser;
    private final HttpServletRequest request;

    UserController(
            UserApplicationService userService,
            GetUserUseCase getUser,
            ListUsersUseCase listUsers,
            GetCurrentUserUseCase getCurrentUser,
            HttpServletRequest request) {
        this.userService = userService;
        this.getUser = getUser;
        this.listUsers = listUsers;
        this.getCurrentUser = getCurrentUser;
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    // ---------------------------------------------------------------- POST /users

    @Override
    public ResponseEntity<UserCreateResponse> createUser(UserCreate body) {
        requireAuthenticated();
        var cmd = new CreateUserCommand(
                body.getEmail(), body.getFirstName(), body.getLastName(),
                ActorType.valueOf(body.getActorType().getValue()),
                body.getActorId(),
                body.getDocumentType() != null ? body.getDocumentType().getValue() : null,
                body.getDocumentNumber(),
                body.getDefaultMaxHoursPerWeek(),
                body.getInitialRole(),
                body.getTrainingCenterId());
        UserCreateResult result = userService.createUser(cmd);
        UserCreateResponse response = new UserCreateResponse()
                .id(result.id())
                .email(result.email())
                .temporaryPassword(result.temporaryPassword());
        return ResponseEntity.created(URI.create("/api/v1/users/" + result.id())).body(response);
    }

    // ---------------------------------------------------------------- GET /users

    @Override
    public ResponseEntity<ListUsers200Response> listUsers(
            Integer page, Integer pageSize, String sort,
            UUID trainingCenterId, String role, Boolean isActive) {
        requireAuthenticated();
        var query = new ListUsersQuery(
                page != null ? page : 1,
                pageSize != null ? pageSize : 20,
                trainingCenterId, role, isActive);
        var userPage = listUsers.listUsers(query);

        var pagination = new ListUsers200ResponsePagination()
                .page(userPage.page())
                .pageSize(userPage.size())
                .totalItems((int) userPage.totalItems())
                .totalPages(userPage.totalPages());

        var body = new ListUsers200Response()
                .data(userPage.items().stream().map(UserController::toApiUser).toList())
                .pagination(pagination);
        return ResponseEntity.ok(body);
    }

    // ---------------------------------------------------------------- GET /users/{id}

    @Override
    public ResponseEntity<User> getUser(UUID id) {
        requireAuthenticated();
        co.sena.iam.domain.model.User user = getUser.getUser(id);
        return ResponseEntity.ok(toApiUser(user));
    }

    // ---------------------------------------------------------------- PUT /users/{id}

    @Override
    public ResponseEntity<User> updateUser(UUID id, UserUpdate body) {
        requireAuthenticated();
        ActorType actorType = body.getActorType() != null
                ? ActorType.valueOf(body.getActorType().getValue()) : null;
        var cmd = new UpdateUserCommand(
                body.getFirstName(), body.getLastName(),
                actorType, body.getActorId(), body.getIsActive());
        co.sena.iam.domain.model.User updated = userService.updateUser(id, cmd);
        return ResponseEntity.ok(toApiUser(updated));
    }

    // ---------------------------------------------------------------- POST /users/{id}/deactivate

    @Override
    public ResponseEntity<Void> deactivateUser(UUID id) {
        requireAuthenticated();
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- auth guard

    private void requireAuthenticated() {
        getCurrentUser.currentUser(BearerToken.from(request));
    }

    // ---------------------------------------------------------------- mappers

    private static User toApiUser(co.sena.iam.domain.model.User u) {
        return new User()
                .id(u.id())
                .email(u.email())
                .firstName(u.firstName())
                .lastName(u.lastName())
                .fullName(u.firstName() + " " + u.lastName())
                .actorType(User.ActorTypeEnum.fromValue(u.actorType().name()))
                .actorId(u.actorId())
                .isActive(u.active());
    }

}
