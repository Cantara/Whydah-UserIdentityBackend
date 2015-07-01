package net.whydah.identity.user.role;

import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-01-18
 */
public class UserPropertyAndRoleDaoTest {
    private final static String basepath = "target/UserPropertyAndRoleDaoTest/";
    private final static String dbpath = basepath + "hsqldb/roles";
    private static BasicDataSource dataSource;
    private static DatabaseMigrationHelper dbHelper;
    private static UserPropertyAndRoleDao roleRepository;

    @BeforeClass
    public static void init() throws Exception {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);

        dbHelper = new DatabaseMigrationHelper(dataSource);
        roleRepository = new UserPropertyAndRoleDao(dataSource);
    }

    @Before
    public void cleanDB() {
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();
        assertEquals(roleRepository.countUserRolesInDB(), 0);
    }


    @Test
    public void testAddAndGet() {
        Map<String, UserPropertyAndRole> added = addTestRoles("uid1", 1);
        for (UserPropertyAndRole expected : added.values()) {
            UserPropertyAndRole fromDb = roleRepository.getUserPropertyAndRole(expected.getRoleId());
            roleEquals(expected, fromDb);
        }
    }

    @Test
    public void testGetUserPropertyAndRoles() {
        String uid = "uid2";
        Map<String, UserPropertyAndRole> added = addTestRoles(uid, 3);
        List<UserPropertyAndRole> roles = roleRepository.getUserPropertyAndRoles(uid);
        assertEquals(added.size(), roles.size());
        for (UserPropertyAndRole fromDb : roles) {
            UserPropertyAndRole expected = added.get(fromDb.getRoleId());
            roleEquals(expected, fromDb);
        }
    }

    @Test
    public void testCountUserRolesInDB() {
        String uid3 = "uid3";
        Map<String, UserPropertyAndRole> added3 = addTestRoles(uid3, 2);
        String uid4 = "uid4";
        Map<String, UserPropertyAndRole> added4 = addTestRoles(uid4, 3);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size() + added4.size());
    }

    /*
    @Test
    public void testDeleteRolesForUid() {
        String uid3 = "uid3";
        Map<String, UserPropertyAndRole> added3 = addTestRoles(uid3, 2);
        String uid4 = "uid4";
        Map<String, UserPropertyAndRole> added4 = addTestRoles(uid4, 3);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size() + added4.size());

        roleRepository.deleteUser(uid4);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size());
        assertEquals(roleRepository.getUserPropertyAndRoles(uid4).size(), 0);
    }
    */

    @Test
    public void testDeleteUserAppRoles() {
        String uid3 = "uid3";
        Map<String, UserPropertyAndRole> added3 = addTestRoles(uid3, 2);
        String uid4 = "uid4";
        Map<String, UserPropertyAndRole> added4 = addTestRoles(uid4, 3);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size() + added4.size());

        String appIdToDelete = "appId2";
        roleRepository.deleteUserAppRoles(uid4, appIdToDelete);
        int expectedRolesForUid4 = added4.size() - 1;
        assertEquals(roleRepository.countUserRolesInDB(), (added3.size() + expectedRolesForUid4));
        assertEquals(roleRepository.getUserPropertyAndRoles(uid4).size(), expectedRolesForUid4);
    }

    @Test
    public void testDeleteRoleByRoleId() {
        String uid3 = "uid3";
        Map<String, UserPropertyAndRole> added3 = addTestRoles(uid3, 2);
        String uid4 = "uid4";
        Map<String, UserPropertyAndRole> added4 = addTestRoles(uid4, 3);
        assertEquals(roleRepository.countUserRolesInDB(), added3.size() + added4.size());

        roleRepository.deleteRole("roleId1");   //Expect role to be remove for both users
        assertEquals(roleRepository.countUserRolesInDB(), (added3.size() - 1 + added4.size() - 1));
        assertEquals(roleRepository.getUserPropertyAndRoles(uid3).size(), 1);
        assertEquals(roleRepository.getUserPropertyAndRoles(uid4).size(), 2);
    }


    private Map<String, UserPropertyAndRole> addTestRoles(String uid, int count) {
        Map<String, UserPropertyAndRole> added = new HashMap<>();
        UserPropertyAndRole role;
        for (int i = 1; i <= count; i++) {
            role = new UserPropertyAndRole();
            role.setRoleId("roleId" + i);
            role.setUid(uid);
            role.setApplicationId("appId" + i);
            role.setApplicationRoleName("appRoleName" + i);
            role.setApplicationRoleValue("appRoleValue" + i);
            //private transient String applicationName;
            //private transient String organizationName;
            added.put(role.getRoleId(), role);
            roleRepository.addUserPropertyAndRole(role);
        }
        return added;
    }

    private void roleEquals(UserPropertyAndRole role, UserPropertyAndRole fromDb) {
        //assertTrue(fromDb.equals(role));  //Not possible because of null to "" in getters.

        assertEquals(fromDb.getRoleId(), role.getRoleId());
        assertEquals(fromDb.getUid(), role.getUid());
        assertEquals(fromDb.getApplicationId(), role.getApplicationId());
        assertEquals(fromDb.getApplicationRoleName(), role.getApplicationRoleName());
        assertEquals(fromDb.getApplicationRoleValue(), role.getApplicationRoleValue());

        //Should not be populated by UserPropertyAndRoleDao
        //assertNull(fromDb.getApplicationName());
        //assertNull(fromDb.getOrganizationName());
        assertEquals(fromDb.getApplicationName(), role.getApplicationName());
        assertEquals(fromDb.getOrganizationName(), role.getOrganizationName());
    }
}
