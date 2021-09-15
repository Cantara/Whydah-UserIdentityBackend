package net.whydah.identity.user.role;

import net.whydah.sso.user.types.UserApplicationRoleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserApplicationRoleEntryRepository {
    private static final Logger log = LoggerFactory.getLogger(UserApplicationRoleEntryRepository.class);

    private final UserApplicationRoleEntryDao userApplicationRoleEntryDao;

    @Autowired
    public UserApplicationRoleEntryRepository(UserApplicationRoleEntryDao userApplicationRoleEntryDao) {
        this.userApplicationRoleEntryDao = userApplicationRoleEntryDao;
    }

    public void addUserApplicationRoleEntry(UserApplicationRoleEntry userApplicationRoleEntry) throws RuntimeException {
        UserApplicationRoleEntry exists;
        if (userApplicationRoleEntry.getId() != null && !userApplicationRoleEntry.getId().trim().isEmpty()) {
            exists = getUserApplicationRoleEntry(userApplicationRoleEntry.getId());
            if (exists != null) {
                userApplicationRoleEntryDao.updateUserRoleValue(userApplicationRoleEntry);
            } else {
                userApplicationRoleEntryDao.addUserApplicationRoleEntry(userApplicationRoleEntry);
            }
        } else {
            exists = getUserApplicationRoleEntryByValues(userApplicationRoleEntry.getUserId(),
                    userApplicationRoleEntry.getApplicationId(), userApplicationRoleEntry.getOrgName(),
                    userApplicationRoleEntry.getRoleName(), userApplicationRoleEntry.getRoleValue()
            );
            if (exists == null) {
                userApplicationRoleEntryDao.addUserApplicationRoleEntry(userApplicationRoleEntry);
            } else {
                log.trace("Role-mapping already exists, will not create a new mapping for: ");
            }
        }
    }

    public UserApplicationRoleEntry getUserApplicationRoleEntry(String roleId) {
        return userApplicationRoleEntryDao.getUserApplicationRoleEntry(roleId);
    }

    public UserApplicationRoleEntry getUserApplicationRoleEntryByValues(String UserID, String AppID, String OrganizationName, String RoleName, String RoleValues) {
        return userApplicationRoleEntryDao.getUserApplicationRoleEntryByValues(UserID, AppID, OrganizationName, RoleName, RoleValues);
    }

    public void deleteUserApplicationRoleWithRoleId(String roleId) {
        userApplicationRoleEntryDao.deleteRoleByRoleID(roleId);
    }

    public void deleteUserAppRoles(String uid, String appId) {
        userApplicationRoleEntryDao.deleteUserAppRoles(uid, appId);
    }

}
