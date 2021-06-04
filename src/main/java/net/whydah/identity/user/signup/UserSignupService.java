package net.whydah.identity.user.signup;

import net.whydah.identity.user.UserAggregateService;
import net.whydah.identity.user.identity.BCryptService;
import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.identity.user.identity.UserIdentityConverter;
import net.whydah.identity.user.identity.UserIdentityService;
import net.whydah.identity.user.identity.UserIdentityServiceV2;
import net.whydah.identity.user.identity.UserIdentityWithAutomaticPasswordGeneration;
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
    private final BCryptService bCryptService;


    @Autowired
    public UserSignupService(UserAggregateService userAggregateService, UserIdentityService userIdentityService,
                             UserIdentityServiceV2 userIdentityServiceV2, BCryptService bCryptService) {
        this.userAggregateService = userAggregateService;
        this.userIdentityService = userIdentityService;
        this.userIdentityServiceV2 = userIdentityServiceV2;
        this.bCryptService = bCryptService;
    }


    public UserAggregate createUserWithRoles(UserAggregate userAggregate) {
        UserAggregate returnUserAggregate = null;
        if (userAggregate != null) {
            UserIdentity createFromIdentity = UserIdentityMapper.fromJson(UserAggregateMapper.toJson(userAggregate));

            RDBMSUserIdentity rdbmsUserIdentity = null;
            UserIdentityWithAutomaticPasswordGeneration userIdentityExtension = new UserIdentityWithAutomaticPasswordGeneration(createFromIdentity);
            try {
                rdbmsUserIdentity = userIdentityServiceV2.addUserIdentityWithGeneratedPassword(userIdentityExtension);
                try {
                    LDAPUserIdentity ldapUserIdentity;
                    ldapUserIdentity = userIdentityService.addUserIdentityWithGeneratedPassword(userIdentityExtension);
                    if (rdbmsUserIdentity == null) {
                        UserIdentityConverter userIdentityConverter = new UserIdentityConverter(bCryptService);
                        rdbmsUserIdentity = userIdentityConverter.convertFromLDAPUserIdentity(ldapUserIdentity);
                        String json = UserAggregateMapper.toJson(userAggregate);
                        log.warn(String.format("Created LDAP user, but not RDBMS user! \njson=%s", json));
                    }
                } catch (Exception e) {
                    String json = UserAggregateMapper.toJson(userAggregate);
                    log.error(String.format("createUserWithRoles LDAP failed! \njson=%s", json), e);
                }
            } catch (Exception ex) {
                log.error("createUserWithRoles DB failed!", ex);
            }


            //Add roles
            if (rdbmsUserIdentity != null && rdbmsUserIdentity.getUid() != null) {
                List<UserApplicationRoleEntry> roles = userAggregate.getRoleList();
                String uid = rdbmsUserIdentity.getUid();
                List<UserApplicationRoleEntry> createdRoles = userAggregateService.addUserApplicationRoleEntries(uid, roles);
                returnUserAggregate = UserAggregateMapper.fromJson(UserIdentityMapper.toJson(rdbmsUserIdentity));
                returnUserAggregate.setRoleList(createdRoles);
            }
        }
        return userAggregate;
    }


}
