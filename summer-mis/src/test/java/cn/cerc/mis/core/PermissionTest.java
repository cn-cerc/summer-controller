package cn.cerc.mis.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.cerc.mis.security.Permission;
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
        assertTrue(police.checkValue(null, null));
        assertTrue(police.checkValue("", ""));
        assertTrue(police.checkValue(null, ""));
        assertTrue(police.checkValue("", null));
    }

    @Test
    public void testCheck_admin() {
        assertTrue(police.checkValue(Permission.ADMIN, Permission.GUEST));
        assertTrue(police.checkValue(Permission.ADMIN, Permission.USERS));
        assertTrue(police.checkValue(Permission.ADMIN, Permission.ADMIN));
        assertTrue(police.checkValue(Permission.ADMIN, "acc"));
        assertTrue(police.checkValue(Permission.ADMIN, "acc.*"));
        assertTrue(police.checkValue(Permission.ADMIN, "acc.*[]"));
        assertTrue(police.checkValue(Permission.ADMIN, "acc.*[append,update]"));
        assertTrue(police.checkValue(Permission.ADMIN, "acc.*;-acc.*[update]"));
        assertFalse(police.checkValue("admin;-acc[delete]", "acc[delete]"));
    }

    @Test
    public void testCheck_guest() {
        assertTrue(police.checkValue(Permission.GUEST, Permission.GUEST));
        assertFalse(police.checkValue(Permission.GUEST, Permission.USERS));
        assertFalse(police.checkValue(Permission.GUEST, Permission.ADMIN));
        assertFalse(police.checkValue(Permission.GUEST, "acc"));
        assertFalse(police.checkValue(Permission.GUEST, "acc.*"));
        assertFalse(police.checkValue(Permission.GUEST, "acc.*[]"));
        assertFalse(police.checkValue(Permission.GUEST, "acc.*[append,update]"));
        assertFalse(police.checkValue(Permission.GUEST, "acc.*;acc.*[update]"));
    }

    @Test
    public void testCheck_users() {
        assertTrue(police.checkValue(Permission.USERS, Permission.GUEST));
        assertTrue(police.checkValue(Permission.USERS, Permission.USERS));
        assertFalse(police.checkValue(Permission.USERS, Permission.ADMIN));
        assertFalse(police.checkValue(Permission.USERS, "acc"));
        assertFalse(police.checkValue(Permission.USERS, "acc.*"));
        assertFalse(police.checkValue(Permission.USERS, "acc.*[]"));
        assertFalse(police.checkValue(Permission.USERS, "acc.*[append,update]"));
        assertFalse(police.checkValue(Permission.USERS, "acc.*;acc.*[update]"));
    }

    @Test
    public void testCheck_star() {
        assertTrue(police.checkValue("acc.*", "acc"));
        assertTrue(police.checkValue("acc.*", "acc."));
        assertTrue(police.checkValue("acc.*", "acc.cash"));
        //
        assertFalse(police.checkValue("acc*", "acc"));
        assertFalse(police.checkValue("acc.*", "ac"));
    }

    @Test
    public void testCheck_detail() {
        assertTrue(police.checkValue("acc[insert,delete,update]", "acc[insert,update,delete]"));
        assertTrue(police.checkValue("acc[insert,update,delete]", "acc[insert,update,delete]"));
        assertTrue(police.checkValue("acc", "acc[insert,update,delete]"));
        assertTrue(police.checkValue("acc[*]", "acc[insert,update,delete]"));
        assertFalse(police.checkValue("acc[]", "acc[insert,update,delete]"));
        assertTrue(police.checkValue("acc[abc,update]", "acc[abc]"));
        assertTrue(police.checkValue("acc[abc,update]", "acc[]"));
        assertFalse(police.checkValue("acc[abc,update]", "acc[*]"));
    }

    @Test
    public void testCheck_diff() {
        assertTrue(police.checkValue("acc", "acc[delete]"));
        assertFalse(police.checkValue("acc;-acc[delete]", "acc"));
        assertFalse(police.checkValue("acc;-acc[delete]", "acc[insert,update,delete]"));
    }

    @Test
    public void testCheck_other() {
        assertTrue(police.checkValue("admin;-acc[delete]", "acc[]"));
        assertTrue(police.checkValue("admin;-acc[delete];hr", "acc[]"));
        assertTrue(police.checkValue("admin;-acc[delete];hr", "hr"));
        assertTrue(police.checkValue("admin;-acc[delete]", "acc[update]"));
        assertFalse(police.checkValue("admin;-acc[delete]", "acc"));
        assertFalse(police.checkValue("admin;-acc[delete]", "acc[delete]"));
        assertFalse(police.checkValue("admin;-acc[]", "acc[update]"));
        assertFalse(police.checkValue("admin;-acc", "acc[update]"));
        assertTrue(police.checkValue("admin", "guest[update]"));
    }
}
