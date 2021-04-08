package net.whydah.identity.user.identity;

import java.util.function.Function;

public class Converter<T, U> {
    private final Function<T, U> fromLDAPUserIdentity;
    private final Function<U, T> fromRDBMSUserIdentity;

    public Converter(final Function<T, U> fromLdap, final Function<U, T> fromRDBMS) {
        this.fromLDAPUserIdentity = fromLdap;
        this.fromRDBMSUserIdentity = fromRDBMS;
    }

    public final U convertFromLDAPUserIdentity(final T ldap) {
        return fromLDAPUserIdentity.apply(ldap);
    }

    public final T convertFromRDBMSUserIdentity(final U userIdentity) {
        return fromRDBMSUserIdentity.apply(userIdentity);
    }

}