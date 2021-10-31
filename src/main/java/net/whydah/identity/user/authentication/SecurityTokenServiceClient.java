package net.whydah.identity.user.authentication;

import net.whydah.identity.application.ApplicationService;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.userauth.CommandGetUserTokenByUserTokenId;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SecurityTokenServiceClient {
	private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceClient.class);

	private  String MY_APPLICATION_ID = "2210";
	private  String securitytokenserviceurl;
	private static WhydahApplicationSession was = null;
	private static SecurityTokenServiceClient securityTokenServiceClient;
	private ApplicationCredential myApplicationCredential;
	private ApplicationService applicationService;

	@Autowired
	@Configure
	public SecurityTokenServiceClient(@Configuration("securitytokenservice") String securitytokenserviceurl, @Configuration("my_applicationid") String MY_APPLICATION_ID, ApplicationService applicationService) {
		this.MY_APPLICATION_ID = MY_APPLICATION_ID;
		this.securitytokenserviceurl = securitytokenserviceurl;
		securityTokenServiceClient = this;
		this.applicationService = applicationService;

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(new Runnable() {
			public void run() {
				getWAS();
				log.debug("Asynchronous startWhydahClient task");
			}
		});
	}


	public static SecurityTokenServiceClient getSecurityTokenServiceClient() {
		return securityTokenServiceClient;
	}

	public String getActiveUibApplicationTokenId(){
		if (was == null) {
			getWAS();
		}
		if (was != null) {
			return was.getActiveApplicationTokenId();
		}
		return null;
	}

	public UserToken getUserToken(String usertokenid){
		if (was == null) {
			getWAS();
		}
		if (was != null) {
			String userTokenXML = new CommandGetUserTokenByUserTokenId(URI.create(was.getSTS()), was.getActiveApplicationTokenId(), was.getActiveApplicationTokenXML(), usertokenid).execute();
			if (userTokenXML != null && userTokenXML.length() > 10) {
				return UserTokenMapper.fromUserTokenXml(userTokenXML);

			}
		}
		return null;
	}

	public WhydahApplicationSession getWAS() {
		if (was == null) {
			try {
				myApplicationCredential = getAppCredentialForApplicationId(MY_APPLICATION_ID);
				if(myApplicationCredential!=null) {
					was = WhydahApplicationSession.getInstance(securitytokenserviceurl,
							null,  // No UAS
							myApplicationCredential);
				} else {
					throw new Exception("No app found for " + MY_APPLICATION_ID);
				}
			} catch (Exception e) {
				log.warn("Trouble to create WhydahApplicationSession, was:", was, e);
			}

		}
		return was;
	}


	private ApplicationCredential getAppCredentialForApplicationId(String appNo) {
		Application app = applicationService.getApplication(appNo);
		if(app!=null) {
			String appid = app.getId();
			String appname=app.getName();
			String secret=app.getSecurity().getSecret();
			return new ApplicationCredential(appid,appname,secret);
		} else {
			return null;
		}
	}
}
