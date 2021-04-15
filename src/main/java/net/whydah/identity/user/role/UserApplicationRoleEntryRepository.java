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
        UserApplicationRoleEntry exists = getUserApplicationRoleEntry(userApplicationRoleEntry.getId());
        if (exists != null) {
            userApplicationRoleEntryDao.updateUserRoleValue(exists);
        } else {
            userApplicationRoleEntryDao.addUserApplicationRoleEntry(userApplicationRoleEntry);
        }
    }

    public UserApplicationRoleEntry getUserApplicationRoleEntry(String roleId) {
        return userApplicationRoleEntryDao.getUserApplicationRoleEntry(roleId);
    }

    public void deleteUserApplicationRoleWithRoleId(String roleId) {
        userApplicationRoleEntryDao.deleteRoleByRoleID(roleId);
    }

    public void deleteUserAppRoles(String uid, String appId) {
        userApplicationRoleEntryDao.deleteUserAppRoles(uid, appId);
    }

}
