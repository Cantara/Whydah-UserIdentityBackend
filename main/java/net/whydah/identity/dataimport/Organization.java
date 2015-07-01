package net.whydah.identity.dataimport;

public class Organization {

	private final String appId;
	private final String name;

	public Organization(String organizationId, String organizationName) {
		this.appId = organizationId;
		this.name = organizationName;
	}

	public String getAppId() {
		return appId;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Organization [appId=" + appId + ", name=" + name + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appId == null) ? 0 : appId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Organization other = (Organization) obj;
		if (appId == null) {
			if (other.appId != null)
				return false;
		} else if (!appId.equals(other.appId))
			return false;
		return true;
	}
}
