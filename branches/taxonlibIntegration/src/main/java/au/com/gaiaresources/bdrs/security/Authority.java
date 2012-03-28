/**
 * 
 */
package au.com.gaiaresources.bdrs.security;

import org.springframework.security.core.GrantedAuthority;

public class Authority implements GrantedAuthority {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String roleName;
    
    Authority(String roleName) {
        this.roleName = roleName;
    }
    
    @Override
    public String getAuthority() {
        return roleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Authority authority = (Authority) o;

        if (!roleName.equals(authority.roleName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return roleName.hashCode();
    }

    public int compareTo(Object o) {
        if (o instanceof GrantedAuthority) {
            GrantedAuthority ga = (GrantedAuthority) o;
            return getAuthority().compareTo(ga.getAuthority());
        }
        return -1;
    }
}
