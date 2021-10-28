package cn.cerc.mis.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cn.cerc.mis.security.Permission;
import cn.cerc.mis.security.SecurityPolice;

public class PermissionTest {

    @Test
    public void testCheck_empty() {
        assertTrue(SecurityPolice.validate("", ""));
        assertTrue(SecurityPolice.validate("", null));
    }

    @Test
    public void testCheck_admin() {
        assertTrue(SecurityPolice.validate(Permission.ADMIN, Permission.GUEST));
        assertTrue(SecurityPolice.validate(Permission.ADMIN, Permission.USERS));
        assertTrue(SecurityPolice.validate(Permission.ADMIN, Permission.ADMIN));
        assertTrue(SecurityPolice.validate(Permission.ADMIN, "acc"));
        assertTrue(SecurityPolice.validate(Permission.ADMIN, "acc.*"));
        assertTrue(SecurityPolice.validate(Permission.ADMIN, "acc.*[]"));
        assertTrue(SecurityPolice.validate(Permission.ADMIN, "acc.*[append,update]"));
        assertTrue(SecurityPolice.validate(Permission.ADMIN, "acc.*;-acc.*[update]"));
        assertFalse(SecurityPolice.validate("admin;-acc[delete]", "acc[delete]"));
    }

    @Test
    public void testCheck_guest() {
        assertTrue(SecurityPolice.validate(Permission.GUEST, Permission.GUEST));
        assertFalse(SecurityPolice.validate(Permission.GUEST, Permission.USERS));
        assertFalse(SecurityPolice.validate(Permission.GUEST, Permission.ADMIN));
        assertFalse(SecurityPolice.validate(Permission.GUEST, "acc"));
        assertFalse(SecurityPolice.validate(Permission.GUEST, "acc.*"));
        assertFalse(SecurityPolice.validate(Permission.GUEST, "acc.*[]"));
        assertFalse(SecurityPolice.validate(Permission.GUEST, "acc.*[append,update]"));
        assertFalse(SecurityPolice.validate(Permission.GUEST, "acc.*;acc.*[update]"));
    }

    @Test
    public void testCheck_users() {
        assertTrue(SecurityPolice.validate(Permission.USERS, Permission.GUEST));
        assertTrue(SecurityPolice.validate(Permission.USERS, Permission.USERS));
        assertFalse(SecurityPolice.validate(Permission.USERS, Permission.ADMIN));
        assertFalse(SecurityPolice.validate(Permission.USERS, "acc"));
        assertFalse(SecurityPolice.validate(Permission.USERS, "acc.*"));
        assertFalse(SecurityPolice.validate(Permission.USERS, "acc.*[]"));
        assertFalse(SecurityPolice.validate(Permission.USERS, "acc.*[append,update]"));
        assertFalse(SecurityPolice.validate(Permission.USERS, "acc.*;acc.*[update]"));
    }

    @Test
    public void testCheck_star() {
        assertTrue(SecurityPolice.validate("acc.*", "acc"));
        assertTrue(SecurityPolice.validate("acc.*", "acc."));
        assertTrue(SecurityPolice.validate("acc.*", "acc.cash"));
        //
        assertFalse(SecurityPolice.validate("acc*", "acc"));
        assertFalse(SecurityPolice.validate("acc.*", "ac"));
    }

    @Test
    public void testCheck_detail() {
        assertTrue(SecurityPolice.validate("acc[insert,delete,update]", "acc[insert,update,delete]"));
        assertTrue(SecurityPolice.validate("acc[insert,update,delete]", "acc[insert,update,delete]"));
        assertTrue(SecurityPolice.validate("acc", "acc[insert,update,delete]"));
        assertTrue(SecurityPolice.validate("acc[*]", "acc[insert,update,delete]"));
        assertFalse(SecurityPolice.validate("acc[]", "acc[insert,update,delete]"));
        assertTrue(SecurityPolice.validate("acc[abc,update]", "acc[abc]"));
        assertTrue(SecurityPolice.validate("acc[abc,update]", "acc[]"));
        assertFalse(SecurityPolice.validate("acc[abc,update]", "acc[*]"));
    }

    @Test
    public void testCheck_diff() {
        assertTrue(SecurityPolice.validate("acc", "acc[delete]"));
        assertFalse(SecurityPolice.validate("acc;-acc[delete]", "acc"));
        assertFalse(SecurityPolice.validate("acc;-acc[delete]", "acc[insert,update,delete]"));
    }

    @Test
    public void testCheck_other() {
        assertTrue(SecurityPolice.validate("admin;-acc[delete]", "acc[]"));
        assertTrue(SecurityPolice.validate("admin;-acc[delete];hr", "acc[]"));
        assertTrue(SecurityPolice.validate("admin;-acc[delete];hr", "hr"));
        assertTrue(SecurityPolice.validate("admin;-acc[delete]", "acc[update]"));
        assertFalse(SecurityPolice.validate("admin;-acc[delete]", "acc"));
        assertFalse(SecurityPolice.validate("admin;-acc[delete]", "acc[delete]"));
        assertFalse(SecurityPolice.validate("admin;-acc[]", "acc[update]"));
        assertFalse(SecurityPolice.validate("admin;-acc", "acc[update]"));
        assertTrue(SecurityPolice.validate("admin", "guest[update]"));
    }
}
