package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for users stored in {@link FakeDatabase}.
 */
@Component
public class UserRepository {

    /**
     * Stores a new user and assigns it a random guid.
     *
     * @param user the user to store
     * @return {@code true} if the user was stored
     */
    public boolean save(User user) {
        user.setGuid(UUID.randomUUID().toString());
        FakeDatabase.TABLE_USER.add(user);
        return true;
    }

    /**
     * Finds a user by guid.
     *
     * @param guid the guid to look for
     * @return the matching user, or an empty optional if the guid is null or unknown
     */
    public Optional<User> getByGuid(String guid) {
        if (guid == null) {
            return Optional.empty();
        }
        return FakeDatabase.TABLE_USER.stream().filter(u -> guid.equals(u.getGuid())).findFirst();
    }

    /**
     * Returns all stored users.
     *
     * @return an unmodifiable snapshot of the user table
     */
    public List<User> getAll() {
        return List.copyOf(FakeDatabase.TABLE_USER);
    }
}
