/*
 * Swift Parallel Scripting Language (http://swift-lang.org)
 * Code from Java CoG Kit Project (see notice below) with modifications.
 *
 * Copyright 2005-2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Nov 20, 2007
 */
package org.globus.cog.abstraction.impl.ssh;

public class ConnectionID {
    public String host;
    public int port;
    public Object credentials;

    public ConnectionID(String host, int port, Object credentials) {
        this.host = host;
        this.port = port;
        this.credentials = credentials;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ConnectionID) {
            ConnectionID other = (ConnectionID) obj;
            return eq(host, other.host) && port == other.port
                    && eq(credentials, other.credentials);
        }
        else {
            return false;
        }
    }

    private boolean eq(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        else {
            return o1.equals(o2);
        }
    }

    public int hashCode() {
        return (host == null ? 0 : host.hashCode()) + port
                + (credentials == null ? 0 : credentials.hashCode());
    }
    
    public String toString() {
        return credentials + "@" + host + ":" + port;
    }
}