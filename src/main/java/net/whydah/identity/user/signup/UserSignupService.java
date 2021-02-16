package net.whydah.identity.user.signup;

import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.UserIdentityService;
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


    @Autowired
    public UserSignupService(UserAggregateService userAggregateService, UserIdentityService userIdentityService) {
        this.userAggregateService = userAggregateService;
        this.userIdentityService = userIdentityService;
    }


    public UserAggregate createUserWithRoles(UserAggregate userAggregate) {
        UserAggregate returnUserAggregate = null;
        if (userAggregate != null) {
            UserIdentity createFromItentity = UserIdentityMapper.fromJson(UserAggregateMapper.toJson(userAggregate));
            UserIdentity userIdentity = userIdentityService.addUserIdentityWithGeneratedPassword(createFromItentity);
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
