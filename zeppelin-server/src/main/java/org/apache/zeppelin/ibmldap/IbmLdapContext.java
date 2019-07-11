/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zeppelin.ibmldap;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import java.sql.SQLException;
import java.util.Properties;

public class IbmLdapContext
{
    private static final String ldapServerName = "ldaps://bluepages.ibm.com:636";
    private static final String defaultSearchBase = "ou=bluepages,o=ibm.com";
    private static final String userObjectClass = "ibmPerson";
    private static IbmLdapContext ibmLdapContext;
    private static LdapContext ldapContext;

    private IbmLdapContext() throws SQLException
    {
        try {
            ldapContext = initLdapContext();
        }
        catch (NamingException e) {
            throw new SQLException("Invalid server name: " + ldapServerName, e);
        }
    }

    public static IbmLdapContext getInstance() throws SQLException
    {
        if (ibmLdapContext == null) {
            ibmLdapContext = new IbmLdapContext();
        }
        return ibmLdapContext;
    }

    private String userSearchFileter(String eMailAddress)
    {
        return "(&(objectclass=" + userObjectClass + ")(emailAddress=" + eMailAddress + "))";
    }

    public String getSerialId(String eMailAddress) throws SQLException
    {
        String serialNumber = null;
        try {
            NamingEnumeration<SearchResult> namingEnum = ldapContext.search(defaultSearchBase, userSearchFileter(eMailAddress), getSimpleSearchControls());
            if (namingEnum.hasMore()) {
                serialNumber = namingEnum
                        .next()
                        .getAttributes()
                        .get("serialNumber")
                        .get()
                        .toString();
            }
            namingEnum.close();
        }
        catch (NamingException e) {
            throw new SQLException("Exception when performing the search: " + e);
        }

        if (serialNumber == null) {
            throw new SQLException("No serial number has been found for: " + eMailAddress);
        }

        return serialNumber;
    }

    public static SearchControls getSimpleSearchControls()
    {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setTimeLimit(30000);
        return searchControls;
    }

    private LdapContext initLdapContext() throws NamingException
    {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapServerName);
        env.put(Context.SECURITY_AUTHENTICATION, "none");
        return new InitialLdapContext(env, null);
    }
}
