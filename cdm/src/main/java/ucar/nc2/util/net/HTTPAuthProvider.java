/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;

import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;

import java.io.Serializable;
import java.util.List;

/**
 * HTTPAuthProvider contains the necessary information to support a given
 * authorization scheme in the context of HTTPSession.
 * <p/>
 * It is intended to be thread safe using, currently,
 * serial (synchronized) access.
 * <p/>
 * The primary component of HTTPAuthCreds is a (key,value) pair
 * store implementing the HttpParams Interface.  The contents of the pair
 * store depends on the particular auth scheme (HTTP Basic, ESG Keystore,
 * etc.)
 * <p/>
 * HTTPAuthProvider implements the CredentialsProvider interface.
 * <p/>
 * With httpclient 4.2, credentials caching is pushed to the
 * credentials provider. So caching is provided here.
 * However, we need a finer grain of caching than provided
 * by AuthScope, so we use the full url.
 */

public class HTTPAuthProvider implements CredentialsProvider
{
    static final int MAX_RETRIES = 3;

    //////////////////////////////////////////////////
    // Predefined keys (Used local to the package)

    static final String PRINCIPAL = "ucar.nc2.principal";
    static final String URI = "ucar.nc2.url";
    static final String CREDENTIALSPROVIDER = "ucar.nc2.credentialsprovider";
    static final String KEYSTOREPATH = "ucar.nc2.keystore";
    static final String KEYSTOREPASSWORD = "ucar.nc2.keystorepassword";
    static final String TRUSTSTOREPATH = "ucar.nc2.truststore";
    static final String TRUSTSTOREPASSWORD = "ucar.nc2.truststorepassword";
    static final String CREDENTIALS = "ucar.nc2.credentials";
    static final String AUTHSTRING = "ucar.nc2.authstring";
    static String SCHEME = "ucar.nc2.scheme";
    static String PASSWORD = "ucar.nc2.password";
    static String USER = "ucar.nc2.user";
    static public final String WWW_AUTH_RESP = "Authorization";   // from HttpMethodDirector
    static public final String PROXY_AUTH_RESP = "Proxy-Authorization"; // from HttpMethodDirector


    //////////////////////////////////////////////////

    static boolean TESTING = true;

    //////////////////////////////////////////////////
    // Instance variables

    protected HTTPAuthStore store = null;
    protected int retryCount = 0;
    protected HTTPAuthScope authscope = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public HTTPAuthProvider(HTTPAuthStore store, HTTPAuthScope authscope)
    {
        this.store = store;
        this.authscope = authscope;
        this.retryCount = MAX_RETRIES;
    }

    //////////////////////////////////////////////////
    // Credentials Provider Interface

    public Credentials
    getCredentials(AuthScope scope)
    {
        // Is this still true in httpclient 4.2.x?
        // There appears to be a bug in HttpMethodDirector such that
        // as long as bad credentials are provided, it will keep on
        // calling the credentials provider.  We fix by checking for
        // retry in same way as HttpMethodDirector.processWWWAuthChallenge.
        // After MAX_RETRIES, we force retries to stop.
        //todo: AuthState authstate = method.getMethod().getHostAuthState();
        //if(retryCount == 0 && authstate.isAuthAttempted() && authscheme.isComplete()) {
        //    return null; // Stop the retry.
        //}
        //retryCount--;

        // Verify that the scope argument "subsumes"
        // this.authscope
        if(!HTTPAuthScope.subsumes(scope, this.authscope))
            throw new IllegalStateException("HTTPAuthProvider: scope :: authscope mismatch");

        // See if the credentials have been cached.
        Credentials credentials = this.store.getCredentials(scope);
        if(credentials != null) {
            if(TESTING)
                System.err.println("Using cached credentials: "+credentials);
            return credentials;
        }

        // Not cached.

        String scheme = scope.getScheme();

        if(scheme == null)
            throw new IllegalStateException("HTTPAuthProvider: unsupported scheme: " + scope.getScheme());

        // search for matching authstore entries
        List<HTTPAuthStore.Entry> matches = this.store.search(this.authscope);
        if(matches.size() == 0)
            throw new IllegalStateException("HTTPAuthProvider: no match for:" + this.authscope);

        // Choose the most restrictive
        HTTPAuthStore.Entry entry = matches.get(0);
        CredentialsProvider provider = entry.provider;

        if(provider == null)
            throw new IllegalStateException("HTTPAuthProvider: no credentials provider provided");

        // If this is an instance of
        // invoke the (real) credentials provider
        // Use the incoming parameters
        credentials = provider.getCredentials(scope);
        if(credentials == null)
            throw new IllegalStateException("HTTPAuthProvider: cannot obtain credentials");

        // Insert into the credentials cache
        this.store.cacheCredentials(this.authscope, credentials);

        if(TESTING)
            System.err.println("Caching credentials: "+credentials);

        return credentials;
    }

    public void setCredentials(AuthScope scope, Credentials creds)
    {
    }

    public void clear()
    {
        this.store.clear();
    }

    ///////////////////////////////////////////////////
    // toString

    public String
    toString()
    {
        return "HTTPAuthProvider(" + this.authscope + ")";
    }

}//HTTPAuthProvider

