package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LibraryManager libraryManager;

    @Nested
    class AddBookTests {

        @Test
        void shouldAddNewBook() {
            libraryManager.addBook("book1", 5);
            assertEquals(5, libraryManager.getAvailableCopies("book1"));
        }

        @Test
        void shouldIncreaseQuantityOfExistingBook() {
            libraryManager.addBook("book1", 3);
            libraryManager.addBook("book1", 2);
            assertEquals(5, libraryManager.getAvailableCopies("book1"));
        }

        @Test
        void shouldAddZeroCopies() {
            libraryManager.addBook("book1", 0);
            assertEquals(0, libraryManager.getAvailableCopies("book1"));
        }
    }

    @Nested
    class BorrowBookTests {

        @BeforeEach
        void setUp() {
            libraryManager.addBook("book1", 5);
            lenient().when(userService.isUserActive("user1")).thenReturn(true);
        }

        @Test
        void shouldSuccessfullyBorrowBookByActiveUser() {
            boolean result = libraryManager.borrowBook("book1", "user1");

            assertTrue(result);
            assertEquals(4, libraryManager.getAvailableCopies("book1"));
            verify(notificationService).notifyUser("user1", "You have borrowed the book: book1");
        }

        @Test
        void shouldRejectBorrowByInactiveUser() {
            when(userService.isUserActive("user2")).thenReturn(false);
            boolean result = libraryManager.borrowBook("book1", "user2");

            assertFalse(result);
            assertEquals(5, libraryManager.getAvailableCopies("book1"));
            verify(notificationService).notifyUser("user2", "Your account is not active.");
        }

        @Test
        void shouldRejectBorrowWhenBookNotAvailable() {
            boolean result = libraryManager.borrowBook("book2", "user1");

            assertFalse(result);
            assertEquals(5, libraryManager.getAvailableCopies("book1"));
        }

        @Test
        void shouldRejectBorrowWhenAllCopiesBorrowed() {
            libraryManager.addBook("book2", 1);
            libraryManager.borrowBook("book2", "user1");

            boolean result = libraryManager.borrowBook("book2", "user1");

            assertFalse(result);
            assertEquals(0, libraryManager.getAvailableCopies("book2"));
        }

        @Test
        void shouldRejectBorrowOfNonExistentBook() {
            boolean result = libraryManager.borrowBook("nonexistent", "user1");

            assertFalse(result);
            assertEquals(0, libraryManager.getAvailableCopies("nonexistent"));
        }
    }

    @Nested
    class ReturnBookTests {

        @BeforeEach
        void setUp() {
            libraryManager.addBook("book1", 2);
            when(userService.isUserActive("user1")).thenReturn(true);
            libraryManager.borrowBook("book1", "user1");
        }

        @Test
        void shouldSuccessfullyReturnBookByCorrectUser() {
            boolean result = libraryManager.returnBook("book1", "user1");

            assertTrue(result);
            assertEquals(2, libraryManager.getAvailableCopies("book1"));
            verify(notificationService).notifyUser("user1", "You have returned the book: book1");
        }

        @Test
        void shouldRejectReturnByWrongUser() {
            boolean result = libraryManager.returnBook("book1", "user2");

            assertFalse(result);
            assertEquals(1, libraryManager.getAvailableCopies("book1"));
        }

        @Test
        void shouldRejectReturnOfNonBorrowedBook() {
            libraryManager.addBook("book2", 3);
            boolean result = libraryManager.returnBook("book2", "user1");

            assertFalse(result);
            assertEquals(3, libraryManager.getAvailableCopies("book2"));
        }

        @Test
        void shouldRejectReturnOfNonExistentBook() {
            boolean result = libraryManager.returnBook("nonexistent", "user1");

            assertFalse(result);
            assertEquals(0, libraryManager.getAvailableCopies("nonexistent"));
        }
    }

    @Nested
    class GetAvailableCopiesTests {

        @Test
        void shouldReturnCorrectQuantityForExistingBook() {
            libraryManager.addBook("book1", 10);

            assertEquals(10, libraryManager.getAvailableCopies("book1"));
        }

        @Test
        void shouldReturnZeroForNonExistentBook() {
            assertEquals(0, libraryManager.getAvailableCopies("nonexistent"));
        }

        @Test
        void shouldReturnCorrectValueAfterAddingAndBorrowing() {
            libraryManager.addBook("book1", 5);
            when(userService.isUserActive("user1")).thenReturn(true);
            libraryManager.borrowBook("book1", "user1");

            assertEquals(4, libraryManager.getAvailableCopies("book1"));
        }
    }

    @Nested
    class CalculateDynamicLateFeeTests {

        @ParameterizedTest
        @CsvSource({
            "5, false, false, 2.50",
            "4, true, false, 3.00",
            "5, false, true, 2.00",
            "4, true, true, 2.40",
            "0, false, false, 0.00",
        })
        void shouldCalculateFeeCorrectly(
                int overdueDays,
                boolean isBestseller,
                boolean isPremiumMember,
                double expectedFee
        ) {
            double fee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);

            assertEquals(expectedFee, fee);
        }

        @Test
        void shouldThrowExceptionForNegativeOverdueDays() {
            assertThrows(
                IllegalArgumentException.class,
                () -> libraryManager.calculateDynamicLateFee(-1, false, false)
            );
        }
    }
}
