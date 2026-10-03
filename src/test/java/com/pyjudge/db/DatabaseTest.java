package com.pyjudge.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pyjudge.Services;
import com.pyjudge.TestSupport;
import com.pyjudge.model.Admin;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Instructor;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Role;
import com.pyjudge.model.Student;
import com.pyjudge.model.Submission;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.User;

/** JDBC layer: CRUD, polymorphic row mapping, transactions, and the schema's constraints. */
class DatabaseTest {

    private Services s;

    @BeforeEach
    void setUp() {
        s = TestSupport.services();
    }

    @AfterEach
    void tearDown() {
        s.close();
    }

    @Test
    void usersAreLoadedAsTheRightSubclass() {
        long a = TestSupport.user(s, "alice", Role.STUDENT).getId();
        long b = TestSupport.user(s, "bob", Role.INSTRUCTOR).getId();
        long c = TestSupport.user(s, "carol", Role.ADMIN).getId();
        assertInstanceOf(Student.class, s.users.findById(a).orElseThrow());
        assertInstanceOf(Instructor.class, s.users.findById(b).orElseThrow());
        assertInstanceOf(Admin.class, s.users.findById(c).orElseThrow());
        assertTrue(s.users.findById(c).orElseThrow().canAuthor(), "admins inherit authoring rights");
    }

    @Test
    void crudRoundTrip() {
        User u = TestSupport.user(s, "dana", Role.STUDENT);
        u.setBio("Learning Python");
        s.users.save(u);
        assertEquals("Learning Python", s.users.findByUsername("dana").orElseThrow().getBio());
        assertTrue(s.users.findByLogin("dana@example.com").isPresent(), "login by email");
        assertTrue(s.users.delete(u.getId()));
        assertTrue(s.users.findById(u.getId()).isEmpty());
    }

    @Test
    void transactionRollsBackEverythingOnError() {
        assertThrows(IllegalStateException.class, () -> s.db.inTransaction((Database.SqlRunnable) c -> {
            TestSupport.user(s, "ghost", Role.STUDENT);
            throw new IllegalStateException("boom");
        }));
        assertTrue(s.users.findByUsername("ghost").isEmpty(), "the insert inside the failed transaction was undone");
    }

    @Test
    void uniqueConstraintIsEnforced() {
        TestSupport.user(s, "erin", Role.STUDENT);
        User dup = User.forRole(Role.STUDENT);
        dup.setUsername("erin");
        dup.setName("Other");
        dup.setEmail("other@example.com");
        dup.setPasswordHash("x");
        DataAccessException e = assertThrows(DataAccessException.class, () -> s.users.save(dup));
        assertTrue(e.isUniqueViolation());
    }

    @Test
    void problemIsSavedWithItsTestCasesAndDeletesCascade() {
        User student = TestSupport.user(s, "finn", Role.STUDENT);
        Problem p = new Problem();
        p.setTitle("Echo");
        p.setSlug("echo");
        p.setDescription("Print the input.");
        p.setStatus(ContentStatus.APPROVED);
        p.getTestCases().add(new TestCase("hi", "hi", true));
        p.getTestCases().add(new TestCase("yo", "yo", false));
        s.problems.save(p);
        assertEquals(2, s.problems.findBySlug("echo").orElseThrow().getTestCases().size());

        Submission sub = new Submission();
        sub.setUserId(student.getId());
        sub.setProblemId(p.getId());
        sub.setCode("print(input())");
        s.submissions.save(sub);

        s.problems.delete(p.getId());
        assertTrue(s.problems.testCases(p.getId()).isEmpty(), "test cases cascade");
        assertTrue(s.submissions.findById(sub.getId()).isEmpty(), "submissions cascade");
    }

    @Test
    void deletingAuthorKeepsContentButClearsTheLink() {
        User teacher = TestSupport.user(s, "gwen", Role.INSTRUCTOR);
        Problem p = new Problem();
        p.setTitle("Mine");
        p.setSlug("mine");
        p.setCreatedBy(teacher.getId());
        s.problems.save(p);
        s.users.delete(teacher.getId());
        Problem after = s.problems.findById(p.getId()).orElseThrow();
        assertEquals(null, after.getCreatedBy(), "ON DELETE SET NULL");
        assertFalse(after.getTitle().isEmpty());
    }
}
