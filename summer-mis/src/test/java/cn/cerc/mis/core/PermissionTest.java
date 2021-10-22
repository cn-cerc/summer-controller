package cn.cerc.mis.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.cerc.mis.security.SecurityPolice;

public class PermissionTest {
    private SecurityPolice police;

    @Before
    public void setUp() throws Exception {
        police = new SecurityPolice();
    }

    @After
    public void tearDown() throws Exception {
        police = null;
    }

    @Test
    public void testCheck_empty() {
        assertTrue(police.checkPassed(null, null));
        assertTrue(police.checkPassed("", ""));
        assertTrue(police.checkPassed(null, ""));
        assertTrue(police.checkPassed("", null));
    }

    @Test
    public void testCheck_admin() {
        assertTrue(police.checkPassed(Permission.ADMIN, Permission.GUEST));
        assertTrue(police.checkPassed(Permission.ADMIN, Permission.USERS));
        assertTrue(police.checkPassed(Permission.ADMIN, Permission.ADMIN));
        assertTrue(police.checkPassed(Permission.ADMIN, "acc"));
        assertTrue(police.checkPassed(Permission.ADMIN, "acc.*"));
        assertTrue(police.checkPassed(Permission.ADMIN, "acc.*[]"));
        assertTrue(police.checkPassed(Permission.ADMIN, "acc.*[append,update]"));
        assertTrue(police.checkPassed(Permission.ADMIN, "acc.*;-acc.*[update]"));
        assertFalse(police.checkPassed("admin;-acc[delete]", "acc[delete]"));
    }

    @Test
    public void testCheck_guest() {
        assertTrue(police.checkPassed(Permission.GUEST, Permission.GUEST));
        assertFalse(police.checkPassed(Permission.GUEST, Permission.USERS));
        assertFalse(police.checkPassed(Permission.GUEST, Permission.ADMIN));
        assertFalse(police.checkPassed(Permission.GUEST, "acc"));
        assertFalse(police.checkPassed(Permission.GUEST, "acc.*"));
        assertFalse(police.checkPassed(Permission.GUEST, "acc.*[]"));
        assertFalse(police.checkPassed(Permission.GUEST, "acc.*[append,update]"));
        assertFalse(police.checkPassed(Permission.GUEST, "acc.*;acc.*[update]"));
    }

    @Test
    public void testCheck_users() {
        assertTrue(police.checkPassed(Permission.USERS, Permission.GUEST));
        assertTrue(police.checkPassed(Permission.USERS, Permission.USERS));
        assertFalse(police.checkPassed(Permission.USERS, Permission.ADMIN));
        assertFalse(police.checkPassed(Permission.USERS, "acc"));
        assertFalse(police.checkPassed(Permission.USERS, "acc.*"));
        assertFalse(police.checkPassed(Permission.USERS, "acc.*[]"));
        assertFalse(police.checkPassed(Permission.USERS, "acc.*[append,update]"));
        assertFalse(police.checkPassed(Permission.USERS, "acc.*;acc.*[update]"));
    }

    @Test
    public void testCheck_star() {
        assertTrue(police.checkPassed("acc.*", "acc"));
        assertTrue(police.checkPassed("acc.*", "acc."));
        assertTrue(police.checkPassed("acc.*", "acc.cash"));
        //
        assertFalse(police.checkPassed("acc*", "acc"));
        assertFalse(police.checkPassed("acc.*", "ac"));
    }

    @Test
    public void testCheck_detail() {
        assertTrue(police.checkPassed("acc[insert,delete,update]", "acc[insert,update,delete]"));
        assertTrue(police.checkPassed("acc[insert,update,delete]", "acc[insert,update,delete]"));
        assertTrue(police.checkPassed("acc", "acc[insert,update,delete]"));
        assertTrue(police.checkPassed("acc[*]", "acc[insert,update,delete]"));
        assertFalse(police.checkPassed("acc[]", "acc[insert,update,delete]"));
        assertTrue(police.checkPassed("acc[abc,update]", "acc[abc]"));
        assertTrue(police.checkPassed("acc[abc,update]", "acc[]"));
        assertFalse(police.checkPassed("acc[abc,update]", "acc[*]"));
    }

    @Test
    public void testCheck_diff() {
        assertTrue(police.checkPassed("acc", "acc[delete]"));
        assertFalse(police.checkPassed("acc;-acc[delete]", "acc"));
        assertFalse(police.checkPassed("acc;-acc[delete]", "acc[insert,update,delete]"));
    }

    @Test
    public void testCheck_other() {
        assertTrue(police.checkPassed("admin;-acc[delete]", "acc[]"));
        assertTrue(police.checkPassed("admin;-acc[delete];hr", "acc[]"));
        assertTrue(police.checkPassed("admin;-acc[delete];hr", "hr"));
        assertTrue(police.checkPassed("admin;-acc[delete]", "acc[update]"));
        assertFalse(police.checkPassed("admin;-acc[delete]", "acc"));
        assertFalse(police.checkPassed("admin;-acc[delete]", "acc[delete]"));
        assertFalse(police.checkPassed("admin;-acc[]", "acc[update]"));
        assertFalse(police.checkPassed("admin;-acc", "acc[update]"));
        assertTrue(police.checkPassed("admin", "guest[update]"));
    }
}
