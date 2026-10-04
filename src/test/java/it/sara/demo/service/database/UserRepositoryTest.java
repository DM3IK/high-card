package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest {

    private final UserRepository repository = new UserRepository();

    private final List<User> createdUsers = new ArrayList<>();

    @AfterEach
    void removeCreatedUsers() {
        FakeDatabase.TABLE_USER.removeAll(createdUsers);
    }

    @Test
    void save_assignsGuidAndStoresUserSoItCanBeFoundByGuid() {
        User user = newUser();

        boolean saved = repository.save(user);

        assertAll(
                () -> assertTrue(saved),
                () -> assertNotNull(user.getGuid()),
                () -> assertSame(user, repository.getByGuid(user.getGuid()).orElseThrow())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown-guid"})
    void getByGuid_withNullEmptyOrUnknownGuid_returnsEmpty(String guid) {
        assertTrue(repository.getByGuid(guid).isEmpty());
    }

    /**
     * Regression: the lookup called {@code equals} on the stored guid and failed
     * with a NullPointerException when a stored user had no guid.
     */
    @Test
    void getByGuid_whenAStoredUserHasNullGuid_doesNotFail() {
        User userWithoutGuid = newUser();
        FakeDatabase.TABLE_USER.add(userWithoutGuid);

        Optional<User> result = repository.getByGuid("unknown-guid");

        assertTrue(result.isEmpty());
    }

    /**
     * Regression: the internal list was returned directly, so callers could change the stored data.
     */
    @Test
    void getAll_returnsAllUsersAsAnUnmodifiableList() {
        List<User> users = repository.getAll();

        assertAll(
                () -> assertEquals(FakeDatabase.TABLE_USER.size(), users.size()),
                () -> assertThrows(UnsupportedOperationException.class, () -> users.add(new User())),
                () -> assertThrows(UnsupportedOperationException.class, users::clear)
        );
    }

    /**
     * Regression: the table was a plain {@code ArrayList}, so concurrent saves could lose users
     * or fail with an exception.
     */
    @Test
    @Timeout(30)
    void save_withConcurrentCalls_storesEveryUser() throws Exception {
        int threads = 8;
        int usersPerThread = 1000;
        int sizeBefore = FakeDatabase.TABLE_USER.size();
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            List<User> batch = new ArrayList<>();
            for (int i = 0; i < usersPerThread; i++) {
                batch.add(newUser());
            }
            tasks.add(() -> {
                start.await();
                batch.forEach(repository::save);
                return null;
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(task));
            }
            start.countDown();
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }

        assertEquals(sizeBefore + threads * usersPerThread, FakeDatabase.TABLE_USER.size());
    }

    private User newUser() {
        User user = new User();
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");
        user.setPhoneNumber("+393331234567");
        createdUsers.add(user);
        return user;
    }
}
