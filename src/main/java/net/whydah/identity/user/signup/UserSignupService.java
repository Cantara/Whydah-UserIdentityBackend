package net.whydah.identity.user.signup;

import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.*;
import net.whydah.identity.util.PasswordGenerator;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.mappers.UserIdentityMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserIdentity;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by baardl on 30.09.15.
 */
@Service
public class UserSignupService {
    private static final Logger log = getLogger(UserSignupService.class);

    private final UserAggregateService userAggregateService;
    private final UserIdentityService userIdentityService;
    private final UserIdentityServiceV2 userIdentityServiceV2;


    @Autowired
    public UserSignupService(UserAggregateService userAggregateService, UserIdentityService userIdentityService, UserIdentityServiceV2 userIdentityServiceV2) {
        this.userAggregateService = userAggregateService;
        this.userIdentityService = userIdentityService;
        this.userIdentityServiceV2 = userIdentityServiceV2;
    }


    public UserAggregate createUserWithRoles(UserAggregate userAggregate) {
        UserAggregate returnUserAggregate = null;
        if (userAggregate != null) {
            UserIdentity createFromIdentity = UserIdentityMapper.fromJson(UserAggregateMapper.toJson(userAggregate));

            UserIdentity userIdentity = null;
            UserIdentityExtension userIdentityExtension = new UserIdentityExtension(createFromIdentity);
            try {
                userIdentity = userIdentityService.addUserIdentityWithGeneratedPassword(userIdentityExtension);
                try {
                    RDBMSUserIdentity rdbmsUserIdentity = userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
                    if (userIdentity == null) {
                        UserIdentityConverter userIdentityConverter = new UserIdentityConverter();
                        userIdentity = userIdentityConverter.convertFromRDBMSUserIdentity(rdbmsUserIdentity);
                    }
                } catch (Exception e) {
                    String json = UserAggregateMapper.toJson(userAggregate);
                    log.error(String.format("createUserWithRoles DB failed! \njson=%s", json), e);
                }

            } catch (Exception e) {
                String json = UserAggregateMapper.toJson(userAggregate);
                log.error(String.format("createUserWithRoles LDAP failed! \njson=%s", json), e);
                try {
                    RDBMSUserIdentity rdbmsUserIdentity = userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
                    if (userIdentity == null) {
                        UserIdentityConverter userIdentityConverter = new UserIdentityConverter();
                        userIdentity = userIdentityConverter.convertFromRDBMSUserIdentity(rdbmsUserIdentity);
                    }
                } catch (Exception ex) {
                    log.error("createUserWithRoles DB failed!", ex);
                }
            }





            //Add roles
            if (userIdentity != null && userIdentity.getUid() != null) {
                List<UserApplicationRoleEntry> roles = userAggregate.getRoleList();
                String uid = userIdentity.getUid();
                List<UserApplicationRoleEntry> createdRoles = userAggregateService.addUserApplicationRoleEntries(uid, roles);
                returnUserAggregate = UserAggregateMapper.fromJson(UserIdentityMapper.toJson(userIdentity));
                returnUserAggregate.setRoleList(createdRoles);
            }
        }
        return userAggregate;
    }


}
